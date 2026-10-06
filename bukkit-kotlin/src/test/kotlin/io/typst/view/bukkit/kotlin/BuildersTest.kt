package io.typst.view.bukkit.kotlin

import io.typst.inventory.ItemStackOps
import io.typst.inventory.bukkit.BukkitItemStackOps
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.BukkitView
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.concurrent.atomic.AtomicReference

class BuildersTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        BukkitView.register(plugin)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `default builder opens a typed view and delivers Java click events to Kotlin callbacks`() {
        val player = server.addPlayer()
        val received = AtomicReference<BukkitClickEvent>()
        val control: BukkitViewControl = ViewControl.consumer(ItemStack(Material.BARRIER), received::set)
        val view: BukkitChestView = chestViewBuilder()
            .title("Kotlin view")
            .row(2)
            .contents(ViewContents.ofControls(mapOf(0 to control)))
            .build()

        BukkitView.openView(view, player, plugin)
        val click = InventoryClickEvent(
            player.openInventory, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL
        )
        server.pluginManager.callEvent(click)

        assertSame(BukkitItemStackOps.INSTANCE, view.itemOps)
        assertEquals("Kotlin view", player.openInventory.title)
        assertEquals(18, player.openInventory.topInventory.size)
        assertTrue(click.isCancelled)
        assertSame(player, received.get().player)
    }

    @Test
    fun `custom item operations determine the number of input slots used by the built view`() {
        val itemOps = object : ItemStackOps<ItemStack> by BukkitItemStackOps.INSTANCE {
            override fun getMaxStackSize(item: ItemStack): Int = 1
        }
        val view = chestViewBuilder(itemOps).build()

        val slots = view.findSpaces(listOf(0, 1, 2), ItemStack(Material.DIAMOND, 2))

        assertSame(itemOps, view.itemOps)
        assertEquals(listOf(0, 1), slots)
    }
}
