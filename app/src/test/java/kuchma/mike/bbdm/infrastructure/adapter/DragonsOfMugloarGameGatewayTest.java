package kuchma.mike.bbdm.infrastructure.adapter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kuchma.mike.bbdm.application.MessageFilterProperties;
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
    private static final String MESSAGE_ID = "ad-1";

    private static final String BASE64_RISKY_PROBABILITY = "Umlza3k=";

    private static final GameId GAME_ID = new GameId("game-1");

    private static final List<String> FILTERED_PROBABILITIES = List.of("Impossible", "Suicide mission");

    private static final List<String> PROBABILITY_ORDER = List.of(
        "Piece of cake",
        "Walk in the park",
        "Sure thing",
        "Quite likely",
        "Hmmm...",
        "Gamble",
        "Rather detrimental",
        "Suicide mission",
        "Impossible"
    );

    private final DragonsOfMugloarRestClient restClient = mock(DragonsOfMugloarRestClient.class);

    @AfterEach
    void shouldNotHaveUnexpectedRestClientInteractions() {
        verifyNoMoreInteractions(restClient);
    }

    @Test
    void shouldSortMessagesByExpirationAscendingAndThenRewardDescending() {
        final GameMessage lowerRewardSameExpiration =
            new GameMessage(new MessageId(MESSAGE_ID), "Lower reward", 5, 4, BASE64_RISKY_PROBABILITY, 0);
        final GameMessage highestReward =
            new GameMessage(new MessageId("ad-2"), "Highest reward", 10, 4, BASE64_RISKY_PROBABILITY, 0);
        final GameMessage longestExpiration =
            new GameMessage(new MessageId("ad-3"), "Longest expiration", 1, 7, BASE64_RISKY_PROBABILITY, 0);
        final GameMessage shorterExpiration =
            new GameMessage(new MessageId("ad-4"), "Shorter expiration", 100, 2, BASE64_RISKY_PROBABILITY, 0);
        when(restClient.getMessages(GAME_ID)).thenReturn(
            List.of(lowerRewardSameExpiration, shorterExpiration, highestReward, longestExpiration)
        );
        final DragonsOfMugloarGameGateway gateway =
            new DragonsOfMugloarGameGateway(
                restClient,
                new MessageFilterProperties(FILTERED_PROBABILITIES, PROBABILITY_ORDER)
            );

        final List<GameMessage> messages = gateway.getMessages(GAME_ID, defaultComparator()).messages();
        verify(restClient).getMessages(GAME_ID);

        assertThat(messages)
            .containsExactly(shorterExpiration, highestReward, lowerRewardSameExpiration, longestExpiration);
    }

    @Test
    void shouldFilterMessagesWithImpossibleProbability() {
        final GameMessage safeMessage =
            new GameMessage(new MessageId(MESSAGE_ID), "Safe task", 10, 4, BASE64_RISKY_PROBABILITY, 0);
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
            new DragonsOfMugloarGameGateway(
                restClient,
                new MessageFilterProperties(FILTERED_PROBABILITIES, PROBABILITY_ORDER)
            );

        final List<GameMessage> messages = gateway.getMessages(GAME_ID, defaultComparator()).messages();
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
    void shouldSortByConfiguredProbabilityBeforeRewardWhenExpirationMatches() {
        final GameMessage walkInThePark =
            new GameMessage(new MessageId("prob-walk"), "Walk", 10, 2, "Walk in the park", 0);
        final GameMessage pieceOfCake =
            new GameMessage(new MessageId("prob-cake"), "Cake", 1, 2, "Piece of cake", 0);
        final GameMessage sureThing =
            new GameMessage(new MessageId("prob-sure"), "Sure", 100, 2, "Sure thing", 0);
        when(restClient.getMessages(GAME_ID)).thenReturn(List.of(walkInThePark, sureThing, pieceOfCake));
        final DragonsOfMugloarGameGateway gateway =
            new DragonsOfMugloarGameGateway(
                restClient,
                new MessageFilterProperties(FILTERED_PROBABILITIES, PROBABILITY_ORDER)
            );

        final List<GameMessage> messages = gateway.getMessages(GAME_ID, probabilityComparator()).messages();
        verify(restClient).getMessages(GAME_ID);

        assertThat(messages).containsExactly(pieceOfCake, walkInThePark, sureThing);
    }

    private static Comparator<GameMessage> defaultComparator() {
        return Comparator.comparingInt(GameMessage::expiresIn)
            .thenComparing(Comparator.comparingInt(GameMessage::reward).reversed());
    }

    private static Comparator<GameMessage> probabilityComparator() {
        Map<String, Integer> ranks = new HashMap<>();
        for (int index = 0; index < PROBABILITY_ORDER.size(); index++) {
            ranks.put(PROBABILITY_ORDER.get(index), index);
        }
        return Comparator.comparingInt(GameMessage::expiresIn)
            .thenComparingInt(message -> message.probability() == null
                ? Integer.MAX_VALUE
                : ranks.getOrDefault(message.probability(), Integer.MAX_VALUE))
            .thenComparing(Comparator.comparingInt(GameMessage::reward).reversed());
    }
}
