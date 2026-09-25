package com.devott.usuarios;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    /**
     * Sincroniza el usuario con los datos del token de Supabase y lo devuelve.
     */
    @Transactional
    public Usuario sincronizar(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        Map<String, Object> metadata = jwt.hasClaim("user_metadata")
                ? jwt.getClaimAsMap("user_metadata")
                : Map.of();

        repository.upsert(id,
                jwt.getClaimAsString("email"),
                primerTexto(metadata, "full_name", "name"),
                primerTexto(metadata, "avatar_url", "picture"));
        return repository.findById(id).orElseThrow();
    }

    private static String primerTexto(Map<String, Object> metadata, String... claves) {
        for (String clave : claves) {
            if (metadata.get(clave) instanceof String valor && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}
