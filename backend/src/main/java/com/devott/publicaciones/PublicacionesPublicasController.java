package com.devott.publicaciones;

import com.devott.compartido.almacenamiento.AlmacenDeArchivos;
import com.devott.compartido.web.Pagina;
import com.devott.publicaciones.PublicacionResponses.PublicacionPublica;
import com.devott.publicaciones.PublicacionResponses.Tarjeta;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;

@RestController
@Tag(name = "Publicaciones", description = "Publicaciones visibles para cualquiera")
class PublicacionesPublicasController {

    private final PublicacionService publicaciones;
    private final VendedorService vendedores;
    private final AlmacenDeArchivos almacen;

    PublicacionesPublicasController(PublicacionService publicaciones, VendedorService vendedores,
                                    AlmacenDeArchivos almacen) {
        this.publicaciones = publicaciones;
        this.vendedores = vendedores;
        this.almacen = almacen;
    }

    @GetMapping("/api/v1/publicaciones")
    @Operation(summary = "Feed de publicaciones activas con filtros",
            description = "Los filtros de precio usan el precio de referencia en dólares; la respuesta muestra el "
                    + "precio y la moneda originales. Con lat, lng y radioKm busca por zona e informa la distancia.")
    Pagina<Tarjeta> buscar(@ParameterObject @Valid FiltrosBusqueda filtros) {
        return publicaciones.buscar(filtros).map(e -> Tarjeta.de(e, almacen));
    }

    @GetMapping("/api/v1/publicaciones/{slug}")
    @Operation(summary = "Detalle de una publicación", description = "Visible si está activa o vendida.")
    @ApiResponse(responseCode = "404", description = "No existe, está en borrador o pausada")
    PublicacionPublica detalle(@PathVariable String slug) {
        PublicacionVista vista = publicaciones.publica(slug);
        return PublicacionPublica.de(vista, vendedores.porId(vista.publicacion().getVendedorId()).orElseThrow(), almacen);
    }

    @GetMapping("/api/v1/vendedores/{slug}/publicaciones")
    @Operation(summary = "Publicaciones activas de un vendedor", description = "Las más nuevas primero.")
    Pagina<Tarjeta> deVendedor(
            @PathVariable String slug,
            @Parameter(description = "Solo 0 km o solo usados") @RequestParam(required = false) Condicion condicion,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "24") @Min(1) @Max(60) int tamano) {
        Set<Condicion> condiciones = condicion == null ? EnumSet.allOf(Condicion.class) : EnumSet.of(condicion);
        return publicaciones.activasDeVendedor(slug, condiciones, pagina, tamano).map(v -> Tarjeta.de(v, almacen));
    }
}
