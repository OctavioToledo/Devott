package com.devott.publicaciones;

import com.devott.catalogo.CatalogoService;
import com.devott.catalogo.ModeloConMarca;
import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.compartido.errores.ConflictoException;
import com.devott.compartido.errores.DatosInvalidosException;
import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.compartido.errores.ServicioNoDisponibleException;
import com.devott.compartido.texto.Slugs;
import com.devott.compartido.web.Pagina;
import com.devott.cotizaciones.CotizacionService;
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

import java.math.BigDecimal;
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
    private final BuscadorPublicaciones buscador;
    private final VendedorService vendedores;
    private final CatalogoService catalogo;
    private final LimitesService limites;
    private final AlmacenDeArchivos almacen;
    private final CotizacionService cotizaciones;
    private final Clock reloj;

    PublicacionService(PublicacionRepository publicaciones, FotoRepository fotos, BuscadorPublicaciones buscador,
                       VendedorService vendedores,
                       CatalogoService catalogo, LimitesService limites, AlmacenDeArchivos almacen,
                       CotizacionService cotizaciones, Clock reloj) {
        this.publicaciones = publicaciones;
        this.fotos = fotos;
        this.buscador = buscador;
        this.vendedores = vendedores;
        this.catalogo = catalogo;
        this.limites = limites;
        this.almacen = almacen;
        this.cotizaciones = cotizaciones;
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
        if (limite.maxPublicaciones() == 0) {
            throw new ConflictoException("Para publicar necesitás un plan activo. Elegí uno en Planes.");
        }
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

    /** Recalcula el precio en dólares de las publicaciones en pesos. Devuelve cuántas cambiaron. */
    @Transactional
    public int recalcularPreciosUsd(BigDecimal dolar) {
        return publicaciones.recalcularPreciosUsd(dolar);
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

    /** Id de una publicación visible en público (activa o vendida). Si no, 404. */
    public UUID idVisible(String slug) {
        return publicaciones.findBySlug(slug)
                .filter(p -> p.getEstado().esVisibleEnPublico())
                .map(Publicacion::getId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esa publicación o ya no está disponible."));
    }

    /** Tarjetas de las publicaciones visibles entre `ids`, en el mismo orden. Las que no están visibles se omiten. */
    public List<PublicacionResponses.Tarjeta> tarjetasVisibles(List<UUID> ids) {
        Map<UUID, PublicacionVista> porId = vistas(publicaciones.findAllById(ids).stream()
                        .filter(p -> p.getEstado().esVisibleEnPublico()).toList())
                .stream().collect(Collectors.toMap(v -> v.publicacion().getId(), v -> v));
        return ids.stream().filter(porId::containsKey)
                .map(id -> PublicacionResponses.Tarjeta.de(porId.get(id), almacen))
                .toList();
    }

    /** Lo necesario para armar el mensaje de WhatsApp de una publicación. */
    public record ParaContacto(UUID publicacionId, UUID vendedorId, String slug, String titulo, String version,
                               int anio, BigDecimal precio, Moneda moneda) {
    }

    /** Publicación activa por la que se puede contactar al vendedor. Vendida, pausada o borrador: 404. */
    public ParaContacto paraContacto(String slug) {
        return publicaciones.findBySlug(slug)
                .filter(p -> p.getEstado() == EstadoPublicacion.ACTIVA)
                .map(p -> new ParaContacto(p.getId(), p.getVendedorId(), p.getSlug(),
                        catalogo.modeloConMarca(p.getModeloId()).orElseThrow().titulo(), p.getVersion(), p.getAnio(),
                        p.getPrecio(), p.getMoneda()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta publicación ya no está disponible."));
    }

    /** Resultado del feed: la publicación y, si se buscó por zona, a cuántos km está. */
    public record Encontrada(PublicacionVista vista, Double distanciaKm) {
    }

    /** Feed: publicaciones activas que cumplen los filtros. */
    public Pagina<Encontrada> buscar(FiltrosBusqueda f) {
        validar(f);
        BigDecimal dolar = f.moneda() == Moneda.ARS && (f.precioMin() != null || f.precioMax() != null)
                ? cotizaciones.dolarDeReferencia().orElseThrow(() -> new ServicioNoDisponibleException(
                        "Todavía no podemos filtrar por precio en pesos. Probá en dólares.", null))
                : null;
        BuscadorPublicaciones.Resultado resultado = buscador.buscar(f,
                f.precioMin() == null ? null : PreciosUsd.calcular(f.precioMin(), f.moneda(), dolar),
                f.precioMax() == null ? null : PreciosUsd.calcular(f.precioMax(), f.moneda(), dolar));

        List<UUID> ids = resultado.coincidencias().stream().map(BuscadorPublicaciones.Coincidencia::id).toList();
        Map<UUID, PublicacionVista> vistas = vistas(publicaciones.findAllById(ids)).stream()
                .collect(Collectors.toMap(v -> v.publicacion().getId(), v -> v));
        List<Encontrada> items = resultado.coincidencias().stream()
                .filter(c -> vistas.containsKey(c.id()))
                .map(c -> new Encontrada(vistas.get(c.id()), c.distanciaKm()))
                .toList();
        long hasta = (long) f.pagina() * f.tamano() + items.size();
        return new Pagina<>(items, f.pagina(), f.tamano(), resultado.total(), hasta < resultado.total());
    }

    private static void validar(FiltrosBusqueda f) {
        boolean algoDeZona = f.lat() != null || f.lng() != null || f.radioKm() != null;
        if (algoDeZona && !f.porZona()) {
            throw new DatosInvalidosException("radioKm", "Para buscar por zona hacen falta lat, lng y radioKm.");
        }
        if (f.orden() == OrdenBusqueda.CERCANIA && !f.porZona()) {
            throw new DatosInvalidosException("orden", "Para ordenar por cercanía elegí una zona.");
        }
        if (f.modelo() != null && f.marca() == null) {
            throw new DatosInvalidosException("modelo", "Para filtrar por modelo elegí también la marca.");
        }
        if (f.precioMin() != null && f.precioMax() != null && f.precioMin().compareTo(f.precioMax()) > 0) {
            throw new DatosInvalidosException("precioMax", "El precio máximo no puede ser menor que el mínimo.");
        }
        if (f.anioMin() != null && f.anioMax() != null && f.anioMin() > f.anioMax()) {
            throw new DatosInvalidosException("anioMax", "El año máximo no puede ser menor que el mínimo.");
        }
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
                PreciosUsd.calcular(r.precio(), r.moneda(), dolar(r.moneda())),
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

    private BigDecimal dolar(Moneda moneda) {
        return moneda == Moneda.USD ? null : cotizaciones.dolarDeReferencia().orElse(null);
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
