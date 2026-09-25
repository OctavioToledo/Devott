package com.devott.vendedores;

import com.devott.compartido.texto.Slugs;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición del perfil de vendedor.
 */
@Schema(name = "VendedorRequest")
record VendedorRequest(
        @NotNull(message = "Elegí si sos concesionaria o particular.")
        TipoVendedor tipo,

        @NotBlank(message = "Ingresá el nombre que van a ver los compradores.")
        @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres.")
        String nombrePublico,

        @NotBlank(message = "Elegí el link de tu perfil.")
        @Size(min = 3, max = 40, message = "El link tiene que tener entre 3 y 40 caracteres.")
        @Pattern(regexp = Slugs.PATRON, message = "Usá solo minúsculas, números y guiones.")
        String slug,

        @Schema(description = "Código de área + número, sin 0 ni 15", example = "3534123456")
        @NotBlank(message = "Ingresá tu WhatsApp.")
        String whatsapp,

        @Size(max = 30, message = "El teléfono puede tener hasta 30 caracteres.")
        String telefono,

        @Size(max = 1000, message = "La descripción puede tener hasta 1000 caracteres.")
        String descripcion,

        @Size(max = 120, message = "La dirección puede tener hasta 120 caracteres.")
        String direccion,

        @NotNull(message = "Elegí tu localidad.")
        @Valid
        LocalidadRequest localidad,

        @Size(max = 200, message = "Los horarios pueden tener hasta 200 caracteres.")
        String horarios,

        @Size(max = 100, message = "El Instagram puede tener hasta 100 caracteres.")
        String instagram,

        @Size(max = 150, message = "El Facebook puede tener hasta 150 caracteres.")
        String facebook) {

    /** Localidad elegida en el buscador de ubicaciones. Las coordenadas tienen que caer en Argentina. */
    @Schema(name = "LocalidadRequest")
    record LocalidadRequest(
            @NotBlank(message = "Elegí tu localidad.") @Size(max = 100) String ciudad,
            @NotBlank(message = "Elegí tu localidad.") @Size(max = 60) String provincia,
            @NotNull @DecimalMin("-56") @DecimalMax("-21") Double lat,
            @NotNull @DecimalMin("-74") @DecimalMax("-53") Double lng) {
    }
}
