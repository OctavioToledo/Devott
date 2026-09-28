package com.devott.usuarios;

import com.devott.compartido.seguridad.Administradores;
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
    private final Administradores administradores;

    MeController(UsuarioService usuarioService, Administradores administradores) {
        this.usuarioService = usuarioService;
        this.administradores = administradores;
    }

    @GetMapping
    @Operation(summary = "Devuelve el usuario logueado",
            description = "Crea el usuario la primera vez y actualiza sus datos con los del token de Supabase.")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return MeResponse.de(usuarioService.sincronizar(jwt), administradores.es(jwt));
    }

    @Schema(name = "Me")
    record MeResponse(UUID id, String email, String nombre, String avatarUrl, Instant creadoEn,
                      @Schema(description = "Tiene acceso a la administración") boolean admin) {

        static MeResponse de(Usuario usuario, boolean admin) {
            return new MeResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(),
                    usuario.getAvatarUrl(), usuario.getCreadoEn(), admin);
        }
    }
}
