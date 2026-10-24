package kuchma.mike.bbdm.infrastructure.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/api/v2", accept = MediaType.APPLICATION_JSON_VALUE)
public interface DragonsOfMugloarRestClient {
    String GAME_ID_PATH_VARIABLE = "gameId";
    String AD_ID_PATH_VARIABLE = "adId";
    String ITEM_ID_PATH_VARIABLE = "itemId";

    @PostExchange("/game/start")
    Object startGame();

    @GetExchange("/{" + GAME_ID_PATH_VARIABLE + "}/messages")
    Object getMessages(@PathVariable(GAME_ID_PATH_VARIABLE) String gameId);

    @PostExchange(
        url = "/{" + GAME_ID_PATH_VARIABLE + "}/solve/{" + AD_ID_PATH_VARIABLE + "}",
        contentType = MediaType.APPLICATION_JSON_VALUE
    )
    Object dealWithMessage(
        @PathVariable(GAME_ID_PATH_VARIABLE) String gameId,
        @PathVariable(AD_ID_PATH_VARIABLE) String adId,
        @RequestBody DealMessageRequest request
    );

    @GetExchange("/{" + GAME_ID_PATH_VARIABLE + "}/shop")
    Object getShop(@PathVariable(GAME_ID_PATH_VARIABLE) String gameId);

    @PostExchange("/{" + GAME_ID_PATH_VARIABLE + "}/shop/buy/{" + ITEM_ID_PATH_VARIABLE + "}")
    Object buyItem(
        @PathVariable(GAME_ID_PATH_VARIABLE) String gameId,
        @PathVariable(ITEM_ID_PATH_VARIABLE) String itemId
    );

    record DealMessageRequest(String action) {
    }
}
