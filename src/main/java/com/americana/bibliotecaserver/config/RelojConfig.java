package com.americana.bibliotecaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
