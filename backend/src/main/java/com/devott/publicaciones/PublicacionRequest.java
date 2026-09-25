package com.devott.publicaciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Alta o edición de una publicación.
 */
@Schema(name = "PublicacionRequest")
record PublicacionRequest(
        @NotNull(message = "Elegí el modelo.")
        Integer modeloId,

        @Size(max = 80, message = "La versión puede tener hasta 80 caracteres.")
        String version,

        @NotNull(message = "Ingresá el año.")
        @Min(value = 1950, message = "El año tiene que ser 1950 o posterior.")
        Integer anio,

        @NotNull(message = "Ingresá el kilometraje.")
        @Min(value = 0, message = "El kilometraje no puede ser negativo.")
        @Max(value = 2_000_000, message = "Revisá el kilometraje.")
        Integer km,

        @NotNull(message = "Elegí si es 0 km o usado.")
        Condicion condicion,

        @NotNull(message = "Ingresá el precio.")
        @DecimalMin(value = "1", message = "Ingresá el precio.")
        @Digits(integer = 12, fraction = 2, message = "Revisá el precio.")
        BigDecimal precio,

        @NotNull(message = "Elegí la moneda.")
        Moneda moneda,

        @NotNull(message = "Elegí la carrocería.")
        Carroceria carroceria,

        @NotNull(message = "Elegí el combustible.")
        Combustible combustible,

        @NotNull(message = "Elegí la transmisión.")
        Transmision transmision,

        Traccion traccion,

        @Size(max = 40, message = "El color puede tener hasta 40 caracteres.")
        String color,

        @Min(value = 2, message = "Revisá la cantidad de puertas.")
        @Max(value = 5, message = "Revisá la cantidad de puertas.")
        Integer puertas,

        Boolean financia,
        Boolean aceptaPermuta,
        Boolean unicoDueno,

        @Size(max = 3000, message = "La descripción puede tener hasta 3000 caracteres.")
        String descripcion,

        @Schema(description = "Si no se indica, se usa la del vendedor")
        @Valid
        LocalidadRequest localidad) {

    @Schema(name = "LocalidadPublicacionRequest")
    record LocalidadRequest(
            @NotBlank(message = "Elegí la localidad.") @Size(max = 100) String ciudad,
            @NotBlank(message = "Elegí la localidad.") @Size(max = 60) String provincia,
            @NotNull @DecimalMin("-56") @DecimalMax("-21") Double lat,
            @NotNull @DecimalMin("-74") @DecimalMax("-53") Double lng) {
    }
}
