<script lang="ts">
    import {onDestroy, onMount} from "svelte";
    import {
        type ScriptingAdventureUpdate,
        startScriptingAdventureSession,
    } from "../lib/game-api";

    const MAX_UPDATES = 20;

    let countdown: number | null = 6;
    let status = "The adventure begins shortly.";
    let updates: ScriptingAdventureUpdate[] = [];
    let eventSource: EventSource | null = null;
    let countdownTimer: ReturnType<typeof setInterval> | undefined;
    let sessionStarted = false;

    function showUpdate(event: Event) {
        const update = JSON.parse((event as MessageEvent<string>).data) as ScriptingAdventureUpdate;
        updates = [update, ...updates].slice(0, MAX_UPDATES);
        if (update.complete) {
            eventSource?.close();
            status = update.error ? `Session stopped: ${update.error}` : "Adventure complete";
        }
    }

    function startOnConnection(sessionId: string) {
        if (sessionStarted) {
            return;
        }
        sessionStarted = true;
        status = "Starting the adventure…";
        void beginSession(sessionId);
    }

    async function beginSession(sessionId: string) {
        try {
            await startScriptingAdventureSession(sessionId);
            if (!updates.some((update) => update.complete)) {
                status = "The dragon's realm is being explored…";
            }
        } catch (cause) {
            eventSource?.close();
            status = `Could not start the adventure: ${cause instanceof Error ? cause.message : String(cause)}`;
        }
    }

    function connectToSession() {
        const sessionId = crypto.randomUUID();
        eventSource = new EventSource(
            `/api/scripting-adventure/sessions/${encodeURIComponent(sessionId)}/events?apiVersion=1.0`,
        );
        eventSource.addEventListener("state", showUpdate);
        eventSource.addEventListener("connected", () => startOnConnection(sessionId));
        eventSource.onopen = () => {
            status = "Connected to the adventure stream; waiting for server confirmation…";
        };
        eventSource.onerror = () => {
            status = sessionStarted
                ? "The adventure stream disconnected."
                : "Could not connect to the adventure stream; retrying…";
        };
    }

    onMount(() => {
        countdownTimer = setInterval(() => {
            if (countdown === null) {
                return;
            }
            if (countdown === 1) {
                countdown = null;
                clearInterval(countdownTimer);
                connectToSession();
                return;
            }
            countdown -= 1;
        }, 1000);
    });

    onDestroy(() => {
        if (countdownTimer !== undefined) {
            clearInterval(countdownTimer);
        }
        eventSource?.close();
    });
</script>

<svelte:head>
    <title>Scripting Adventure — Mugloar</title>
</svelte:head>

<main class="game-shell">
    <header class="topbar">
        <a class="wordmark" href="/" aria-label="Return home"><span class="mark">✳</span> MUGLOAR <span
            class="wordmark-sub">/ FIELD JOURNAL</span></a>
    </header>

    <section class="dashboard scripting-dashboard">
        <div class="dashboard-heading">
            <div>
                <p class="eyebrow">Scripting adventure</p>
                <h1>Automated expedition <span class="gold-symbol">✦</span></h1>
            </div>
            {#if countdown !== null}
                <div class="countdown" aria-live="polite">
                    <span>Adventure starts in</span>
                    <strong>{countdown}</strong>
                </div>
            {/if}
        </div>

        <p class="adventure-status" role="status">{status}</p>

        <section class="adventure-log" aria-labelledby="adventure-log-heading">
            <div class="section-caption">
                <span id="adventure-log-heading">ADVENTURE LOG</span>
                <span class="caption-line"></span>
                <span>{updates.length} events</span>
            </div>
            {#if updates.length > 0}
                <ol class="adventure-events" aria-live="polite">
                    {#each updates as update, index (`${update.turn}-${update.task}-${index}`)}
                        <li class="adventure-event" class:adventure-event-newest={index === 0}>
                            <div class="event-heading">
                                <strong>{update.task || "Session update"}</strong>
                                <span>TURN {update.turn}</span>
                            </div>
                            <p>{update.message}</p>
                            <div class="event-state">
                                <span>♥ {update.lives} lives</span>
                                <span>◈ {update.gold} gold</span>
                                <span>✦ {update.score} score</span>
                                <span>⌁ Level {update.level}</span>
                                <span>♜ {update.hpotCount} Healing potion bought</span>
                            </div>
                        </li>
                    {/each}
                </ol>
            {:else}
                <div class="adventure-empty">
                    <span>✧</span>
                    <p>Waiting for the first update from the expedition…</p>
                </div>
            {/if}
        </section>
    </section>

    <footer class="game-footer">
        <span>KEEP YOUR WITS ABOUT YOU</span><span>✳</span><span>THE DRAGON IS ALWAYS WATCHING</span>
    </footer>
</main>
