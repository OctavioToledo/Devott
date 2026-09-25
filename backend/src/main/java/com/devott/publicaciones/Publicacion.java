package com.devott.publicaciones;

import com.devott.compartido.errores.ConflictoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vehículo publicado por un vendedor. Nace como BORRADOR; el slug se genera al crearla y no cambia,
 * para que los links compartidos sigan funcionando.
 */
@Entity
@Table(name = "publicacion")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendedor_id", nullable = false, updatable = false)
    private UUID vendedorId;

    @Column(name = "modelo_id")
    private Integer modeloId;

    private String version;

    private int anio;

    private int km;

    private Condicion condicion;

    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    private Moneda moneda;

    /** Precio en dólares para filtrar y ordenar. En ARS se calcula con la cotización (paso 6). */
    @Column(name = "precio_usd_ref")
    private BigDecimal precioUsdRef;

    @Enumerated(EnumType.STRING)
    private Carroceria carroceria;

    @Enumerated(EnumType.STRING)
    private Combustible combustible;

    @Enumerated(EnumType.STRING)
    private Transmision transmision;

    private Traccion traccion;

    private String color;

    private Integer puertas;

    private boolean financia;

    @Column(name = "acepta_permuta")
    private boolean aceptaPermuta;

    @Column(name = "unico_dueno")
    private boolean unicoDueno;

    private String descripcion;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point ubicacion;

    private String ciudad;

    private String provincia;

    @Enumerated(EnumType.STRING)
    private EstadoPublicacion estado = EstadoPublicacion.BORRADOR;

    @Column(updatable = false)
    private String slug;

    @Column(name = "publicada_en")
    private Instant publicadaEn;

    @Column(name = "vendida_en")
    private Instant vendidaEn;

    @Column(name = "creada_en", insertable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en")
    private Instant actualizadaEn = Instant.now();

    protected Publicacion() {
    }

    Publicacion(UUID vendedorId, String slug) {
        this.vendedorId = vendedorId;
        this.slug = slug;
    }

    void actualizar(DatosPublicacion datos) {
        this.modeloId = datos.modeloId();
        this.version = datos.version();
        this.anio = datos.anio();
        this.km = datos.km();
        this.condicion = datos.condicion();
        this.precio = datos.precio();
        this.moneda = datos.moneda();
        this.precioUsdRef = datos.moneda() == Moneda.USD ? datos.precio() : null;
        this.carroceria = datos.carroceria();
        this.combustible = datos.combustible();
        this.transmision = datos.transmision();
        this.traccion = datos.traccion();
        this.color = datos.color();
        this.puertas = datos.puertas();
        this.financia = datos.financia();
        this.aceptaPermuta = datos.aceptaPermuta();
        this.unicoDueno = datos.unicoDueno();
        this.descripcion = datos.descripcion();
        this.ciudad = datos.ciudad();
        this.provincia = datos.provincia();
        this.ubicacion = datos.ubicacion();
    }

    /**
     * Cambia el estado respetando las transiciones permitidas. Las reglas que dependen de otros
     * datos (fotos, límite del plan) las valida el servicio antes de llamar.
     */
    void cambiarEstado(EstadoPublicacion nuevo, Instant ahora) {
        if (nuevo == estado) {
            return;
        }
        if (!estado.puedePasarA(nuevo)) {
            throw new ConflictoException("No se puede pasar una publicación de " + nombre(estado)
                    + " a " + nombre(nuevo) + ".");
        }
        if (nuevo == EstadoPublicacion.ACTIVA && publicadaEn == null) {
            publicadaEn = ahora;
        }
        vendidaEn = nuevo == EstadoPublicacion.VENDIDA ? ahora : null;
        estado = nuevo;
    }

    private static String nombre(EstadoPublicacion estado) {
        return switch (estado) {
            case BORRADOR -> "borrador";
            case ACTIVA -> "activa";
            case PAUSADA -> "pausada";
            case VENDIDA -> "vendida";
        };
    }

    @PreUpdate
    void alActualizar() {
        actualizadaEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVendedorId() {
        return vendedorId;
    }

    public Integer getModeloId() {
        return modeloId;
    }

    public String getVersion() {
        return version;
    }

    public int getAnio() {
        return anio;
    }

    public int getKm() {
        return km;
    }

    public Condicion getCondicion() {
        return condicion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public BigDecimal getPrecioUsdRef() {
        return precioUsdRef;
    }

    public Carroceria getCarroceria() {
        return carroceria;
    }

    public Combustible getCombustible() {
        return combustible;
    }

    public Transmision getTransmision() {
        return transmision;
    }

    public Traccion getTraccion() {
        return traccion;
    }

    public String getColor() {
        return color;
    }

    public Integer getPuertas() {
        return puertas;
    }

    public boolean isFinancia() {
        return financia;
    }

    public boolean isAceptaPermuta() {
        return aceptaPermuta;
    }

    public boolean isUnicoDueno() {
        return unicoDueno;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Point getUbicacion() {
        return ubicacion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getPublicadaEn() {
        return publicadaEn;
    }

    public Instant getVendidaEn() {
        return vendidaEn;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }
}
