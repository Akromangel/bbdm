package kuchma.mike.bbdm.application;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.port.GameGateway;

public class GameService {
    private final GameGateway gameGateway;

    public GameService(GameGateway gameGateway) {
        this.gameGateway = gameGateway;
    }

    public Object startGame() {
        return gameGateway.startGame();
    }

    public Object getMessages(String gameId) {
        return gameGateway.getMessages(new GameId(gameId));
    }

    public Object dealWithMessage(String gameId, String messageId, DealMessageCommand command) {
        MessageDecision decision = MessageDecision.fromAction(command.action());
        return gameGateway.dealWithMessage(new GameId(gameId), new MessageId(messageId), decision);
    }

    public Object getShop(String gameId) {
        return gameGateway.getShop(new GameId(gameId));
    }

    public Object buyItem(String gameId, String itemId) {
        return gameGateway.buyItem(new GameId(gameId), new ShopItemId(itemId));
    }
}
