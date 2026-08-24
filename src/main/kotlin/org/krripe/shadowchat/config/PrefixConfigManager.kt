package org.krripe.shadowchat.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File

object PrefixConfigManager {

    private val LOGGER = LoggerFactory.getLogger("ShadowChat/Prefixes")
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("shadowchat")
        .resolve("prefixes.json")
        .toFile()

    private const val PRUNE_UNUSED_KEYS = true
    private const val WRITE_BACK_MERGED = true

    @Volatile
    var lastLoadOk: Boolean = true
        private set

    var prefixConfig: PrefixesConfig = PrefixesConfig()

    fun load() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(prefixConfig))
            lastLoadOk = true
            LOGGER.info("[SC] Created prefixes config at: ${configFile.absolutePath}")
            return
        }

        val rawText = runCatching {
            configFile.readText()
        }.getOrElse { t ->
            lastLoadOk = false
            LOGGER.error("[SC] Failed to read prefixes config: ${configFile.absolutePath}", t)
            return
        }

        val parsed: JsonElement = try {
            JsonParser.parseString(rawText)
        } catch (t: Throwable) {
            lastLoadOk = false
            LOGGER.error(
                "[SC] Invalid JSON syntax in prefixes config. Keeping previous config and NOT overwriting file: ${configFile.absolutePath}",
                t
            )
            return
        }

        if (!parsed.isJsonObject) {
            lastLoadOk = false
            LOGGER.error("[SC] Prefixes config root must be a JSON object: ${configFile.absolutePath}")
            return
        }

        val existing = parsed.asJsonObject
        val defaults = gson.toJsonTree(PrefixesConfig()).asJsonObject

        val merged = deepMerge(existing, defaults)
        mergePrefixEntries(merged)

        if (PRUNE_UNUSED_KEYS) {
            deepPrune(merged, defaults)
            prunePrefixEntries(merged)
        }

        val loaded = try {
            gson.fromJson(merged, PrefixesConfig::class.java)
        } catch (t: Throwable) {
            lastLoadOk = false
            LOGGER.error(
                "[SC] Prefixes config has invalid values/types. Keeping previous config and NOT overwriting file: ${configFile.absolutePath}",
                t
            )
            return
        }

        if (loaded == null) {
            lastLoadOk = false
            LOGGER.error("[SC] Prefixes config parsed as null: ${configFile.absolutePath}")
            return
        }

        prefixConfig = loaded
        lastLoadOk = true
        LOGGER.info("[SC] Loaded prefixes config: ${configFile.absolutePath}")

        if (WRITE_BACK_MERGED) {
            runCatching {
                configFile.writeText(gson.toJson(merged))
            }.onFailure { t ->
                LOGGER.error("[SC] Failed to update prefixes config: ${configFile.absolutePath}", t)
            }
        }
    }

    fun save() {
        if (!lastLoadOk) {
            LOGGER.warn(
                "[SC] Skipping prefixes config save because the last load failed: ${configFile.absolutePath}"
            )
            return
        }

        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(prefixConfig))
        } catch (t: Throwable) {
            LOGGER.error("[SC] Failed to save prefixes config: ${configFile.absolutePath}", t)
        }
    }

    private fun deepMerge(existing: JsonObject, defaults: JsonObject): JsonObject {
        val output = existing.deepCopy().asJsonObject

        for ((key, defaultValue) in defaults.entrySet()) {
            val currentValue = output.get(key)

            if (currentValue == null || currentValue.isJsonNull) {
                output.add(key, defaultValue.deepCopy())
                continue
            }

            if (currentValue.isJsonObject && defaultValue.isJsonObject) {
                output.add(
                    key,
                    deepMerge(currentValue.asJsonObject, defaultValue.asJsonObject)
                )
            }
        }

        return output
    }

    private fun deepPrune(existing: JsonObject, defaults: JsonObject) {
        val unusedKeys = existing.entrySet()
            .map { it.key }
            .filter { !defaults.has(it) }

        unusedKeys.forEach(existing::remove)

        for ((key, defaultValue) in defaults.entrySet()) {
            val currentValue = existing.get(key) ?: continue

            if (currentValue.isJsonObject && defaultValue.isJsonObject) {
                deepPrune(currentValue.asJsonObject, defaultValue.asJsonObject)
            }
        }
    }

    private fun mergePrefixEntries(root: JsonObject) {
        val prefixesElement = root.get("prefixes") ?: return
        if (!prefixesElement.isJsonArray) return

        val entryDefaults = gson.toJsonTree(PrefixDefinition()).asJsonObject
        val output = JsonArray()

        for (element in prefixesElement.asJsonArray) {
            if (element.isJsonObject) {
                output.add(deepMerge(element.asJsonObject, entryDefaults))
            } else {
                output.add(element)
            }
        }

        root.add("prefixes", output)
    }

    private fun prunePrefixEntries(root: JsonObject) {
        val prefixesElement = root.get("prefixes") ?: return
        if (!prefixesElement.isJsonArray) return

        val entryDefaults = gson.toJsonTree(PrefixDefinition()).asJsonObject

        for (element in prefixesElement.asJsonArray) {
            if (element.isJsonObject) {
                deepPrune(element.asJsonObject, entryDefaults)
            }
        }
    }
}

data class PrefixesConfig(
    val enabled: Boolean = true,
    val aliases: List<String> = listOf(
        "prefix",
        "tag",
        "tags"
    ),
    val maxActivePrefixes: Int = 3,
    val separator: String = " ",
    val prefixes: List<PrefixDefinition> = listOf(
        PrefixDefinition(
            id = "master",
            display = "<#555555>[</#><#AAAAAA>Master</#><#555555>]</#>",
            description = "Default Master prefix",
            permission = "",
            priority = 100,
            enabled = true,
            selectable = true,
            defaultOwned = true,
            autoEquip = true,
            exclusiveGroup = "rank"
        ),
        PrefixDefinition(
            id = "elite",
            display = "<gradient:#FFD700:#FF7A00>[Elite]</gradient>",
            description = "Reward for winning an event",
            permission = "",
            priority = 50,
            enabled = true,
            selectable = true,
            defaultOwned = false,
            autoEquip = false,
            exclusiveGroup = "reward"
        )
    )
)

data class PrefixDefinition(
    val id: String = "",
    val display: String = "",
    val description: String = "",
    val permission: String = "",
    val priority: Int = 0,
    val enabled: Boolean = true,
    val selectable: Boolean = true,
    val defaultOwned: Boolean = false,
    val autoEquip: Boolean = false,
    val exclusiveGroup: String = ""
)