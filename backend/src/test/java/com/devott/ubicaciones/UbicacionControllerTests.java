package com.devott.ubicaciones;

import com.devott.TestcontainersConfiguration;
import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UbicacionControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProveedorDeLocalidades proveedor;

    @Test
    void buscaSinLoginNormalizandoElTextoYSinRepetidas() throws Exception {
        when(proveedor.buscar("villa maria")).thenReturn(List.of(
                new Localidad("1", "Villa María", "Córdoba", -32.41, -63.24),
                new Localidad("2", "Villa Maria", "Córdoba", -32.40, -63.25),
                new Localidad("3", "Villa María", "Buenos Aires", -34.88, -60.34)));

        mockMvc.perform(get("/api/v1/ubicaciones").param("q", "  Villa  María "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].provincia").value("Córdoba"))
                .andExpect(jsonPath("$[1].provincia").value("Buenos Aires"));

        verify(proveedor).buscar("villa maria");
    }

    @Test
    void textoMuyCortoResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/ubicaciones").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("q"));
    }

    @Test
    void siElProveedorFallaResponde503() throws Exception {
        when(proveedor.buscar(anyString()))
                .thenThrow(new ServicioNoDisponibleException("No pudimos buscar localidades. Probá de nuevo en un rato.", null));

        mockMvc.perform(get("/api/v1/ubicaciones").param("q", "rosario"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio no disponible"));
    }
}
