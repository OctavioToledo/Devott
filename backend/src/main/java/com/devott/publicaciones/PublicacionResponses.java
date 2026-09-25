package com.devott.publicaciones;

import com.devott.catalogo.ModeloConMarca;
import com.devott.vendedores.TipoVendedor;
import com.devott.vendedores.Vendedor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class PublicacionResponses {

    private PublicacionResponses() {
    }

    @Schema(name = "ModeloDePublicacion")
    record ModeloResumen(Integer marcaId, String marca, Integer modeloId, String modelo) {

        static ModeloResumen de(ModeloConMarca m) {
            return new ModeloResumen(m.marcaId(), m.marca(), m.modeloId(), m.modelo());
        }
    }

    @Schema(name = "LocalidadDePublicacion")
    record Localidad(String ciudad, String provincia, Double lat, Double lng) {

        static Localidad de(Publicacion p) {
            var punto = p.getUbicacion();
            return new Localidad(p.getCiudad(), p.getProvincia(),
                    punto == null ? null : punto.getY(), punto == null ? null : punto.getX());
        }
    }

    /** Publicación propia, con todos los datos para editarla. */
    @Schema(name = "MiPublicacion")
    record MiPublicacion(
            UUID id,
            String slug,
            EstadoPublicacion estado,
            String titulo,
            ModeloResumen modelo,
            String version,
            int anio,
            int km,
            Condicion condicion,
            BigDecimal precio,
            Moneda moneda,
            Carroceria carroceria,
            Combustible combustible,
            Transmision transmision,
            Traccion traccion,
            String color,
            Integer puertas,
            boolean financia,
            boolean aceptaPermuta,
            boolean unicoDueno,
            String descripcion,
            Localidad localidad,
            long cantidadFotos,
            Instant publicadaEn,
            Instant vendidaEn,
            Instant creadaEn,
            Instant actualizadaEn) {

        static MiPublicacion de(PublicacionVista v) {
            Publicacion p = v.publicacion();
            return new MiPublicacion(p.getId(), p.getSlug(), p.getEstado(), v.modelo().titulo(),
                    ModeloResumen.de(v.modelo()), p.getVersion(), p.getAnio(), p.getKm(), p.getCondicion(),
                    p.getPrecio(), p.getMoneda(), p.getCarroceria(), p.getCombustible(), p.getTransmision(),
                    p.getTraccion(), p.getColor(), p.getPuertas(), p.isFinancia(), p.isAceptaPermuta(),
                    p.isUnicoDueno(), p.getDescripcion(), Localidad.de(p), v.cantidadFotos(), p.getPublicadaEn(),
                    p.getVendidaEn(), p.getCreadaEn(), p.getActualizadaEn());
        }
    }

    @Schema(name = "VendedorDePublicacion")
    record VendedorResumen(String slug, String nombrePublico, TipoVendedor tipo, boolean verificado,
                           String ciudad, String provincia) {

        static VendedorResumen de(Vendedor v) {
            return new VendedorResumen(v.getSlug(), v.getNombrePublico(), v.getTipo(), v.isVerificado(),
                    v.getCiudad(), v.getProvincia());
        }
    }

    /** Publicación vista por cualquiera. Sin ids internos ni datos de contacto. */
    @Schema(name = "PublicacionPublica")
    record PublicacionPublica(
            String slug,
            EstadoPublicacion estado,
            String titulo,
            ModeloResumen modelo,
            String version,
            int anio,
            int km,
            Condicion condicion,
            BigDecimal precio,
            Moneda moneda,
            Carroceria carroceria,
            Combustible combustible,
            Transmision transmision,
            Traccion traccion,
            String color,
            Integer puertas,
            boolean financia,
            boolean aceptaPermuta,
            boolean unicoDueno,
            String descripcion,
            String ciudad,
            String provincia,
            Instant publicadaEn,
            VendedorResumen vendedor) {

        static PublicacionPublica de(PublicacionVista v, Vendedor vendedor) {
            Publicacion p = v.publicacion();
            return new PublicacionPublica(p.getSlug(), p.getEstado(), v.modelo().titulo(),
                    ModeloResumen.de(v.modelo()), p.getVersion(), p.getAnio(), p.getKm(), p.getCondicion(),
                    p.getPrecio(), p.getMoneda(), p.getCarroceria(), p.getCombustible(), p.getTransmision(),
                    p.getTraccion(), p.getColor(), p.getPuertas(), p.isFinancia(), p.isAceptaPermuta(),
                    p.isUnicoDueno(), p.getDescripcion(), p.getCiudad(), p.getProvincia(), p.getPublicadaEn(),
                    VendedorResumen.de(vendedor));
        }
    }

    /** Resumen para listados: stock del perfil y, más adelante, el feed. */
    @Schema(name = "TarjetaPublicacion")
    record Tarjeta(
            String slug,
            EstadoPublicacion estado,
            String titulo,
            String version,
            int anio,
            int km,
            Condicion condicion,
            BigDecimal precio,
            Moneda moneda,
            boolean financia,
            String ciudad,
            String provincia,
            long cantidadFotos) {

        static Tarjeta de(PublicacionVista v) {
            Publicacion p = v.publicacion();
            return new Tarjeta(p.getSlug(), p.getEstado(), v.modelo().titulo(), p.getVersion(), p.getAnio(),
                    p.getKm(), p.getCondicion(), p.getPrecio(), p.getMoneda(), p.isFinancia(), p.getCiudad(),
                    p.getProvincia(), v.cantidadFotos());
        }
    }
}
