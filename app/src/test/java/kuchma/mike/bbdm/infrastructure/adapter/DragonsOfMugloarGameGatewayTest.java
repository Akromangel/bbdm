package kuchma.mike.bbdm.infrastructure.adapter;

import java.util.List;
import java.nio.charset.StandardCharsets;

import kuchma.mike.bbdm.application.MessageResolutionRejectedException;
import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.infrastructure.config.MessageFilterProperties;
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
    private static final List<String> FILTERED_PROBABILITIES = List.of("Impossible", "Suicide mission");

    private final DragonsOfMugloarRestClient restClient = mock(DragonsOfMugloarRestClient.class);

    @AfterEach
    void shouldNotHaveUnexpectedRestClientInteractions() {
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void shouldSortMessagesByExpirationAscendingAndThenRewardDescending() {
        final GameMessage lowerRewardSameExpiration =
            new GameMessage(new MessageId("ad-1"), "Lower reward", 5, 4, "Umlza3k=", 0);
        final GameMessage highestReward =
            new GameMessage(new MessageId("ad-2"), "Highest reward", 10, 4, "Umlza3k=", 0);
        final GameMessage longestExpiration =
            new GameMessage(new MessageId("ad-3"), "Longest expiration", 1, 7, "Umlza3k=", 0);
        final GameMessage shorterExpiration =
            new GameMessage(new MessageId("ad-4"), "Shorter expiration", 100, 2, "Umlza3k=", 0);
        when(restClient.getMessages(GAME_ID)).thenReturn(
            List.of(lowerRewardSameExpiration, shorterExpiration, highestReward, longestExpiration)
        );
        final DragonsOfMugloarGameGateway gateway =
            new DragonsOfMugloarGameGateway(restClient, new MessageFilterProperties(FILTERED_PROBABILITIES));

        final List<GameMessage> messages = gateway.getMessages(GAME_ID).messages();
        verify(restClient).getMessages(GAME_ID);

        assertThat(messages)
            .containsExactly(shorterExpiration, highestReward, lowerRewardSameExpiration, longestExpiration);
    }

    @Test
    void shouldFilterMessagesWithImpossibleProbability() {
        final GameMessage safeMessage =
            new GameMessage(new MessageId("ad-1"), "Safe task", 10, 4, "Umlza3k=", 0);
        final GameMessage alsoAcceptedMessage =
            new GameMessage(new MessageId("ad-4"), "Also accepted task", 5, 5, "U3VyZSB0aGluZw==", 0);
        final GameMessage detrimentalMessage =
            new GameMessage(new MessageId("ad-2"), "Detrimental task", 20, 3, "UmF0aGVyIGRldHJpbWVudGFs", 0);
        final GameMessage suicideMessage =
            new GameMessage(new MessageId("YWRfMw=="), "U3VpY2lkZSB0YXNr", 30, 2, "U3VpY2lkZSBtaXNzaW9u", 1);
        final GameMessage impossibleMessage =
            new GameMessage(new MessageId("YWRfNg=="), "SW1wb3NzaWJsZSB0YXNr", 50, 6, "SW1wb3NzaWJsZQ==", 1);
        final GameMessage unknownProbability =
            new GameMessage(new MessageId("ad-7"), "Unknown task", 5, 7, "NzUl", 0);
        final GameMessage missingProbability =
            new GameMessage(new MessageId("ad-5"), "Missing probability", 40, 1, null, 0);
        when(restClient.getMessages(GAME_ID)).thenReturn(
            List.of(
                safeMessage,
                detrimentalMessage,
                suicideMessage,
                alsoAcceptedMessage,
                impossibleMessage,
                unknownProbability,
                missingProbability
            )
        );
        final DragonsOfMugloarGameGateway gateway =
            new DragonsOfMugloarGameGateway(restClient, new MessageFilterProperties(FILTERED_PROBABILITIES));

        final List<GameMessage> messages = gateway.getMessages(GAME_ID).messages();
        verify(restClient).getMessages(GAME_ID);

        assertThat(messages).containsExactly(
            missingProbability,
            detrimentalMessage,
            safeMessage,
            alsoAcceptedMessage,
            unknownProbability
        );
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
        final DragonsOfMugloarGameGateway gateway =
            new DragonsOfMugloarGameGateway(restClient, new MessageFilterProperties(FILTERED_PROBABILITIES));

        assertThatThrownBy(() -> gateway.dealWithMessage(GAME_ID, new MessageId("ad-1")))
            .isInstanceOfSatisfying(
                MessageResolutionRejectedException.class,
                exception -> assertThat(exception.messageId()).isEqualTo("ad-1")
            );
        verify(restClient).dealWithMessage(GAME_ID, new MessageId("ad-1"));
    }
}
