package com.devott.catalogo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo", description = "Marcas y modelos de vehículos")
class CatalogoController {

    /** El catálogo casi no cambia: los navegadores y el frontend pueden guardarlo una hora. */
    private static final CacheControl UNA_HORA = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final CatalogoService catalogo;

    CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/marcas")
    @Operation(summary = "Lista las marcas, ordenadas por nombre")
    ResponseEntity<List<MarcaResponse>> marcas() {
        List<MarcaResponse> marcas = catalogo.marcas().stream().map(MarcaResponse::de).toList();
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(marcas);
    }

    @GetMapping("/marcas/{marcaId}/modelos")
    @Operation(summary = "Lista los modelos de una marca, ordenados por nombre")
    @ApiResponse(responseCode = "200", description = "Modelos de la marca")
    @ApiResponse(responseCode = "404", description = "La marca no existe")
    ResponseEntity<List<ModeloResponse>> modelos(@PathVariable Integer marcaId) {
        List<ModeloResponse> modelos = catalogo.modelosDeMarca(marcaId).stream().map(ModeloResponse::de).toList();
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(modelos);
    }

    @Schema(name = "Marca")
    record MarcaResponse(Integer id, String nombre, String slug) {

        static MarcaResponse de(Marca marca) {
            return new MarcaResponse(marca.getId(), marca.getNombre(), marca.getSlug());
        }
    }

    @Schema(name = "Modelo")
    record ModeloResponse(Integer id, Integer marcaId, String nombre, String slug) {

        static ModeloResponse de(Modelo modelo) {
            return new ModeloResponse(modelo.getId(), modelo.getMarcaId(), modelo.getNombre(), modelo.getSlug());
        }
    }
}
