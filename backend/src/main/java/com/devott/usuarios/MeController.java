package com.devott.usuarios;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Mi cuenta", description = "Datos del usuario logueado")
class MeController {

    private final UsuarioService usuarioService;

    MeController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Devuelve el usuario logueado",
            description = "Crea el usuario la primera vez y actualiza sus datos con los del token de Supabase.")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return MeResponse.de(usuarioService.sincronizar(jwt));
    }

    @Schema(name = "Me")
    record MeResponse(UUID id, String email, String nombre, String avatarUrl, Instant creadoEn) {

        static MeResponse de(Usuario usuario) {
            return new MeResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(),
                    usuario.getAvatarUrl(), usuario.getCreadoEn());
        }
    }
}
