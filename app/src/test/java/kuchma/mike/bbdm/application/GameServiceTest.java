package kuchma.mike.bbdm.application;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.port.GameGateway;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameServiceTest {
    private static final String GAME_ID = "game-1";

    @Test
    void dealsWithMessagesThroughTheDomainPort() {
        RecordingGameGateway gateway = new RecordingGameGateway();
        GameService gameService = new GameService(gateway);

        Object result = gameService.dealWithMessage(GAME_ID, "message-1", new DealMessageCommand("true"));

        assertEquals(GAME_ID, gateway.gameId);
        assertEquals("message-1", gateway.messageId);
        assertEquals(new MessageDecision(true), gateway.decision);
        assertEquals(Map.of("gold", 42), result);
    }

    @Test
    void rejectsInvalidMessageDecisionsBeforeCallingTheGateway() {
        RecordingGameGateway gateway = new RecordingGameGateway();
        GameService gameService = new GameService(gateway);

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> gameService.dealWithMessage("game-1", "message-1", new DealMessageCommand("maybe"))
        );
        assertNull(gateway.messageId);
    }

    private static final class RecordingGameGateway implements GameGateway {
        private String gameId;
        private String messageId;
        private MessageDecision decision;

        @Override
        public Object startGame() {
            return Map.of("gameId", GAME_ID);
        }

        @Override
        public Object getMessages(GameId gameId) {
            this.gameId = gameId.value();
            return List.of();
        }

        @Override
        public Object dealWithMessage(GameId gameId, MessageId messageId, MessageDecision decision) {
            this.gameId = gameId.value();
            this.messageId = messageId.value();
            this.decision = decision;
            return Map.of("gold", 42);
        }

        @Override
        public Object getShop(GameId gameId) {
            return List.of();
        }

        @Override
        public Object buyItem(GameId gameId, ShopItemId itemId) {
            return Map.of();
        }
    }
}
