package kuchma.mike.bbdm.domain.model;

public record GameMessage(MessageId adId, String message, String reward, int expiresIn) {
}
