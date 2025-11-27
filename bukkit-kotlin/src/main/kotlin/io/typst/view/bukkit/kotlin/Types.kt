package io.typst.view.bukkit.kotlin

import io.typst.view.ChestView
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.ViewAction
import io.typst.view.page.PageContext
import io.typst.view.page.PageViewLayout
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

typealias BukkitViewControl = ViewControl<ItemStack, Player>

typealias BukkitViewAction = ViewAction<ItemStack, Player>

typealias BukkitPageContext = PageContext<ItemStack, Player>

typealias BukkitPageViewLayout = PageViewLayout<ItemStack, Player>

typealias BukkitChestView = ChestView<ItemStack, Player>

typealias BukkitViewContents = ViewContents<ItemStack, Player>
