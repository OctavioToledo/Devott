package com.devott.metricas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Eventos", description = "Registro anónimo de vistas para las métricas de los vendedores")
class EventosController {

    private final VistaService vistas;

    EventosController(VistaService vistas) {
        this.vistas = vistas;
    }

    @PostMapping("/api/v1/eventos/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registra la vista de un perfil o de una publicación",
            description = "Cuenta una vista por visitante, perfil o publicación y día. Responde 204 aunque el "
                    + "slug no exista o la vista ya se haya contado.")
    void vista(@Valid @RequestBody VistaRequest request) {
        vistas.registrar(request.tipo(), request.slug(), request.visitante());
    }
}
