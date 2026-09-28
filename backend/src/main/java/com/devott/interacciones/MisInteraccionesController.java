package com.devott.interacciones;

import com.devott.compartido.web.Pagina;
import com.devott.interacciones.SeguimientoService.VendedorSeguido;
import com.devott.publicaciones.PublicacionResponses.Tarjeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Guardados y seguimientos del usuario logueado. Guardar y seguir son idempotentes (PUT y DELETE). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Guardados y seguimientos", description = "Publicaciones guardadas y vendedores seguidos por el usuario logueado")
class MisInteraccionesController {

    private final GuardadoService guardados;
    private final SeguimientoService seguimientos;

    MisInteraccionesController(GuardadoService guardados, SeguimientoService seguimientos) {
        this.guardados = guardados;
        this.seguimientos = seguimientos;
    }

    @GetMapping("/guardados")
    @Operation(summary = "Mis publicaciones guardadas", description = "Activas y vendidas, las últimas guardadas primero.")
    Pagina<Tarjeta> guardados(@AuthenticationPrincipal Jwt jwt,
                              @RequestParam(defaultValue = "0") @Min(0) int pagina,
                              @RequestParam(defaultValue = "24") @Min(1) @Max(60) int tamano) {
        return guardados.listar(usuarioId(jwt), pagina, tamano);
    }

    @GetMapping("/guardados/slugs")
    @Operation(summary = "Slugs de mis guardados", description = "Para marcar los corazones en listados.")
    List<String> slugsGuardados(@AuthenticationPrincipal Jwt jwt) {
        return guardados.slugs(usuarioId(jwt));
    }

    @PutMapping("/guardados/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Guarda una publicación")
    @ApiResponse(responseCode = "404", description = "No existe o no está visible")
    void guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        guardados.guardar(jwt, slug);
    }

    @DeleteMapping("/guardados/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita una publicación de guardados")
    void quitarGuardado(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        guardados.quitar(usuarioId(jwt), slug);
    }

    @GetMapping("/seguimientos")
    @Operation(summary = "Vendedores que sigo", description = "Los últimos seguidos primero.")
    List<VendedorSeguido> seguimientos(@AuthenticationPrincipal Jwt jwt) {
        return seguimientos.listar(usuarioId(jwt));
    }

    @GetMapping("/seguimientos/slugs")
    @Operation(summary = "Slugs de los vendedores que sigo")
    List<String> slugsSeguidos(@AuthenticationPrincipal Jwt jwt) {
        return seguimientos.slugs(usuarioId(jwt));
    }

    @PutMapping("/seguimientos/{vendedorSlug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Sigue a un vendedor")
    @ApiResponse(responseCode = "404", description = "No existe el vendedor")
    @ApiResponse(responseCode = "409", description = "Es el perfil propio")
    void seguir(@AuthenticationPrincipal Jwt jwt, @PathVariable String vendedorSlug) {
        seguimientos.seguir(jwt, vendedorSlug);
    }

    @DeleteMapping("/seguimientos/{vendedorSlug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deja de seguir a un vendedor")
    void dejarDeSeguir(@AuthenticationPrincipal Jwt jwt, @PathVariable String vendedorSlug) {
        seguimientos.dejarDeSeguir(usuarioId(jwt), vendedorSlug);
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
