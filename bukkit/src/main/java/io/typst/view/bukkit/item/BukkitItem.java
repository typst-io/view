package io.typst.view.bukkit.item;

import lombok.Data;
import lombok.With;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@Data(staticConstructor = "of")
@With
public class BukkitItem {
    private final Material material;
    private final int amount;
    private final short durability;
    private final String name;
    private final List<String> lore;
    private final Integer modelData;
    private final Map<Enchantment, Integer> enchants;

    public static BukkitItem ofJust(Material material) {
        return of(material, 1, (short) 0, "", Collections.emptyList(), 0, Collections.emptyMap());
    }

    public static BukkitItem from(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return of(
            item.getType(),
            item.getAmount(),
            item.getDurability(),
            meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "",
            meta != null && meta.hasLore() ? meta.getLore() : Collections.emptyList(),
            meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : 0,
            meta != null && meta.hasEnchants() ? meta.getEnchants() : Collections.emptyMap()
        );
    }

    public BukkitItem withEnchant(Enchantment enchant, int level) {
        Map<Enchantment, Integer> enchants = new HashMap<>(this.enchants);
        enchants.put(enchant, level);
        return withEnchants(enchants);
    }

    public Map<Enchantment, Integer> getEnchants() {
        return new HashMap<>(enchants);
    }

    @SuppressWarnings("deprecation")
    public void update(ItemStack x) {
        // header
        x.setAmount(getAmount());
        x.setDurability(getDurability());
        // meta
        ItemMeta meta = x.getItemMeta();
        if (meta != null) {
            if (!getName().isEmpty()) {
                meta.setDisplayName(getName());
            }
            if (!getLore().isEmpty()) {
                meta.setLore(new ArrayList<>(getLore()));
            }
            if (getModelData() != 0) {
                meta.setCustomModelData(getModelData());
            }
            x.setItemMeta(meta);
        }
        for (Map.Entry<Enchantment, Integer> pair : getEnchants().entrySet()) {
            x.addUnsafeEnchantment(pair.getKey(), pair.getValue());
        }
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material);
        update(item);
        return item;
    }
}
