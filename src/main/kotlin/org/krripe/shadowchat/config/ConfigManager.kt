package org.krripe.shadowchat.config

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader

object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configFile = FabricLoader.getInstance()
        .configDir
        .resolve("shadowchat")
        .resolve("chat_config.json")
        .toFile()

    var chatConfig = ChatConfig(
        rankFormats = listOf(
            RankFormat(
                rankDisplayName = "<#ff5555>[Admin] %player:name%</#>",
                rankChatFormat = "<#ff5555>[Admin]</#> \${display_name} <#aaaaaa>➤</#> \${message}",
                rankPermission = "shadowchat.rank.admin"
            ),
            RankFormat(
                rankDisplayName = "<#55ffff>[Helper] %player:name%</#>",
                rankChatFormat = "<#55ffff>[Helper]</#> \${display_name} <#aaaaaa>➤</#> \${message}",
                rankPermission = "shadowchat.rank.helper"
            )
        )
    )

    fun load() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(chatConfig))
        } else {
            try {
                val type = object : TypeToken<ChatConfig>() {}.type
                chatConfig = gson.fromJson(configFile.readText(), type)
            } catch (_: Exception) {}
        }
    }

    fun save() {
        try {
            configFile.writeText(gson.toJson(chatConfig))
        } catch (_: Exception) {}
    }
}

data class ChatConfig(
    val displayName: String = "<#00FF00>%player:name%</#>",
    val chatFormat: String = "%luckperms:prefix% \${display_name} <#AAAAAA>➤</#> \${message}",
    val joinMessage: String = "<#FFD700>%player:name% joined the server!</#>",
    val leaveMessage: String = "<#FFD700>%player:name% left the server!</#>",
    val baseDeath: String = "<#424242>☠</#> <#a9a9a9>\${default_message}</#>",
    val commandFormats: CommandFormats = CommandFormats(),
    val mentionFormats: MentionFormats = MentionFormats(),
    val nameTag: NameTagConfig = NameTagConfig(),
    val chatSystems: ChatSystems = ChatSystems(),
    val rankFormats: List<RankFormat> = listOf(),
    val weights: List<WeightEntry> = listOf(
        WeightEntry("Owner",   "shadowchat.weight.owner",   1),
        WeightEntry("Admin",   "shadowchat.weight.admin",  10),
        WeightEntry("default", "shadowchat.weight.default", 9999)
    ),
    val spam: SpamConfig = SpamConfig(),
    val tab: TabListConfig = TabListConfig(),
    val tabSettings: TabSettings = TabSettings()
)

data class CommandFormats(
    val msgSent: String = "<#b77df9>You whisper to <#ffe856>%receiver%</#>: <#e0c8fb>%message%</#>",
    val msgReceived: String = "<#b77df9>%sender%</#> <#b77df9>whispers to you: <#e0c8fb>%message%</#>",
    val say: String = "<#ffe856>%player%</#> <#2c2c2c>↔ <#eafff7>%message%</#>",
    val me: String = "<#d7f33b>* %player% %message%</#>",
    val spy: String = "<#141414>[</#><#26D0FF>Spy</#><#141414>]</#> <#ffba26>%sender%</#> <#AAAAAA>→</#> <#55ffff>%targets%</#>: <#FFFFFF>%message%</#>",
    val broadcast: String = "<#fcc00>[Broadcast]</#> <#ffffff>%message%</#>"
)

data class MentionFormats(
    val mentionedTitle: String = "<#ffe033>%sender% mentioned you in the message:</#>",
    val mentionedMessage: String = "<#aaaaaa>→ %message%</#>"
)

data class NameTagConfig(
    val prefix: String = "%luckperms:prefix% ",
    val suffix: String = ""
)

data class RankFormat(
    val rankDisplayName: String,
    val rankChatFormat: String,
    val rankPermission: String
)

data class WeightEntry(
    val weightName: String,
    val weightPerm: String,
    val weight: Int
)

data class ChatSystems(
    val announcements: Boolean = true,
    val logMessages: Boolean = true,
    val rankFormat: Boolean = true,
    val weights: Boolean = true,
    val spam: Boolean = true,
    val tab: Boolean = true,
    val nameTags: Boolean = true
)

data class SpamConfig(
    val windowMs: Long = 3_000,
    val maxMessages: Int = 5,
    val warnMessage: String = "<#ffe033>⚠</#> <#ff7253>Slow down, %player%! You are sending messages too fast.</#>"
)

data class TabListConfig(
    val header: List<String> = listOf("<#55ffff>ShadowChat</#>", "<#aaaaaa>Enjoy your stay!</#>"),
    val footer: List<String> = listOf("<#aaaaaa>Online: %server:online% / %server:max_players%</#>"),
    val playerFormat: String = "%luckperms:prefix% %nickname%"
)

data class TabSettings(
    val autoRefreshTicks: Int = 100
)
