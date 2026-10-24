import { cleanup, fireEvent, render, screen } from "@testing-library/svelte";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Shop from "./Shop.svelte";

const api = vi.hoisted(() => ({
    fetchShop: vi.fn(),
    purchaseItem: vi.fn(),
}));

vi.mock("../lib/game-api", () => api);

describe("shop component", () => {
    const game = { gameId: "game-1", gold: 10 };

    beforeEach(() => {
        api.fetchShop.mockResolvedValue([
            { id: "potion", name: "Potion", cost: 5 },
        ]);
        api.purchaseItem.mockResolvedValue({
            gold: 5,
            message: "Potion purchased.",
        });
    });

    afterEach(() => {
        cleanup();
        vi.clearAllMocks();
    });

    it("loads wares, completes a purchase, and refreshes the inventory", async () => {
        const onGameUpdate = vi.fn();
        const onToast = vi.fn();
        render(Shop, { props: { game, onGameUpdate, onToast } });

        await fireEvent.click(
            screen.getByRole("button", { name: /visit the shop/i }),
        );
        expect(await screen.findByText("Potion")).toBeTruthy();
        expect(api.fetchShop).toHaveBeenCalledWith("game-1");

        await fireEvent.click(screen.getByRole("button", { name: "Buy" }));

        expect(api.purchaseItem).toHaveBeenCalledWith("game-1", {
            id: "potion",
            name: "Potion",
            cost: 5,
        });
        expect(onGameUpdate).toHaveBeenCalledWith({
            gold: 5,
            message: "Potion purchased.",
        });
        expect(onToast).toHaveBeenCalledWith("info", "Potion purchased.");
        expect(api.fetchShop).toHaveBeenCalledTimes(2);
    });

    it("reports an error when the shop cannot be loaded", async () => {
        api.fetchShop.mockRejectedValue(new Error("Unavailable"));
        const onToast = vi.fn();
        render(Shop, { props: { game, onGameUpdate: vi.fn(), onToast } });

        await fireEvent.click(
            screen.getByRole("button", { name: /visit the shop/i }),
        );

        expect(onToast).toHaveBeenCalledWith(
            "error",
            "The shop could not be loaded. Unavailable",
        );
    });
});
