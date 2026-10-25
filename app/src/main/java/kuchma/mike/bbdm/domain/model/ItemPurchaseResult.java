package kuchma.mike.bbdm.domain.model;

public record ItemPurchaseResult(boolean shoppingSuccess, long gold, int lives, int level, int turn) {
}
