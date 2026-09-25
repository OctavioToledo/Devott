package com.devott.vendedores;

import com.devott.vendedores.VendedorResponses.VendedorPublico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendedores")
@Tag(name = "Vendedores", description = "Perfiles públicos de concesionarias y particulares")
class VendedorPublicoController {

    private final VendedorService vendedores;

    VendedorPublicoController(VendedorService vendedores) {
        this.vendedores = vendedores;
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Perfil público de un vendedor", description = "No incluye el WhatsApp ni el teléfono.")
    @ApiResponse(responseCode = "404", description = "No existe el vendedor")
    VendedorPublico perfil(@PathVariable String slug) {
        return VendedorPublico.de(vendedores.porSlug(slug));
    }
}
