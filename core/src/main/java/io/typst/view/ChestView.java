package io.typst.view;

import io.typst.inventory.ItemStackOps;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Value(staticConstructor = "of")
@With
@Builder
public class ChestView<I, P> {
    @Builder.Default
    String title = "";
    @Builder.Default
    int row = 6;
    @Builder.Default
    ViewContents<I, P> contents = ViewContents.ofControls(Collections.emptyMap());
    @Builder.Default
    Function<CloseEvent<I, P>, ViewAction<I, P>> onClose = e -> ViewAction.nothing();
    @Builder.Default
    Consumer<UpdateEvent<I, P>> onContentsUpdate = e -> {
    };
    @Builder.Default
    List<InputSlot> overrideMoveToOtherInventorySlots = Collections.emptyList();
    @Builder.Default
    ChestView<I, P> parent = null;
    ItemStackOps<I> itemOps;

    public List<Integer> findSpaces(List<Integer> slots, I item) {
        List<Integer> ret = new ArrayList<>();

        int amount = itemOps.getAmount(item);
        for (Integer slot : slots) {
            if (getContents().getControls().containsKey(slot)) {
                continue;
            }
            if (amount <= 0) {
                break;
            }
            I slotItem = getContents().getItems().get(slot);
            if (itemOps.isEmpty(slotItem)) {
                ret.add(slot);
                amount -= itemOps.getMaxStackSize(item);
            } else if (itemOps.isSimilar(slotItem, item)) {
                int newAmount = Math.min(itemOps.getAmount(slotItem) + itemOps.getAmount(item), itemOps.getMaxStackSize(slotItem));
                if (newAmount > itemOps.getAmount(slotItem)) {
                    ret.add(slot);
                    amount -= newAmount - itemOps.getAmount(slotItem);
                }
            }
        }
        return ret;
    }
}
