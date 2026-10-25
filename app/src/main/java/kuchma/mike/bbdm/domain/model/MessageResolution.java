package kuchma.mike.bbdm.domain.model;

public record MessageResolution(
    boolean success,
    int lives,
    long gold,
    int score,
    int highScore,
    int turn,
    String message
) {
}
