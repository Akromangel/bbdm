package kuchma.mike.bbdm.domain.model;

import java.util.List;

public record MessageBoard(List<GameMessage> messages) {
    public MessageBoard {
        messages = List.copyOf(messages);
    }
}
