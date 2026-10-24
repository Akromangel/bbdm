import { afterEach, describe, expect, it, vi } from "vitest";
import {
    fetchMessages,
    fetchShop,
    purchaseItem,
    resolveMessage,
    startGame,
} from "./game-api";

describe("game API", () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it("starts a game with the API version header", async () => {
        const fetchMock = vi.fn().mockResolvedValue(
            new Response(JSON.stringify({ gameId: "game-1" }), {
                status: 200,
            }),
        );
        vi.stubGlobal("fetch", fetchMock);

        await expect(startGame()).resolves.toEqual({ gameId: "game-1" });
        expect(fetchMock).toHaveBeenCalledWith(
            "/api/game",
            expect.objectContaining({
                method: "POST",
                headers: expect.objectContaining({
                    get: expect.any(Function),
                }),
            }),
        );
        const headers = fetchMock.mock.calls[0][1].headers as Headers;
        expect(headers.get("API-Version")).toBe("1.0");
        expect(headers.get("Content-Type")).toBe("application/json");
    });

    it("normalizes message and shop collections from array and object responses", async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(
                new Response(
                    JSON.stringify({
                        messages: [{ adId: "ad-1", message: "Help" }],
                    }),
                ),
            )
            .mockResolvedValueOnce(
                new Response(
                    JSON.stringify([{ id: "potion", name: "Potion" }]),
                ),
            );
        vi.stubGlobal("fetch", fetchMock);

        await expect(fetchMessages("game/1")).resolves.toEqual([
            { adId: "ad-1", message: "Help" },
        ]);
        await expect(fetchShop("game/1")).resolves.toEqual([
            { id: "potion", name: "Potion" },
        ]);
        expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
            "/api/game%2F1/messages",
            "/api/game%2F1/shop",
        ]);
    });

    it("resolves a message without a request body and purchases the encoded item", async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(
                new Response(JSON.stringify({ message: "Done" })),
            )
            .mockResolvedValueOnce(
                new Response(JSON.stringify({ message: "Purchased" })),
            );
        vi.stubGlobal("fetch", fetchMock);

        await resolveMessage("game-1", "ad/1");
        await purchaseItem("game-1", { id: "health potion" });

        expect(fetchMock.mock.calls[0]).toEqual([
            "/api/game-1/messages/ad%2F1",
            expect.objectContaining({
                method: "POST",
            }),
        ]);
        expect(fetchMock.mock.calls[0][1]).not.toHaveProperty("body");
        expect(fetchMock.mock.calls[1]).toEqual([
            "/api/game-1/shop/health%20potion",
            expect.objectContaining({ method: "POST" }),
        ]);
    });

    it("throws the response body when a request fails", async () => {
        vi.stubGlobal(
            "fetch",
            vi
                .fn()
                .mockResolvedValue(
                    new Response("Game unavailable", { status: 503 }),
                ),
        );

        await expect(startGame()).rejects.toThrow("Game unavailable");
    });
});
