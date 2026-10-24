export type ReputationGroup = "people" | "state" | "underworld";

export interface GameState {
    gameId: string;
    lives?: number;
    gold?: number;
    level?: number;
    score?: number;
    highScore?: number;
    turn?: number;
    reputation?: Partial<Record<ReputationGroup, number>>;
    [key: string]: unknown;
}

export interface Message {
    adId: string;
    message: string;
    reward?: number;
    probability?: string | null;
    expiresIn?: string | number;
}

export interface ShopItem {
    id?: string;
    name?: string;
    cost?: number;
    description?: string;
}

export interface ActionOutcome extends Partial<GameState> {
    game?: Partial<GameState>;
    message?: string;
    result?: string;
}

export class ApiProblemError extends Error {
    constructor(
        public readonly status: number,
        message: string,
        public readonly messageId?: string,
    ) {
        super(message);
        this.name = "ApiProblemError";
    }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set("Content-Type", "application/json");
    headers.set("API-Version", "1.0");
    headers.set("Accept", "application/json, application/problem+json");

    const response = await fetch(path, {
        ...options,
        headers,
    });

    if (!response.ok) {
        if (
            response.headers
                .get("content-type")
                ?.includes("application/problem+json")
        ) {
            const problem: unknown = await response.json();
            if (typeof problem === "object" && problem !== null) {
                const fields = problem as Record<string, unknown>;
                const message =
                    typeof fields.detail === "string"
                        ? fields.detail
                        : typeof fields.title === "string"
                          ? fields.title
                          : `Request failed (${response.status})`;
                throw new ApiProblemError(
                    response.status,
                    message,
                    typeof fields.messageId === "string"
                        ? fields.messageId
                        : undefined,
                );
            }
        }
        const body = await response.text();
        throw new Error(body || `Request failed (${response.status})`);
    }

    return (await response.json()) as T;
}

function getCollection<T>(
    response: T[] | { messages?: T[]; items?: T[] },
    key: "messages" | "items",
): T[] {
    return Array.isArray(response) ? response : (response[key] ?? []);
}

export async function startGame(): Promise<GameState> {
    return request<GameState>("/api/game", { method: "POST" });
}

export async function fetchMessages(gameId: string): Promise<Message[]> {
    const response = await request<Message[] | { messages?: Message[] }>(
        `/api/${encodeURIComponent(gameId)}/messages`,
    );
    return getCollection(response, "messages");
}

export async function resolveMessage(
    gameId: string,
    messageId: string,
): Promise<ActionOutcome> {
    return request<ActionOutcome>(
        `/api/${encodeURIComponent(gameId)}/messages/${encodeURIComponent(messageId)}`,
        { method: "POST" },
    );
}

export async function fetchShop(gameId: string): Promise<ShopItem[]> {
    const response = await request<ShopItem[] | { items?: ShopItem[] }>(
        `/api/${encodeURIComponent(gameId)}/shop`,
    );
    return getCollection(response, "items");
}

export async function purchaseItem(
    gameId: string,
    item: ShopItem,
): Promise<ActionOutcome> {
    return request<ActionOutcome>(
        `/api/${encodeURIComponent(gameId)}/shop/${encodeURIComponent(item.id ?? item.name ?? "")}`,
        { method: "POST" },
    );
}
