package kuchma.mike.bbdm.domain.port;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;

public interface GameGateway {
    StartedGame startGame();

    MessageBoard getMessages(GameId gameId);

    MessageResolution dealWithMessage(GameId gameId, MessageId messageId);

    ShopInventory getShop(GameId gameId);

    ItemPurchaseResult buyItem(GameId gameId, ShopItemId itemId);
}
