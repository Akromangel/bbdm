package kuchma.mike.bbdm.application;

import java.util.List;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {
    private static final String GAME_ID = "game-1";

    @Test
    void shouldRecordTheGatewayInteractionForMessageResolution() {
        final RecordingGameGateway gateway = new RecordingGameGateway();
        final GameService gameService = new GameService(gateway);

        gameService.dealWithMessage(new GameId(GAME_ID), new MessageId("message-1"));

        assertThat(gateway.interaction())
            .returns(GAME_ID, GatewayInteraction::gameId)
            .returns("message-1", GatewayInteraction::messageId);
    }

    @Test
    void shouldReturnTheResolvedMessageOutcome() {
        final RecordingGameGateway gateway = new RecordingGameGateway();
        final GameService gameService = new GameService(gateway);

        final MessageResolution result =
            gameService.dealWithMessage(
                new GameId(GAME_ID),
                new MessageId("message-1")
            );

        assertThat(result)
            .returns(true, MessageResolution::success)
            .returns(2, MessageResolution::lives)
            .returns(42L, MessageResolution::gold)
            .returns(8, MessageResolution::score)
            .returns(12, MessageResolution::highScore)
            .returns(3, MessageResolution::turn)
            .returns("Result", MessageResolution::message);
    }

    private record GatewayInteraction(String gameId, String messageId) {
    }

    private static final class RecordingGameGateway implements GameGateway {
        private GatewayInteraction interaction;

        @Override
        public StartedGame startGame() {
            return new StartedGame(GAME_ID, 3, 10, 1, 0, 0, 0);
        }

        @Override
        public MessageBoard getMessages(final GameId gameId) {
            return new MessageBoard(List.of());
        }

        @Override
        public MessageResolution dealWithMessage(final GameId gameId, final MessageId messageId) {
            interaction = new GatewayInteraction(gameId.value(), messageId.value());
            return new MessageResolution(true, 2, 42, 8, 12, 3, "Result");
        }

        @Override
        public ShopInventory getShop(final GameId gameId) {
            return new ShopInventory(List.of());
        }

        @Override
        public ItemPurchaseResult buyItem(final GameId gameId, final ShopItemId itemId) {
            return new ItemPurchaseResult(true, 5, 3, 1, 2);
        }

        private GatewayInteraction interaction() {
            return interaction;
        }
    }
}
