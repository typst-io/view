package io.typst.view.bukkit;

import io.typst.view.ChestView;
import io.typst.view.UpdateEvent;
import io.typst.inventory.ItemStackOps;
import io.typst.inventory.bukkit.BukkitInventoryAdapter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ViewHolder implements InventoryHolder {
    private final Plugin plugin;
    private final ItemStackOps<ItemStack> itemOps;
    private ChestView<ItemStack, Player> view = null;
    private Inventory inventory = null;
    private boolean giveBackItems = true;
    private boolean dirty = false;
    private boolean closed = false;
    private long revision = 0;
    private boolean transferringItems = false;

    public ViewHolder(Plugin plugin, ItemStackOps<ItemStack> itemOps) {
        this.plugin = plugin;
        this.itemOps = itemOps;
    }

    /*
    user input contents 업데이트 시점:
    - onClick
    - onDrag
     */
    void updateViewContents() {
        ChestView<ItemStack, Player> view = getView();
        Inventory inv = getInventory();
        if (view == null || closed) {
            return;
        }
        // A snapshot is part of the same view revision, including while an async action is pending.
        this.view = view.withContents(view.getContents().updated(
                itemOps, new BukkitInventoryAdapter(inv, itemOps.empty())));
    }

    void updateViewContentsWithPlayer(Player player) {
        updateViewContents();
        ChestView<ItemStack, Player> view = getView();
        if (view != null && !closed) {
            view.getOnContentsUpdate().accept(new UpdateEvent<>(player, view.getContents().getItems()));
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Nullable
    public ChestView<ItemStack, Player> getView() {
        return view;
    }

    public void setView(ChestView<ItemStack, Player> view) {
        this.view = view;
        revision++;
    }

    boolean isClosed() {
        return closed;
    }

    void markClosed() {
        closed = true;
    }

    long getRevision() {
        return revision;
    }

    boolean isTransferringItems() {
        return transferringItems;
    }

    void setTransferringItems(boolean transferringItems) {
        this.transferringItems = transferringItems;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isGiveBackItems() {
        return giveBackItems;
    }

    public void setGiveBackItems(boolean giveBackItems) {
        this.giveBackItems = giveBackItems;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
