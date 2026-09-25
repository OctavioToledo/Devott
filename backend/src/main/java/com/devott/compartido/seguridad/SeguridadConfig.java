package com.devott.compartido.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SeguridadConfig {

    /** Supabase emite los tokens de usuarios logueados con esta audiencia. */
    static final String AUDIENCIA_SUPABASE = "authenticated";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/publicaciones/**",
                                "/api/v1/vendedores/**",
                                "/api/v1/catalogo/**",
                                "/api/v1/contacto/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/eventos/vista").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Valida los JWT de Supabase con sus claves públicas (JWKS). Requiere que el proyecto use
     * claves de firma asimétricas (JWT Signing Keys), no el secreto HS256 heredado.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${devott.supabase.url}") String supabaseUrl,
                          @Value("${devott.supabase.jwt-issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
                .jwsAlgorithms(algs -> {
                    algs.add(SignatureAlgorithm.ES256);
                    algs.add(SignatureAlgorithm.RS256);
                })
                .build();
        decoder.setJwtValidator(validadorSupabase(issuer));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validadorSupabase(String issuer) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud",
                        aud -> aud != null && aud.contains(AUDIENCIA_SUPABASE)));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${devott.frontend-url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontendUrl));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
