package com.devott.publicaciones;

import com.devott.catalogo.CatalogoService;
import com.devott.catalogo.ModeloConMarca;
import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.compartido.errores.ConflictoException;
import com.devott.compartido.errores.DatosInvalidosException;
import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.compartido.texto.Slugs;
import com.devott.compartido.web.Pagina;
import com.devott.suscripciones.Limites;
import com.devott.suscripciones.LimitesService;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PublicacionService {

    /** Un 0 km puede tener algunos kilómetros de traslado. */
    static final int MAX_KM_CERO_KM = 1000;

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);
    private static final SecureRandom AZAR = new SecureRandom();
    private static final String ALFABETO_SLUG = "abcdefghijkmnpqrstuvwxyz23456789";

    private final PublicacionRepository publicaciones;
    private final FotoRepository fotos;
    private final VendedorService vendedores;
    private final CatalogoService catalogo;
    private final LimitesService limites;
    private final AlmacenDeArchivos almacen;
    private final Clock reloj;

    PublicacionService(PublicacionRepository publicaciones, FotoRepository fotos, VendedorService vendedores,
                       CatalogoService catalogo, LimitesService limites, AlmacenDeArchivos almacen, Clock reloj) {
        this.publicaciones = publicaciones;
        this.fotos = fotos;
        this.vendedores = vendedores;
        this.catalogo = catalogo;
        this.limites = limites;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    // Mis publicaciones ------------------------------------------------------

    public List<PublicacionVista> misPublicaciones(UUID usuarioId) {
        return vendedores.deUsuario(usuarioId)
                .map(v -> vistas(publicaciones.findByVendedorIdOrderByActualizadaEnDesc(v.getId())))
                .orElse(List.of());
    }

    public PublicacionVista miPublicacion(UUID usuarioId, UUID id) {
        return vista(propia(usuarioId, id));
    }

    @Transactional
    public PublicacionVista crear(UUID usuarioId, PublicacionRequest request) {
        Vendedor vendedor = vendedores.deUsuario(usuarioId)
                .orElseThrow(() -> new ConflictoException("Primero creá tu perfil de vendedor."));
        ModeloConMarca modelo = modeloValido(request.modeloId());
        Publicacion publicacion = new Publicacion(vendedor.getId(), slugNuevo(modelo, request.anio()));
        publicacion.actualizar(datosDe(request, vendedor));
        return vista(publicaciones.save(publicacion));
    }

    @Transactional
    public PublicacionVista actualizar(UUID usuarioId, UUID id, PublicacionRequest request) {
        Publicacion publicacion = propia(usuarioId, id);
        Vendedor vendedor = vendedores.porId(publicacion.getVendedorId()).orElseThrow();
        modeloValido(request.modeloId());
        publicacion.actualizar(datosDe(request, vendedor));
        return vista(publicacion);
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID id) {
        Publicacion publicacion = propia(usuarioId, id);
        List<String> rutas = fotos.findByPublicacionIdOrderByOrdenAsc(id).stream().map(Foto::getStoragePath).toList();
        // Las filas de foto se borran en cascada; los archivos, después de confirmar la transacción.
        publicaciones.delete(publicacion);
        despuesDeConfirmar(() -> almacen.eliminar(rutas));
    }

    @Transactional
    public PublicacionVista cambiarEstado(UUID usuarioId, UUID id, EstadoPublicacion nuevo) {
        Publicacion publicacion = propia(usuarioId, id);
        if (nuevo == EstadoPublicacion.ACTIVA && publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            validarQuePuedePublicar(publicacion);
        }
        publicacion.cambiarEstado(nuevo, Instant.now(reloj));
        return vista(publicacion);
    }

    private void validarQuePuedePublicar(Publicacion publicacion) {
        if (fotos.countByPublicacionId(publicacion.getId()) == 0) {
            throw new ConflictoException("Agregá al menos una foto antes de publicar.");
        }
        Limites limite = limites.de(publicacion.getVendedorId());
        long activas = publicaciones.countByVendedorIdAndEstado(publicacion.getVendedorId(), EstadoPublicacion.ACTIVA);
        if (activas >= limite.maxPublicaciones()) {
            throw new ConflictoException("Llegaste al límite de " + limite.maxPublicaciones()
                    + " publicaciones activas de tu plan. Pausá o marcá como vendida alguna para publicar esta.");
        }
    }

    /** Publicación del vendedor del usuario. Si es de otro, responde como si no existiera. */
    Publicacion propia(UUID usuarioId, UUID id) {
        return vendedores.deUsuario(usuarioId)
                .flatMap(v -> publicaciones.findByIdAndVendedorId(id, v.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esa publicación."));
    }

    // Públicas ---------------------------------------------------------------

    /** Publicación visible por su link: activa o vendida. */
    public PublicacionVista publica(String slug) {
        return publicaciones.findBySlug(slug)
                .filter(p -> p.getEstado().esVisibleEnPublico())
                .map(this::vista)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esa publicación o ya no está disponible."));
    }

    /** Publicaciones activas de un vendedor, las más nuevas primero. */
    public Pagina<PublicacionVista> activasDeVendedor(String vendedorSlug, Collection<Condicion> condiciones,
                                                      int pagina, int tamano) {
        Vendedor vendedor = vendedores.porSlug(vendedorSlug);
        Page<Publicacion> page = publicaciones.findByVendedorIdAndEstadoAndCondicionIn(vendedor.getId(),
                EstadoPublicacion.ACTIVA, condiciones,
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "publicadaEn")));
        return Pagina.de(page, vistas(page.getContent()));
    }

    // Auxiliares -------------------------------------------------------------

    PublicacionVista vista(Publicacion publicacion) {
        return vistas(List.of(publicacion)).getFirst();
    }

    private List<PublicacionVista> vistas(List<Publicacion> lista) {
        Set<Integer> modeloIds = lista.stream().map(Publicacion::getModeloId).collect(Collectors.toSet());
        Map<Integer, ModeloConMarca> modelos = catalogo.modelosConMarca(modeloIds);
        Map<UUID, List<Foto>> fotosPorPublicacion = fotos.findByPublicacionIdInOrderByOrdenAsc(
                        lista.stream().map(Publicacion::getId).toList())
                .stream().collect(Collectors.groupingBy(Foto::getPublicacionId));
        return lista.stream()
                .map(p -> new PublicacionVista(p, modelos.get(p.getModeloId()),
                        fotosPorPublicacion.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private ModeloConMarca modeloValido(Integer modeloId) {
        return catalogo.modeloConMarca(modeloId)
                .orElseThrow(() -> new DatosInvalidosException("modeloId", "Elegí un modelo del catálogo."));
    }

    private DatosPublicacion datosDe(PublicacionRequest r, Vendedor vendedor) {
        int anioMaximo = Year.now(reloj).getValue() + 1;
        if (r.anio() > anioMaximo) {
            throw new DatosInvalidosException("anio", "El año no puede ser posterior a " + anioMaximo + ".");
        }
        if (r.condicion() == Condicion.CERO_KM && r.km() > MAX_KM_CERO_KM) {
            throw new DatosInvalidosException("km", "Un 0 km no puede tener más de " + MAX_KM_CERO_KM + " km.");
        }

        String ciudad = vendedor.getCiudad();
        String provincia = vendedor.getProvincia();
        Point ubicacion = vendedor.getUbicacion();
        if (r.localidad() != null) {
            ciudad = r.localidad().ciudad().trim();
            provincia = r.localidad().provincia().trim();
            ubicacion = WGS84.createPoint(new Coordinate(r.localidad().lng(), r.localidad().lat()));
        }

        return new DatosPublicacion(
                r.modeloId(),
                textoOpcional(r.version()),
                r.anio(),
                r.km(),
                r.condicion(),
                r.precio(),
                r.moneda(),
                r.carroceria(),
                r.combustible(),
                r.transmision(),
                r.traccion(),
                textoOpcional(r.color()),
                r.puertas(),
                Boolean.TRUE.equals(r.financia()),
                Boolean.TRUE.equals(r.aceptaPermuta()),
                Boolean.TRUE.equals(r.unicoDueno()),
                textoOpcional(r.descripcion()),
                ciudad,
                provincia,
                ubicacion);
    }

    /** "toyota-hilux-2021-k3f9x2": legible y con un sufijo al azar para que no se repita. */
    private String slugNuevo(ModeloConMarca modelo, int anio) {
        String base = Slugs.generar(modelo.titulo() + " " + anio);
        for (int intento = 0; intento < 5; intento++) {
            String slug = base + "-" + sufijoAlAzar();
            if (!publicaciones.existsBySlug(slug)) {
                return slug;
            }
        }
        throw new IllegalStateException("No se pudo generar un slug único para " + base);
    }

    private static String sufijoAlAzar() {
        StringBuilder sufijo = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sufijo.append(ALFABETO_SLUG.charAt(AZAR.nextInt(ALFABETO_SLUG.length())));
        }
        return sufijo.toString();
    }

    /** Corre la acción cuando la transacción actual se confirma (o ya, si no hay transacción). */
    static void despuesDeConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
