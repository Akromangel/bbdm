package kuchma.mike.bbdm.application;

import java.util.List;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.ItemId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UpgradePurchaseServiceTest {
    private static final GameId GAME_ID = new GameId("game-1");

    private static final String SESSION_ID = "session-1";

    private final GameService gameService = mock(GameService.class);

    @AfterEach
    void shouldNotHaveUnexpectedGameServiceInteractions() {
        verifyNoMoreInteractions(gameService);
    }

    @Test
    void shouldBuyOnlyConfiguredNumberOfUpgradeItems() {
        when(gameService.getShop(GAME_ID)).thenReturn(new ShopInventory(List.of(
            new ShopItem(new ItemId("cs"), "Claw Sharpening", 10),
            new ShopItem(new ItemId("gas"), "Gasoline", 10)
        )));
        when(gameService.buyItem(eq(GAME_ID), anyString()))
            .thenReturn(new ItemPurchaseResult(true, 90, 3, 1, 1));
        final ScriptingAdventureProperties properties = new ScriptingAdventureProperties(1, 0, 4, 1);
        final UpgradePurchaseService service = new UpgradePurchaseService(gameService, properties);

        final UpgradePurchaseService.PurchaseOutcome outcome =
            service.buyRandomUpgradeItems(GAME_ID, 100, SESSION_ID);

        verify(gameService).getShop(GAME_ID);
        verify(gameService).buyItem(eq(GAME_ID), anyString());
        assertThat(outcome.purchases()).hasSize(1);
    }
}
