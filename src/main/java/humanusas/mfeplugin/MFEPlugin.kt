package humanusas.mfeplugin

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import com.google.gson.Gson
import java.io.File
import com.google.gson.JsonObject
import com.google.gson.GsonBuilder
import org.json.simple.parser.JSONParser
import java.io.FileWriter
import java.io.IOException
import com.google.gson.reflect.TypeToken
import java.io.FileReader

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import humanusas.mfeplugin.commands.PayPlayer
import humanusas.mfeplugin.commands.Balance
import humanusas.mfeplugin.commands.Withdraw
import humanusas.mfeplugin.commands.Menu

class MFEplugin : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] Successfully launched!")
        val javaVersion = System.getProperty("java.version")
        Bukkit.getConsoleSender().sendMessage("§2[MFEPlugin] Plugin used version Java $javaVersion")
        //commands register
        getCommand("pay")?.setExecutor(PayPlayer(this))
        getCommand("balance")?.setExecutor(Balance(this))
        getCommand("withdraw")?.setExecutor(Withdraw(this))
        getCommand("menu")?.setExecutor(Menu(this))
        //event register
        server.pluginManager.registerEvents(PlayerJoinListener(), this)
    }
}

class PlayerJoinListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val pluginFolder = File("plugins/MFEPlugin") // Замените на название вашего плагина
        if (!pluginFolder.exists()) {
            pluginFolder.mkdir()
        }

        val playersFile = File(pluginFolder, "MFEPlugin.json")
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()

        val playersObject = JsonObject()
        if (playersFile.exists() && playersFile.length() > 0) {
            try {
                val fileReader = FileReader(playersFile)
                val existingPlayersType = object : TypeToken<Map<String, Player>>() {}.type
                val existingPlayers: Map<String, Player> = gson.fromJson(fileReader, existingPlayersType) ?: emptyMap()

                existingPlayers.forEach { (key, value) ->
                    val playerJson = gson.toJsonTree(value)
                    playersObject.add(key, playerJson)
                }

                fileReader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val jsonString = gson.toJson(playersObject)
        val playerMapType = object : TypeToken<Map<String, Player>>() {}.type
        val players: Map<String, Player> = gson.fromJson(jsonString, playerMapType) ?: emptyMap()
        players.forEach { (_, playerInfo) ->
            Bukkit.getConsoleSender().sendMessage("Player Name: ${playerInfo.name}, UUID: ${playerInfo.uuid}, Diamonds: ${playerInfo.diamonds}")
        }

        val isPlayerExists = players.containsKey(player.uniqueId.toString())
        if (!isPlayerExists) {
            val newPlayer = Player(player.name, player.uniqueId.toString(), 10)
            playersObject.add(player.uniqueId.toString(), gson.toJsonTree(newPlayer))

            try {
                FileWriter(playersFile).use { fileWriter ->
                    fileWriter.write(gson.toJson(playersObject))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    data class Player(val name: String, val uuid: String, val diamonds: Int)
}




