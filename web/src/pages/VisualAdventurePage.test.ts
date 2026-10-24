import {
    cleanup,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/svelte";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import VisualAdventurePage from "./VisualAdventurePage.svelte";

const api = vi.hoisted(() => ({
    fetchMessages: vi.fn(),
    fetchShop: vi.fn(),
    purchaseItem: vi.fn(),
    resolveMessage: vi.fn(),
    startGame: vi.fn(),
    ApiProblemError: class ApiProblemError extends Error {
        status: number;
        messageId?: string;

        constructor(status: number, message: string, messageId?: string) {
            super(message);
            this.name = "ApiProblemError";
            this.status = status;
            this.messageId = messageId;
        }
    },
}));

vi.mock("../lib/game-api", () => api);

describe("visual adventure page", () => {
    beforeEach(() => {
        sessionStorage.clear();
        api.fetchMessages.mockResolvedValue([
            { adId: "ad-1", message: "Rescue the baker" },
        ]);
        api.fetchShop.mockResolvedValue([
            { id: "potion", name: "Potion", cost: 5 },
        ]);
        api.purchaseItem.mockResolvedValue({
            gold: 15,
            message: "Potion purchased.",
        });
        api.resolveMessage.mockResolvedValue({
            game: { gold: 20 },
            message: "Quest accepted.",
        });
        api.startGame.mockResolvedValue({
            gameId: "game-1",
            lives: 3,
            gold: 10,
            turn: 1,
            reputation: { people: 2 },
        });
    });

    afterEach(() => {
        cleanup();
        sessionStorage.clear();
        vi.clearAllMocks();
    });

    it("loads game data and displays the dashboard and available messages", async () => {
        render(VisualAdventurePage);

        expect(
            await screen.findByRole("heading", { name: /field journal/i }),
        ).toBeTruthy();
        expect(await screen.findByText("Rescue the baker")).toBeTruthy();
        expect(screen.getByText("10")).toBeTruthy();
        expect(api.startGame).toHaveBeenCalledOnce();
        expect(api.fetchMessages).toHaveBeenCalledWith("game-1");
    });

    it("displays game over and disables the shop after messages return gone", async () => {
        api.fetchMessages.mockRejectedValueOnce(
            new api.ApiProblemError(410, "Game Over"),
        );
        render(VisualAdventurePage);

        const gameOverMessage = await screen.findByText("This game is over.");
        const shopButton = screen.getByRole("button", {
            name: /visit the shop/i,
        });
        expect([
            gameOverMessage.textContent,
            shopButton.hasAttribute("disabled"),
        ]).toEqual(["This game is over.", true]);
    });

    it("disables message actions when resolving a task returns game over", async () => {
        api.resolveMessage.mockRejectedValueOnce(
            new api.ApiProblemError(410, "Game Over"),
        );
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );

        const gameOverMessage = await screen.findByText("This game is over.");
        const messageButton = screen.getByRole("button", {
            name: /open message: rescue the baker/i,
        });
        const shopButton = screen.getByRole("button", {
            name: /visit the shop/i,
        });
        expect([
            gameOverMessage.textContent,
            messageButton.hasAttribute("disabled"),
            shopButton.hasAttribute("disabled"),
        ]).toEqual(["This game is over.", true, true]);
    });

    it("displays the board probability beneath each message", async () => {
        api.fetchMessages.mockResolvedValueOnce([
            {
                adId: "ad-1",
                message: "Rescue the baker",
                probability: "75%",
            },
        ]);
        render(VisualAdventurePage);

        const probability = await screen.findByText("Probability: 75%");
        expect(
            probability.parentElement?.querySelector(".paper-title")
                ?.textContent,
        ).toBe("Rescue the baker");
    });

    it("emphasizes rewards and colors expiry values by urgency", async () => {
        const expiryDays = [1, 2, 3, 4, 5, 6];
        api.fetchMessages.mockResolvedValueOnce(
            expiryDays.map((expiresIn) => ({
                adId: `ad-${expiresIn}`,
                message: `Quest ${expiresIn}`,
                reward: expiresIn === 1 ? 0 : expiresIn * 10,
                expiresIn,
            })),
        );
        render(VisualAdventurePage);

        const cards = await screen.findAllByRole("button", {
            name: /open message/i,
        });

        expect(
            cards.map((card) => ({
                emphasizedValues: Array.from(
                    card.querySelectorAll("strong"),
                    (element) => element.textContent,
                ),
                expiryTone:
                    card.querySelector(".paper-meta strong")?.className ?? "",
            })),
        ).toEqual([
            {
                emphasizedValues: ["0", "1"],
                expiryTone: "expires-urgent",
            },
            {
                emphasizedValues: ["20", "2"],
                expiryTone: "expires-urgent",
            },
            { emphasizedValues: ["30", "3"], expiryTone: "expires-soon" },
            { emphasizedValues: ["40", "4"], expiryTone: "expires-soon" },
            { emphasizedValues: ["50", "5"], expiryTone: "expires-soon" },
            { emphasizedValues: ["60", "6"], expiryTone: "" },
        ]);
    });

    it("resolves a selected message and refreshes the board", async () => {
        api.fetchMessages
            .mockResolvedValueOnce([
                { adId: "ad-1", message: "Rescue the baker" },
            ])
            .mockResolvedValueOnce([
                { adId: "ad-2", message: "Find the lost crown" },
            ]);
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );

        expect((await screen.findByRole("status")).textContent).toContain(
            "Quest accepted.",
        );
        expect(await screen.findByText("Find the lost crown")).toBeTruthy();
        expect(screen.getByText("20")).toBeTruthy();
        expect(api.resolveMessage).toHaveBeenCalledWith("game-1", "ad-1");
        await waitFor(() => expect(api.fetchMessages).toHaveBeenCalledTimes(2));
    });

    it("shows mission failures as error toasts", async () => {
        api.resolveMessage.mockResolvedValue({
            message: " You failed on the mission!",
        });
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );

        const toast = await screen.findByRole("alert");
        expect(toast.textContent).toContain("You failed on the mission!");
        expect(toast.classList.contains("toast-error")).toBe(true);
    });

    it("removes a rejected message for the rest of the browser session", async () => {
        api.resolveMessage.mockRejectedValue(
            new api.ApiProblemError(
                400,
                "The game server rejected this message. It has been removed from the message board.",
                "ad-1",
            ),
        );
        const firstRender = render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );
        expect((await screen.findByRole("alert")).textContent).toContain(
            "The game server rejected this message.",
        );
        await waitFor(() =>
            expect(screen.queryByText("Rescue the baker")).toBeNull(),
        );

        firstRender.unmount();
        api.startGame.mockResolvedValueOnce({ gameId: "game-2" });
        render(VisualAdventurePage);

        expect(
            await screen.findByText("The board is quiet for now."),
        ).toBeTruthy();
    });

    it("shows an info toast when lives decrease to two", async () => {
        api.resolveMessage.mockResolvedValue({
            game: { lives: 2 },
            message: "Quest accepted.",
        });
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );

        const toast = (
            await screen.findByText(
                "You have 2 lives left. Visit the shop and buy a Healing potion.",
            )
        ).closest('[role="status"]');
        expect(toast).not.toBeNull();
    });

    it("shows an orange warning toast when lives decrease to one", async () => {
        api.resolveMessage.mockResolvedValue({
            game: { lives: 1 },
            message: "Quest accepted.",
        });
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /open message/i }),
        );
        await fireEvent.click(
            screen.getByRole("button", { name: /resolve the task/i }),
        );

        const toast = (
            await screen.findByText(
                "You have 1 life left. Visit the shop and buy a Healing potion.",
            )
        ).closest('[role="status"]');
        expect(toast).not.toBeNull();
        expect(toast?.classList.contains("toast-warning")).toBe(true);
    });

    it("loads the shop, completes a purchase, and refreshes its inventory", async () => {
        render(VisualAdventurePage);

        await fireEvent.click(
            await screen.findByRole("button", { name: /visit the shop/i }),
        );
        await fireEvent.click(
            await screen.findByRole("button", { name: "Buy" }),
        );

        expect((await screen.findByRole("status")).textContent).toContain(
            "Potion purchased.",
        );
        expect(api.purchaseItem).toHaveBeenCalledWith("game-1", {
            id: "potion",
            name: "Potion",
            cost: 5,
        });
        expect(api.fetchShop).toHaveBeenCalledTimes(2);
    });
});
