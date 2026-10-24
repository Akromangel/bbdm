package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.application.MessageResolutionRejectedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MessageResolutionRejectedException.class)
    public ResponseEntity<ProblemDetail> handleRejectedMessage(
        final MessageResolutionRejectedException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.getMessage()
        );
        problem.setTitle("Message could not be resolved");
        problem.setProperty("messageId", exception.messageId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(HttpClientErrorException.Gone.class)
    public ResponseEntity<ProblemDetail> handleGameOver(
        final HttpClientErrorException.Gone exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            exception.getStatusCode(),
            "Game Over"
        );
        problem.setTitle("Game Over");
        return ResponseEntity.status(exception.getStatusCode())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
}
