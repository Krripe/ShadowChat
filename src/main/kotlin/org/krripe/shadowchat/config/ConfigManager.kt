package org.krripe.shadowchat.config

import com.google.gson.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File

object ConfigManager {
    private val LOGGER = LoggerFactory.getLogger("ShadowChat/Config")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("shadowchat")
        .resolve("chat_config.json")
        .toFile()

    private const val PRUNE_UNUSED_KEYS = true
    private const val WRITE_BACK_MERGED = true

    @Volatile
    var lastLoadOk: Boolean = true
        private set

    var chatConfig = ChatConfig(
        rankFormats = listOf(
            RankFormat(
                rankDisplayName = "%player:name%",
                rankChatFormat = "<#ff5555>%luckperms:prefix%</#> \${display_name} <#aaaaaa>➤</#> \${message}",
                rankPermission = "shadowchat.rank.admin"
            ),
            RankFormat(
                rankDisplayName = "%player:name%",
                rankChatFormat = "<#55ffff>%luckperms:prefix%</#> \${display_name} <#aaaaaa>➤</#> \${message}",
                rankPermission = "shadowchat.rank.default"
            )
        )
    )

    fun load() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(chatConfig))
            lastLoadOk = true
            LOGGER.info("[SC] Created default config at: ${configFile.absolutePath}")
            return
        }

        val rawText = runCatching { configFile.readText() }.getOrElse { t ->
            lastLoadOk = false
            LOGGER.error("[SC] Failed to read config file: ${configFile.absolutePath}", t)
            return
        }

        val parsed: JsonElement = try {
            JsonParser.parseString(rawText)
        } catch (t: Throwable) {
            lastLoadOk = false
            LOGGER.error(
                "[SC] Invalid JSON syntax in config. Keeping previous config and NOT overwriting file: ${configFile.absolutePath}",
                t
            )
            return
        }

        if (!parsed.isJsonObject) {
            lastLoadOk = false
            LOGGER.error("[SC] Config root must be a JSON object. File: ${configFile.absolutePath}")
            return
        }

        val existingObj = parsed.asJsonObject
        val defaultsObj = gson.toJsonTree(ChatConfig()).asJsonObject
        val merged = deepMerge(existingObj, defaultsObj)

        if (PRUNE_UNUSED_KEYS) {
            deepPrune(merged, defaultsObj)
        }

        val loaded = try {
            val type = object : TypeToken<ChatConfig>() {}.type
            gson.fromJson<ChatConfig>(merged, type)
        } catch (t: Throwable) {
            lastLoadOk = false
            LOGGER.error(
                "[SC] Config has invalid structure/types. Keeping previous config and NOT overwriting file: ${configFile.absolutePath}",
                t
            )
            return
        }

        if (loaded == null) {
            lastLoadOk = false
            LOGGER.error("[SC] Config parsed as null (keeping previous). File: ${configFile.absolutePath}")
            return
        }

        chatConfig = loaded
        lastLoadOk = true
        LOGGER.info("[SC] Loaded config: ${configFile.absolutePath}")

        if (WRITE_BACK_MERGED) {
            runCatching {
                configFile.writeText(gson.toJson(merged))
            }.onFailure { t ->
                LOGGER.error("[SC] Failed to write merged config back to disk: ${configFile.absolutePath}", t)
            }
        }
    }

    fun save() {
        if (!lastLoadOk) {
            LOGGER.warn("[SC] Skipping config save because last load failed (prevents overwriting a broken JSON). File: ${configFile.absolutePath}")
            return
        }

        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(chatConfig))
        } catch (t: Throwable) {
            LOGGER.error("[SC] Failed to save config: ${configFile.absolutePath}", t)
        }
    }

    private fun deepMerge(existing: JsonObject, defaults: JsonObject): JsonObject {
        val out = existing.deepCopy().asJsonObject

        for ((key, defVal) in defaults.entrySet()) {
            val curVal = out.get(key)

            if (curVal == null || curVal.isJsonNull) {
                out.add(key, defVal.deepCopy())
                continue
            }

            if (curVal.isJsonObject && defVal.isJsonObject) {
                out.add(key, deepMerge(curVal.asJsonObject, defVal.asJsonObject))
            }
        }

        return out
    }

    private fun deepPrune(obj: JsonObject, defaults: JsonObject) {
        val toRemove = obj.entrySet().map { it.key }.filter { !defaults.has(it) }
        toRemove.forEach(obj::remove)

        for ((key, defVal) in defaults.entrySet()) {
            val curVal = obj.get(key) ?: continue
            if (curVal.isJsonObject && defVal.isJsonObject) {
                deepPrune(curVal.asJsonObject, defVal.asJsonObject)
            }
        }
    }
}

data class ChatConfig(
    val displayName: String = "<#00FF00>%player:name%</#>",
    val chatFormat: String = "%luckperms:prefix% \${display_name} <#AAAAAA>➤</#> \${message}",
    val joinMessage: String = "<#FFD700>%player:name% joined the server!</#>",
    val leaveMessage: String = "<#FFD700>%player:name% left the server!</#>",
    val baseDeath: String = "<#424242>☠</#> <#a9a9a9>\${default_message}</#>",
    val commandFormats: CommandFormats = CommandFormats(),
    val advancementFormats: AdvancementFormats = AdvancementFormats(),
    val mentionFormats: MentionFormats = MentionFormats(),
    val nameTag: NameTagConfig = NameTagConfig(),
    val chatSystems: ChatSystems = ChatSystems(),
    val rankFormats: List<RankFormat> = listOf(),
    val spam: SpamConfig = SpamConfig(),
    val tab: TabListConfig = TabListConfig(),
    val tabSettings: TabSettings = TabSettings(),
    val joinMotd: JoinMotdConfig = JoinMotdConfig()
)

data class CommandFormats(
    val msgSent: String = "<#b77df9>You whisper to <#ffe856>%receiver%</#>: <#e0c8fb>%message%</#>",
    val msgReceived: String = "<#b77df9>%sender%</#> <#b77df9>whispers to you: <#e0c8fb>%message%</#>",
    val say: String = "<#ffe856>%player%</#> <#2c2c2c>↔ <#eafff7>%message%</#>",
    val me: String = "<#d7f33b>* %player% %message%</#>",
    val spy: String = "<#141414>[</#><#26D0FF>Spy</#><#141414>]</#> <#ffba26>%sender%</#> <#AAAAAA>→</#> <#55ffff>%targets%</#>: <#FFFFFF>%message%</#>",
    val broadcast: String = "<#fcc00>[Broadcast]</#> <#ffffff>%message%</#>"
)

data class AdvancementFormats(
    val enabled: Boolean = true,
    val task: String = "&7%player% has made the advancement &a[%advancement%]&7!",
    val goal: String = "&7%player% has reached the goal &e[%advancement%]&7!",
    val challenge: String = "&7%player% has completed the challenge &d[%advancement%]&7!"
)

data class MentionFormats(
    val mentionedTitle: String = "<#ffe033>%sender% mentioned you in the message:</#>",
    val mentionedMessage: String = "<#aaaaaa>→ %message%</#>"
)

data class NameTagConfig(
    val prefix: String = "%luckperms:prefix% ",
    val nameColor: String = "&f",
    val suffix: String = "",
)

data class RankFormat(
    val rankDisplayName: String,
    val rankChatFormat: String,
    val rankPermission: String
)

data class ChatSystems(
    val announcements: Boolean = true,
    val logMessages: Boolean = true,
    val rankFormat: Boolean = true,
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
    val playerFormat: String = "%luckperms:prefix% \${displayname}"
)

data class TabSettings(
    val autoRefreshTicks: Int = 100
)

data class JoinMotdConfig(
    val enabled: Boolean = true,
    val sendBeforeJoinMessage: Boolean = true,
    val lines: List<String> = listOf(
        "<#3D3D3D>--------------------------------</#>",
        "<#55ffff>Welcome, %player:name%!</#>",
        "<#aaaaaa>Use </#><#ffe856>/rules</#><#aaaaaa> and </#><#ffe856>/discord</#><#aaaaaa>.</#>",
        "<#3D3D3D>--------------------------------</#>"
    ),
    val sendAfterTicks: Int = 10
)
