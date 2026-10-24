<script lang="ts">
    import {onDestroy} from "svelte";
    import Shop from "../components/Shop.svelte";
    import Toaster, {type ToastMessage} from "../components/Toaster.svelte";
    import {
        type ActionOutcome,
        ApiProblemError,
        fetchMessages,
        type GameState,
        type Message,
        type ReputationGroup,
        resolveMessage,
        startGame,
    } from "../lib/game-api";

    let game: GameState | null = null;
    let messages: Message[] = [];
    let selectedMessage: Message | null = null;
    let busy = false;
    let gameOver = false;
    let toasts: ToastMessage[] = [];
    let nextToastId = 0;
    const toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
    let hiddenMessageIds: Set<string> | undefined;

    const statistics: { key: keyof GameState; label: string; icon: string }[] = [
        {key: "lives", label: "Lives", icon: "♥"},
        {key: "gold", label: "Gold", icon: "◈"},
        {key: "level", label: "Level", icon: "⌁"},
        {key: "score", label: "Score", icon: "✦"},
        {key: "highScore", label: "Best", icon: "♛"},
        {key: "turn", label: "Day", icon: "↻"},
    ];
    const reputations: ReputationGroup[] = ["people", "state", "underworld"];

    function errorMessage(cause: unknown): string {
        return cause instanceof Error ? cause.message : String(cause);
    }

    function markGameOver(cause: unknown): boolean {
        if (cause instanceof ApiProblemError && cause.status === 410) {
            gameOver = true;
            selectedMessage = null;
            return true;
        }
        return false;
    }

    function expirationTone(expiresIn: Message["expiresIn"]): "urgent" | "soon" | undefined {
        const days = Number(expiresIn);
        if (days === 1 || days === 2) {
            return "urgent";
        }
        if (days >= 3 && days <= 5) {
            return "soon";
        }
        return undefined;
    }

    function dismissToast(id: number) {
        const timeout = toastTimeouts.get(id);
        if (timeout) {
            clearTimeout(timeout);
        }
        toastTimeouts.delete(id);
        toasts = toasts.filter((toast) => toast.id !== id);
    }

    function showToast(type: ToastMessage["type"], message: string) {
        const id = ++nextToastId;
        toasts = [...toasts, {id, type, message}];
        toastTimeouts.set(id, setTimeout(() => dismissToast(id), 5000));
    }

    function hiddenMessages(): Set<string> {
        if (hiddenMessageIds) {
            return hiddenMessageIds;
        }
        const stored = sessionStorage.getItem("bbdm:hidden-message-ids");
        const parsed: unknown = stored ? JSON.parse(stored) : [];
        hiddenMessageIds = new Set(
            Array.isArray(parsed)
                ? parsed.filter((messageId): messageId is string => typeof messageId === "string")
                : [],
        );
        return hiddenMessageIds;
    }

    function hideMessage(messageId: string) {
        const hidden = hiddenMessages();
        hidden.add(messageId);
        sessionStorage.setItem("bbdm:hidden-message-ids", JSON.stringify([...hidden]));
        messages = messages.filter((message) => message.adId !== messageId);
    }

    async function loadMessages(gameId: string) {
        const loadedMessages = await fetchMessages(gameId);
        const hidden = hiddenMessages();
        messages = loadedMessages.filter((message) => !hidden.has(message.adId));
    }

    onDestroy(() => {
        for (const timeout of toastTimeouts.values()) clearTimeout(timeout);
    });

    async function openAdventure() {
        busy = true;
        try {
            game = await startGame();
            await loadMessages(game.gameId);
        } catch (cause) {
            markGameOver(cause);
            showToast("error", `The adventure could not be started. ${errorMessage(cause)}`);
        } finally {
            busy = false;
        }
    }

    async function refreshMessages() {
        if (game) {
            await loadMessages(game.gameId);
        }
    }

    function updateGame(outcome: ActionOutcome) {
        if (!game) {
            return;
        }
        const update = outcome.game ?? outcome;
        const previousLives = game.lives;
        const updatedLives = update.lives;
        game = {...game, ...update, gameId: update.gameId ?? game.gameId};

        if (previousLives !== undefined && updatedLives !== undefined && updatedLives < previousLives) {
            if (updatedLives === 2) {
                showToast("info", "You have 2 lives left. Visit the shop and buy a Healing potion.");
            } else if (updatedLives === 1) {
                showToast("warning", "You have 1 life left. Visit the shop and buy a Healing potion.");
            }
        }
    }

    async function dealWithMessage() {
        if (!selectedMessage || !game || busy) {
            return;
        }
        const message = selectedMessage;
        const currentGame = game;
        busy = true;
        try {
            const outcome = await resolveMessage(currentGame.gameId, message.adId);
            updateGame(outcome);
            const messageResult = outcome.message ?? outcome.result ?? "The message has been resolved.";
            showToast(
                messageResult.includes("You failed on the mission!") ? "error" : "info",
                messageResult,
            );
            selectedMessage = null;
            try {
                await refreshMessages();
            } catch (cause) {
                markGameOver(cause);
                showToast(
                    "error",
                    `Your response was recorded, but the messages could not be refreshed. ${errorMessage(cause)}`,
                );
            }
        } catch (cause) {
            markGameOver(cause);
            if (
                cause instanceof ApiProblemError &&
                cause.status === 400 &&
                cause.messageId === message.adId
            ) {
                hideMessage(cause.messageId);
                selectedMessage = null;
            }
            showToast("error", `That action could not be completed. ${errorMessage(cause)}`);
        } finally {
            busy = false;
        }
    }

    openAdventure();
</script>

<svelte:head>
    <title>Adventure — Mugloar</title>
</svelte:head>

<main class="game-shell">
    <header class="topbar">
        <a aria-label="Return home" class="wordmark" href="/"><span class="mark">✳</span> MUGLOAR <span
            class="wordmark-sub">/ FIELD JOURNAL</span></a>
    </header>

    {#if busy && !game}
        <section class="loading"><span class="spinner"></span>
            <p>Opening the field journal…</p></section>
    {:else if game}
        <section class="dashboard">
            <div class="dashboard-heading">
                <div>
                    <p class="eyebrow">Your adventure</p>
                    <h1>Field journal <span class="gold-symbol">✦</span></h1>
                </div>
                <Shop
                    game={game}
                    bind:busy
                    {gameOver}
                    onGameUpdate={updateGame}
                    onToast={showToast}
                    onGameOver={() => (gameOver = true)}
                />
            </div>

            {#if gameOver}
                <p class="game-over-banner" role="alert">This game is over.</p>
            {/if}

            <div class="stat-grid">
                {#each statistics as stat}
                    <div class="stat-card">
                        <span class="stat-icon">{stat.icon}</span>
                        <span class="stat-label">{stat.label}</span>
                        <strong>{game[stat.key] ?? 0}</strong>
                    </div>
                {/each}
            </div>

            <section class="reputation">
                <div class="section-caption"><span>REPUTATION</span><span class="caption-line"></span></div>
                <div class="reputation-grid">
                    {#each reputations as group}
                        <div class="reputation-item">
                            <span>{group}</span><strong>{game.reputation?.[group] ?? 0}</strong></div>
                    {/each}
                </div>
            </section>

            <section class="notice-board">
                <div class="board-heading">
                    <div><p class="eyebrow">Fresh from the kingdom</p>
                        <h2>Messages <span class="count">{messages.length}</span></h2></div>
                    <span class="board-hint">SELECT A SHEET TO RESPOND</span>
                </div>
                {#if messages.length}
                    <div class="message-grid">
                        {#each messages as message, index (message.adId)}
                            <button
                                class="paper"
                                class:paper-featured={index === 0}
                                disabled={gameOver}
                                on:click={() => (selectedMessage = message)}
                                aria-label={`Open message: ${message.message}`}
                            >
                                {#if message.reward !== undefined}
                                    <span class="paper-stamp">REWARD · <strong>{message.reward}</strong></span>
                                {:else}
                                    <span class="paper-stamp">KINGDOM POST</span>
                                {/if}
                                <span class="paper-content">
                                    <span class="paper-title">{message.message}</span>
                                    {#if message.probability}
                                        <span class="paper-probability">Probability: {message.probability}</span>
                                    {/if}
                                </span>
                                <span class="paper-meta">
                                    <span>
                                        {#if message.expiresIn !== undefined}
                                            Expires in
                                            <strong
                                                class:expires-urgent={expirationTone(message.expiresIn) === "urgent"}
                                                class:expires-soon={expirationTone(message.expiresIn) === "soon"}
                                            >{message.expiresIn}</strong>
                                        {:else}
                                            Read & respond
                                        {/if}
                                        →
                                    </span>
                                </span>
                            </button>
                        {/each}
                    </div>
                {:else}
                    <div class="empty-board"><span>✧</span>
                        <p>The board is quiet for now.</p></div>
                {/if}
            </section>
        </section>
    {:else}
        <section class="loading"><p>The adventure could not be opened.</p>
            <button class="primary-button retry" on:click={openAdventure}>Try again</button>
        </section>
    {/if}

    <footer class="game-footer">
        <span>KEEP YOUR WITS ABOUT YOU</span><span>✳</span><span>THE DRAGON IS ALWAYS WATCHING</span>
    </footer>
</main>

<Toaster onDismiss={dismissToast} {toasts}/>

{#if selectedMessage}
    <div class="modal-backdrop" role="presentation"
         on:click={(event) => event.target === event.currentTarget && (selectedMessage = null)}>
        <div class="message-modal" role="dialog" aria-modal="true" aria-labelledby="message-heading" tabindex="-1">
            <button class="close-button" aria-label="Close message" on:click={() => (selectedMessage = null)}>×</button>
            <p class="eyebrow">A message for the adventurer</p>
            <h2 id="message-heading">{selectedMessage.message}</h2>
            <div class="message-details">
                {#if selectedMessage.reward !== undefined}
                    <span>Reward <strong>{selectedMessage.reward}</strong></span>{/if}
                {#if selectedMessage.probability}
                    <span>Probability <strong>{selectedMessage.probability}</strong></span>{/if}
            </div>
            <p class="modal-prompt">Ready to resolve this task?</p>
            <div class="modal-actions">
                <button class="primary-button" disabled={busy || gameOver}
                        on:click={dealWithMessage}>{busy ? "Working…" : "Resolve the task"}</button>
            </div>
        </div>
    </div>
{/if}
