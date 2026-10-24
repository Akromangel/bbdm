package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.application.DealMessageCommand;
import kuchma.mike.bbdm.application.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/game")
    public Object startGame() {
        return gameService.startGame();
    }

    @GetMapping("/{" + GAME_ID_PATH_VARIABLE + "}/messages")
    public Object getMessages(@PathVariable(GAME_ID_PATH_VARIABLE) String gameId) {
        return gameService.getMessages(gameId);
    }

    @PostMapping("/{" + GAME_ID_PATH_VARIABLE + "}/messages/{" + MESSAGE_ID_PATH_VARIABLE + "}")
    public Object dealWithMessage(
        @PathVariable(GAME_ID_PATH_VARIABLE) String gameId,
        @PathVariable(MESSAGE_ID_PATH_VARIABLE) String messageId,
        @RequestBody DealMessageCommand command
    ) {
        return gameService.dealWithMessage(gameId, messageId, command);
    }

    @GetMapping("/{" + GAME_ID_PATH_VARIABLE + "}/shop")
    public Object getShop(@PathVariable(GAME_ID_PATH_VARIABLE) String gameId) {
        return gameService.getShop(gameId);
    }

    @PostMapping("/{" + GAME_ID_PATH_VARIABLE + "}/shop/{" + ITEM_ID_PATH_VARIABLE + "}")
    public Object buyItem(
        @PathVariable(GAME_ID_PATH_VARIABLE) String gameId,
        @PathVariable(ITEM_ID_PATH_VARIABLE) String itemId
    ) {
        return gameService.buyItem(gameId, itemId);
    }
}
