package kuchma.mike.bbdm.application;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;

public class GameService {
    private final GameGateway gameGateway;

    public GameService(GameGateway gameGateway) {
        this.gameGateway = gameGateway;
    }

    public StartedGame startGame() {
        return gameGateway.startGame();
    }

    public MessageBoard getMessages(GameId gameId) {
        return gameGateway.getMessages(gameId);
    }

    public MessageResolution dealWithMessage(GameId gameId, MessageId messageId, DealMessageCommand command) {
        MessageDecision decision = MessageDecision.fromAction(command.action());
        return gameGateway.dealWithMessage(gameId, messageId, decision);
    }

    public ShopInventory getShop(GameId gameId) {
        return gameGateway.getShop(gameId);
    }

    public ItemPurchaseResult buyItem(GameId gameId, String itemId) {
        return gameGateway.buyItem(gameId, new ShopItemId(itemId));
    }
}
