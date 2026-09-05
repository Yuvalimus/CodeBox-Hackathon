package com.example.demo.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] origins;
    private final String profilePictureLocation;

    public WebConfig(@Value("${app.cors-origins}") String configuredOrigins,
                     @Value("${app.upload-dir:uploads/profile-pictures}") String uploadDirectory) {
        this.origins = Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        String location = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
        this.profilePictureLocation = location.endsWith("/") ? location : location + "/";
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/**")
            .allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile-pictures/**").addResourceLocations(profilePictureLocation);
    }
}
