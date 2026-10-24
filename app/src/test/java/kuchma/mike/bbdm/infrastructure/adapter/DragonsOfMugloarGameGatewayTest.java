package kuchma.mike.bbdm.infrastructure.adapter;

import java.util.List;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
}
