package kuchma.mike.bbdm.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersionConfiguration implements WebMvcConfigurer {
    private static final String API_PREFIX = "/api";

    private static final String DEFAULT_FRONTEND_VERSION = "1.0";

    @Override
    public void configureApiVersioning(final ApiVersionConfigurer configurer) {
        configurer.useVersionResolver(request -> {
            final String path = request.getRequestURI().substring(request.getContextPath().length());
            if (API_PREFIX.equals(path) || path.startsWith(API_PREFIX + "/")) {
                final String headerVersion = request.getHeader("API-Version");
                return headerVersion == null && path.endsWith("/events")
                    ? request.getParameter("apiVersion")
                    : headerVersion;
            }
            return DEFAULT_FRONTEND_VERSION;
        });
    }
}
