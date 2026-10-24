package kuchma.mike.bbdm.infrastructure.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class UpstreamLoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(UpstreamLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {
        if (!log.isDebugEnabled()) {
            return execution.execute(request, body);
        }

        log.atDebug()
            .addKeyValue("method", request.getMethod())
            .addKeyValue("uri", request.getURI())
            .addKeyValue("body", () -> bodyAsString(body))
            .setMessage("Upstream request")
            .log();

        ClientHttpResponse response = execution.execute(request, body);
        byte[] responseBody;
        try {
            responseBody = response.getBody().readAllBytes();
            log.atDebug()
                .addKeyValue("method", request.getMethod())
                .addKeyValue("uri", request.getURI())
                .addKeyValue("status", response.getStatusCode().value())
                .addKeyValue("body", () -> bodyAsString(responseBody))
                .setMessage("Upstream response")
                .log();
        } catch (IOException exception) {
            response.close();
            throw exception;
        }

        return response;
    }

    private static String bodyAsString(byte[] body) {
        return body.length == 0 ? "(empty)" : new String(body, StandardCharsets.UTF_8);
    }
}
