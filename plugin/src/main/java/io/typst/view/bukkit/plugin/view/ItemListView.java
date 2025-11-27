package io.typst.view.bukkit.plugin.view;

import io.typst.view.ChestView;
import io.typst.view.ClickEvent;
import io.typst.view.ViewControl;
import io.typst.view.ViewAction;
import io.typst.view.page.PageContext;
import io.typst.view.page.PageViewLayout;
import io.typst.inventory.bukkit.BukkitItemStackOps;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemListView {
    public static ChestView<ItemStack, Player> create(Stream<Material> materials) {
        return createLayout(materials).toView(1);
    }

    public static PageViewLayout<ItemStack, Player> createLayout(Stream<Material> materials) {
        List<Function<PageContext<ItemStack, Player>, ViewControl<ItemStack, Player>>> pagingContents =
                materials
                        .map(material ->
                                // Why `Function<PageContext, ViewControl` not just `ViewControl`?
                                // Because of laziness to avoid lag from immediate calculation the all page items.
                                (Function<PageContext<ItemStack, Player>, ViewControl<ItemStack, Player>>) ctx ->
                                        ViewControl.<ItemStack, Player>of(
                                                e -> createDisplayItem(material),
                                                e -> onClick(e, material)
                                        )
                        )
                        .collect(Collectors.toList());
        return PageViewLayout.ofDefault(
                BukkitItemStackOps.INSTANCE,
                "page",
                6,
                Material.STONE_BUTTON.getKeyOrThrow().toString(),
                pagingContents
        );
    }

    private static ItemStack createDisplayItem(Material material) {
        return new ItemStack(material);
    }

    private static ViewAction<ItemStack, Player> onClick(ClickEvent<ItemStack, Player> event, Material material) {
        Player clicker = event.getPlayer();
        if (clicker.isOp()) {
            clicker.getInventory().addItem(new ItemStack(material));
        }
        return ViewAction.nothing();
    }
}
