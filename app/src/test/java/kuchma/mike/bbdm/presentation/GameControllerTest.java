package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.application.GameService;
import kuchma.mike.bbdm.application.MessageResolutionRejectedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

class GameControllerTest {
    private final GameService gameService = mock(GameService.class);

    @AfterEach
    void shouldNotHaveUnexpectedGameServiceInteractions() {
        verifyNoMoreInteractions(gameService);
    }

    @Test
    void shouldReturnAProblemResponseForRejectedMessages() {
        final GameController controller = new GameController(gameService);

        assertThat(controller.handleRejectedMessage(new MessageResolutionRejectedException("ad-1")))
            .satisfies(response -> {
                assertThat(response.getStatusCode().value()).isEqualTo(400);
                assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_PROBLEM_JSON);
                assertThat(response.getBody())
                    .returns("Message could not be resolved", problem -> problem.getTitle())
                    .returns("ad-1", problem -> problem.getProperties().get("messageId"));
            });
    }
}
