package kuchma.mike.bbdm.domain.model;

public record StartedGame(
    String gameId,
    int lives,
    long gold,
    int level,
    int score,
    int highScore,
    int turn
) {
}
