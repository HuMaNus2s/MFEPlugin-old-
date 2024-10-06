package humanusas.mfeplugin.commands

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.io.File
import java.io.FileWriter
import humanusas.mfeplugin.MFEplugin

class Balance(private val plugin: MFEplugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is org.bukkit.entity.Player) {
            if (args.isEmpty()) {
                val playerName = sender.name
                handleBalanceCommand(sender, playerName)
            } else if (args.size == 1) {
                val playerName = args[0]
                handleBalanceCommand(sender, playerName)
            }
        } else {
            if (args.isEmpty()) {
                plugin.logger.warning("Эта команда доступна только для игроков. Используйте balance <ник>")
            } else {
                val playerName = args[0]
                handleBalanceCommandFromConsole(playerName)
            }
        }
        return true
    }

    private fun handleBalanceCommand(player: org.bukkit.entity.Player, playerName: String) {
        val playersFile = File(plugin.dataFolder, "MFEPlugin.json")
        if (playersFile.exists()) {
            try {
                val jsonString = playersFile.readText()
                val playerMapType = object : TypeToken<Map<String, Player>>() {}.type
                val players: Map<String, Player> = GsonBuilder().create().fromJson(jsonString, playerMapType)

                val playerToUpdate = players.values.find { it.name == playerName }
                if (playerToUpdate != null) {
                    player.sendMessage("§6Баланс игрока $playerName: ${playerToUpdate.diamonds} алмазов.")
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Баланс $playerName: ${playerToUpdate.diamonds} алмазов.")
                } else {
                    player.sendMessage("§cИгрок $playerName не найден.")
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName не найден.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            plugin.logger.warning("Файл MFEPlugin.json не найден!")
        }
    }

    private fun handleBalanceCommandFromConsole(playerName: String) {
        val playersFile = File(plugin.dataFolder, "MFEPlugin.json")
        if (playersFile.exists()) {
            try {
                val jsonString = playersFile.readText()
                val playerMapType = object : TypeToken<Map<String, Player>>() {}.type
                val players: Map<String, Player> = GsonBuilder().create().fromJson(jsonString, playerMapType)

                val playerToUpdate = players.values.find { it.name == playerName }
                if (playerToUpdate != null) {
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §6Баланс игрока $playerName: ${playerToUpdate.diamonds} алмазов.")
                } else {
                    plugin.logger.warning("Игрок $playerName не найден.")
                    Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] §cОперация отклонена. \n§2[MFEPlugin] §cПричина: Игрок $playerName не найден.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            plugin.logger.warning("Файл MFEPlugin.json не найден!")
        }
    }

    data class Player(var name: String, var uuid: String, var diamonds: Int)
}





