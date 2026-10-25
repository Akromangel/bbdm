package kuchma.mike.bbdm.application;

import java.util.Comparator;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;

public class GameService {
    private static final Comparator<GameMessage> DEFAULT_MESSAGE_COMPARATOR =
        Comparator.comparingInt(GameMessage::expiresIn)
            .thenComparing(Comparator.comparingInt(GameMessage::reward).reversed());

    private final GameGateway gameGateway;

    public GameService(final GameGateway gameGateway) {
        this.gameGateway = gameGateway;
    }

    public StartedGame startGame() {
        return gameGateway.startGame();
    }

    public MessageBoard getMessages(final GameId gameId) {
        return getMessages(gameId, DEFAULT_MESSAGE_COMPARATOR);
    }

    public MessageBoard getMessages(
        final GameId gameId,
        final Comparator<GameMessage> comparator
    ) {
        return gameGateway.getMessages(gameId, comparator);
    }

    public MessageResolution dealWithMessage(final GameId gameId, final MessageId messageId) {
        return gameGateway.dealWithMessage(gameId, messageId);
    }

    public ShopInventory getShop(final GameId gameId) {
        return gameGateway.getShop(gameId);
    }

    public ItemPurchaseResult buyItem(final GameId gameId, final String itemId) {
        return gameGateway.buyItem(gameId, new ShopItemId(itemId));
    }
}
