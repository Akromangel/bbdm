package kuchma.mike.bbdm.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class UpgradePurchaseService {
    private static final Logger log = LoggerFactory.getLogger(UpgradePurchaseService.class);

    private final GameService gameService;

    private final ScriptingAdventureProperties scriptingAdventureProperties;

    public UpgradePurchaseService(
        final GameService gameService,
        final ScriptingAdventureProperties scriptingAdventureProperties
    ) {
        this.gameService = gameService;
        this.scriptingAdventureProperties = scriptingAdventureProperties;
    }

    private static void logPurchaseFailure(
        final String sessionId,
        final ShopItem item,
        final RestClientException exception
    ) {
        log.atWarn()
            .addKeyValue("sessionId", sessionId)
            .addKeyValue("itemId", item.id().value())
            .addKeyValue("error", exception.getMessage())
            .setMessage("Could not buy upgrade item after consecutive mission failures")
            .log();
    }

    private static String purchaseMessage(
        final List<String> purchasedItems,
        final int failedMissionsBeforeUpgrades
    ) {
        if (purchasedItems.isEmpty()) {
            return "No affordable upgrade items could be bought.";
        }
        return "Bought %s after %d consecutive mission failures.".formatted(
            String.join(" and ", purchasedItems),
            failedMissionsBeforeUpgrades
        );
    }

    private static Optional<UpgradeCategory> upgradeCategory(final String itemId) {
        return switch (itemId) {
            case "cs", "ch" -> Optional.of(UpgradeCategory.CLAW);
            case "gas", "rf" -> Optional.of(UpgradeCategory.FUEL);
            case "wax", "iron" -> Optional.of(UpgradeCategory.PLATING);
            case "tricks", "mtrix" -> Optional.of(UpgradeCategory.TRICKS);
            case "wingpot", "wingpotmax" -> Optional.of(UpgradeCategory.WINGS);
            default -> Optional.empty();
        };
    }

    private static List<UpgradeCategory> availableCategories(final ShopInventory inventory, final long availableGold) {
        return new ArrayList<>(inventory.items().stream()
            .filter(item -> item.cost() <= availableGold)
            .map(item -> upgradeCategory(item.id().value()))
            .flatMap(Optional::stream)
            .distinct()
            .toList());
    }

    public PurchaseOutcome buyRandomUpgradeItems(
        final GameId gameId,
        final long availableGold,
        final String sessionId
    ) {
        final Optional<ShopInventory> inventory = loadShop(gameId, sessionId);
        if (inventory.isEmpty()) {
            return new PurchaseOutcome("Could not load upgrade items from the shop.", List.of());
        }
        final List<UpgradeCategory> categories = availableCategories(inventory.orElseThrow(), availableGold);
        Collections.shuffle(categories);
        return purchaseCategories(
            gameId,
            sessionId,
            inventory.orElseThrow(),
            categories,
            availableGold
        );
    }

    private Optional<ShopInventory> loadShop(final GameId gameId, final String sessionId) {
        try {
            return Optional.of(gameService.getShop(gameId));
        } catch (final RestClientException exception) {
            log.atWarn()
                .addKeyValue("sessionId", sessionId)
                .addKeyValue("error", exception.getMessage())
                .setMessage("Could not load shop after consecutive mission failures")
                .log();
            return Optional.empty();
        }
    }

    private PurchaseOutcome purchaseCategories(
        final GameId gameId,
        final String sessionId,
        final ShopInventory inventory,
        final List<UpgradeCategory> categories,
        final long availableGold
    ) {
        long remainingGold = availableGold;
        final List<String> purchasedItems = new ArrayList<>();
        final List<ItemPurchaseResult> purchases = new ArrayList<>();
        for (final UpgradeCategory category : categories.stream()
            .limit(scriptingAdventureProperties.upgradeItemsToBuy())
            .toList()) {
            final CategoryPurchase purchase = purchaseCategory(gameId, sessionId, inventory, category, remainingGold);
            remainingGold = purchase.remainingGold();
            purchase.result().ifPresent(purchases::add);
            purchase.itemName().ifPresent(purchasedItems::add);
        }
        return new PurchaseOutcome(
            purchaseMessage(purchasedItems, scriptingAdventureProperties.failedMissionsBeforeUpgrades()),
            purchases
        );
    }

    private CategoryPurchase purchaseCategory(
        final GameId gameId,
        final String sessionId,
        final ShopInventory inventory,
        final UpgradeCategory category,
        final long availableGold
    ) {
        final List<ShopItem> affordableItems = inventory.items().stream()
            .filter(item -> item.cost() <= availableGold)
            .filter(item -> upgradeCategory(item.id().value()).filter(category::equals).isPresent())
            .toList();
        if (affordableItems.isEmpty()) {
            return new CategoryPurchase(availableGold, Optional.empty(), Optional.empty());
        }
        return buyRandomItem(gameId, sessionId, affordableItems, availableGold);
    }

    private CategoryPurchase buyRandomItem(
        final GameId gameId,
        final String sessionId,
        final List<ShopItem> affordableItems,
        final long availableGold
    ) {
        final ShopItem item = affordableItems.get(ThreadLocalRandom.current().nextInt(affordableItems.size()));
        try {
            final ItemPurchaseResult purchase = gameService.buyItem(gameId, item.id().value());
            final Optional<String> purchasedName = purchase.shoppingSuccess()
                ? Optional.of(item.name())
                : Optional.empty();
            return new CategoryPurchase(purchase.gold(), Optional.of(purchase), purchasedName);
        } catch (final RestClientException exception) {
            logPurchaseFailure(sessionId, item, exception);
            return new CategoryPurchase(availableGold, Optional.empty(), Optional.empty());
        }
    }

    private enum UpgradeCategory {
        CLAW,
        FUEL,
        PLATING,
        TRICKS,
        WINGS
    }

    public record PurchaseOutcome(String message, List<ItemPurchaseResult> purchases) {
        public PurchaseOutcome {
            purchases = List.copyOf(purchases);
        }
    }

    private record CategoryPurchase(
        long remainingGold,
        Optional<ItemPurchaseResult> result,
        Optional<String> itemName
    ) {
    }
}
