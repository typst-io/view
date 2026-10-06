package io.typst.view.bukkit;

import io.typst.inventory.bukkit.BukkitItemStackOps;
import io.typst.view.ChestView;
import io.typst.view.ClickEvent;
import io.typst.view.InputSlot;
import io.typst.view.ViewAction;
import io.typst.view.ViewContents;
import io.typst.view.ViewControl;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitViewTest {
    private ServerMock server;
    private Plugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        player = server.addPlayer();
        BukkitView.register(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void opensControlsAndEditableItemsInTheRequestedSlots() {
        ChestView<ItemStack, Player> view = ChestView.<ItemStack, Player>builder(BukkitItemStackOps.INSTANCE)
                .title("Test view")
                .row(2)
                .contents(ViewContents.of(
                        Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER))),
                        Map.of(1, new ItemStack(Material.DIAMOND, 8))))
                .build();

        BukkitView.openView(view, player, plugin);

        Inventory inventory = player.getOpenInventory().getTopInventory();
        assertEquals("Test view", player.getOpenInventory().getTitle());
        assertEquals(18, inventory.getSize());
        assertEquals(Material.BARRIER, inventory.getItem(0).getType());
        assertEquals(8, inventory.getItem(1).getAmount());
        assertEquals(view, assertInstanceOf(ViewHolder.class, inventory.getHolder()).getView());
    }

    @Test
    void cancelsTakingControlItemsButStillNotifiesTheControl() {
        AtomicReference<ClickEvent<ItemStack, Player>> received = new AtomicReference<>();
        open(ViewContents.ofControls(Map.of(0, ViewControl.consumer(new ItemStack(Material.BARRIER), received::set))));

        InventoryClickEvent click = click(0, InventoryAction.PICKUP_ALL);

        assertTrue(click.isCancelled());
        assertSame(player, received.get().getPlayer());
        assertEquals("LEFT", received.get().getClick());
        assertEquals(Material.BARRIER, player.getOpenInventory().getTopInventory().getItem(0).getType());
    }

    @Test
    void snapshotsDepositedItemsAfterTheClickHasBeenApplied() {
        open(ViewContents.ofControls(Map.of()));
        ViewHolder holder = holder();

        InventoryClickEvent click = click(0, InventoryAction.PLACE_ALL);
        assertFalse(click.isCancelled());
        // Dispatching an event does not apply the vanilla inventory transaction.
        holder.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 8));
        server.getScheduler().performOneTick();

        assertEquals(8, holder.getView().getContents().getItems().get(0).getAmount());
    }

    @Test
    void cancelsDraggingItemsIntoControlSlots() {
        open(ViewContents.ofControls(Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER)))));
        InventoryDragEvent drag = new InventoryDragEvent(player.getOpenInventory(),
                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND, 2), false,
                Map.of(0, new ItemStack(Material.DIAMOND)));

        server.getPluginManager().callEvent(drag);

        assertTrue(drag.isCancelled());
        assertEquals(Material.BARRIER, player.getOpenInventory().getTopInventory().getItem(0).getType());
    }

    @Test
    void returnsEditableItemsAndExcludesControlsWhenClosedNormally() {
        open(ViewContents.of(Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER))),
                Map.of(1, new ItemStack(Material.DIAMOND, 8))));

        player.closeInventory();
        server.getScheduler().performOneTick();

        assertEquals(8, inventoryAmount(Material.DIAMOND));
        assertEquals(0, inventoryAmount(Material.BARRIER));
    }

    @Test
    void honorsTheCloseCallbackWhenInputItemsMustNotBeReturned() {
        ChestView<ItemStack, Player> view = baseView(ViewContents.of(Map.of(),
                Map.of(0, new ItemStack(Material.DIAMOND, 8)))).withOnClose(event -> new ViewAction.Close<>(false));
        BukkitView.openView(view, player, plugin);

        player.closeInventory();
        server.getScheduler().performOneTick();

        assertEquals(0, inventoryAmount(Material.DIAMOND));
    }

    @Test
    void rejectsUpdatesThatChangeTheTitleOrSize() {
        ChestView<ItemStack, Player> view = baseView(ViewContents.ofControls(Map.of()));
        BukkitView.openView(view, player, plugin);
        Inventory originalInventory = player.getOpenInventory().getTopInventory();

        assertFalse(BukkitView.updateView(view.withTitle("Different"), player));
        assertFalse(BukkitView.updateView(view.withRow(2), player));
        assertSame(originalInventory, player.getOpenInventory().getTopInventory());
        assertSame(view, holder().getView());
    }

    @Test
    void openActionTransitionsToTheRequestedViewOnTheNextTick() {
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("Target");
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.COMPASS),
                event -> new ViewAction.Open<>(target)))));

        click(0, InventoryAction.PICKUP_ALL);
        server.getScheduler().performOneTick();

        assertSame(target, holder().getView());
        assertEquals("Target", player.getOpenInventory().getTitle());
    }

    @Test
    void updateActionReplacesControlsWithoutReopeningTheInventory() {
        ViewContents<ItemStack, Player> replacement = ViewContents.ofControls(
                Map.of(1, ViewControl.just(new ItemStack(Material.DIAMOND))));
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.CLOCK),
                event -> new ViewAction.Update<>(replacement)))));
        Inventory originalInventory = player.getOpenInventory().getTopInventory();

        click(0, InventoryAction.PICKUP_ALL);

        assertSame(originalInventory, player.getOpenInventory().getTopInventory());
        assertEquals(Material.DIAMOND, originalInventory.getItem(1).getType());
        assertEquals(replacement, holder().getView().getContents());
    }

    @Test
    @Timeout(5)
    void asyncOpenActionOpensItsResultWhileTheOriginalViewIsStillActive() {
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("Async target");
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.COMPASS),
                event -> new ViewAction.OpenAsync<>(CompletableFuture.completedFuture(target))))));

        click(0, InventoryAction.PICKUP_ALL);
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        assertSame(target, holder().getView());
        assertEquals("Async target", player.getOpenInventory().getTitle());
    }

    @Test
    @Timeout(5)
    void asyncUpdateActionUpdatesTheCurrentInventoryWithoutReopeningIt() {
        ViewContents<ItemStack, Player> replacement = ViewContents.ofControls(
                Map.of(1, ViewControl.just(new ItemStack(Material.DIAMOND))));
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.CLOCK),
                event -> new ViewAction.UpdateAsync<>(CompletableFuture.completedFuture(replacement))))));
        Inventory originalInventory = player.getOpenInventory().getTopInventory();

        click(0, InventoryAction.PICKUP_ALL);
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();

        assertSame(originalInventory, player.getOpenInventory().getTopInventory());
        assertEquals(Material.DIAMOND, originalInventory.getItem(1).getType());
        assertEquals(replacement, holder().getView().getContents());
    }

    @Test
    void closingAModalViewReopensItsParent() {
        ChestView<ItemStack, Player> parent = baseView(ViewContents.ofControls(Map.of())).withTitle("Parent");
        ChestView<ItemStack, Player> child = baseView(ViewContents.ofControls(Map.of()))
                .withTitle("Child").withParent(parent);
        BukkitView.openView(child, player, plugin);

        player.closeInventory();
        server.getScheduler().performOneTick();

        assertSame(parent, holder().getView());
        assertEquals("Parent", player.getOpenInventory().getTitle());
    }

    @Test
    void shiftClickMovesAWhitelistedItemToTheConfiguredInputSlot() {
        openRestrictedInputView();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 8));

        InventoryClickEvent click = shiftClickHotbarSlotZero();
        server.getScheduler().performOneTick();

        assertTrue(click.isCancelled(), "The listener replaces the default transfer");
        assertEquals(8, player.getOpenInventory().getTopInventory().getItem(2).getAmount());
        assertEquals(0, inventoryAmount(Material.DIAMOND));
        assertEquals(Material.BARRIER, player.getOpenInventory().getTopInventory().getItem(0).getType());
    }

    @Test
    void shiftClickRejectsItemsOutsideTheInputSlotWhitelist() {
        openRestrictedInputView();
        player.getInventory().setItem(0, new ItemStack(Material.GOLD_INGOT, 8));

        InventoryClickEvent click = shiftClickHotbarSlotZero();
        server.getScheduler().performOneTick();

        assertTrue(click.isCancelled());
        assertEquals(8, inventoryAmount(Material.GOLD_INGOT));
        assertEquals(Map.of(), holder().getView().getContents().getItems());
    }

    @Test
    @Tag("regression")
    void returnsAnItemDepositedImmediatelyBeforeClosing() {
        open(ViewContents.ofControls(Map.of()));
        click(0, InventoryAction.PLACE_ALL);
        player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.DIAMOND, 8));

        player.closeInventory();
        server.getScheduler().performOneTick();

        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void returnsTheDepositedStackInsteadOfDuplicatingTheStackSwappedOntoTheCursor() {
        open(ViewContents.of(Map.of(), Map.of(0, new ItemStack(Material.DIAMOND, 8))));
        ViewHolder holder = holder();
        holder.updateViewContents();
        player.setItemOnCursor(new ItemStack(Material.GOLD_INGOT));
        assertFalse(click(0, InventoryAction.SWAP_WITH_CURSOR).isCancelled());
        ItemStack withdrawn = holder.getInventory().getItem(0).clone();
        holder.getInventory().setItem(0, player.getItemOnCursor());
        player.setItemOnCursor(withdrawn);

        // MockBukkit clears the cursor on close. Materialize its normal vanilla return here.
        player.getInventory().addItem(player.getItemOnCursor());
        player.setItemOnCursor(null);
        player.closeInventory();
        server.getScheduler().performOneTick();

        assertEquals(8, inventoryAmount(Material.DIAMOND), "The withdrawn stack must not be returned twice");
        assertEquals(1, inventoryAmount(Material.GOLD_INGOT), "The deposited stack must be returned");
    }

    @Test
    @Tag("regression")
    void returnsInputItemsBeforeThePlayerDisconnects() {
        open(ViewContents.of(Map.of(), Map.of(0, new ItemStack(Material.DIAMOND, 8))));

        player.closeInventory(InventoryCloseEvent.Reason.DISCONNECT);
        player.disconnect();
        server.getScheduler().performOneTick();

        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @ParameterizedTest
    @EnumSource(UpdateMode.class)
    @Tag("regression")
    @Timeout(5)
    void returnsThePlayerItemWhenAnUpdateReplacesItsSlotWithAControl(UpdateMode mode) {
        ViewContents<ItemStack, Player> replacement = ViewContents.ofControls(
                Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER))));
        ViewControl<ItemStack, Player> updateControl = ViewControl.of(new ItemStack(Material.CLOCK), event ->
                mode == UpdateMode.ASYNC_ACTION
                        ? new ViewAction.UpdateAsync<>(CompletableFuture.completedFuture(replacement))
                        : new ViewAction.Update<>(replacement));
        open(ViewContents.of(Map.of(8, updateControl), Map.of(0, new ItemStack(Material.DIAMOND, 8))));

        if (mode == UpdateMode.PUBLIC_METHOD) {
            assertTrue(BukkitView.updateView(holder().getView().withContents(replacement), player));
        } else {
            click(8, InventoryAction.PICKUP_ALL);
            if (mode == UpdateMode.ASYNC_ACTION) {
                server.getScheduler().waitAsyncTasksFinished();
            }
        }
        server.getScheduler().performOneTick();

        assertEquals(Material.BARRIER, player.getOpenInventory().getTopInventory().getItem(0).getType());
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void preservesAnItemDepositedJustBeforeAnUpdateToOtherControls() {
        open(ViewContents.ofControls(Map.of()));
        click(0, InventoryAction.PLACE_ALL);
        Inventory inventory = player.getOpenInventory().getTopInventory();
        inventory.setItem(0, new ItemStack(Material.DIAMOND, 8));
        ChestView<ItemStack, Player> updated = holder().getView().withContents(ViewContents.ofControls(
                Map.of(8, ViewControl.just(new ItemStack(Material.BARRIER)))));

        assertTrue(BukkitView.updateView(updated, player));

        assertNotNull(inventory.getItem(0));
        assertEquals(8, inventory.getItem(0).getAmount());
        assertEquals(0, inventoryAmount(Material.DIAMOND));
        player.closeInventory();
        server.getScheduler().performOneTick();
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    void updatingWithAnUnchangedInputStackDoesNotReturnItTwice() {
        open(ViewContents.of(Map.of(), Map.of(0, new ItemStack(Material.DIAMOND, 8))));
        ChestView<ItemStack, Player> updated = holder().getView().withContents(ViewContents.of(
                Map.of(8, ViewControl.just(new ItemStack(Material.BARRIER))),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))));

        assertTrue(BukkitView.updateView(updated, player));
        assertEquals(0, inventoryAmount(Material.DIAMOND));
        player.closeInventory();
        server.getScheduler().performOneTick();
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void duplicateCloseNotificationsReturnInputItemsOnlyOnce() {
        open(ViewContents.of(Map.of(), Map.of(0, new ItemStack(Material.DIAMOND, 8))));
        InventoryView view = player.getOpenInventory();
        player.closeInventory();
        server.getPluginManager().callEvent(new InventoryCloseEvent(view));
        server.getScheduler().performOneTick();

        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void queuedOpenDoesNotReopenAViewAfterThePlayerClosesIt() {
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("Target");
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.COMPASS),
                event -> new ViewAction.Open<>(target)))));
        click(0, InventoryAction.PICKUP_ALL);
        player.closeInventory();
        Inventory afterClose = player.getOpenInventory().getTopInventory();

        server.getScheduler().performOneTick();

        assertSame(afterClose, player.getOpenInventory().getTopInventory());
    }

    @Test
    void closingAViewCanOpenTheViewRequestedByItsCloseCallback() {
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("On close");
        ChestView<ItemStack, Player> view = baseView(ViewContents.of(Map.of(),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))))
                .withOnClose(event -> new ViewAction.Open<>(target));
        BukkitView.openView(view, player, plugin);

        player.closeInventory();
        server.getScheduler().performOneTick();

        assertSame(target, holder().getView());
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void closingAModalViewDoesNotReplaceAnInventoryOpenedBeforeTheNextTick() {
        ChestView<ItemStack, Player> parent = baseView(ViewContents.ofControls(Map.of())).withTitle("Parent");
        ChestView<ItemStack, Player> child = baseView(ViewContents.ofControls(Map.of()))
                .withTitle("Child").withParent(parent);
        BukkitView.openView(child, player, plugin);
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("New view");

        BukkitView.openView(target, player, plugin);
        server.getScheduler().performOneTick();

        assertSame(target, holder().getView());
    }

    @Test
    @Tag("regression")
    @Timeout(5)
    void ignoresAnAsyncUpdateAfterTheOriginalViewWasClosed() {
        CompletableFuture<ViewContents<ItemStack, Player>> future = new CompletableFuture<>();
        try {
            open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.CLOCK),
                    event -> new ViewAction.UpdateAsync<>(future)))));
            ViewHolder original = holder();
            click(0, InventoryAction.PICKUP_ALL);
            player.closeInventory();
            future.complete(ViewContents.ofControls(Map.of(1, ViewControl.just(new ItemStack(Material.BARRIER)))));

            server.getScheduler().waitAsyncTasksFinished();
            server.getScheduler().performTicks(2);

            assertFalse(original.getView().getContents().getControls().containsKey(1));
        } finally {
            future.cancel(true);
        }
    }

    @Test
    @Tag("regression")
    @Timeout(5)
    void ignoresAnAsyncUpdateAfterANewerPublicUpdate() {
        CompletableFuture<ViewContents<ItemStack, Player>> future = new CompletableFuture<>();
        try {
            open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.CLOCK),
                    event -> new ViewAction.UpdateAsync<>(future)))));
            click(0, InventoryAction.PICKUP_ALL);
            ViewContents<ItemStack, Player> newer = ViewContents.ofControls(
                    Map.of(1, ViewControl.just(new ItemStack(Material.BARRIER))));
            assertTrue(BukkitView.updateView(holder().getView().withContents(newer), player));
            future.complete(ViewContents.ofControls(Map.of(2, ViewControl.just(new ItemStack(Material.GOLD_INGOT)))));

            server.getScheduler().waitAsyncTasksFinished();
            server.getScheduler().performTicks(2);

            assertNotNull(player.getOpenInventory().getTopInventory().getItem(1));
            assertEquals(Material.BARRIER, player.getOpenInventory().getTopInventory().getItem(1).getType());
            assertFalse(holder().getView().getContents().getControls().containsKey(2));
        } finally {
            future.cancel(true);
        }
    }

    @Test
    @Tag("regression")
    void reopenActionCreatesANewInventoryForTheCurrentView() {
        open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.CLOCK),
                event -> ViewAction.reopen()))));
        Inventory original = player.getOpenInventory().getTopInventory();

        click(0, InventoryAction.PICKUP_ALL);
        server.getScheduler().performOneTick();

        assertNotSame(original, player.getOpenInventory().getTopInventory());
        assertEquals("Test view", player.getOpenInventory().getTitle());
    }

    @Test
    @Tag("regression")
    void reopeningTransfersInputItemsWithoutRefundingThemTwice() {
        ChestView<ItemStack, Player> view = baseView(ViewContents.of(
                Map.of(8, ViewControl.of(new ItemStack(Material.CLOCK), event -> ViewAction.reopen())),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))))
                .withOnClose(event -> new ViewAction.Close<>(true));
        BukkitView.openView(view, player, plugin);
        Inventory original = player.getOpenInventory().getTopInventory();

        click(8, InventoryAction.PICKUP_ALL);
        server.getScheduler().performOneTick();

        assertNotSame(original, player.getOpenInventory().getTopInventory());
        assertEquals(8, player.getOpenInventory().getTopInventory().getItem(0).getAmount());
        assertEquals(0, inventoryAmount(Material.DIAMOND));
        player.closeInventory();
        server.getScheduler().performOneTick();
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void aCloseCallbackThatReopensTheViewDoesNotDuplicateItsInputItems() {
        ChestView<ItemStack, Player> view = baseView(ViewContents.of(Map.of(),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))))
                .withOnClose(event -> ViewAction.reopen());
        BukkitView.openView(view, player, plugin);

        player.closeInventory();
        server.getScheduler().performOneTick();

        int displayed = Arrays.stream(player.getOpenInventory().getTopInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.DIAMOND)
                .mapToInt(ItemStack::getAmount).sum();
        assertEquals(8, displayed + inventoryAmount(Material.DIAMOND));
    }

    @Test
    void returnsItemsToTheWorldWhenThePlayerInventoryHasNoSpace() {
        // MockBukkit's addItem scans equipment slots too, so fill them to model full storage.
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }
        open(ViewContents.of(Map.of(8, ViewControl.just(new ItemStack(Material.BARRIER))),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))));

        player.closeInventory();
        server.getScheduler().performOneTick();

        List<ItemStack> dropped = player.getWorld().getEntities().stream()
                .filter(Item.class::isInstance).map(Item.class::cast).map(Item::getItemStack).toList();
        assertEquals(List.of(new ItemStack(Material.DIAMOND, 8)), dropped);
    }

    @Test
    void failedControlRenderingLeavesTheCurrentViewAndInputItemsIntact() {
        open(ViewContents.of(Map.of(), Map.of(0, new ItemStack(Material.DIAMOND, 8))));
        ChestView<ItemStack, Player> original = holder().getView();
        ChestView<ItemStack, Player> updated = original.withContents(ViewContents.ofControls(
                Map.of(0, ViewControl.<ItemStack, Player>of(event -> {
                    throw new IllegalStateException("Cannot render control");
                }, event -> ViewAction.nothing()))));

        assertThrows(IllegalStateException.class, () -> BukkitView.updateView(updated, player));

        assertSame(original, holder().getView());
        assertEquals(8, player.getOpenInventory().getTopInventory().getItem(0).getAmount());
        assertEquals(0, inventoryAmount(Material.DIAMOND));
        player.closeInventory();
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Tag("regression")
    void takingAnInputItemThatShadowsAControlSupportsImmutableContents() {
        open(ViewContents.of(Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER))),
                Map.of(0, new ItemStack(Material.DIAMOND, 8))));

        assertFalse(click(0, InventoryAction.PICKUP_ALL).isCancelled());
        Inventory inventory = player.getOpenInventory().getTopInventory();
        player.getInventory().addItem(inventory.getItem(0));
        inventory.setItem(0, null);
        server.getScheduler().performOneTick();

        assertNotNull(inventory.getItem(0));
        assertEquals(Material.BARRIER, inventory.getItem(0).getType());
        player.closeInventory();
        server.getScheduler().performOneTick();
        assertEquals(8, inventoryAmount(Material.DIAMOND));
    }

    @Test
    @Timeout(5)
    void aCloseCallbackCanAsynchronouslyOpenItsRequestedView() {
        ChestView<ItemStack, Player> target = baseView(ViewContents.ofControls(Map.of())).withTitle("After close");
        ChestView<ItemStack, Player> view = baseView(ViewContents.ofControls(Map.of()))
                .withOnClose(event -> new ViewAction.OpenAsync<>(CompletableFuture.completedFuture(target)));
        BukkitView.openView(view, player, plugin);

        player.closeInventory();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        assertSame(target, holder().getView());
    }

    @Test
    @Tag("regression")
    @Timeout(5)
    void ignoresAnAsyncOpenResultAfterItsOriginalViewWasClosed() {
        CompletableFuture<ChestView<ItemStack, Player>> future = new CompletableFuture<>();
        try {
            open(ViewContents.ofControls(Map.of(0, ViewControl.of(new ItemStack(Material.COMPASS),
                    event -> new ViewAction.OpenAsync<>(future)))));
            click(0, InventoryAction.PICKUP_ALL);
            player.closeInventory();
            Inventory afterClose = player.getOpenInventory().getTopInventory();
            future.complete(baseView(ViewContents.ofControls(Map.of())).withTitle("Late result"));

            server.getScheduler().waitAsyncTasksFinished();
            server.getScheduler().performTicks(2);

            assertSame(afterClose, player.getOpenInventory().getTopInventory());
        } finally {
            future.cancel(true);
        }
    }

    private void open(ViewContents<ItemStack, Player> contents) {
        BukkitView.openView(baseView(contents), player, plugin);
    }

    private void openRestrictedInputView() {
        ChestView<ItemStack, Player> view = baseView(ViewContents.ofControls(
                Map.of(0, ViewControl.just(new ItemStack(Material.BARRIER)))))
                .withOverrideMoveToOtherInventorySlots(List.of(new InputSlot(2, Set.of("minecraft:diamond"))));
        BukkitView.openView(view, player, plugin);
    }

    private InventoryClickEvent shiftClickHotbarSlotZero() {
        InventoryView view = player.getOpenInventory();
        // The hotbar follows 27 main inventory slots in the raw chest-view slot numbering.
        int rawSlot = view.getTopInventory().getSize() + 27;
        InventoryClickEvent click = new InventoryClickEvent(view, InventoryType.SlotType.QUICKBAR,
                rawSlot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            @Override
            public ItemStack getCurrentItem() {
                // MockBukkit's getItem(rawSlot) omits the hotbar remapping done by convertSlot().
                return getClickedInventory().getItem(getSlot());
            }
        };
        assertEquals(0, click.getSlot());
        server.getPluginManager().callEvent(click);
        return click;
    }

    private ChestView<ItemStack, Player> baseView(ViewContents<ItemStack, Player> contents) {
        return ChestView.<ItemStack, Player>builder(BukkitItemStackOps.INSTANCE)
                .title("Test view").row(1).contents(contents).build();
    }

    private ViewHolder holder() {
        return assertInstanceOf(ViewHolder.class, player.getOpenInventory().getTopInventory().getHolder());
    }

    private InventoryClickEvent click(int slot, InventoryAction action) {
        InventoryView view = player.getOpenInventory();
        InventoryClickEvent click = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, action);
        server.getPluginManager().callEvent(click);
        return click;
    }

    private int inventoryAmount(Material material) {
        return Arrays.stream(player.getInventory().getStorageContents())
                .filter(item -> item != null && item.getType() == material)
                .mapToInt(ItemStack::getAmount).sum();
    }

    private enum UpdateMode {
        PUBLIC_METHOD, ACTION, ASYNC_ACTION
    }
}
