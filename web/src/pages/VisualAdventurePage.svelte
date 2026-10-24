<script lang="ts">
    import { onDestroy } from "svelte";
    import Shop from "../components/Shop.svelte";
    import Toaster, { type ToastMessage } from "../components/Toaster.svelte";
    import {
        fetchMessages,
        resolveMessage,
        startGame,
        type ActionOutcome,
        type GameState,
        type Message,
        type ReputationGroup,
    } from "../lib/game-api";

    let game: GameState | null = null;
    let messages: Message[] = [];
    let selectedMessage: Message | null = null;
    let busy = false;
    let toasts: ToastMessage[] = [];
    let nextToastId = 0;
    const toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();

    const statistics: { key: keyof GameState; label: string; icon: string }[] = [
        { key: "lives", label: "Lives", icon: "♥" },
        { key: "gold", label: "Gold", icon: "◈" },
        { key: "level", label: "Level", icon: "⌁" },
        { key: "score", label: "Score", icon: "✦" },
        { key: "highScore", label: "Best", icon: "♛" },
        { key: "turn", label: "Day", icon: "↻" },
    ];
    const reputations: ReputationGroup[] = ["people", "state", "underworld"];

    function errorMessage(cause: unknown): string {
        return cause instanceof Error ? cause.message : String(cause);
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
        toasts = [...toasts, { id, type, message }];
        toastTimeouts.set(id, setTimeout(() => dismissToast(id), 5000));
    }

    onDestroy(() => {
        for (const timeout of toastTimeouts.values()) clearTimeout(timeout);
    });

    async function openAdventure() {
        busy = true;
        try {
            game = await startGame();
            messages = await fetchMessages(game.gameId);
        } catch (cause) {
            showToast("error", `The adventure could not be started. ${errorMessage(cause)}`);
        } finally {
            busy = false;
        }
    }

    async function refreshMessages() {
        if (game) {
            messages = await fetchMessages(game.gameId);
        }
    }

    function updateGame(outcome: ActionOutcome) {
        if (!game) {
            return;
        }
        const update = outcome.game ?? outcome;
        const previousLives = game.lives;
        const updatedLives = update.lives;
        game = { ...game, ...update, gameId: update.gameId ?? game.gameId };

        if (previousLives !== undefined && updatedLives !== undefined && updatedLives < previousLives) {
            if (updatedLives === 2) {
                showToast("info", "You have 2 lives left. Visit the shop and buy a pot.");
            } else if (updatedLives === 1) {
                showToast("warning", "You have 1 life left. Visit the shop and buy a pot.");
            }
        }
    }

    async function dealWithMessage() {
        if (!selectedMessage || !game || busy) {
            return;
        }
        busy = true;
        try {
            const outcome = await resolveMessage(game.gameId, selectedMessage.adId);
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
                showToast(
                    "error",
                    `Your response was recorded, but the messages could not be refreshed. ${errorMessage(cause)}`,
                );
            }
        } catch (cause) {
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
        <a class="wordmark" href="/" aria-label="Return home"><span class="mark">✳</span> MUGLOAR <span
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
                <Shop game={game} bind:busy onGameUpdate={updateGame} onToast={showToast} />
            </div>

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

    <footer class="game-footer"><span>KEEP YOUR WITS ABOUT YOU</span><span>✳</span><span>THE DRAGON IS ALWAYS WATCHING</span>
    </footer>
</main>

<Toaster {toasts} onDismiss={dismissToast} />

{#if selectedMessage}
    <div class="modal-backdrop" role="presentation"
         on:click={(event) => event.target === event.currentTarget && (selectedMessage = null)}>
        <div class="message-modal" role="dialog" aria-modal="true" aria-labelledby="message-heading" tabindex="-1">
            <button class="close-button" aria-label="Close message" on:click={() => (selectedMessage = null)}>×</button>
            <p class="eyebrow">A message for the adventurer</p>
            <h2 id="message-heading">{selectedMessage.message}</h2>
            <div class="message-details">
                {#if selectedMessage.reward !== undefined}<span>Reward <strong>{selectedMessage.reward}</strong></span>{/if}
                {#if selectedMessage.probability}
                    <span>Probability <strong>{selectedMessage.probability}</strong></span>{/if}
            </div>
            <p class="modal-prompt">Ready to resolve this task?</p>
            <div class="modal-actions">
                <button class="primary-button" disabled={busy}
                        on:click={dealWithMessage}>{busy ? "Working…" : "Resolve the task"}</button>
            </div>
        </div>
    </div>
{/if}
