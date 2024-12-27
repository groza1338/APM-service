package dev.sc.apm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMPServiceConfig {
    @Bean
    public int defaultPageSize() {
        return 10;
    }

    @Bean
    public int maxNameLength() {
        return 64;
    }

    @Bean
    public int passportLength() {
        return 10;
    }

    @Bean
    public int addressLength() {
        return 128;
    }

    @Bean
    public int phoneLength() {
        return 15;
    }

    @Bean
    public String phonePattern() {
        return "^\\+?\\d{11}$";
    }

    @Bean
    public String namePattern() {
        return "^[A-Z][a-z]*(?:[ '-][A-Za-z]+)*$";
    }

    @Bean
    public String passportPattern() {
        return "^\\d{10}$";
    }

    @Bean
    public int organizationNameLength() {
        return 96;
    }

    @Bean
    public int positionLength() {
        return 64;
    }
}
