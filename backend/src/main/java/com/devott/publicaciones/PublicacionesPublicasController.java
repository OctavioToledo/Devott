package com.devott.publicaciones;

import com.devott.compartido.web.Pagina;
import com.devott.publicaciones.PublicacionResponses.PublicacionPublica;
import com.devott.publicaciones.PublicacionResponses.Tarjeta;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;

@RestController
@Tag(name = "Publicaciones", description = "Publicaciones visibles para cualquiera")
class PublicacionesPublicasController {

    private final PublicacionService publicaciones;
    private final VendedorService vendedores;

    PublicacionesPublicasController(PublicacionService publicaciones, VendedorService vendedores) {
        this.publicaciones = publicaciones;
        this.vendedores = vendedores;
    }

    @GetMapping("/api/v1/publicaciones/{slug}")
    @Operation(summary = "Detalle de una publicación", description = "Visible si está activa o vendida.")
    @ApiResponse(responseCode = "404", description = "No existe, está en borrador o pausada")
    PublicacionPublica detalle(@PathVariable String slug) {
        PublicacionVista vista = publicaciones.publica(slug);
        return PublicacionPublica.de(vista, vendedores.porId(vista.publicacion().getVendedorId()).orElseThrow());
    }

    @GetMapping("/api/v1/vendedores/{slug}/publicaciones")
    @Operation(summary = "Publicaciones activas de un vendedor", description = "Las más nuevas primero.")
    Pagina<Tarjeta> deVendedor(
            @PathVariable String slug,
            @Parameter(description = "Solo 0 km o solo usados") @RequestParam(required = false) Condicion condicion,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "24") @Min(1) @Max(60) int tamano) {
        Set<Condicion> condiciones = condicion == null ? EnumSet.allOf(Condicion.class) : EnumSet.of(condicion);
        return publicaciones.activasDeVendedor(slug, condiciones, pagina, tamano).map(Tarjeta::de);
    }
}
