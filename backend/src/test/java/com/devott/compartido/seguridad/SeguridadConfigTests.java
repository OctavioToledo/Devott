package com.devott.compartido.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadConfigTests {

    static final String ISSUER = "https://proyecto.supabase.co/auth/v1";

    final OAuth2TokenValidator<Jwt> validador = SeguridadConfig.validadorSupabase(ISSUER);

    @Test
    void aceptaTokenDeSupabaseDeUsuarioLogueado() {
        assertThat(validador.validate(token(ISSUER, "authenticated")).hasErrors()).isFalse();
    }

    @Test
    void rechazaTokenDeOtroEmisor() {
        assertThat(validador.validate(token("https://otro.supabase.co/auth/v1", "authenticated")).hasErrors())
                .isTrue();
    }

    @Test
    void rechazaTokenAnonimo() {
        assertThat(validador.validate(token(ISSUER, "anon")).hasErrors()).isTrue();
    }

    private static Jwt token(String issuer, String audiencia) {
        Instant ahora = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .issuer(issuer)
                .audience(List.of(audiencia))
                .subject("00000000-0000-0000-0000-000000000001")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
    }
}
