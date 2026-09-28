package com.devott.publicaciones;

import com.devott.catalogo.ModeloConMarca;
import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.vendedores.TipoVendedor;
import com.devott.vendedores.Vendedor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PublicacionResponses {

    private PublicacionResponses() {
    }

    @Schema(name = "Foto")
    record FotoResponse(UUID id, String url, int orden, Integer ancho, Integer alto) {

        static FotoResponse de(Foto f, AlmacenDeArchivos almacen) {
            return new FotoResponse(f.getId(), almacen.urlPublica(f.getStoragePath()), f.getOrden(), f.getAncho(), f.getAlto());
        }

        static List<FotoResponse> de(List<Foto> fotos, AlmacenDeArchivos almacen) {
            return fotos.stream().map(f -> de(f, almacen)).toList();
        }
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
            List<FotoResponse> fotos,
            @Schema(description = "Máximo de fotos que permite el plan") int maxFotos,
            Instant publicadaEn,
            Instant vendidaEn,
            Instant creadaEn,
            Instant actualizadaEn) {

        static MiPublicacion de(PublicacionVista v, AlmacenDeArchivos almacen, int maxFotos) {
            Publicacion p = v.publicacion();
            return new MiPublicacion(p.getId(), p.getSlug(), p.getEstado(), v.modelo().titulo(),
                    ModeloResumen.de(v.modelo()), p.getVersion(), p.getAnio(), p.getKm(), p.getCondicion(),
                    p.getPrecio(), p.getMoneda(), p.getCarroceria(), p.getCombustible(), p.getTransmision(),
                    p.getTraccion(), p.getColor(), p.getPuertas(), p.isFinancia(), p.isAceptaPermuta(),
                    p.isUnicoDueno(), p.getDescripcion(), Localidad.de(p), FotoResponse.de(v.fotos(), almacen), maxFotos,
                    p.getPublicadaEn(), p.getVendidaEn(), p.getCreadaEn(), p.getActualizadaEn());
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
            List<FotoResponse> fotos,
            Instant publicadaEn,
            VendedorResumen vendedor) {

        static PublicacionPublica de(PublicacionVista v, Vendedor vendedor, AlmacenDeArchivos almacen) {
            Publicacion p = v.publicacion();
            return new PublicacionPublica(p.getSlug(), p.getEstado(), v.modelo().titulo(),
                    ModeloResumen.de(v.modelo()), p.getVersion(), p.getAnio(), p.getKm(), p.getCondicion(),
                    p.getPrecio(), p.getMoneda(), p.getCarroceria(), p.getCombustible(), p.getTransmision(),
                    p.getTraccion(), p.getColor(), p.getPuertas(), p.isFinancia(), p.isAceptaPermuta(),
                    p.isUnicoDueno(), p.getDescripcion(), p.getCiudad(), p.getProvincia(),
                    FotoResponse.de(v.fotos(), almacen), p.getPublicadaEn(), VendedorResumen.de(vendedor));
        }
    }

    /** Resumen para listados: stock del perfil y feed. */
    @Schema(name = "TarjetaPublicacion")
    public record Tarjeta(
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
            @Schema(description = "URL de la primera foto, o null si no tiene") String portada,
            int cantidadFotos,
            @Schema(description = "Distancia en km al centro de la búsqueda. Solo si se buscó por zona") Double distanciaKm) {

        static Tarjeta de(PublicacionService.Encontrada e, AlmacenDeArchivos almacen) {
            Tarjeta t = de(e.vista(), almacen);
            Double km = e.distanciaKm() == null ? null : Math.round(e.distanciaKm() * 10) / 10.0;
            return new Tarjeta(t.slug(), t.estado(), t.titulo(), t.version(), t.anio(), t.km(), t.condicion(),
                    t.precio(), t.moneda(), t.financia(), t.ciudad(), t.provincia(), t.portada(), t.cantidadFotos(), km);
        }

        static Tarjeta de(PublicacionVista v, AlmacenDeArchivos almacen) {
            Publicacion p = v.publicacion();
            String portada = v.fotos().isEmpty() ? null : almacen.urlPublica(v.fotos().getFirst().getStoragePath());
            return new Tarjeta(p.getSlug(), p.getEstado(), v.modelo().titulo(), p.getVersion(), p.getAnio(),
                    p.getKm(), p.getCondicion(), p.getPrecio(), p.getMoneda(), p.isFinancia(), p.getCiudad(),
                    p.getProvincia(), portada, v.fotos().size(), null);
        }
    }
}
