package id.raisal.taskmanager.common.config;

import id.raisal.taskmanager.common.error.ErrorWriter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Lets the browser frontend call the API from its own origin (R18, A15).
 * It is a servlet filter, so that also a rejected origin gets the one error body.
 */
@Configuration
public class CorsConfig {

    /** allowedOrigins is a comma-separated list. It comes from APP_CORS_ALLOWED_ORIGINS, see application.properties. */
    @Bean
    CorsFilter corsFilter(@Value("${app.cors.allowed-origins}") String[] allowedOrigins, ErrorWriter errors) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        CorsFilter filter = new CorsFilter(source);
        filter.setCorsProcessor(new ProblemDetailCorsProcessor(errors));
        return filter;
    }
}
