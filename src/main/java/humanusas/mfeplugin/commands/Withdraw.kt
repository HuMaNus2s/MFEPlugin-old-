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
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.inventory.ItemStack
import kotlin.math.abs

class Withdraw(private val plugin: MFEplugin) : CommandExecutor {
    data class Player(var name: String, var uuid: String, var diamonds: Int)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty() && sender is org.bukkit.entity.Player || args.size > 3 && sender is org.bukkit.entity.Player) {
            sender.sendMessage("§2[MFEPlugin] §cНеверно введена команда. \n§2[MFEPlugin] §cПример команды: /withdraw <сумма>")
            return false
        }

        if (args.isEmpty() && sender !is org.bukkit.entity.Player || args.size > 3 && sender !is org.bukkit.entity.Player ) {
            sender.sendMessage("§2[MFEPlugin] §cИспользование: /withdraw <ник> <сумма>")
            return false
        }

        if (sender !is org.bukkit.entity.Player && args.size == 1) {
            sender.sendMessage("§2[MFEPlugin] §cИспользование: /withdraw <ник> <сумма>")
            return false
        }

        if (sender is org.bukkit.entity.Player && args.size == 1) {
            val additionalDiamonds = args[0].toIntOrNull() ?: 0
            if (additionalDiamonds <= 0) {
                sender.sendMessage("§2[MFEPlugin] §cОтклонено: Cумма платежа должна быть положительным числом.")
                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Cумма платежа должна быть положительным числом.")
                return false
            }
            handleWithdrawCommand(sender, sender.name, additionalDiamonds)
            Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Игрок ${sender.name} снял со счёта $additionalDiamonds алмазов.")
            return true
        }

        if (sender is ConsoleCommandSender && args.size == 2) {
            val playerName = args[0]
            val additionalDiamonds = args[1].toIntOrNull() ?: 0
            if (additionalDiamonds <= 0) {
                sender.sendMessage("§2[MFEPlugin] §cОтклонено: Cумма платежа должна быть положительным числом.")
                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Cумма платежа должна быть положительным числом.")
                return false
            }
            val player = Bukkit.getPlayer(playerName)
            if (player != null) {
                if (player.isOnline) {
                    handleWithdrawCommand(sender, playerName, additionalDiamonds) // Fixed argument passing
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Вы сняли у игрока $playerName $additionalDiamonds алмазов.")
                    return true
                } else {
                    sender.sendMessage("§2[MFEPlugin] §cИгрок $playerName оффлайн.")
                    return false
                }
            }
            sender.sendMessage("§2[MFEPlugin] §cИгрок $playerName не найден")
            return false
        }
        sender.sendMessage("§2[MFEPlugin] §cНеверно введена команда. \n§2[MFEPlugin] §cПример команды: /withdraw <сумма>")
        return false
    }

    private fun handleWithdrawCommand(
        sender: CommandSender,
        playerName: String,
        additionalDiamonds: Int
    ) { // Fixed argument type
        val playersFile = File(plugin.dataFolder, "MFEPlugin.json")

        if (playersFile.exists()) {
            try {
                val jsonString = playersFile.readText()
                val playersListType = object : TypeToken<List<Player>>() {}.type
                val players: MutableList<Player> = Gson().fromJson(jsonString, playersListType)

                val senderAccount = players.find { it.name == playerName } // Check the account by playerName

                if (senderAccount != null) {
                    if (senderAccount.diamonds >= abs(additionalDiamonds)) {
                        senderAccount.diamonds -= abs(additionalDiamonds)
                        val updatedJson = Gson().toJson(players)
                        playersFile.writeText(updatedJson)
                        if (sender !is ConsoleCommandSender) {
                            val player = Bukkit.getPlayer(playerName)
                            if (player != null && player.isOnline) { // Ensure the player is online before interacting
                                val diamondStack = ItemStack(Material.DIAMOND, additionalDiamonds)
                                val playerInventory = player.inventory
                                val remaining = playerInventory.addItem(diamondStack)
                                if (remaining.isNotEmpty()) {
                                    val remainingAmount = remaining.values.sumOf { it.amount }
                                    val givenAmount = additionalDiamonds - remainingAmount
                                    player.sendMessage("§aУспешно выдано $givenAmount алмазов, $remainingAmount алмазов не поместилось в инвентарь.")
                                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Успешно выдано $givenAmount алмазов игроку $playerName.")
                                } else {
                                    player.sendMessage("§aУспешно выдано $additionalDiamonds алмазов.\n§aВаш баланс ${senderAccount.diamonds} алмазов.")
                                }
                            } else {
                                sender.sendMessage("§cИгрок $playerName оффлайн или не найден.")
                                Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName оффлайн или не найден.")
                            }
                        }
                    } else {
                        sender.sendMessage("§cНа вашем счёте недостаточно алмазов.")
                        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: На вашем счёте игрока $playerName недостаточно алмазов.")
                    }
                } else {
                    sender.sendMessage("§cВаш аккаунт не найден в базе данных.")
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName не в базе данных.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            plugin.logger.warning("Файл MFEPlugin.json не найден!")
        }
    }
}






