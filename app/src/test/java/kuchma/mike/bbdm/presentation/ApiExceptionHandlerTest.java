package kuchma.mike.bbdm.presentation;

import java.nio.charset.StandardCharsets;

import kuchma.mike.bbdm.application.MessageResolutionRejectedException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void shouldReturnAProblemResponseForRejectedMessages() {
        final ResponseEntity<ProblemDetail> response = exceptionHandler.handleRejectedMessage(
            new MessageResolutionRejectedException("ad-1")
        );

        assertThat(response.getBody())
            .returns(HttpStatus.BAD_REQUEST.value(), ProblemDetail::getStatus)
            .returns("Message could not be resolved", ProblemDetail::getTitle)
            .returns("ad-1", problem -> problem.getProperties().get("messageId"));
    }

    @Test
    void shouldReturnAGoneProblemResponseForGameOver() {
        final HttpClientErrorException.Gone exception = (HttpClientErrorException.Gone)
            HttpClientErrorException.create(
                HttpStatus.GONE,
                "Gone",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
            );

        final ResponseEntity<ProblemDetail> response = exceptionHandler.handleGameOver(exception);

        assertThat(response)
            .returns(HttpStatus.GONE, ResponseEntity::getStatusCode)
            .returns(APPLICATION_PROBLEM_JSON, result -> result.getHeaders().getContentType())
            .satisfies(result -> assertThat(result.getBody())
                .returns(HttpStatus.GONE.value(), ProblemDetail::getStatus)
                .returns("Game Over", ProblemDetail::getTitle)
                .returns("Game Over", ProblemDetail::getDetail));
    }
}
