package kuchma.mike.bbdm.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersionConfiguration implements WebMvcConfigurer {
    private static final String API_PREFIX = "/api";

    private static final String DEFAULT_FRONTEND_VERSION = "1.0";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useVersionResolver(request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return path.equals(API_PREFIX) || path.startsWith(API_PREFIX + "/")
                ? request.getHeader("API-Version")
                : DEFAULT_FRONTEND_VERSION;
        });
    }
}
