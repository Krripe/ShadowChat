package org.krripe.shadowchat.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.utils.CustomColorParser
import java.io.File

object MessageManager {

    private val file: File = FabricLoader.getInstance().configDir
        .resolve("shadowchat")
        .resolve("messages.json")
        .toFile()

    private const val PRUNE_UNUSED_KEYS = true
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    data class Messages(
        val prefix: String = "&8[&bShadowChat&8] &7",
        val noPermission: String = "%prefix%&cYou don't have permission to do that.",

        val reloaded: String = "%prefix%&aConfig & messages reloaded.",

        val nickSet: String = "%prefix%&aNickname set to: &f%nickname%&a.",
        val nickReset: String = "%prefix%&cNickname reset.",

        val onlyPlayers: String = "%prefix%&cOnly players can use this command.",
        val replyNoOne: String = "%prefix%&cNo one to reply to.",
        val replyTargetOffline: String = "%prefix%&cThat player is no longer online.",

        val spyEnabled: String = "%prefix%&eSpyChat enabled.",
        val spyDisabled: String = "%prefix%&eSpyChat disabled.",

        val ignoreCannotSelf: String = "%prefix%&cYou cannot ignore yourself.",
        val ignoreCannotIgnore: String = "%prefix%&cYou cannot ignore this player.",
        val ignoreOn: String = "%prefix%&aYou are now ignoring &f%target%&a.",
        val ignoreOff: String = "%prefix%&eYou stopped ignoring &f%target%&e.",
        val ignoreListEmpty: String = "%prefix%&7You are not ignoring anyone.",
        val ignoreListHeader: String = "%prefix%&aIgnored (&f%count%&a): &7%list%",
        val ignoreClearedSelf: String = "%prefix%&aYour ignore list has been cleared.",
        val ignoreClearedOther: String = "%prefix%&aCleared ignore list for &f%target%&a.",
        val ignoreClearedNotified: String = "%prefix%&eYour ignore list was cleared by staff.",
        val ignoreMsgBlockedByTarget: String = "%prefix%&cYou can't message &f%target%&c because they ignore you.",
        val ignoreMsgYouIgnoring: String = "%prefix%&eYou are ignoring &f%target%&e. Use &f/ignore %target%&e to unignore."
    )

    @Volatile private var messages: Messages = Messages()

    init { load() }

    fun load() {
        val defaults = gson.toJsonTree(Messages()).asJsonObject

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(defaults))
            messages = gson.fromJson(defaults, Messages::class.java)
            return
        }

        val existing = try {
            gson.fromJson(file.readText(), JsonObject::class.java) ?: JsonObject()
        } catch (_: Exception) {
            JsonObject()
        }

        for ((k, v) in defaults.entrySet()) {
            if (!existing.has(k) || existing.get(k).isJsonNull) existing.add(k, v)
        }

        if (PRUNE_UNUSED_KEYS) {
            val toRemove = existing.entrySet().map { it.key }.filter { !defaults.has(it) }
            toRemove.forEach(existing::remove)
        }

        file.writeText(gson.toJson(existing))
        messages = gson.fromJson(existing, Messages::class.java)
    }

    private fun raw(key: String): String = when (key) {
        "prefix" -> messages.prefix
        "noPermission" -> messages.noPermission

        "reloaded" -> messages.reloaded

        "nickSet" -> messages.nickSet
        "nickReset" -> messages.nickReset

        "onlyPlayers" -> messages.onlyPlayers
        "replyNoOne" -> messages.replyNoOne
        "replyTargetOffline" -> messages.replyTargetOffline

        "spyEnabled" -> messages.spyEnabled
        "spyDisabled" -> messages.spyDisabled

        "ignoreCannotSelf" -> messages.ignoreCannotSelf
        "ignoreCannotIgnore" -> messages.ignoreCannotIgnore
        "ignoreOn" -> messages.ignoreOn
        "ignoreOff" -> messages.ignoreOff
        "ignoreListEmpty" -> messages.ignoreListEmpty
        "ignoreListHeader" -> messages.ignoreListHeader
        "ignoreClearedSelf" -> messages.ignoreClearedSelf
        "ignoreClearedOther" -> messages.ignoreClearedOther
        "ignoreClearedNotified" -> messages.ignoreClearedNotified
        "ignoreMsgBlockedByTarget" -> messages.ignoreMsgBlockedByTarget
        "ignoreMsgYouIgnoring" -> messages.ignoreMsgYouIgnoring

        else -> "%prefix%&cUnknown message key: &f$key"
    }

    private fun buildRaw(key: String, vararg args: Pair<String, String>): String {
        var s = raw(key).replace("%prefix%", raw("prefix"))
        args.forEach { (ph, v) -> s = s.replace("%$ph%", v) }
        return s
    }

    private fun parseForPlayer(p: ServerPlayerEntity, raw: String): Text {
        val bridged = PlaceholderBridge.apply(p, raw)
        val colored = CustomColorParser.processAllFormats(bridged)
        return Placeholders.parseText(colored, PlaceholderContext.of(p))
    }

    private fun parseForSource(src: ServerCommandSource, raw: String): Text {
        val player = src.player
        val bridged = if (player != null) PlaceholderBridge.apply(player, raw) else raw
        val colored = CustomColorParser.processAllFormats(bridged)
        return Placeholders.parseText(colored, PlaceholderContext.of(src))
    }

    fun get(p: ServerPlayerEntity, key: String, vararg args: Pair<String, String>): Text =
        parseForPlayer(p, buildRaw(key, *args))

    fun get(src: ServerCommandSource, key: String, vararg args: Pair<String, String>): Text =
        parseForSource(src, buildRaw(key, *args))

    @JvmStatic
    fun getMessage(src: ServerCommandSource, key: String, vararg args: Pair<String, String>): Text =
        get(src, key, *args)
}