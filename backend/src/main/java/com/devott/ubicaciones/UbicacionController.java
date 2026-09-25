package com.devott.ubicaciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ubicaciones")
@Tag(name = "Ubicaciones", description = "Localidades argentinas para ubicar vendedores y buscar por zona")
class UbicacionController {

    private final UbicacionService ubicaciones;

    UbicacionController(UbicacionService ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    @GetMapping
    @Operation(summary = "Busca localidades por nombre", description = "Devuelve hasta 10 localidades con su centroide.")
    ResponseEntity<List<Localidad>> buscar(
            @Parameter(description = "Parte del nombre de la localidad", example = "villa mar")
            @RequestParam @NotBlank @Size(min = 2, max = 60) String q) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(ubicaciones.buscar(q));
    }
}
