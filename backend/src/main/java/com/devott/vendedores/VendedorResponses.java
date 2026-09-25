package com.devott.vendedores;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

final class VendedorResponses {

    private VendedorResponses() {
    }

    /** Perfil propio, con todos los datos para editarlo. */
    @Schema(name = "MiVendedor")
    record MiVendedor(
            UUID id,
            TipoVendedor tipo,
            String slug,
            String nombrePublico,
            @Schema(description = "Código de área + número, sin 0 ni 15") String whatsapp,
            String telefono,
            String descripcion,
            String direccion,
            Localidad localidad,
            String horarios,
            String instagram,
            String facebook,
            boolean verificado,
            Instant creadoEn) {

        static MiVendedor de(Vendedor v) {
            return new MiVendedor(v.getId(), v.getTipo(), v.getSlug(), v.getNombrePublico(),
                    Whatsapp.local(v.getWhatsapp()), v.getTelefono(), v.getDescripcion(), v.getDireccion(),
                    Localidad.de(v), v.getHorarios(), v.getInstagram(), v.getFacebook(), v.isVerificado(),
                    v.getCreadoEn());
        }
    }

    /** Perfil público. No incluye el WhatsApp ni el teléfono: el contacto pasa por /contacto. */
    @Schema(name = "VendedorPublico")
    record VendedorPublico(
            String slug,
            TipoVendedor tipo,
            String nombrePublico,
            String descripcion,
            String direccion,
            String ciudad,
            String provincia,
            String horarios,
            String instagram,
            String facebook,
            boolean verificado,
            Instant creadoEn) {

        static VendedorPublico de(Vendedor v) {
            return new VendedorPublico(v.getSlug(), v.getTipo(), v.getNombrePublico(), v.getDescripcion(),
                    v.getDireccion(), v.getCiudad(), v.getProvincia(), v.getHorarios(), v.getInstagram(),
                    v.getFacebook(), v.isVerificado(), v.getCreadoEn());
        }
    }

    @Schema(name = "LocalidadVendedor")
    record Localidad(String ciudad, String provincia, Double lat, Double lng) {

        static Localidad de(Vendedor v) {
            var punto = v.getUbicacion();
            return new Localidad(v.getCiudad(), v.getProvincia(),
                    punto == null ? null : punto.getY(), punto == null ? null : punto.getX());
        }
    }

    @Schema(name = "SlugDisponible")
    record SlugDisponible(String slug, boolean disponible, String motivo) {
    }
}
