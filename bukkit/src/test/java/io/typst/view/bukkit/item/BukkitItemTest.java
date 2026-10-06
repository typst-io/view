package io.typst.view.bukkit.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BukkitItemTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void buildsTheConfiguredMaterialAmountAndMetadata() {
        BukkitItem specification = BukkitItem.of(Material.DIAMOND_SWORD, 1, (short) 3,
                "Test sword", List.of("First line", "Second line"), 42, Map.of(Enchantment.UNBREAKING, 2));

        ItemStack item = specification.build();

        assertEquals(Material.DIAMOND_SWORD, item.getType());
        assertEquals(1, item.getAmount());
        assertEquals(3, item.getDurability());
        assertEquals("Test sword", item.getItemMeta().getDisplayName());
        assertEquals(List.of("First line", "Second line"), item.getItemMeta().getLore());
        assertEquals(42, item.getItemMeta().getCustomModelData());
        assertEquals(2, item.getEnchantmentLevel(Enchantment.UNBREAKING));
    }

    @Test
    void preservesSupportedMetadataWhenConvertingAnItemBackToASpecification() {
        ItemStack original = BukkitItem.ofJust(Material.DIAMOND_SWORD)
                .withName("Named sword").withLore(List.of("Lore")).withModelData(7)
                .withEnchant(Enchantment.UNBREAKING, 2).build();

        assertEquals(original, BukkitItem.from(original).build());
    }

    @Test
    void addingTheFirstEnchantmentLeavesTheOriginalSpecificationUnenchanted() {
        BukkitItem original = BukkitItem.ofJust(Material.DIAMOND_SWORD);

        BukkitItem enchanted = original.withEnchant(Enchantment.UNBREAKING, 2);

        assertEquals(Map.of(), original.getEnchants());
        assertEquals(Map.of(Enchantment.UNBREAKING, 2), enchanted.getEnchants());
    }

    @Test
    @Tag("regression")
    void addingAnEnchantmentDoesNotMutateAnExistingSpecification() {
        BukkitItem original = BukkitItem.ofJust(Material.DIAMOND_SWORD)
                .withEnchants(new HashMap<>(Map.of(Enchantment.UNBREAKING, 2)));

        BukkitItem updated = original.withEnchant(Enchantment.MENDING, 1);

        assertFalse(original.getEnchants().containsKey(Enchantment.MENDING));
        assertEquals(Map.of(Enchantment.UNBREAKING, 2, Enchantment.MENDING, 1), updated.getEnchants());
    }

    @Test
    @Tag("regression")
    void canAddAnEnchantmentToASpecificationCreatedFromAnEnchantedItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 2);
        BukkitItem original = BukkitItem.from(item);

        BukkitItem updated = assertDoesNotThrow(() -> original.withEnchant(Enchantment.MENDING, 1));

        assertEquals(Map.of(Enchantment.UNBREAKING, 2), original.getEnchants());
        assertEquals(Map.of(Enchantment.UNBREAKING, 2, Enchantment.MENDING, 1), updated.getEnchants());
    }
}
