package humanusas.mfeplugin.commands

import humanusas.mfeplugin.MFEplugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class Menu(private val plugin: MFEplugin) : CommandExecutor, Listener {

    init {
        // Регистрация слушателя событий
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            openMenu(sender)
        } else {
            sender.sendMessage("Эту команду можно использовать только в игре.")
        }
        return true
    }

    private fun openMenu_pay(player: Player) {
        val menu = createMenu("Меню", 9)

        // Добавление предметов в меню (пример)
        val itemStack1 = createCommandItemStack(Material.DIAMOND_SWORD, "Ваш счёт", "balance")
        val itemStack2 = createCommandItemStack(Material.GOLDEN_APPLE, "Пополнить счёт", "pay")

        menu.setItem(0, itemStack1)
        menu.setItem(1, itemStack2)

        player.openInventory(menu)
    }
    private fun openMenu(player: Player) {
        val menu = createMenu("Меню", 9)

        // Добавление предметов в меню (пример)
        val itemStack1 = createCommandItemStack(Material.DIAMOND_SWORD, "Ваш счёт", "balance")
        val itemStack2 = createCommandItemStack(Material.GOLDEN_APPLE, "Пополнить счёт", "pay")

        menu.setItem(0, itemStack1)
        menu.setItem(1, itemStack2)

        player.openInventory(menu)
    }

    private fun createMenu(title: String, size: Int): Inventory {
        return plugin.server.createInventory(null, size, title)
    }

    private fun createCommandItemStack(material: Material, displayName: String, command: String): ItemStack {
        val itemStack = ItemStack(material)
        val itemMeta = itemStack.itemMeta

        if (itemMeta != null) {
            // Установка имени предмета
            itemMeta.setDisplayName(displayName)

            // Создание списка команд и добавление команды
            val lore = mutableListOf<String>()
            lore.add("§7Кликните, чтобы выполнить команду.")
            lore.add("§7Команда: $command")

            itemMeta.lore = lore

            itemStack.itemMeta = itemMeta
        }

        return itemStack
    }


    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return

        if (event.view.title == "Меню") {
            // Проверка, является ли предмет выполняемой командой
            val lore = clickedItem.itemMeta?.lore
            if (lore != null && lore.contains("§7Кликните, чтобы выполнить команду.")) {
                // Извлечение команды из лора и выполнение её
                val command = lore.last().replace("§7Команда: ", "")
                plugin.server.dispatchCommand(player, command)
                event.isCancelled = true
                player.closeInventory()
            }
        }
    }
}
