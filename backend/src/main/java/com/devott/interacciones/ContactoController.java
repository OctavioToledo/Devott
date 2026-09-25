package com.devott.interacciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contacto")
@Tag(name = "Contacto", description = "Botones de WhatsApp: registran el contacto y redirigen")
class ContactoController {

    private final ContactoService contactos;

    ContactoController(ContactoService contactos) {
        this.contactos = contactos;
    }

    @GetMapping("/vendedores/{slug}")
    @Operation(summary = "Contacta a un vendedor desde su perfil",
            description = "Registra el contacto (con el usuario si hay sesión) y redirige a WhatsApp. "
                    + "El número no se expone en el perfil.")
    @ApiResponse(responseCode = "302", description = "Redirección a wa.me")
    @ApiResponse(responseCode = "404", description = "No existe el vendedor")
    ResponseEntity<Void> contactarVendedor(@PathVariable String slug, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(contactos.contactarVendedor(slug, jwt))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
