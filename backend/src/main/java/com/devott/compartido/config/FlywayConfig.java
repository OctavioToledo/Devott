package com.devott.compartido.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class FlywayConfig {

    /**
     * Después de migrar, activa RLS en la tabla de historial de Flyway para que la API automática
     * de Supabase no la exponga. No se puede hacer desde una migración porque Flyway la tiene bloqueada.
     */
    @Bean
    FlywayMigrationStrategy migrarYProtegerHistorial() {
        return flyway -> {
            flyway.migrate();
            activarRlsEnHistorial(flyway);
        };
    }

    private static void activarRlsEnHistorial(Flyway flyway) {
        var config = flyway.getConfiguration();
        String tabla = "\"" + config.getTable() + "\"";
        if (config.getDefaultSchema() != null) {
            tabla = "\"" + config.getDefaultSchema() + "\"." + tabla;
        }
        new JdbcTemplate(config.getDataSource())
                .execute("ALTER TABLE " + tabla + " ENABLE ROW LEVEL SECURITY");
    }
}
