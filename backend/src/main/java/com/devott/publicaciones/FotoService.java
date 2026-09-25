package com.devott.publicaciones;

import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.compartido.almacenamiento.SubidaFirmada;
import com.devott.compartido.errores.ConflictoException;
import com.devott.compartido.errores.DatosInvalidosException;
import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.suscripciones.LimitesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Fotos de las publicaciones. El navegador sube el archivo directo al almacenamiento con una URL
 * firmada; acá se valida quién puede subir, el límite del plan, y se registra la foto al confirmar.
 */
@Service
@Transactional(readOnly = true)
public class FotoService {

    private static final Pattern NOMBRE_DE_ARCHIVO = Pattern.compile("^[0-9a-f-]{36}\\.(webp|jpg)$");

    private final PublicacionService publicaciones;
    private final FotoRepository fotos;
    private final LimitesService limites;
    private final AlmacenDeArchivos almacen;

    FotoService(PublicacionService publicaciones, FotoRepository fotos, LimitesService limites,
                AlmacenDeArchivos almacen) {
        this.publicaciones = publicaciones;
        this.fotos = fotos;
        this.limites = limites;
        this.almacen = almacen;
    }

    public int maxFotos(UUID vendedorId) {
        return limites.de(vendedorId).maxFotos();
    }

    /** Paso 2 del flujo: URL para subir una foto nueva a la publicación. */
    public SubidaFirmada pedirSubida(UUID usuarioId, UUID publicacionId, String contentType) {
        Publicacion publicacion = publicaciones.propia(usuarioId, publicacionId);
        if (!AlmacenDeArchivos.TIPOS_PERMITIDOS.contains(contentType)) {
            throw new DatosInvalidosException("contentType", "Subí la foto en formato WebP o JPEG.");
        }
        validarLugarParaOtraFoto(publicacion);
        String ruta = carpeta(publicacionId) + UUID.randomUUID() + "." + AlmacenDeArchivos.extension(contentType);
        return almacen.firmarSubida(ruta, contentType);
    }

    /** Paso 4 del flujo: registra la foto ya subida al final de la publicación. */
    @Transactional
    public PublicacionVista confirmar(UUID usuarioId, UUID publicacionId, String ruta, Integer ancho, Integer alto) {
        Publicacion publicacion = publicaciones.propia(usuarioId, publicacionId);
        String carpeta = carpeta(publicacionId);
        if (ruta == null || !ruta.startsWith(carpeta)
                || !NOMBRE_DE_ARCHIVO.matcher(ruta.substring(carpeta.length())).matches()) {
            throw new DatosInvalidosException("ruta", "La foto no corresponde a esta publicación.");
        }
        List<Foto> actuales = fotos.findByPublicacionIdOrderByOrdenAsc(publicacionId);
        if (actuales.stream().anyMatch(f -> f.getStoragePath().equals(ruta))) {
            return publicaciones.vista(publicacion);
        }
        validarLugarParaOtraFoto(publicacion);
        if (!almacen.existe(ruta)) {
            throw new DatosInvalidosException("ruta", "No encontramos la foto subida. Probá subirla de nuevo.");
        }
        fotos.save(new Foto(publicacionId, ruta, actuales.size(), ancho, alto));
        return publicaciones.vista(publicacion);
    }

    @Transactional
    public PublicacionVista eliminar(UUID usuarioId, UUID publicacionId, UUID fotoId) {
        Publicacion publicacion = publicaciones.propia(usuarioId, publicacionId);
        List<Foto> actuales = fotos.findByPublicacionIdOrderByOrdenAsc(publicacionId);
        Foto foto = actuales.stream().filter(f -> f.getId().equals(fotoId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esa foto."));
        if (actuales.size() == 1 && publicacion.getEstado() == EstadoPublicacion.ACTIVA) {
            throw new ConflictoException("Una publicación activa necesita al menos una foto. Pausala o subí otra antes.");
        }
        fotos.delete(foto);
        List<Foto> restantes = actuales.stream().filter(f -> f != foto).toList();
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).ordenar(i);
        }
        String ruta = foto.getStoragePath();
        PublicacionService.despuesDeConfirmar(() -> almacen.eliminar(List.of(ruta)));
        return publicaciones.vista(publicacion);
    }

    /** Reordena las fotos. `fotoIds` tiene que tener exactamente las fotos de la publicación. */
    @Transactional
    public PublicacionVista reordenar(UUID usuarioId, UUID publicacionId, List<UUID> fotoIds) {
        Publicacion publicacion = publicaciones.propia(usuarioId, publicacionId);
        List<Foto> actuales = fotos.findByPublicacionIdOrderByOrdenAsc(publicacionId);
        Set<UUID> esperadas = new HashSet<>(actuales.stream().map(Foto::getId).toList());
        if (fotoIds.size() != esperadas.size() || !esperadas.equals(new HashSet<>(fotoIds))) {
            throw new DatosInvalidosException("fotoIds", "El orden tiene que incluir todas las fotos, una vez cada una.");
        }
        for (Foto foto : actuales) {
            foto.ordenar(fotoIds.indexOf(foto.getId()));
        }
        return publicaciones.vista(publicacion);
    }

    private void validarLugarParaOtraFoto(Publicacion publicacion) {
        int maximo = maxFotos(publicacion.getVendedorId());
        if (fotos.countByPublicacionId(publicacion.getId()) >= maximo) {
            throw new ConflictoException("Llegaste al máximo de " + maximo + " fotos por publicación de tu plan.");
        }
    }

    static String carpeta(UUID publicacionId) {
        return "publicaciones/" + publicacionId + "/";
    }
}
