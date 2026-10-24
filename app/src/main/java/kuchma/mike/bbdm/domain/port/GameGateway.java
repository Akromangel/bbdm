package kuchma.mike.bbdm.domain.port;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.ShopItemId;

public interface GameGateway {
    Object startGame();

    Object getMessages(GameId gameId);

    Object dealWithMessage(GameId gameId, MessageId messageId, MessageDecision decision);

    Object getShop(GameId gameId);

    Object buyItem(GameId gameId, ShopItemId itemId);
}
