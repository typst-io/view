package io.typst.view.bukkit.kotlin

import io.typst.view.ChestView
import io.typst.view.ClickEvent
import io.typst.view.CloseEvent
import io.typst.view.OpenEvent
import io.typst.view.UpdateEvent
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

typealias BukkitClickEvent = ClickEvent<ItemStack, Player>

typealias BukkitCloseEvent = CloseEvent<ItemStack, Player>

typealias BukkitOpenEvent = OpenEvent<ItemStack, Player>

typealias BukkitUpdateEvent = UpdateEvent<ItemStack, Player>
