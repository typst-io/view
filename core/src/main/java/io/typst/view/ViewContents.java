package io.typst.view;

import io.typst.inventory.InventoryAdapter;
import io.typst.inventory.ItemStackOps;
import lombok.Value;
import lombok.With;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value(staticConstructor = "of")
@With
public class ViewContents<I, P> {
    /**
     * Fixed, readonly slot controls.
     */
    Map<Integer, ViewControl<I, P>> controls;
    /**
     * Player accessible slot items.
     */
    Map<Integer, I> items;

    /**
     * Update the player accessible slots from the given inventory.
     */
    public ViewContents<I, P> updated(ItemStackOps<I> itemOps, InventoryAdapter<I> inv) {
        Map<Integer, I> newItems = new HashMap<>(items);
        for (Map.Entry<Integer, I> pair : inv) {
            int slot = pair.getKey();
            if (controls.containsKey(slot)) continue;
            I item = inv.get(slot);
            if (itemOps.isEmpty(item)) {
                newItems.remove(slot);
            } else {
                newItems.put(slot, item);
            }
        }
        return withItems(newItems);
    }

    public static <I, P> ViewContents<I, P> ofControls(Map<Integer, ViewControl<I, P>> controls) {
        return new ViewContents<>(controls, Collections.emptyMap());
    }
}
