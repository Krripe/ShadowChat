package org.krripe.shadowchat.api

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface ShadowChatListener {

    /**
     * Called **before** a chat message is processed by ShadowChat (spam, bad-words, formatting).
     *
     * You may:
     * - Mutate [ChatPreEvent.message] to change the content.
     * - Set `ctx.cancel = true` to block the message entirely.
     * - Provide `ctx.overrideFormatted` to skip formatting and use your final [Text].
     * - Set `ctx.bypassFilters = true` to skip spam/bad-words/format-permission checks.
     *
     * Typical uses:
     * - Custom profanity / link filters.
     * - Chat command shortcuts (e.g., replace "!roll" with a formatted result).
     * - Auto-translation or content rewriting.
     *
     * Example:
     * ```
     * override fun onChatPre(ctx: ChatPreEvent) {
     *     if (ctx.message.startsWith("!shout ")) {
     *         ctx.overrideFormatted = Text.literal("[SHOUT] ${ctx.message.removePrefix("!shout ")}")
     *     }
     * }
     * ```
     *
     * @param ctx Mutable context for the in-flight chat message.
     */
    fun onChatPre(ctx: ChatPreEvent) {}

    /**
     * Called **after** a chat message has been formatted and broadcast to all players.
     *
     * The message has already reached players; use this to react (e.g., analytics, mirroring).
     *
     * Example:
     * ```
     * override fun onChatPost(sender: ServerPlayerEntity, formatted: Text) {
     *     myDiscordBridge.send(formatted.string)
     * }
     * ```
     *
     * @param sender The original player who sent the message.
     * @param formatted The exact [Text] that was broadcast.
     */
    fun onChatPost(sender: ServerPlayerEntity, formatted: Text) {}

    /**
     * Fired when ShadowChat processes a **private message** (/msg).
     *
     * This is invoked after ShadowChat formats & delivers the PM,
     * so you can log, mirror, or trigger alerts.
     *
     * Example:
     * ```
     * override fun onPrivateMessage(
     *     sender: ServerPlayerEntity,
     *     targets: List<ServerPlayerEntity>,
     *     content: String
     * ) {
     *     auditLog("PM", sender.name.string, targets.map { it.name.string }, content)
     * }
     * ```
     *
     * @param sender The PM sender.
     * @param targets Recipients of the PM.
     * @param content The plain PM content (unformatted).
     */
    fun onPrivateMessage(sender: ServerPlayerEntity, targets: List<ServerPlayerEntity>, content: String) {}

    /**
     * Fired when ShadowChat processes a **broadcast** (/broadcast, /bc).
     *
     * Use this for logging, cross-mod hooks, or to forward broadcasts elsewhere.
     *
     * Example:
     * ```
     * override fun onBroadcast(source: ServerCommandSource, content: String, formatted: Text) {
     *     console.info("[Broadcast by ${source.name}] $content")
     * }
     * ```
     *
     * @param source The command source that initiated the broadcast (player/console/RCON).
     * @param content Raw content used to build [formatted].
     * @param formatted The final [Text] that was broadcast to players.
     */
    fun onBroadcast(source: ServerCommandSource, content: String, formatted: Text) {}

    /**
     * Fired after ShadowChat formats and announces a **player join** message.
     *
     * Example:
     * ```
     * override fun onJoin(player: ServerPlayerEntity, formatted: Text) {
     *     welcomeStats.increment()
     * }
     * ```
     *
     * @param player The player who joined.
     * @param formatted The join [Text] as displayed by ShadowChat.
     */
    fun onJoin(player: ServerPlayerEntity, formatted: Text) {}

    /**
     * Fired after ShadowChat formats and announces a **player leave** message.
     *
     * Example:
     * ```
     * override fun onLeave(player: ServerPlayerEntity, formatted: Text) {
     *     sessionStore.end(player.uuid)
     * }
     * ```
     *
     * @param player The player who left.
     * @param formatted The leave [Text] as displayed by ShadowChat.
     */
    fun onLeave(player: ServerPlayerEntity, formatted: Text) {}

    /**
     * Fired after ShadowChat formats and announces a **death** message.
     *
     * Example:
     * ```
     * override fun onDeath(player: ServerPlayerEntity, formatted: Text) {
     *     deathFeed.push(formatted.string)
     * }
     * ```
     *
     * @param player The player who died.
     * @param formatted The death [Text] as displayed by ShadowChat.
     */
    fun onDeath(player: ServerPlayerEntity, formatted: Text) {}

    /**
     * Fired when a player's nickname changes (set or cleared).
     *
     * `newRaw == null` indicates the nickname was cleared and the player will
     * fall back to their vanilla name in displays (unless formatted elsewhere).
     *
     * Example:
     * ```
     * override fun onNicknameChanged(player: ServerPlayerEntity, newRaw: String?) {
     *     log.info("Nick change: ${player.name.string} -> ${newRaw ?: "<reset>"}")
     * }
     * ```
     *
     * @param player The player whose nickname changed.
     * @param newRaw New raw nickname string with ShadowChat markup, or `null` if cleared.
     */
    fun onNicknameChanged(player: ServerPlayerEntity, newRaw: String?) {}
}
