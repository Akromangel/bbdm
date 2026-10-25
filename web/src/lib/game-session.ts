import { get, writable } from "svelte/store";
import {
    type ActionOutcome,
    ApiProblemError,
    fetchMessages,
    fetchShop,
    type GameState,
    type Message,
    purchaseItem,
    resolveMessage,
    type ShopItem,
    startGame,
} from "./game-api";

interface GameSessionState {
    game: GameState | null;
    messages: Message[];
    shopItems: ShopItem[];
    busy: boolean;
    gameOver: boolean;
}

export interface MessageActionResult {
    outcome: ActionOutcome;
    refreshError?: unknown;
    lostLives?: number;
}

export interface ShopActionResult {
    outcome: ActionOutcome;
    refreshError?: unknown;
}

const initialState: GameSessionState = {
    game: null,
    messages: [],
    shopItems: [],
    busy: false,
    gameOver: false,
};

export const gameSession = writable<GameSessionState>(initialState);

export function resetGameSession() {
    gameSession.set(initialState);
}

function markGameOver(cause: unknown) {
    if (cause instanceof ApiProblemError && cause.status === 410) {
        gameSession.update((session) => ({ ...session, gameOver: true }));
    }
}

function hiddenMessageIds(): Set<string> {
    const stored = sessionStorage.getItem("bbdm:hidden-message-ids");
    const parsed: unknown = stored ? JSON.parse(stored) : [];
    return new Set(
        Array.isArray(parsed)
            ? parsed.filter(
                  (messageId): messageId is string =>
                      typeof messageId === "string",
              )
            : [],
    );
}

function hideMessage(messageId: string) {
    const hidden = hiddenMessageIds();
    hidden.add(messageId);
    sessionStorage.setItem(
        "bbdm:hidden-message-ids",
        JSON.stringify([...hidden]),
    );
    gameSession.update((session) => ({
        ...session,
        messages: session.messages.filter(
            (message) => message.adId !== messageId,
        ),
    }));
}

async function refreshMessages(gameId: string) {
    try {
        const loadedMessages = await fetchMessages(gameId);
        const hidden = hiddenMessageIds();
        gameSession.update((session) => ({
            ...session,
            messages: loadedMessages.filter(
                (message) => !hidden.has(message.adId),
            ),
        }));
    } catch (cause) {
        markGameOver(cause);
        throw cause;
    }
}

function updateGame(outcome: ActionOutcome): number | undefined {
    let lostLives: number | undefined;
    gameSession.update((session) => {
        if (!session.game) {
            return session;
        }
        const update = outcome.game ?? outcome;
        const previousLives = session.game.lives;
        const updatedLives = update.lives;
        if (
            previousLives !== undefined &&
            updatedLives !== undefined &&
            updatedLives < previousLives &&
            (updatedLives === 1 || updatedLives === 2)
        ) {
            lostLives = updatedLives;
        }
        return {
            ...session,
            game: {
                ...session.game,
                ...update,
                gameId: update.gameId ?? session.game.gameId,
            },
        };
    });
    return lostLives;
}

export async function startAdventure() {
    gameSession.update((session) => ({ ...session, busy: true }));
    try {
        const game = await startGame();
        gameSession.update((session) => ({ ...session, game }));
        await refreshMessages(game.gameId);
    } catch (cause) {
        markGameOver(cause);
        throw cause;
    } finally {
        gameSession.update((session) => ({ ...session, busy: false }));
    }
}

export async function resolveAdventureMessage(
    messageId: string,
): Promise<MessageActionResult | undefined> {
    const session = get(gameSession);
    if (!session?.game || session.busy) {
        return undefined;
    }

    const gameId = session.game.gameId;
    gameSession.update((value) => ({ ...value, busy: true }));
    try {
        const outcome = await resolveMessage(gameId, messageId);
        const lostLives = updateGame(outcome);
        let refreshError: unknown;
        try {
            await refreshMessages(gameId);
        } catch (cause) {
            refreshError = cause;
        }
        return { outcome, refreshError, lostLives };
    } catch (cause) {
        markGameOver(cause);
        if (
            cause instanceof ApiProblemError &&
            cause.status === 400 &&
            cause.messageId === messageId
        ) {
            hideMessage(messageId);
        }
        throw cause;
    } finally {
        gameSession.update((value) => ({ ...value, busy: false }));
    }
}

export async function loadShop() {
    const gameId = getCurrentGameId();
    if (!gameId) {
        return;
    }
    try {
        const shopItems = await fetchShop(gameId);
        gameSession.update((session) => ({ ...session, shopItems }));
    } catch (cause) {
        markGameOver(cause);
        throw cause;
    }
}

export async function purchaseShopItem(
    item: ShopItem,
): Promise<ShopActionResult | undefined> {
    const gameId = getCurrentGameId();
    if (!gameId || getBusy()) {
        return undefined;
    }
    gameSession.update((session) => ({ ...session, busy: true }));
    try {
        const outcome = await purchaseItem(gameId, item);
        updateGame(outcome);
        let refreshError: unknown;
        try {
            const shopItems = await fetchShop(gameId);
            gameSession.update((session) => ({ ...session, shopItems }));
        } catch (cause) {
            refreshError = cause;
        }
        return { outcome, refreshError };
    } catch (cause) {
        markGameOver(cause);
        throw cause;
    } finally {
        gameSession.update((session) => ({ ...session, busy: false }));
    }
}

function getCurrentGameId(): string | undefined {
    return get(gameSession).game?.gameId;
}

function getBusy(): boolean {
    return get(gameSession).busy;
}
