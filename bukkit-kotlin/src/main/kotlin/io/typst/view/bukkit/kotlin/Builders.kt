package io.typst.view.bukkit.kotlin

import io.typst.inventory.ItemStackOps
import io.typst.inventory.bukkit.BukkitItemStackOps
import io.typst.view.ChestView
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun chestViewBuilder(itemOps: ItemStackOps<ItemStack> = BukkitItemStackOps.INSTANCE): ChestView.ChestViewBuilder<ItemStack, Player> =
    ChestView.builder<ItemStack, Player>()
        .itemOps(itemOps)
