package io.typst.view.bukkit.plugin;

import io.typst.inventory.bukkit.BukkitItemStackOps;
import io.typst.view.ChestView;
import io.typst.view.bukkit.BukkitView;
import io.typst.view.bukkit.plugin.config.ViewConfigCodec;
import io.typst.view.bukkit.plugin.view.ItemListView;
import io.typst.view.bukkit.plugin.view.PlayerChestView;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ViewPlugin extends JavaPlugin {
    public static ViewPlugin inst = null;
    public Map<String, ChestView<ItemStack, Player>> views = new HashMap<>();
    private boolean placeholderLoaded = false;

    @Override
    public void onLoad() {
        inst = this;
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderLoaded = true;
        }
        BukkitView.register(BukkitItemStackOps.INSTANCE, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        saveDefaultConfig();
        reloadConfig();
        views = ViewConfigCodec.loadViewConfigs(this, getConfig());
    }

    public String replacePlaceholder(Player p, String x) {
        if (placeholderLoaded) {
            return PlaceholderAPI.setPlaceholders(p, x);
        }
        return x;
    }

    public ItemStack replacePlaceholder(Player p, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(replacePlaceholder(p, meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                meta.setLore(Objects.requireNonNull(meta.getLore()).stream()
                        .map(a -> replacePlaceholder(p, a))
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player p = sender instanceof Player ? ((Player) sender) : null;
        String head = args.length >= 1 ? args[0] : "";
        switch (head) {
            case "page": {
                if (p != null && p.isOp()) {
                    ChestView<ItemStack, Player> view = ItemListView.create(
                            Arrays.stream(Material.values())
                                    .filter(mat -> mat.isItem() && !mat.isAir())
                    ).withItemOps(BukkitItemStackOps.INSTANCE);
                    BukkitView.openView(view, p, this);
                }
                break;
            }
            case "chest": {
                if (p != null && p.isOp()) {
                    ChestView<ItemStack, Player> view = PlayerChestView.main(p)
                            .withItemOps(BukkitItemStackOps.INSTANCE);
                    BukkitView.openView(view, p, this);
                }
                break;
            }
            case "reload": {
                if (p.isOp()) {
                    reloadConfig();
                    views = ViewConfigCodec.loadViewConfigs(this, getConfig());
                    sender.sendMessage("Reloaded.");
                }
            }
            case "open": {
                String name = args.length >= 2 ? args[1] : "";
                views = ViewConfigCodec.loadViewConfigs(this, getConfig());
                ChestView view = views.get(name);
                if (view != null && p != null) {
                    BukkitView.openView(view, p, this);
                }
                break;
            }
            default: {
                sender.sendMessage(String.format("§a/%s page: §fshow a demo pagination view.", label));
                sender.sendMessage(String.format("§a/%s chest: §fshow a demo chest view.", label));
                break;
            }
        }
        return true;
    }
}
