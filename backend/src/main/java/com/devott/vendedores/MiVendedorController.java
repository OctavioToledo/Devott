package com.devott.vendedores;

import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.usuarios.UsuarioService;
import com.devott.vendedores.VendedorResponses.MiVendedor;
import com.devott.vendedores.VendedorResponses.SlugDisponible;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/vendedor")
@Tag(name = "Mi perfil de vendedor", description = "Alta y edición del perfil del usuario logueado")
class MiVendedorController {

    private final VendedorService vendedores;
    private final UsuarioService usuarios;

    MiVendedorController(VendedorService vendedores, UsuarioService usuarios) {
        this.vendedores = vendedores;
        this.usuarios = usuarios;
    }

    @GetMapping
    @Operation(summary = "Devuelve mi perfil de vendedor")
    @ApiResponse(responseCode = "404", description = "Todavía no creó su perfil")
    MiVendedor miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return vendedores.deUsuario(usuarioId(jwt))
                .map(MiVendedor::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Todavía no creaste tu perfil de vendedor."));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea mi perfil de vendedor")
    @ApiResponse(responseCode = "409", description = "Ya tiene perfil, o el link está en uso")
    MiVendedor crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VendedorRequest request) {
        // El usuario puede no existir todavía si nunca llamó a GET /me.
        usuarios.sincronizar(jwt);
        return MiVendedor.de(vendedores.crear(usuarioId(jwt), request));
    }

    @PutMapping
    @Operation(summary = "Edita mi perfil de vendedor")
    MiVendedor actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VendedorRequest request) {
        return MiVendedor.de(vendedores.actualizar(usuarioId(jwt), request));
    }

    @GetMapping("/slug-disponible")
    @Operation(summary = "Indica si un link de perfil está disponible")
    SlugDisponible slugDisponible(@AuthenticationPrincipal Jwt jwt, @RequestParam String slug) {
        var motivo = vendedores.motivoSlugNoDisponible(slug, usuarioId(jwt));
        return new SlugDisponible(slug, motivo.isEmpty(), motivo.orElse(null));
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
