package kuchma.mike.bbdm.infrastructure.client;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.Reputation;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.StartedGame;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@HttpExchange(url = "/api/v2", accept = APPLICATION_JSON_VALUE)
public interface DragonsOfMugloarRestClient {
    String GAME_ID_PATH_VARIABLE = "gameId";
    String MESSAGE_ID_PATH_VARIABLE = "adId";
    String ITEM_ID_PATH_VARIABLE = "itemId";

    @PostExchange("/game/start")
    StartedGame startGame();

    @GetExchange("/{" + GAME_ID_PATH_VARIABLE + "}/investigate/reputation")
    Reputation getReputation(@PathVariable("gameId") GameId gameId);

    @GetExchange("/{" + GAME_ID_PATH_VARIABLE + "}/messages")
    MessageBoard getMessages(@PathVariable(GAME_ID_PATH_VARIABLE) GameId gameId);

    @PostExchange(
        url = "/{" + GAME_ID_PATH_VARIABLE + "}/solve/{" + MESSAGE_ID_PATH_VARIABLE + "}",
        contentType = APPLICATION_JSON_VALUE
    )
    MessageResolution dealWithMessage(
        @PathVariable(GAME_ID_PATH_VARIABLE) GameId gameId,
        @PathVariable(MESSAGE_ID_PATH_VARIABLE) MessageId messageId,
        @RequestBody MessageDecision decision
    );

    @GetExchange("/{" + GAME_ID_PATH_VARIABLE + "}/shop")
    ShopInventory getShop(@PathVariable(GAME_ID_PATH_VARIABLE) GameId gameId);

    @PostExchange("/{" + GAME_ID_PATH_VARIABLE + "}/shop/buy/{" + ITEM_ID_PATH_VARIABLE + "}")
    ItemPurchaseResult buyItem(
        @PathVariable(GAME_ID_PATH_VARIABLE) GameId gameId,
        @PathVariable(ITEM_ID_PATH_VARIABLE) ItemId itemId
    );
}
