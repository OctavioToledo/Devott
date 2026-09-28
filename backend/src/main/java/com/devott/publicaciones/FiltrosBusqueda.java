package com.devott.publicaciones;

import com.devott.vendedores.TipoVendedor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros del feed. Los que llegan vacíos no filtran. Las listas aceptan varios valores
 * ({@code carroceria=SUV&carroceria=PICKUP} o {@code carroceria=SUV,PICKUP}).
 */
public record FiltrosBusqueda(
        @Schema(description = "Latitud del centro de la búsqueda por zona") @DecimalMin("-90") @DecimalMax("90") Double lat,
        @Schema(description = "Longitud del centro de la búsqueda por zona") @DecimalMin("-180") @DecimalMax("180") Double lng,
        @Schema(description = "Radio en km alrededor de lat/lng") @Min(1) @Max(1000) Integer radioKm,
        Condicion condicion,
        TipoVendedor tipoVendedor,
        @Schema(description = "Slug de la marca") String marca,
        @Schema(description = "Slug del modelo. Requiere marca") String modelo,
        @Positive BigDecimal precioMin,
        @Positive BigDecimal precioMax,
        @Schema(description = "Moneda de precioMin y precioMax. Por defecto USD") Moneda moneda,
        @Min(1900) @Max(2100) Integer anioMin,
        @Min(1900) @Max(2100) Integer anioMax,
        @PositiveOrZero Integer kmMax,
        List<Carroceria> carroceria,
        List<Combustible> combustible,
        List<Transmision> transmision,
        @Schema(description = "true: solo las que ofrecen financiación") Boolean financia,
        @Schema(description = "true: solo las que aceptan permuta") Boolean permuta,
        @Schema(description = "true: solo de único dueño") Boolean unicoDueno,
        @Schema(description = "Por defecto RECIENTES") OrdenBusqueda orden,
        @Min(0) Integer pagina,
        @Min(1) @Max(60) Integer tamano) {

    public FiltrosBusqueda {
        moneda = moneda == null ? Moneda.USD : moneda;
        orden = orden == null ? OrdenBusqueda.RECIENTES : orden;
        pagina = pagina == null ? 0 : pagina;
        tamano = tamano == null ? 24 : tamano;
        carroceria = carroceria == null ? List.of() : carroceria;
        combustible = combustible == null ? List.of() : combustible;
        transmision = transmision == null ? List.of() : transmision;
        marca = vacioANull(marca);
        modelo = vacioANull(modelo);
    }

    boolean porZona() {
        return lat != null && lng != null && radioKm != null;
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
