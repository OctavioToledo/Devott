package com.devott.publicaciones;

import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.compartido.almacenamiento.SubidaFirmada;
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
    private final FotoService fotos;
    private final AlmacenDeArchivos almacen;

    MisPublicacionesController(PublicacionService publicaciones, FotoService fotos, AlmacenDeArchivos almacen) {
        this.publicaciones = publicaciones;
        this.fotos = fotos;
        this.almacen = almacen;
    }

    @GetMapping
    @Operation(summary = "Lista mis publicaciones, en todos los estados", description = "Las editadas más recientemente primero.")
    List<MiPublicacion> listar(@AuthenticationPrincipal Jwt jwt) {
        return publicaciones.misPublicaciones(usuarioId(jwt)).stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Devuelve una de mis publicaciones")
    @ApiResponse(responseCode = "404", description = "No existe o es de otro vendedor")
    MiPublicacion ver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return respuesta(publicaciones.miPublicacion(usuarioId(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una publicación en borrador")
    @ApiResponse(responseCode = "409", description = "El usuario todavía no tiene perfil de vendedor")
    MiPublicacion crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PublicacionRequest request) {
        return respuesta(publicaciones.crear(usuarioId(jwt), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una publicación", description = "No cambia su estado ni su slug.")
    MiPublicacion actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @Valid @RequestBody PublicacionRequest request) {
        return respuesta(publicaciones.actualizar(usuarioId(jwt), id, request));
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
        return respuesta(publicaciones.cambiarEstado(usuarioId(jwt), id, request.estado()));
    }

    // Fotos -------------------------------------------------------------------

    @PostMapping("/{id}/fotos/url-subida")
    @Operation(summary = "Pide una URL para subir una foto",
            description = "Valida que la publicación sea propia y el límite de fotos. El navegador sube el archivo "
                    + "directo al almacenamiento con esa URL y después confirma con POST .../fotos.")
    @ApiResponse(responseCode = "409", description = "Límite de fotos alcanzado")
    SubidaFirmada urlSubida(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Valid @RequestBody FotoRequests.PedidoSubida request) {
        return fotos.pedirSubida(usuarioId(jwt), id, request.contentType());
    }

    @PostMapping("/{id}/fotos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirma una foto subida", description = "La agrega al final. Devuelve la publicación actualizada.")
    MiPublicacion confirmarFoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody FotoRequests.Confirmacion request) {
        return respuesta(fotos.confirmar(usuarioId(jwt), id, request.ruta(), request.ancho(), request.alto()));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    @Operation(summary = "Elimina una foto", description = "Reordena las que quedan. Devuelve la publicación actualizada.")
    @ApiResponse(responseCode = "409", description = "Es la única foto de una publicación activa")
    MiPublicacion eliminarFoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID fotoId) {
        return respuesta(fotos.eliminar(usuarioId(jwt), id, fotoId));
    }

    @PutMapping("/{id}/fotos/orden")
    @Operation(summary = "Reordena las fotos", description = "La primera es la portada.")
    MiPublicacion reordenarFotos(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody FotoRequests.Orden request) {
        return respuesta(fotos.reordenar(usuarioId(jwt), id, request.fotoIds()));
    }

    private MiPublicacion respuesta(PublicacionVista vista) {
        return MiPublicacion.de(vista, almacen, fotos.maxFotos(vista.publicacion().getVendedorId()));
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
