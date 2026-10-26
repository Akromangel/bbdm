package kuchma.mike.bbdm.application;

import java.util.ArrayList;
import java.util.List;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.ItemId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItem;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ScriptingAdventureServiceTest {
    private static final String SESSION_ID = "session-1";
    private static final String PIECE_OF_CAKE = "Piece of cake";

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @AfterEach
    void shouldNotHaveUnexpectedEventPublisherInteractions() {
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void shouldBuyHpotAtOneLifeAndPublishTheUpdatedState() {
        final RecordingGameGateway gateway = new RecordingGameGateway();
        final ScriptingAdventureService service =
            new ScriptingAdventureService(
                new GameService(gateway),
                new UpgradePurchaseService(
                    new GameService(gateway),
                    new ScriptingAdventureProperties(1, 0, 4, 2)
                ),
                new ScriptingAdventureProperties(1, 0, 4, 2),
                new MessageFilterProperties(List.of(), List.of(PIECE_OF_CAKE)),
                eventPublisher
            );

        service.startSession(SESSION_ID);

        final org.mockito.ArgumentCaptor<Object> eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).containsExactly(
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(10)
                .score(12)
                .lives(1)
                .level(2)
                .turn(4)
                .hpotCount(0)
                .task("")
                .message("Adventure started")
                .complete(false)
                .error("")
                .build(),
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(8)
                .score(25)
                .lives(0)
                .level(2)
                .turn(5)
                .hpotCount(1)
                .task("Help the village")
                .message("Bought Healing pot. Quest completed.")
                .complete(true)
                .error("")
                .build()
        );
    }

    @Test
    void shouldSkipRejectedMessageAndContinueWithTheNextMessage() {
        final RecordingGameGateway gateway = new RecordingGameGateway(true);
        final ScriptingAdventureService service =
            new ScriptingAdventureService(
                new GameService(gateway),
                new UpgradePurchaseService(
                    new GameService(gateway),
                    new ScriptingAdventureProperties(1, 0, 4, 2)
                ),
                new ScriptingAdventureProperties(1, 0, 4, 2),
                new MessageFilterProperties(List.of(), List.of(PIECE_OF_CAKE)),
                eventPublisher
            );

        service.startSession(SESSION_ID);

        final org.mockito.ArgumentCaptor<Object> eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).containsExactly(
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(10)
                .score(12)
                .lives(1)
                .level(2)
                .turn(4)
                .hpotCount(0)
                .task("")
                .message("Adventure started")
                .complete(false)
                .error("")
                .build(),
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(8)
                .score(25)
                .lives(0)
                .level(2)
                .turn(5)
                .hpotCount(1)
                .task("Defend the village")
                .message("Bought Healing pot. Quest completed.")
                .complete(true)
                .error("")
                .build()
        );
    }

    @Test
    void shouldSkipMessageWhenResolutionFailsAndContinueWithTheNextMessage() {
        final RecordingGameGateway gateway = new RecordingGameGateway(false, true);
        final ScriptingAdventureService service =
            new ScriptingAdventureService(
                new GameService(gateway),
                new UpgradePurchaseService(
                    new GameService(gateway),
                    new ScriptingAdventureProperties(1, 0, 4, 2)
                ),
                new ScriptingAdventureProperties(1, 0, 4, 2),
                new MessageFilterProperties(List.of(), List.of(PIECE_OF_CAKE)),
                eventPublisher
            );

        service.startSession(SESSION_ID);

        final org.mockito.ArgumentCaptor<Object> eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).containsExactly(
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(10)
                .score(12)
                .lives(1)
                .level(2)
                .turn(4)
                .hpotCount(0)
                .task("")
                .message("Adventure started")
                .complete(false)
                .error("")
                .build(),
            ScriptingAdventureStateEvent.builder()
                .sessionId(SESSION_ID)
                .gold(8)
                .score(25)
                .lives(0)
                .level(2)
                .turn(5)
                .hpotCount(1)
                .task("Defend the village")
                .message("Bought Healing pot. Quest completed.")
                .complete(true)
                .error("")
                .build()
        );
    }

    @Test
    void shouldBuyTwoUpgradeItemsFromDifferentCategoriesAfterFourFailedMissions() {
        final UpgradePurchaseGameGateway gateway = new UpgradePurchaseGameGateway();
        final ScriptingAdventureService service =
            new ScriptingAdventureService(
                new GameService(gateway),
                new UpgradePurchaseService(
                    new GameService(gateway),
                    new ScriptingAdventureProperties(1, 0, 4, 2)
                ),
                new ScriptingAdventureProperties(1, 0, 4, 2),
                new MessageFilterProperties(List.of(), List.of()),
                eventPublisher
            );

        service.startSession(SESSION_ID);

        verify(eventPublisher, times(6)).publishEvent(any(Object.class));
        assertThat(gateway.purchasedCategories).hasSize(2).doesNotHaveDuplicates();
    }

    private static final class RecordingGameGateway implements GameGateway {
        private final boolean rejectFirstMessage;

        private final boolean failFirstMessage;

        private RecordingGameGateway() {
            this(false, false);
        }

        private RecordingGameGateway(final boolean rejectFirstMessage) {
            this(rejectFirstMessage, false);
        }

        private RecordingGameGateway(final boolean rejectFirstMessage, final boolean failFirstMessage) {
            this.rejectFirstMessage = rejectFirstMessage;
            this.failFirstMessage = failFirstMessage;
        }

        @Override
        public StartedGame startGame() {
            return new StartedGame("game-1", 1, 10, 2, 12, 0, 4);
        }

        @Override
        public MessageBoard getMessages(
            final GameId gameId,
            final java.util.Comparator<GameMessage> comparator
        ) {
            final List<GameMessage> messages = new java.util.ArrayList<>(List.of(new GameMessage(
                new MessageId("ad-1"), "Help the village", 15, 2, PIECE_OF_CAKE, 0
            )));
            if (rejectFirstMessage || failFirstMessage) {
                messages.add(new GameMessage(
                    new MessageId("ad-2"), "Defend the village", 10, 3, PIECE_OF_CAKE, 0
                ));
            }
            return new MessageBoard(messages);
        }

        @Override
        public MessageResolution dealWithMessage(final GameId gameId, final MessageId messageId) {
            if (failFirstMessage && "ad-1".equals(messageId.value())) {
                throw new RestClientException("Message resolution failed");
            }
            if (rejectFirstMessage && "ad-1".equals(messageId.value())) {
                throw new MessageResolutionRejectedException(messageId.value());
            }
            return new MessageResolution(false, 0, 8, 25, 0, 5, "Quest completed.");
        }

        @Override
        public ShopInventory getShop(final GameId gameId) {
            return new ShopInventory(List.of(new ShopItem(new ItemId("hpot"), "Healing potion", 2)));
        }

        @Override
        public ItemPurchaseResult buyItem(final GameId gameId, final ShopItemId itemId) {
            return new ItemPurchaseResult(true, 8, 1, 2, 4);
        }
    }

    private static final class UpgradePurchaseGameGateway implements GameGateway {
        private final List<GameMessage> messages = new ArrayList<>();

        private final List<ShopItem> shopItems = List.of(
            new ShopItem(new ItemId("cs"), "Claw Sharpening", 100),
            new ShopItem(new ItemId("ch"), "Claw Honing", 300),
            new ShopItem(new ItemId("gas"), "Gasoline", 100),
            new ShopItem(new ItemId("rf"), "Rocket Fuel", 300),
            new ShopItem(new ItemId("wax"), "Copper Plating", 100),
            new ShopItem(new ItemId("iron"), "Iron Plating", 300),
            new ShopItem(new ItemId("tricks"), "Book of Tricks", 100),
            new ShopItem(new ItemId("mtrix"), "Book of Megatricks", 300),
            new ShopItem(new ItemId("wingpot"), "Potion of Stronger Wings", 100),
            new ShopItem(new ItemId("wingpotmax"), "Potion of Awesome Wings", 300)
        );

        private final List<String> purchasedCategories = new ArrayList<>();

        private long gold = 1_000;

        private int resolutionCount;

        private UpgradePurchaseGameGateway() {
            for (int turn = 1; turn <= 5; turn++) {
                messages.add(new GameMessage(
                    new MessageId("ad-" + turn),
                    "Mission " + turn,
                    10,
                    turn,
                    "Easy",
                    0
                ));
            }
        }

        private static String categoryFor(final String itemId) {
            return switch (itemId) {
                case "cs", "ch" -> "claw";
                case "gas", "rf" -> "fuel";
                case "wax", "iron" -> "plating";
                case "tricks", "mtrix" -> "tricks";
                case "wingpot", "wingpotmax" -> "wings";
                default -> throw new IllegalArgumentException("Unknown upgrade item: " + itemId);
            };
        }

        @Override
        public StartedGame startGame() {
            return new StartedGame("game-1", 3, gold, 1, 0, 0, 0);
        }

        @Override
        public MessageBoard getMessages(
            final GameId gameId,
            final java.util.Comparator<GameMessage> comparator
        ) {
            return new MessageBoard(messages.stream().sorted(comparator).toList());
        }

        @Override
        public MessageResolution dealWithMessage(final GameId gameId, final MessageId messageId) {
            messages.removeIf(message -> message.adId().equals(messageId));
            resolutionCount++;
            return new MessageResolution(
                resolutionCount > 4,
                resolutionCount == 5 ? 0 : 3,
                gold,
                resolutionCount,
                0,
                resolutionCount,
                resolutionCount == 5 ? "Mission succeeded" : "Mission failed"
            );
        }

        @Override
        public ShopInventory getShop(final GameId gameId) {
            return new ShopInventory(shopItems);
        }

        @Override
        public ItemPurchaseResult buyItem(final GameId gameId, final ShopItemId itemId) {
            final ShopItem item = shopItems.stream()
                .filter(shopItem -> shopItem.id().value().equals(itemId.value()))
                .findFirst()
                .orElseThrow();
            gold -= item.cost();
            purchasedCategories.add(categoryFor(itemId.value()));
            return new ItemPurchaseResult(true, gold, 3, 1, resolutionCount);
        }
    }
}
