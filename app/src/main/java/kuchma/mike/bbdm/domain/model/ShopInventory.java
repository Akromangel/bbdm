package kuchma.mike.bbdm.domain.model;

import java.util.List;

public record ShopInventory(List<ShopItem> items) {
    public ShopInventory {
        items = List.copyOf(items);
    }
}
