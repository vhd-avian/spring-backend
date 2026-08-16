package com.issuetracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

/** Serves uploaded attachment files at /api/v1/files/**. */
@Configuration
public class StaticFilesConfig implements WebMvcConfigurer {

  @Value("${app.uploads.dir:./uploads}")
  private String uploadsDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/api/v1/files/**")
        .addResourceLocations(Paths.get(uploadsDir).toAbsolutePath().toUri().toString());
  }
}
