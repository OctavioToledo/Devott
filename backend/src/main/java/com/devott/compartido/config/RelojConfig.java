package com.devott.compartido.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    /** Reloj inyectable, para poder fijar la fecha en los tests. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
