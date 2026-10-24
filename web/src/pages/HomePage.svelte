<svelte:head>
    <title>Visual Adventure — Mugloar</title>
</svelte:head>

<main class="landing">
    <div class="landing-content">
        <button class="primary-button" onclick={() => window.location.assign("/game")}>
            Visual adventure <span aria-hidden="true">↗</span>
        </button>
        <button
            class="primary-button"
            onclick={() => window.location.assign("/scripting-adventure")}
        >
            Scripting adventure <span aria-hidden="true">↗</span>
        </button>
    </div>
</main>
