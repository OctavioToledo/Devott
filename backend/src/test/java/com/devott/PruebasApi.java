package com.devott;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Atajos para tests de integración que necesitan usuarios y vendedores.
 */
public final class PruebasApi {

    private PruebasApi() {
    }

    public static RequestPostProcessor usuario(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("email", id + "@example.com"));
    }

    public static String slugUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Crea un vendedor en Villa María, Córdoba, para el usuario y devuelve su slug. */
    public static String crearVendedor(MockMvc mockMvc, UUID usuarioId) throws Exception {
        String slug = slugUnico("vendedor");
        mockMvc.perform(post("/api/v1/me/vendedor").with(usuario(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "CONCESIONARIA", "nombrePublico": "Automotores del Sur", "slug": "%s",
                                 "whatsapp": "3534123456",
                                 "localidad": {"ciudad": "Villa María", "provincia": "Córdoba", "lat": -32.41, "lng": -63.24}}
                                """.formatted(slug)))
                .andExpect(status().isCreated());
        return slug;
    }
}
