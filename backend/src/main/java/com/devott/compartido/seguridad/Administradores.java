package com.devott.compartido.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quiénes administran Devott: los emails de `devott.admins` (variable DEVOTT_ADMINS). El email sale del
 * token de Supabase, que ya viene verificado por el proveedor de login.
 */
@Component
public class Administradores {

    private final Set<String> emails;

    Administradores(@Value("${devott.admins:}") String emails) {
        this.emails = Arrays.stream(emails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean es(Jwt jwt) {
        String email = jwt == null ? null : jwt.getClaimAsString("email");
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public void exigir(Jwt jwt) {
        if (!es(jwt)) {
            throw new AccessDeniedException("Solo para administradores.");
        }
    }
}
