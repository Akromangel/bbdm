package kuchma.mike.bbdm.application;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.StartedGame;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class ScriptingAdventureService {
    private static final Logger log = LoggerFactory.getLogger(ScriptingAdventureService.class);

    private static final String HPOT_ITEM_ID = "hpot";

    private final GameService gameService;

    private final UpgradePurchaseService upgradePurchaseService;

    private final ScriptingAdventureProperties scriptingAdventureProperties;

    private final Comparator<GameMessage> messageComparator;

    private final ApplicationEventPublisher eventPublisher;

    private final ThreadLocal<@Nullable CurrentGameState> currentState = new ThreadLocal<>();

    public ScriptingAdventureService(
        final GameService gameService,
        final UpgradePurchaseService upgradePurchaseService,
        final ScriptingAdventureProperties scriptingAdventureProperties,
        final MessageFilterProperties messageFilterProperties,
        final ApplicationEventPublisher eventPublisher
    ) {
        this.gameService = gameService;
        this.upgradePurchaseService = upgradePurchaseService;
        this.scriptingAdventureProperties = scriptingAdventureProperties;
        messageComparator = createMessageComparator(messageFilterProperties.probabilityOrder());
        this.eventPublisher = eventPublisher;
    }

    private static Comparator<GameMessage> createMessageComparator(final List<String> probabilityOrder) {
        final Map<String, Integer> probabilityRanks = new HashMap<>();
        for (int index = 0; index < probabilityOrder.size(); index++) {
            probabilityRanks.put(probabilityOrder.get(index), index);
        }
        return Comparator.comparingInt(GameMessage::expiresIn)
            .thenComparingInt(message -> message.probability() == null
                ? Integer.MAX_VALUE
                : probabilityRanks.getOrDefault(message.probability(), Integer.MAX_VALUE))
            .thenComparing(Comparator.comparingInt(GameMessage::reward).reversed());
    }

    private static String combineMessages(final String first, final String second) {
        return first.isEmpty() ? second : "%s. %s".formatted(first, second);
    }

    private static void logPotionPurchaseFailure(final String sessionId, final RestClientException exception) {
        log.atWarn()
            .addKeyValue("sessionId", sessionId)
            .addKeyValue("error", exception.getMessage())
            .setMessage("Automatic Healing pot purchase failed")
            .log();
    }

    private static void logRejectedMessage(
        final String sessionId,
        final GameMessage message,
        final RuntimeException exception
    ) {
        log.atWarn()
            .addKeyValue("sessionId", sessionId)
            .addKeyValue("messageId", message.adId().value())
            .addKeyValue("message", message.message())
            .addKeyValue("error", exception.getMessage())
            .setMessage("Skipping scripting adventure message after resolution failure")
            .log();
    }

    @Async("scriptingAdventureExecutor")
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void startSession(final String sessionId) {
        final SessionContext context = new SessionContext(sessionId);
        try {
            runSession(context);
        } catch (final GameOverException exception) {
            publishGameOver(context);
        } catch (final RuntimeException exception) {
            publishSessionFailure(context, exception);
        } finally {
            currentState.remove();
        }
    }

    private void runSession(final SessionContext context) {
        final StartedGame startedGame = gameService.startGame();
        context.gameId = new GameId(startedGame.gameId());
        currentState.set(new CurrentGameState(startedGame));
        publish(context.sessionId, gameState(), context.hpotCount, "", "Adventure started", false, "");
        while (gameState().lives > scriptingAdventureProperties.noLives()) {
            if (!processTurn(context)) {
                return;
            }
        }
    }

    private boolean processTurn(final SessionContext context) {
        final String potionUpdate = purchaseHealingPotion(context);
        final Optional<ResolvedMessage> resolvedMessage = findAndResolveMessage(context, potionUpdate);
        if (resolvedMessage.isEmpty()) {
            return false;
        }
        final ResolvedMessage result = resolvedMessage.orElseThrow();
        gameState().update(result.resolution());
        final String resultMessage = updateFailureStreakAndUpgrade(context, result.resolution(), potionUpdate);
        publishTurn(context, result.message(), resultMessage);
        return gameState().lives > scriptingAdventureProperties.noLives();
    }

    private String purchaseHealingPotion(final SessionContext context) {
        if (gameState().lives != scriptingAdventureProperties.lastLife()) {
            return "";
        }
        try {
            return buyHealingPotion(context);
        } catch (final RestClientException exception) {
            logPotionPurchaseFailure(context.sessionId, exception);
            return "Healing pot purchase failed: " + exception.getMessage();
        }
    }

    private String buyHealingPotion(final SessionContext context) {
        final Optional<ItemPurchaseResult> purchaseResult = gameService.getShop(context.gameId()).items().stream()
            .filter(item -> HPOT_ITEM_ID.equals(item.id().value()))
            .findFirst()
            .map(item -> gameService.buyItem(context.gameId(), item.id().value()));
        if (purchaseResult.isEmpty()) {
            return "Could not buy Healing pot";
        }
        final ItemPurchaseResult purchase = purchaseResult.orElseThrow();
        gameState().update(purchase);
        if (purchase.shoppingSuccess()) {
            context.hpotCount++;
            return "Bought Healing pot";
        }
        return "Could not buy Healing pot";
    }

    private Optional<ResolvedMessage> findAndResolveMessage(
        final SessionContext context,
        final String potionUpdate
    ) {
        while (true) {
            final MessageBoard messageBoard = gameService.getMessages(context.gameId(), messageComparator);
            context.rejectedMessageIds.retainAll(
                messageBoard.messages().stream().map(GameMessage::adId).toList()
            );
            final Optional<GameMessage> nextMessage = messageBoard.messages().stream()
                .filter(candidate -> !context.rejectedMessageIds.contains(candidate.adId()))
                .findFirst();
            if (nextMessage.isEmpty()) {
                publishNoMoreMessages(context, messageBoard.messages().isEmpty(), potionUpdate);
                return Optional.empty();
            }
            final Optional<ResolvedMessage> resolvedMessage = tryResolveMessage(context, nextMessage.orElseThrow());
            if (resolvedMessage.isPresent()) {
                return resolvedMessage;
            }
        }
    }

    private Optional<ResolvedMessage> tryResolveMessage(final SessionContext context, final GameMessage message) {
        try {
            final MessageResolution resolution = gameService.dealWithMessage(context.gameId(), message.adId());
            return Optional.of(new ResolvedMessage(message, resolution));
        } catch (final MessageResolutionRejectedException | RestClientException exception) {
            context.rejectedMessageIds.add(message.adId());
            logRejectedMessage(context.sessionId, message, exception);
            return Optional.empty();
        }
    }

    private String updateFailureStreakAndUpgrade(
        final SessionContext context,
        final MessageResolution resolution,
        final String potionUpdate
    ) {
        if (resolution.success()) {
            context.consecutiveFailedMissions = 0;
            return combineMessages(potionUpdate, resolution.message());
        }
        context.consecutiveFailedMissions++;
        if (shouldBuyUpgrades(context)) {
            return "%s %s".formatted(
                combineMessages(potionUpdate, resolution.message()),
                buyUpgradeItems(context)
            );
        }
        return combineMessages(potionUpdate, resolution.message());
    }

    private boolean shouldBuyUpgrades(final SessionContext context) {
        return context.consecutiveFailedMissions >= scriptingAdventureProperties.failedMissionsBeforeUpgrades()
            && gameState().lives > scriptingAdventureProperties.noLives();
    }

    private String buyUpgradeItems(final SessionContext context) {
        final UpgradePurchaseService.PurchaseOutcome outcome = upgradePurchaseService.buyRandomUpgradeItems(
            context.gameId(),
            gameState().gold,
            context.sessionId
        );
        outcome.purchases().forEach(gameState()::update);
        context.consecutiveFailedMissions = 0;
        return outcome.message();
    }

    private void publishTurn(
        final SessionContext context,
        final GameMessage message,
        final String resultMessage
    ) {
        publish(
            context.sessionId,
            gameState(),
            context.hpotCount,
            message.message(),
            resultMessage,
            gameState().lives <= scriptingAdventureProperties.noLives(),
            ""
        );
    }

    private void publishNoMoreMessages(
        final SessionContext context,
        final boolean noMessages,
        final String potionUpdate
    ) {
        final String completionMessage = noMessages ? "No more messages" : "No actionable messages";
        publish(
            context.sessionId,
            gameState(),
            context.hpotCount,
            "",
            combineMessages(potionUpdate, completionMessage),
            true,
            ""
        );
    }

    private void publishGameOver(final SessionContext context) {
        final CurrentGameState state = currentState.get();
        publish(context.sessionId, state, context.hpotCount, "", "Game over", true, "");
    }

    private void publishSessionFailure(final SessionContext context, final RuntimeException exception) {
        final CurrentGameState state = currentState.get();
        log.atError()
            .addKeyValue("sessionId", context.sessionId)
            .addKeyValue("error", exception.getMessage())
            .setMessage("Scripting adventure session failed")
            .log();
        final String error = exception.getMessage() == null
            ? exception.getClass().getSimpleName()
            : exception.getMessage();
        publish(context.sessionId, state, context.hpotCount, "", "Adventure stopped", true, error);
    }

    private CurrentGameState gameState() {
        final CurrentGameState state = currentState.get();
        if (state == null) {
            throw new IllegalStateException("No current game state is available for this thread");
        }
        return state;
    }

    private void publish(
        final String sessionId,
        final @Nullable CurrentGameState state,
        final int hpotCount,
        final String task,
        final String message,
        final boolean complete,
        final String error
    ) {
        eventPublisher.publishEvent(ScriptingAdventureStateEvent.builder()
            .sessionId(sessionId)
            .gold(state == null ? 0 : state.gold)
            .score(state == null ? 0 : state.score)
            .lives(state == null ? 0 : state.lives)
            .level(state == null ? 0 : state.level)
            .turn(state == null ? 0 : state.turn)
            .hpotCount(hpotCount)
            .task(task)
            .message(message)
            .complete(complete)
            .error(error)
            .build());
    }

    private static final class SessionContext {
        private final String sessionId;

        private final Set<MessageId> rejectedMessageIds = new HashSet<>();

        private @Nullable GameId gameId;

        private int hpotCount;

        private int consecutiveFailedMissions;

        private SessionContext(final String sessionId) {
            this.sessionId = sessionId;
        }

        private GameId gameId() {
            final GameId currentGameId = gameId;
            if (currentGameId == null) {
                throw new IllegalStateException("No game has been started for this session");
            }
            return currentGameId;
        }
    }

    private record ResolvedMessage(GameMessage message, MessageResolution resolution) {
    }

    private static final class CurrentGameState {
        private long gold;

        private int score;

        private int lives;

        private int level;

        private int turn;

        private CurrentGameState(final StartedGame game) {
            gold = game.gold();
            score = game.score();
            lives = game.lives();
            level = game.level();
            turn = game.turn();
        }

        private void update(final MessageResolution resolution) {
            gold = resolution.gold();
            score = resolution.score();
            lives = resolution.lives();
            turn = resolution.turn();
        }

        private void update(final ItemPurchaseResult purchase) {
            gold = purchase.gold();
            lives = purchase.lives();
            level = purchase.level();
            turn = purchase.turn();
        }
    }
}
