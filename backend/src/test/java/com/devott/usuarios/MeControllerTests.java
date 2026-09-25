package com.devott.usuarios;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creaElUsuarioConLosDatosDelToken() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j
                        .subject(id.toString())
                        .claim("email", "ana@example.com")
                        .claim("user_metadata", Map.of(
                                "full_name", "Ana Pérez",
                                "avatar_url", "https://example.com/ana.png")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/ana.png"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());

        assertThat(usuarios.findById(id)).isPresent();
    }

    @Test
    void actualizaLosDatosSinPisarLosQueFaltanEnElToken() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j
                .subject(id.toString())
                .claim("email", "juan@example.com")
                .claim("user_metadata", Map.of("name", "Juan")))));

        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j
                        .subject(id.toString())
                        .claim("email", "juan.nuevo@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan.nuevo@example.com"))
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }
}
