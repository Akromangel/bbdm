package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.application.GameService;
import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.StartedGame;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", version = GameController.API_VERSION)
public class GameController {
    public static final String API_VERSION = "1.0";

    private static final String GAME_ID_PATH_VARIABLE = "gameId";

    private static final String MESSAGE_ID_PATH_VARIABLE = "messageId";

    private static final String ITEM_ID_PATH_VARIABLE = "itemId";

    private final GameService gameService;

    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/game")
    public StartedGame startGame() {
        return gameService.startGame();
    }

    @GetMapping("/{" + GAME_ID_PATH_VARIABLE + "}/messages")
    public MessageBoard getMessages(@PathVariable(GAME_ID_PATH_VARIABLE) final GameId gameId) {
        return gameService.getMessages(gameId);
    }

    @PostMapping("/{" + GAME_ID_PATH_VARIABLE + "}/messages/{" + MESSAGE_ID_PATH_VARIABLE + "}")
    public MessageResolution dealWithMessage(
        @PathVariable(GAME_ID_PATH_VARIABLE) final GameId gameId,
        @PathVariable(MESSAGE_ID_PATH_VARIABLE) final MessageId messageId
    ) {
        return gameService.dealWithMessage(gameId, messageId);
    }

    @GetMapping("/{" + GAME_ID_PATH_VARIABLE + "}/shop")
    public ShopInventory getShop(@PathVariable(GAME_ID_PATH_VARIABLE) final GameId gameId) {
        return gameService.getShop(gameId);
    }

    @PostMapping("/{" + GAME_ID_PATH_VARIABLE + "}/shop/{" + ITEM_ID_PATH_VARIABLE + "}")
    public ItemPurchaseResult buyItem(
        @PathVariable(GAME_ID_PATH_VARIABLE) final GameId gameId,
        @PathVariable(ITEM_ID_PATH_VARIABLE) final String itemId
    ) {
        return gameService.buyItem(gameId, itemId);
    }
}
