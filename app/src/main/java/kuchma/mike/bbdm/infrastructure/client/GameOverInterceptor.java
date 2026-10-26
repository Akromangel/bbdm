package kuchma.mike.bbdm.infrastructure.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import kuchma.mike.bbdm.application.GameOverException;
import kuchma.mike.bbdm.application.MessageResolutionRejectedException;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;

public class GameOverInterceptor implements ClientHttpRequestInterceptor {
    private static HttpClientErrorException createCause(final ClientHttpResponse response) throws IOException {
        try (response) {
            return HttpClientErrorException.create(
                response.getStatusCode(),
                response.getStatusText(),
                response.getHeaders(),
                response.getBody().readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
    }

    private static @Nullable String messageIdFrom(final String path) {
        final String resolutionPathMarker = "/solve/";
        final int markerIndex = path.lastIndexOf(resolutionPathMarker);
        if (markerIndex < 0) {
            return null;
        }
        final String messageId = path.substring(markerIndex + resolutionPathMarker.length());
        return messageId.isEmpty() || messageId.contains("/") ? null : messageId;
    }

    @Override
    public ClientHttpResponse intercept(
        final HttpRequest request,
        final byte[] body,
        final ClientHttpRequestExecution execution
    ) throws IOException {
        final ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().isSameCodeAs(HttpStatus.GONE)
            || response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            throw new GameOverException(createCause(response));
        }
        if (response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            final String messageId = messageIdFrom(request.getURI().getPath());
            if (messageId != null) {
                throw new MessageResolutionRejectedException(messageId, createCause(response));
            }
        }
        return response;
    }
}
