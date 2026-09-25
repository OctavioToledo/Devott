package com.devott.publicaciones;

import com.devott.publicaciones.PublicacionResponses.MiPublicacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/publicaciones")
@Tag(name = "Mis publicaciones", description = "Publicaciones del vendedor logueado")
class MisPublicacionesController {

    private final PublicacionService publicaciones;

    MisPublicacionesController(PublicacionService publicaciones) {
        this.publicaciones = publicaciones;
    }

    @GetMapping
    @Operation(summary = "Lista mis publicaciones, en todos los estados", description = "Las editadas más recientemente primero.")
    List<MiPublicacion> listar(@AuthenticationPrincipal Jwt jwt) {
        return publicaciones.misPublicaciones(usuarioId(jwt)).stream().map(MiPublicacion::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Devuelve una de mis publicaciones")
    @ApiResponse(responseCode = "404", description = "No existe o es de otro vendedor")
    MiPublicacion ver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return MiPublicacion.de(publicaciones.miPublicacion(usuarioId(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una publicación en borrador")
    @ApiResponse(responseCode = "409", description = "El usuario todavía no tiene perfil de vendedor")
    MiPublicacion crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PublicacionRequest request) {
        return MiPublicacion.de(publicaciones.crear(usuarioId(jwt), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una publicación", description = "No cambia su estado ni su slug.")
    MiPublicacion actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @Valid @RequestBody PublicacionRequest request) {
        return MiPublicacion.de(publicaciones.actualizar(usuarioId(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una publicación y sus fotos")
    void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        publicaciones.eliminar(usuarioId(jwt), id);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de una publicación",
            description = "Borrador → activa (requiere una foto y lugar en el plan), activa ↔ pausada, "
                    + "activa o pausada → vendida, vendida → activa.")
    @ApiResponse(responseCode = "409", description = "Transición no permitida, sin fotos o límite del plan alcanzado")
    MiPublicacion cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody CambioEstadoRequest request) {
        return MiPublicacion.de(publicaciones.cambiarEstado(usuarioId(jwt), id, request.estado()));
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
