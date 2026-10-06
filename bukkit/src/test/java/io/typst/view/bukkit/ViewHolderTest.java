package io.typst.view.bukkit;

import io.typst.inventory.bukkit.BukkitItemStackOps;
import io.typst.view.ChestView;
import io.typst.view.UpdateEvent;
import io.typst.view.ViewContents;
import io.typst.view.ViewControl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ViewHolderTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reportsTheCurrentEditableItemsAndPlayerAfterUpdatingItsSnapshot() {
        PlayerMock player = server.addPlayer();
        AtomicReference<UpdateEvent<ItemStack, Player>> notified = new AtomicReference<>();
        ViewContents<ItemStack, Player> previous = ViewContents.of(
                Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER))),
                Map.of(1, new ItemStack(Material.DIAMOND, 8)));
        ChestView<ItemStack, Player> view = ChestView.<ItemStack, Player>builder(BukkitItemStackOps.INSTANCE)
                .contents(previous).onContentsUpdate(notified::set).build();
        ViewHolder holder = new ViewHolder(MockBukkit.createMockPlugin(), BukkitItemStackOps.INSTANCE);
        Inventory inventory = Bukkit.createInventory(holder, 9);
        holder.setInventory(inventory);
        holder.setView(view);
        inventory.setItem(0, new ItemStack(Material.BARRIER));
        inventory.setItem(2, new ItemStack(Material.GOLD_INGOT, 4));

        holder.updateViewContentsWithPlayer(player);

        assertEquals(Map.of(2, new ItemStack(Material.GOLD_INGOT, 4)), holder.getView().getContents().getItems());
        assertSame(player, notified.get().getPlayer());
        assertEquals(holder.getView().getContents().getItems(), notified.get().getItems());
        assertEquals(8, previous.getItems().get(1).getAmount());
    }
}
