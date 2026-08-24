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
        val nickTooLong: String = "%prefix%&cNickname is too long. Maximum length is &f%max%&c characters.",

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
        val ignoreMsgYouIgnoring: String = "%prefix%&eYou are ignoring &f%target%&e. Use &f/ignore %target%&e to unignore.",

        val prefixSystemDisabled: String = "%prefix%&cThe custom prefix system is disabled.",
        val prefixNotFound: String = "%prefix%&cUnknown prefix: &f%id%&c.",
        val prefixDisabled: String = "%prefix%&cPrefix &f%id% &cis disabled.",
        val prefixNoAccess: String = "%prefix%&cYou don't have access to prefix &f%id%&c.",
        val prefixNotSelectable: String = "%prefix%&cPrefix &f%id% &ccannot be selected manually.",
        val prefixAlreadyActive: String = "%prefix%&ePrefix &f%id% &eis already active.",
        val prefixNotActive: String = "%prefix%&ePrefix &f%id% &eis not active.",
        val prefixLimitReached: String = "%prefix%&cYou have reached the active prefix limit.",

        val prefixListEmpty: String = "%prefix%&7You don't have access to any prefixes.",
        val prefixListHeader: String = "%prefix%&aAvailable prefixes (&f%count%&a): &7%list%",
        val prefixActiveEmpty: String = "%prefix%&7You don't have any active prefixes.",
        val prefixActiveHeader: String = "%prefix%&aActive prefixes (&f%count%&a): &7%list%",
        val prefixListEntry: String = "%display% &8(&7%id%&8)",
        val prefixListSeparator: String = "&7, ",

        val prefixEquipped: String = "%prefix%&aEquipped prefix &f%id%&a.",
        val prefixUnequipped: String = "%prefix%&eUnequipped prefix &f%id%&e.",
        val prefixCleared: String = "%prefix%&aYour active prefixes have been cleared.",
        val prefixNothingToClear: String = "%prefix%&7You don't have any active prefixes to clear.",

        val prefixAlreadyOwned: String = "%prefix%&ePlayer &f%target% &ealready owns prefix &f%id%&e.",
        val prefixNotOwned: String = "%prefix%&ePlayer &f%target% &edoesn't own prefix &f%id%&e.",
        val prefixGranted: String = "%prefix%&aGranted prefix &f%id% &ato &f%target%&a.",
        val prefixGrantedTarget: String = "%prefix%&aYou received prefix &f%id%&a.",
        val prefixRevoked: String = "%prefix%&cRevoked prefix &f%id% &cfrom &f%target%&c.",
        val prefixRevokedTarget: String = "%prefix%&cPrefix &f%id% &cwas removed from you.",

        val prefixEquippedOther: String = "%prefix%&aEquipped prefix &f%id% &afor &f%target%&a.",
        val prefixEquippedByStaff: String = "%prefix%&aPrefix &f%id% &awas equipped.",
        val prefixUnequippedOther: String = "%prefix%&eUnequipped prefix &f%id% &efor &f%target%&e.",
        val prefixUnequippedByStaff: String = "%prefix%&ePrefix &f%id% &ewas unequipped.",
        val prefixSetOther: String = "%prefix%&aSet active prefix &f%id% &afor &f%target%&a.",
        val prefixSetByStaff: String = "%prefix%&aYour active prefix was set to &f%id%&a.",
        val prefixClearedOther: String = "%prefix%&aCleared active prefixes for &f%target%&a.",
        val prefixClearedByStaff: String = "%prefix%&eYour active prefixes were cleared by staff.",

        val prefixInfo: String = "%prefix%&7ID: &f%id% &8| &7Description: &f%description% &8| &7Priority: &f%priority% &8| &7Permission: &f%permission%",
        val prefixReloaded: String = "%prefix%&aPrefixes config reloaded.",
        val prefixReloadFailed: String = "%prefix%&cPrefixes config could not be reloaded. Check the console for JSON errors."
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
        "nickTooLong" -> messages.nickTooLong

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

        "prefixSystemDisabled" -> messages.prefixSystemDisabled
        "prefixNotFound" -> messages.prefixNotFound
        "prefixDisabled" -> messages.prefixDisabled
        "prefixNoAccess" -> messages.prefixNoAccess
        "prefixNotSelectable" -> messages.prefixNotSelectable
        "prefixAlreadyActive" -> messages.prefixAlreadyActive
        "prefixNotActive" -> messages.prefixNotActive
        "prefixLimitReached" -> messages.prefixLimitReached

        "prefixListEmpty" -> messages.prefixListEmpty
        "prefixListHeader" -> messages.prefixListHeader
        "prefixActiveEmpty" -> messages.prefixActiveEmpty
        "prefixActiveHeader" -> messages.prefixActiveHeader
        "prefixListEntry" -> messages.prefixListEntry
        "prefixListSeparator" -> messages.prefixListSeparator

        "prefixEquipped" -> messages.prefixEquipped
        "prefixUnequipped" -> messages.prefixUnequipped
        "prefixCleared" -> messages.prefixCleared
        "prefixNothingToClear" -> messages.prefixNothingToClear

        "prefixAlreadyOwned" -> messages.prefixAlreadyOwned
        "prefixNotOwned" -> messages.prefixNotOwned
        "prefixGranted" -> messages.prefixGranted
        "prefixGrantedTarget" -> messages.prefixGrantedTarget
        "prefixRevoked" -> messages.prefixRevoked
        "prefixRevokedTarget" -> messages.prefixRevokedTarget

        "prefixEquippedOther" -> messages.prefixEquippedOther
        "prefixEquippedByStaff" -> messages.prefixEquippedByStaff
        "prefixUnequippedOther" -> messages.prefixUnequippedOther
        "prefixUnequippedByStaff" -> messages.prefixUnequippedByStaff
        "prefixSetOther" -> messages.prefixSetOther
        "prefixSetByStaff" -> messages.prefixSetByStaff
        "prefixClearedOther" -> messages.prefixClearedOther
        "prefixClearedByStaff" -> messages.prefixClearedByStaff

        "prefixInfo" -> messages.prefixInfo
        "prefixReloaded" -> messages.prefixReloaded
        "prefixReloadFailed" -> messages.prefixReloadFailed

        else -> "%prefix%&cUnknown message key: &f$key"
    }

    private fun buildRaw(key: String, vararg args: Pair<String, String>): String {
        var s = raw(key).replace("%prefix%", raw("prefix"))
        args.forEach { (ph, v) -> s = s.replace("%$ph%", v) }
        return s
    }

    fun getRaw(key: String, vararg args: Pair<String, String>): String =
        buildRaw(key, *args)

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