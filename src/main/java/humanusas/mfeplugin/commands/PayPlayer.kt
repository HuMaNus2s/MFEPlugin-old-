package humanusas.mfeplugin.commands

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

import humanusas.mfeplugin.MFEplugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.math.abs

class PayPlayer(private val plugin: MFEplugin) : CommandExecutor {

    data class Player(var name: String, var uuid: String, var diamonds: Int)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.size == 1) {
            val playerName = sender.name
            val additionalDiamonds = args[0].toIntOrNull() ?: 0
            if (additionalDiamonds <= 0) {
                sender.sendMessage("§cОтклонено: сумма платежа должна быть положительным числом.")
                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: сумма пополнения меньше нуля.")
                return false
            }
            handlePayCommand(sender, playerName, additionalDiamonds, true)
            return true
        }
        if (args.size == 2) {
            val playerName = args[0]
            val additionalDiamonds = args[1].toIntOrNull() ?: 0
            if (additionalDiamonds <= 0) {
                sender.sendMessage("§cОтклонено: сумма платежа должна быть положительным числом.")
                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Сумма пополнения меньше нуля.")
                return false
            }
            handlePayCommand(sender, playerName, additionalDiamonds, false)
            return true
        }
        return true
    }

    private fun handlePayCommand(sender: CommandSender, playerName: String, additionalDiamonds: Int, isSingleArgument: Boolean) {
        val playersFile = File(plugin.dataFolder, "MFEPlugin.json")
        val isConsole = sender !is org.bukkit.entity.Player

        if (playersFile.exists()) {
            try {
                val jsonString = playersFile.readText()
                val playersListType = object : TypeToken<List<Player>>() {}.type
                val players: MutableList<Player> = Gson().fromJson(jsonString, playersListType)

                val senderPlayer = if (sender is org.bukkit.entity.Player) sender else null

                val senderAccount = players.find { it.name == senderPlayer?.name }
                val playerToUpdate = players.find { it.name == playerName }

                if (isSingleArgument && senderPlayer != null && !isConsole) {
                    val diamondsInInventory = senderPlayer.inventory.contents.filter { it?.type == Material.DIAMOND }
                    val totalDiamonds = diamondsInInventory.sumBy { it?.amount ?: 0 }

                    if (totalDiamonds >= additionalDiamonds) {
                        // If there are diamonds in the inventory, we write them off
                        var diamondsLeft = additionalDiamonds
                        for (item in diamondsInInventory) {
                            if (item != null) {
                                val amount = item.amount
                                if (amount >= diamondsLeft) {
                                    item.amount -= diamondsLeft
                                    diamondsLeft = 0
                                    break
                                } else {
                                    diamondsLeft -= amount
                                    item.amount = 0
                                }
                            }
                        }
                        val playerAccount = players.find { it.name == senderPlayer.name }
                        playerAccount?.diamonds = playerAccount?.diamonds?.plus(additionalDiamonds - diamondsLeft) ?: 0
                        if (playerToUpdate != null) {
                            senderPlayer.sendMessage("§aВаш личный счёт пополнен на: ${additionalDiamonds - diamondsLeft} алмазов. \n§6Ваш баланс ${playerToUpdate.diamonds} алмазов.")
                            Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §aИгрок ${playerToUpdate.name} пополнил свой счёт на ${additionalDiamonds - diamondsLeft} алмазов.")
                        }
                        val updatedJson = Gson().toJson(players)
                        playersFile.writeText(updatedJson)
                        return
                    } else {
                        // If there are not enough diamonds in the inventory, we inform the player about it
                        senderPlayer.sendMessage("§eУ вас недостаточно алмазов в инвентаре для перевода.")
                        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §сОперация отклонена. \n§2[MFEPlugin] §сПричина: Недостаточно алмазов в инвентаре для перевода.")
                        return
                    }
                }

                if (senderAccount != null) {
                    // Diamond processing for the player executing the command from the game
                    if (senderPlayer != null) {
                        if (senderAccount.diamonds >= abs(additionalDiamonds)) {
                            senderAccount.diamonds -= abs(additionalDiamonds)
                            val updatedJson = Gson().toJson(players)
                            playersFile.writeText(updatedJson)
                            if (playerToUpdate != null) {
                                senderPlayer.sendMessage("§cС вашего счёта было списано: ${abs(additionalDiamonds)} алмазов.\n§6Ваш счёт ${senderAccount.diamonds} алмазов.")
                                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Игрок ${senderPlayer.name} пополнил счёт игрока ${playerToUpdate.name} на ${senderAccount.diamonds} алмазов.")
                            }
                        } else {
                            val remainingDiamonds = abs(additionalDiamonds) - senderAccount.diamonds
                            if (senderPlayer.inventory.contains(Material.DIAMOND, remainingDiamonds)) {
                                senderPlayer.inventory.removeItem(ItemStack(Material.DIAMOND, remainingDiamonds))
                                senderPlayer.sendMessage("§7С вашего счёта было списано: ${abs(additionalDiamonds)} алмазов.")
                                senderPlayer.sendMessage("§aНа вашем счету недостаточно алмазов. \n§eИз инвентаря было списано: $remainingDiamonds алмазов.")
                                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Игрок ${senderPlayer.name} пополнил свой счёт.")

                            } else {
                                senderPlayer.sendMessage("§сНа вашем счёте и в инвентаре недостаточно алмазов для перевода.")
                                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Недостаточно алмазов для перевода.")
                                return
                            }
                        }
                        if (playerToUpdate != null) {
                            val currentDiamonds = playerToUpdate.diamonds
                            val updatedDiamonds = currentDiamonds + additionalDiamonds
                            playerToUpdate.diamonds = updatedDiamonds
                            val recipientPlayer = Bukkit.getPlayer(playerToUpdate.name)
                            recipientPlayer?.sendMessage("§aВы получили $additionalDiamonds алмазов от ${senderPlayer?.name ?: "консоли."}\n§6Ваш счёт ${senderAccount.diamonds} алмазов.")
                            Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Баланс игрока ${playerToUpdate.name} пополнен на ${additionalDiamonds} алмазов.")

                            val updatedJson = Gson().toJson(players)
                            playersFile.writeText(updatedJson)
                        } else {
                            sender.sendMessage("§сИгрок $playerName не найден.")
                            Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName не найден")
                        }
                    }
                } else {

                    // Processing commands from the console
                    if (playerToUpdate != null) {
                        val currentDiamonds = playerToUpdate.diamonds
                        val updatedDiamonds = currentDiamonds + additionalDiamonds
                        playerToUpdate.diamonds = updatedDiamonds
                        val recipientPlayer = Bukkit.getPlayer(playerToUpdate.name)
                        if (senderAccount != null) {
                            recipientPlayer?.sendMessage("§6Вы получили $additionalDiamonds алмазов от ${senderPlayer?.name ?: "консоли."}\n§6Ваш счёт ${senderAccount.diamonds} алмазов.")
                        }
                        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Баланс игрока ${playerToUpdate.name} пополнен на ${additionalDiamonds} алмазов.")

                        val updatedJson = Gson().toJson(players)
                        playersFile.writeText(updatedJson)
                    } else {
                        sender.sendMessage("§сИгрок $playerName не найден.")
                        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName не найден")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            plugin.logger.warning("Файл MFEPlugin.json не найден!")
        }
    }

}

