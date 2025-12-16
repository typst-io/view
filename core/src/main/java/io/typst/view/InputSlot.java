package io.typst.view;

import lombok.Value;
import lombok.With;

import java.util.Set;

@Value
@With
public class InputSlot {
    int slot;
    /**
     * Minecraft: NamespacedKey#toString from Material
     *
     * Empty means no item whitelist for the slot
     */
    Set<String> whitelist;
}
