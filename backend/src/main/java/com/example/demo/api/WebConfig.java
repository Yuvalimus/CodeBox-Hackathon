package com.example.demo.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] origins;

    public WebConfig(@Value("${app.cors-origins}") String origings) {
        this.origins = Arrays.stream(origings.split(",")).map(String::trim).toArray(String[]::new);
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/**")
            .allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE")
            .allowedHeaders("Authorization", "Content-Type");
    }
}
