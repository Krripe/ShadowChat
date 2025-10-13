package org.krripe.shadowchat.api

import eu.pb4.placeholders.api.PlaceholderContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface ShadowChatAPI {
    /** The live Minecraft server instance used by this API. */
    val server: MinecraftServer

    /* ===== Formatting / parsing ===== */

    /**
     * Parses ShadowChat color/format tags into a styled [Text].
     *
     * Supported tags include hex colors (e.g. `<#55ffff>`), gradient blocks
     * (`<gradient:#ff0000:#00ff00> ... </gradient>`), and basic tags
     * like `<b>`, `<i>`, `<u>`, `<strike>`, `<obf>`, plus click/hover/link blocks.
     *
     * Example:
     * `parseColors("<#55ffff>Hello</#> <gradient:#ff0000:#ffff00>world</gradient>")`
     *
     * @param raw String containing ShadowChat markup.
     * @return A fully-styled Minecraft [Text].
     */
    fun parseColors(raw: String): Text

    /**
     * Parses ShadowChat tags **and** applies placeholders in the given [source] context.
     *
     * Use this when you need `%placeholders%` resolution (LuckPerms, pb4, server/player, etc.)
     * in addition to color/format parsing.
     *
     * Example:
     * ```
     * api.parseWithPlaceholders(player.commandSource, "<#ffe856>Hello %player:name%</#>")
     * ```
     *
     * @param source The command source providing placeholder context.
     * @param raw String with ShadowChat markup + placeholders.
     * @return Styled [Text] with placeholders resolved.
     */
    fun parseWithPlaceholders(source: ServerCommandSource, raw: String): Text

    /**
     * Formats a player chat line using ShadowChat's configured chat/rank formats.
     *
     * This only creates the final [Text]; it does not broadcast it.
     *
     * Example:
     * `val out = api.formatPlayerChat(player, "hi there!"); server.playerManager.broadcast(out, false)`
     *
     * @param player Sender of the message.
     * @param rawMessage Plain message content (unformatted).
     * @return The final styled [Text] to display.
     */
    fun formatPlayerChat(player: ServerPlayerEntity, rawMessage: String): Text

    /* ===== Nicknames ===== */

    /**
     * Returns the player's raw nickname string if set, otherwise `null`.
     *
     * @param player Target player.
     * @return Raw nickname or `null` if none.
     */
    fun getNicknameRaw(player: ServerPlayerEntity): String?

    /**
     * Returns the player's display name as styled [Text] (nickname if present, otherwise vanilla name).
     *
     * @param player Target player.
     * @return Styled display name.
     */
    fun getNicknameDisplay(player: ServerPlayerEntity): Text

    /**
     * Sets or clears the player's nickname.
     *
     * - Passing `null` (or blank) clears the nickname.
     * - Triggers listener hooks (`onNicknameChanged`).
     * - Automatically refreshes Tab/Teams via the implementation.
     *
     * Example:
     * `api.setNickname(player, "<#55ffff>CoolGuy</#>")`
     *
     * @param player Target player.
     * @param nicknameRaw New nickname with ShadowChat markup, or `null` to reset.
     * @return `true` if applied successfully.
     */
    fun setNickname(player: ServerPlayerEntity, nicknameRaw: String?): Boolean

    /* ===== Tab & Nametag ===== */

    /**
     * Refreshes Tab header/footer, the player's display name row, and team weight for a single player.
     *
     * Use this after changing nickname or nametag prefix/suffix for that player only.
     *
     * @param player Target player to refresh.
     */
    fun refreshTabFor(player: ServerPlayerEntity)

    /**
     * Refreshes Tab header/footer and display names for **all** online players.
     *
     * Useful after reloading config or rank/weight changes.
     */
    fun refreshTabAll()

    /**
     * Overrides the player's **nametag prefix** (above-head/Tab team prefix).
     * Pass `null` to remove the override and fall back to config.
     *
     * Implementation usually re-applies the player team to update nametag immediately.
     *
     * @param player Target player.
     * @param prefix Styled prefix [Text] or `null` to clear override.
     */
    fun setNametagPrefix(player: ServerPlayerEntity, prefix: Text?)

    /**
     * Overrides the player's **nametag suffix** (above-head/Tab team suffix).
     * Pass `null` to remove the override and fall back to config.
     *
     * @param player Target player.
     * @param suffix Styled suffix [Text] or `null` to clear override.
     */
    fun setNametagSuffix(player: ServerPlayerEntity, suffix: Text?)

    /* ===== Spam / Moderation ===== */

    /**
     * Checks whether the player should be temporarily blocked for spam,
     * based on ShadowChat's spam window and message count.
     *
     * @param player Player to evaluate.
     * @return `true` if the current attempt exceeds spam limits.
     */
    fun isSpamming(player: ServerPlayerEntity): Boolean

    /**
     * Sends a standard anti-spam warning message to the player (if enabled),
     * respecting a short cooldown to avoid spammy warnings.
     *
     * @param player Player to warn.
     */
    fun warnSpam(player: ServerPlayerEntity)

    /* ===== Messaging helpers ===== */

    /**
     * Broadcasts a styled message to everyone (server-wide).
     *
     * **Note:** This assumes the [text] is already formatted. If you need placeholders,
     * use [parseWithPlaceholders] first or prefer [sendRawToAll].
     *
     * @param text The final styled [Text] to broadcast.
     */
    fun broadcastSystem(text: Text)

    /**
     * Sends a styled message directly to a single player.
     *
     * **Note:** This assumes the [text] is already formatted. If you need placeholders,
     * use [parseWithPlaceholders] first or prefer [sendRawTo].
     *
     * @param player Recipient player.
     * @param text Styled [Text] to send.
     */
    fun sendStyledTo(player: ServerPlayerEntity, text: Text)

    /**
     * Parses and sends a RAW string (supports ShadowChat colors/tags and placeholders) to a player.
     *
     * Example:
     * ```
     * api.sendRawTo(player, "<#ffe856>Hello %player:name%</#>")
     * ```
     *
     * @param player Recipient player.
     * @param raw String with ShadowChat markup and placeholders.
     * @param ctx Optional placeholder context; defaults to the player's own [ServerCommandSource].
     */
    fun sendRawTo(player: ServerPlayerEntity, raw: String, ctx: ServerCommandSource? = player.commandSource)

    /**
     * Parses and broadcasts a RAW string (colors + placeholders) to everyone.
     *
     * Example:
     * ```
     * api.sendRawToAll("<#55ffff>[INFO]</#> <#ffffff>Event starts in 10 minutes!</#>")
     * ```
     *
     * @param raw String with ShadowChat markup and placeholders.
     * @param ctx Optional context for placeholders (e.g., `server.commandSource`); if `null`, implementation chooses a sensible default.
     */
    fun sendRawToAll(raw: String, ctx: ServerCommandSource? = null)

    /**
     * Sends a chat message **as if it were typed by the player**, going through the chat pipeline.
     *
     * - If `bypassFilters=false` (default): message passes through spam/bad-words/permission filters.
     * - If `bypassFilters=true`: skips those filters (be careful; intended for admin/system use).
     * - Triggers pre/post chat listeners.
     *
     * Example:
     * `api.sendChatFrom(player, "gg everyone!")`
     *
     * @param player The simulated sender.
     * @param raw The plain message content.
     * @param bypassFilters If `true`, skip spam/bad-words/format-permission checks.
     * @return `true` if broadcast happened; `false` if canceled by a pre-listener.
     */
    fun sendChatFrom(player: ServerPlayerEntity, raw: String, bypassFilters: Boolean = false): Boolean

    /* ===== Placeholders ===== */

    /**
     * Builds a pb4 [PlaceholderContext] for the given player.
     *
     * Useful if you need to call pb4 APIs directly with the exact context ShadowChat uses.
     *
     * @param player Player for the placeholder context.
     * @return A [PlaceholderContext] bound to the player.
     */
    fun placeholderContextOf(player: ServerPlayerEntity): PlaceholderContext

    /* ===== Listeners (hooks) ===== */

    /**
     * Registers a [ShadowChatListener] to receive ShadowChat events.
     *
     * Safe to call multiple times across different mods. Implementations are expected
     * to guard against listener exceptions (i.e., they won't crash the dispatcher).
     *
     * @param listener The listener implementation to add.
     */
    fun addListener(listener: ShadowChatListener)

    /**
     * Unregisters a previously added [ShadowChatListener].
     *
     * @param listener The listener to remove.
     */
    fun removeListener(listener: ShadowChatListener)

    /* ===== Event dispatch (public; other mods can trigger manually if needed) ===== */

    /**
     * Fires the **Pre Chat** event. Listeners may:
     * - mutate [ChatPreEvent.message],
     * - set `cancel = true` to stop the message,
     * - set `overrideFormatted` to provide a final [Text],
     * - set `bypassFilters = true` to skip spam/bad-words/format-permission checks.
     *
     * @param ctx Mutable pre-chat context.
     * @return The same [ChatPreEvent] after all listeners have processed it.
     */
    fun fireChatPre(ctx: ChatPreEvent): ChatPreEvent

    /**
     * Fires the **Post Chat** event after a message was formatted and broadcast.
     *
     * @param sender Original sender.
     * @param formatted The final formatted [Text] that was broadcast.
     */
    fun fireChatPost(sender: ServerPlayerEntity, formatted: Text)

    /**
     * Fires a **Private Message** event emitted by ShadowChat's /msg pipeline.
     *
     * @param sender The PM sender.
     * @param targets Recipients of the PM.
     * @param content The plain PM content.
     */
    fun firePrivateMessage(sender: ServerPlayerEntity, targets: List<ServerPlayerEntity>, content: String)

    /**
     * Fires a **Broadcast** event emitted by ShadowChat's /broadcast (and aliases).
     *
     * @param source The command source that initiated the broadcast.
     * @param content Raw content string used to build [formatted].
     * @param formatted The final formatted [Text] that was broadcast.
     */
    fun fireBroadcast(source: ServerCommandSource, content: String, formatted: Text)

    /**
     * Fires a **Join** event with the actual formatted join message ShadowChat displayed.
     *
     * @param player The player who joined.
     * @param formatted The formatted join [Text].
     */
    fun fireJoin(player: ServerPlayerEntity, formatted: Text)

    /**
     * Fires a **Leave** event with the actual formatted leave message ShadowChat displayed.
     *
     * @param player The player who left.
     * @param formatted The formatted leave [Text].
     */
    fun fireLeave(player: ServerPlayerEntity, formatted: Text)

    /**
     * Fires a **Death** event with the final formatted death message ShadowChat displayed.
     *
     * @param player The player who died.
     * @param formatted The formatted death [Text].
     */
    fun fireDeath(player: ServerPlayerEntity, formatted: Text)

    /* ===== Chat Channels ===== */

    /**
     * Registers a custom [ChatChannel].
     *
     * Channels provide: (1) custom formatting of messages, and (2) custom audience selection,
     * optionally guarded by a permission.
     *
     * Example:
     * ```kotlin
     * api.registerChannel(StaffChannel())
     * api.sendToChannel("staff", player, "Server maintenance soon.")
     * ```
     *
     * @param channel Channel descriptor to register (id must be unique, case-insensitive).
     */
    fun registerChannel(channel: ChatChannel)

    /**
     * Unregisters a previously registered [ChatChannel] by id.
     *
     * @param id Channel identifier (case-insensitive).
     */
    fun unregisterChannel(id: String)

    /**
     * Sends a message to a registered channel.
     *
     * Flow:
     * - Checks channel existence and (if set) permission for the sender.
     * - Builds the styled [Text] via `channel.format`.
     * - Delivers it to `channel.audience`.
     *
     * @param id Channel identifier (case-insensitive).
     * @param sender The player sending within the channel.
     * @param content Plain content to format.
     * @return `true` if the channel existed and the message was delivered; `false` otherwise.
     */
    fun sendToChannel(id: String, sender: ServerPlayerEntity, content: String): Boolean
}
