package org.krripe.shadowchat.data

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader

object NicknameStorage {
    private val file = FabricLoader.getInstance().configDir.resolve("shadowchat/data/nicknames.json").toFile()
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun save(data: Map<String, String>) {
        try {
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            println("[ShadowChat] Failed to save nicknames: ${e.message}")
        }
    }

    fun load(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            println("[ShadowChat] Failed to load nicknames: ${e.message}")
            emptyMap()
        }
    }
}
