package com.devott.vendedores;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * Perfil de vendedor: concesionaria o particular. Cada usuario tiene como máximo uno.
 */
@Entity
@Table(name = "vendedor")
public class Vendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    private TipoVendedor tipo;

    private String slug;

    @Column(name = "nombre_publico")
    private String nombrePublico;

    private String whatsapp;

    private String telefono;

    private String descripcion;

    @Column(name = "logo_path")
    private String logoPath;

    private String direccion;

    private String ciudad;

    private String provincia;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point ubicacion;

    private String horarios;

    private String instagram;

    private String facebook;

    private boolean verificado;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    protected Vendedor() {
    }

    Vendedor(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    void actualizar(DatosVendedor datos) {
        this.tipo = datos.tipo();
        this.slug = datos.slug();
        this.nombrePublico = datos.nombrePublico();
        this.whatsapp = datos.whatsapp();
        this.telefono = datos.telefono();
        this.descripcion = datos.descripcion();
        this.direccion = datos.direccion();
        this.ciudad = datos.ciudad();
        this.provincia = datos.provincia();
        this.ubicacion = datos.ubicacion();
        this.horarios = datos.horarios();
        this.instagram = datos.instagram();
        this.facebook = datos.facebook();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public TipoVendedor getTipo() {
        return tipo;
    }

    public String getSlug() {
        return slug;
    }

    public String getNombrePublico() {
        return nombrePublico;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public Point getUbicacion() {
        return ubicacion;
    }

    public String getHorarios() {
        return horarios;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getFacebook() {
        return facebook;
    }

    public boolean isVerificado() {
        return verificado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
