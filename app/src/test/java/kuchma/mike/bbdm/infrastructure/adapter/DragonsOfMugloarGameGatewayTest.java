package kuchma.mike.bbdm.infrastructure.adapter;

import java.util.List;
import java.nio.charset.StandardCharsets;

import kuchma.mike.bbdm.application.MessageResolutionRejectedException;
import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DragonsOfMugloarGameGatewayTest {
    private static final GameId GAME_ID = new GameId("game-1");

    private final DragonsOfMugloarRestClient restClient = mock(DragonsOfMugloarRestClient.class);

    @AfterEach
    void shouldNotHaveUnexpectedRestClientInteractions() {
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void shouldSortMessagesByExpirationAscendingAndThenRewardDescending() {
        final GameMessage lowerRewardSameExpiration =
            new GameMessage(new MessageId("ad-1"), "Lower reward", 5, 4, null);
        final GameMessage highestReward =
            new GameMessage(new MessageId("ad-2"), "Highest reward", 10, 4, null);
        final GameMessage longestExpiration =
            new GameMessage(new MessageId("ad-3"), "Longest expiration", 1, 7, null);
        final GameMessage shorterExpiration =
            new GameMessage(new MessageId("ad-4"), "Shorter expiration", 100, 2, null);
        when(restClient.getMessages(GAME_ID)).thenReturn(
            List.of(lowerRewardSameExpiration, shorterExpiration, highestReward, longestExpiration)
        );
        final DragonsOfMugloarGameGateway gateway = new DragonsOfMugloarGameGateway(restClient);

        final List<GameMessage> messages = gateway.getMessages(GAME_ID).messages();
        verify(restClient).getMessages(GAME_ID);

        assertThat(messages)
            .containsExactly(shorterExpiration, highestReward, lowerRewardSameExpiration, longestExpiration);
    }

    @Test
    void shouldTranslateAnUpstreamBadRequestWhenResolvingAMessage() {
        when(restClient.dealWithMessage(GAME_ID, new MessageId("ad-1"))).thenThrow(
            HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                "<html><body><pre>Bad Request</pre></body></html>".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            )
        );
        final DragonsOfMugloarGameGateway gateway = new DragonsOfMugloarGameGateway(restClient);

        assertThatThrownBy(() -> gateway.dealWithMessage(GAME_ID, new MessageId("ad-1")))
            .isInstanceOfSatisfying(
                MessageResolutionRejectedException.class,
                exception -> assertThat(exception.messageId()).isEqualTo("ad-1")
            );
        verify(restClient).dealWithMessage(GAME_ID, new MessageId("ad-1"));
    }
}
