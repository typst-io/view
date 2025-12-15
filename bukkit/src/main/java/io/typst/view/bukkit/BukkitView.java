package io.typst.view.bukkit;

import io.typst.inventory.ItemStackOps;
import io.typst.inventory.bukkit.BukkitInventoryAdapter;
import io.typst.inventory.bukkit.BukkitItemStackOps;
import io.typst.view.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class BukkitView {
    /**
     * Open the given view as a new inventory.
     * This will cause InventoryCloseEvent and InventoryOpenEvent.
     */
    public static void openView(ChestView<ItemStack, Player> view, Player player, Plugin plugin) {
        ViewHolder holder = new ViewHolder(plugin, view.getItemOps());
        holder.setView(view);
        Inventory inv = Bukkit.createInventory(holder, view.getRow() * 9, view.getTitle());
        holder.setInventory(inv);
        OpenEvent<ItemStack, Player> event = new OpenEvent<>(player, view);
        updateInventory(view.getContents(), inv, event);
        player.openInventory(inv);
    }

    /**
     * Update the inventory to the given items of view.
     * This won't cause InventoryCloseEvent and InventoryOpenEvent.
     * If the new controls overwrite a player accessible slot, then the item will be returned back to player.
     *
     * @return false if the view can't be updated -- the title and size of inventory player seeing is different from the given view; true if success.
     */
    public static boolean updateView(ChestView<ItemStack, Player> newView, Player player) {
        Inventory topInv = player.getOpenInventory().getTopInventory();
        InventoryHolder holder = topInv.getHolder();
        String title = player.getOpenInventory().getTitle();
        int size = topInv.getSize();
        if (holder instanceof ViewHolder && title.equals(newView.getTitle()) && size == (newView.getRow() * 9)) {
            ViewHolder viewHolder = (ViewHolder) holder;
            // update contents
            viewHolder.setView(newView);
            updateInventory(newView.getContents(), topInv, new OpenEvent<ItemStack, Player>(player, newView));
            return true;
        }
        return false;
    }

    private static void updateInventory(ViewContents<ItemStack, Player> contents, Inventory inv, OpenEvent<ItemStack, Player> event) {
        inv.clear();
        for (Map.Entry<Integer, ViewControl<ItemStack, Player>> pair : contents.getControls().entrySet()) {
            inv.setItem(pair.getKey(), pair.getValue().getItem().apply(event));
        }
        for (Map.Entry<Integer, ItemStack> pair : contents.getItems().entrySet()) {
            inv.setItem(pair.getKey(), pair.getValue());
        }
    }

    public static Listener viewListener(ItemStackOps<ItemStack> itemOps, Plugin plugin) {
        return new BukkitViewListener(plugin, itemOps);
    }

    public static void register(ItemStackOps<ItemStack> itemOps, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(viewListener(itemOps, plugin), plugin);
    }

    public static void register(Plugin plugin) {
        register(BukkitItemStackOps.INSTANCE, plugin);
    }

    private static class BukkitViewListener implements Listener {
        private final Plugin plugin;
        private ItemStackOps<ItemStack> itemOps;

        public BukkitViewListener(Plugin plugin, ItemStackOps<ItemStack> itemOps) {
            this.plugin = plugin;
            this.itemOps = itemOps;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (e.getAction() == InventoryAction.NOTHING) {
                return;
            }
            Inventory topInv = e.getView().getTopInventory();
            Inventory bottomInv = e.getView().getBottomInventory();
            ViewHolder holder = topInv.getHolder() instanceof ViewHolder ? ((ViewHolder) topInv.getHolder()) : null;
            if (holder == null || !holder.getPlugin().getName().equals(plugin.getName())) {
                return;
            }
            ChestView<ItemStack, Player> view = holder.getView();
            if (view == null) {
                e.setCancelled(true);
                return;
            }
            // update user input items
            Player p = (Player) e.getWhoClicked();
            ViewControl<ItemStack, Player> viewControl = view.getContents().getControls().get(e.getRawSlot());
            // Cancel if tried to move the control items


            switch (e.getClick()) {
                case LEFT:
                case RIGHT:
                    if (viewControl != null) {
                        // don't cancel on pickup a slot that conflicts between items and controls
                        ItemStack cursor = e.getCursor();
                        if ((cursor == null || cursor.getType() == Material.AIR) && view.getContents().getItems().containsKey(e.getRawSlot())) {
                            // set control item
                            view.getContents().getItems().remove(e.getRawSlot());
                            runSync(() -> topInv.setItem(e.getRawSlot(), viewControl.getItem(new OpenEvent(p, view))));
                        } else {
                            e.setCancelled(true);
                        }
                    }
                    break;
                case SHIFT_LEFT:
                    Inventory clickedInv = e.getClickedInventory();
                    if (clickedInv == null) {
                        break;
                    }
                    if (viewControl != null) {
                        e.setCancelled(true);
                    }
                    Inventory targetInventory = e.getView().getTopInventory().equals(clickedInv)
                            ? bottomInv
                            : e.getView().getTopInventory();
                    int targetSlot = targetInventory.firstEmpty();
                    if (clickedInv.equals(bottomInv) && view.getContents().getControls().containsKey(targetSlot)) {
                        e.setCancelled(true);
                    }
                    // override target slots
                    List<InputSlot> overriddenSlots = view.getOverrideMoveToOtherInventorySlots();
                    if (clickedInv.equals(bottomInv) && !overriddenSlots.isEmpty()) {
                        ItemStack item = e.getCurrentItem();
                        if (item == null || item.getType() == Material.AIR) {
                            return;
                        }
                        List<Integer> slots = overriddenSlots.stream()
                                .flatMap(slot -> (slot.getWhitelist().isEmpty() || slot.getWhitelist().contains(item.getType().getKey().toString()))
                                        ? Stream.of(slot.getSlot())
                                        : Stream.empty())
                                .toList();
                        List<Integer> targetEmptySlots = view.findSpaces(slots, item);
                        // cancel if there's no space
                        if (targetEmptySlots.isEmpty()) {
                            e.setCancelled(true);
                        }
                        // only if the default target slot and the overwritten slot is different
                        if (!targetEmptySlots.isEmpty() && !targetEmptySlots.equals(Collections.singletonList(targetSlot))) {
                            e.setCancelled(true);
                            runSync(() -> {
                                InventoryHolder theHolder = targetInventory.getHolder();
                                ViewHolder viewHolder = theHolder instanceof ViewHolder ? ((ViewHolder) theHolder) : null;
                                ChestView theView = viewHolder != null ? viewHolder.getView() : null;
                                if (theView == null) {
                                    return;
                                }
                                ItemStack clickedItem = clickedInv.getItem(e.getSlot());
                                // check the item is equal after 1 tick
                                if (clickedItem == null || !clickedItem.equals(item)) {
                                    return;
                                }
                                List<Integer> newTargetSlots = theView.findSpaces(slots, item);
                                if (newTargetSlots.isEmpty()) {
                                    return;
                                }
                                // add
                                for (Integer newTargetSlot : newTargetSlots) {
                                    ItemStack targetItem = targetInventory.getItem(newTargetSlot);
                                    if (clickedItem.getAmount() <= 0) {
                                        break;
                                    }
                                    if (targetItem == null || targetItem.getType() == Material.AIR) {
                                        targetInventory.setItem(newTargetSlot, clickedItem);
                                        clickedInv.setItem(e.getSlot(), null);
                                        clickedItem.setAmount(0);
                                    } else if (targetItem.isSimilar(clickedItem)) {
                                        int oldAmount = targetItem.getAmount();
                                        int newAmount = Math.min(oldAmount + clickedItem.getAmount(), targetItem.getType().getMaxStackSize());
                                        targetItem.setAmount(newAmount);
                                        clickedItem.setAmount(clickedItem.getAmount() - (newAmount - oldAmount));
                                    }
                                }
                                viewHolder.updateViewContentsWithPlayer(p);
                            });
                        }
                    } else {
                        ItemStack clickedItem = e.getView().getItem(e.getRawSlot());
                        int slot = clickedItem != null ? topInv.first(clickedItem) : -1;
                        if (slot >= 0 && view.getContents().getControls().containsKey(slot)) {
                            e.setCancelled(true);
                        }
                    }
                    break;
                default:
                    e.setCancelled(true);
                    break;
            }
            // Notify control onClick
            if (viewControl != null) {
                ViewAction<ItemStack, Player> action;
                try {
                    action = viewControl.getOnClick().apply(new ClickEvent<>(view, p, e.getClick().name(), e.getAction().name(), e.getHotbarButton()));
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, ex, () -> "Error on inventory click!");
                    // To block after actions
                    action = new ViewAction.Close<>(true);
                }
                handleAction(p, holder, action);
            }
            // update user input items
            runSync(() -> holder.updateViewContentsWithPlayer(p));
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            Inventory topInv = e.getView().getTopInventory();
            ViewHolder holder = topInv.getHolder() instanceof ViewHolder ? ((ViewHolder) topInv.getHolder()) : null;
            if (holder == null || !holder.getPlugin().getName().equals(plugin.getName())) {
                return;
            }
            ChestView<ItemStack, Player> view = holder.getView();
            if (view == null) {
                e.setCancelled(true);
                return;
            }
            // update user input items
            Player p = (Player) e.getWhoClicked();
            ChestView<ItemStack, Player> newView = view.withContents(view.getContents().updated(itemOps, new BukkitInventoryAdapter(topInv, itemOps.empty())));
            holder.setView(newView);
            if (
                    e.getRawSlots().stream()
                            .anyMatch(a -> newView.getContents().getControls().get(a) != null)
            ) {
                e.setCancelled(true);
            }
            runSync(() -> holder.updateViewContentsWithPlayer(p));
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e) {
            Inventory topInv = e.getView().getTopInventory();
            ViewHolder holder = topInv.getHolder() instanceof ViewHolder ? ((ViewHolder) topInv.getHolder()) : null;
            if (holder == null || !holder.getPlugin().getName().equals(plugin.getName())) {
                return;
            }
            Player p = (Player) e.getPlayer();
            ChestView<ItemStack, Player> view = holder.getView();
            if (view == null) {
                return;
            }
            boolean giveBackInputItems = holder.isGiveBackItems();
            ViewAction<ItemStack, Player> action = ViewAction.nothing();
            try {
                action = view.getOnClose().apply(new CloseEvent<>(p, view));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, ex, () -> "Error on inventory close!");
            }
            if (action instanceof ViewAction.Close<ItemStack, Player> close) {
                giveBackInputItems = close.isGiveBackItems();
            } else {
                handleAction(p, holder, action);
            }

            // modal
            if (!holder.isDirty() && view.getParent() != null) {
                runSync(() -> {
                    if (p.isOnline()) {
                        openView(view.getParent(), p, plugin);
                    }
                });
            }

            // give back the items
            if (giveBackInputItems) {
                runSync(() -> {
                    if (p.isOnline()) {
                        giveBackContents(view, p);
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        private void handleAction(Player p, ViewHolder holder, ViewAction<ItemStack, Player> action) {
            ChestView<ItemStack, Player> currentView = holder.getView();
            if (currentView == null) {
                return;
            }
            if (action instanceof ViewAction.Open<?, ?> && !holder.isDirty()) {
                ViewAction.Open<ItemStack, Player> open = (ViewAction.Open<ItemStack, Player>) action;
                holder.setDirty(true);
                runSync(() -> openView(open.getView(), p, plugin));
            } else if (action instanceof ViewAction.Open<ItemStack, Player>) {
                runSync(() -> openView(currentView, p, plugin));
            } else if (action instanceof ViewAction.Close<ItemStack, Player>) {
                holder.setGiveBackItems(((ViewAction.Close<ItemStack, Player>) action).isGiveBackItems());
                runSync(p::closeInventory);
            } else if (action instanceof ViewAction.OpenAsync<ItemStack, Player> openAsync) {
                runAsync(() -> {
                    try {
                        ChestView<ItemStack, Player> chestView = openAsync.getFuture().get(30, TimeUnit.SECONDS);
                        runSync(() -> handleAction(p, holder, new ViewAction.Open<>(chestView)));
                    } catch (Exception ex) {
                        handleException(plugin.getLogger(), ex);
                    }
                });
            } else if (action instanceof ViewAction.Update<ItemStack, Player> update) {
                ChestView<ItemStack, Player> newView = currentView.withContents(update.getContents());
                updateInventory(update.getContents(), holder.getInventory(), new OpenEvent<ItemStack, Player>(p, newView));
                holder.setView(newView);
            } else if (action instanceof ViewAction.UpdateAsync<ItemStack, Player> updateAsync) {
                runAsync(() -> {
                    try {
                        ViewContents<ItemStack, Player> contents = updateAsync.getContentsFuture().get(30, TimeUnit.SECONDS);
                        runSync(() -> {
                            ChestView<ItemStack, Player> newView = currentView.withContents(contents);
                            updateInventory(contents, holder.getInventory(), new OpenEvent<>(p, newView));
                            holder.setView(newView);
                        });
                    } catch (Exception ex) {
                        handleException(plugin.getLogger(), ex);
                    }
                });
            }
        }

        private static void handleException(Logger logger, Throwable throwable) {
            if (throwable instanceof ExecutionException) {
                handleException(logger, throwable.getCause());
            } else if (!(throwable instanceof CancellationException) && !(throwable instanceof TimeoutException)) {
                logger.log(Level.WARNING, throwable, () -> "Error while getting a view to open.");
            }
        }

        private static void updateView(Inventory inv, ChestView view) {
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (
                        item != null && item.getType() != Material.AIR &&
                                !view.getContents().getControls().containsKey(i)
                ) {
                    view.getContents().getItems().put(i, item);
                } else {
                    view.getContents().getItems().remove(i);
                }
            }
        }

        private static void giveBackContents(ChestView<ItemStack, Player> view, Player p) {
            ItemStack[] items = view.getContents().getItems().values().stream()
                    .filter(item -> item != null && item.getType() != Material.AIR)
                    .toArray(ItemStack[]::new);
            HashMap<Integer, ItemStack> failures = p.getInventory().addItem(items);
            for (ItemStack item : failures.values()) {
                p.getWorld().dropItem(p.getEyeLocation(), item);
            }
        }

        private void runSync(Runnable runnable) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }

        private void runAsync(Runnable runnable) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}
