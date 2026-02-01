package org.krripe.shadowchat.manager

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.query.QueryOptions
import net.minecraft.scoreboard.AbstractTeam
import net.minecraft.scoreboard.Team
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.krripe.shadowchat.config.ConfigManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object WeightManager {

    private const val TEAM_PREFIX = "slp_"
    private const val MAX_WEIGHT = 9999

    private val pendingLoads = ConcurrentHashMap.newKeySet<UUID>()

    private val sortCache = ConcurrentHashMap<UUID, Int>()

    private fun lp(): LuckPerms? =
        try { LuckPermsProvider.get() } catch (_: Throwable) { null }

    private fun queryOptions(lp: LuckPerms, player: ServerPlayerEntity): QueryOptions {
        return lp.contextManager.getQueryOptions(player)
    }

    private fun computeLuckPermsWeight(lp: LuckPerms, player: ServerPlayerEntity, user: User): Int {
        val q = queryOptions(lp, player)

        var maxGroupWeight: Int? = null
        for (g: Group in user.getInheritedGroups(q)) {
            val opt = g.weight
            if (opt.isPresent) {
                val w = opt.asInt
                maxGroupWeight = if (maxGroupWeight == null) w else maxOf(maxGroupWeight, w)
            }
        }

        val metaWeight = user.cachedData.getMetaData(q)
            .getMetaValue("weight")
            ?.toIntOrNull()

        return maxGroupWeight ?: metaWeight ?: 0
    }

    private fun sortKey(player: ServerPlayerEntity): Int {
        val uuid = player.uuid
        sortCache[uuid]?.let { return it }

        val lp = lp() ?: return 9999

        val user = lp.userManager.getUser(uuid)
        if (user == null) {
            if (pendingLoads.add(uuid)) {
                lp.userManager.loadUser(uuid).whenComplete { _, _ ->
                    pendingLoads.remove(uuid)

                    val server = player.server
                    val online = server.playerManager.getPlayer(uuid) ?: return@whenComplete

                    server.execute {
                        sortCache.remove(uuid)

                        applyTeam(online)

                        if (ConfigManager.chatConfig.chatSystems.tab) {
                            TabListManager.pushDisplayNameUpdate(online)
                        }
                    }
                }
            }

            return 9999
        }

        val weight = computeLuckPermsWeight(lp, player, user)
        val clamped = weight.coerceIn(0, MAX_WEIGHT)
        val inv = MAX_WEIGHT - clamped

        sortCache[uuid] = inv
        return inv
    }

    private fun teamNameFor(player: ServerPlayerEntity): String {
        val key = String.format("%04d", sortKey(player))
        val short = player.uuid.toString().substring(0, 8)
        return TEAM_PREFIX + key + short
    }

    fun applyTeam(player: ServerPlayerEntity) {
        val sb = player.server.scoreboard
        val holder = player.gameProfile.name

        sb.getScoreHolderTeam(holder)?.let { current: AbstractTeam ->
            val name = current.name
            if (name.startsWith("scw_") || name.startsWith("scw") || name.startsWith(TEAM_PREFIX)) {
                sb.removeScoreHolderFromTeam(holder, current as? Team)
                sb.getTeam(name)?.let { t -> if (t.playerList.isEmpty()) sb.removeTeam(t) }
            }
        }

        val teamName = teamNameFor(player)
        val team = sb.getTeam(teamName) ?: sb.addTeam(teamName).apply {
            setShowFriendlyInvisibles(false)
            nameTagVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
            deathMessageVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
            color = Formatting.WHITE
        }

        val nameTagsEnabled = ConfigManager.chatConfig.chatSystems.nameTags

        if (nameTagsEnabled) {
            team.prefix = NameTagManager.prefix(player)
            team.suffix = NameTagManager.suffix(player)
            team.color = NameTagManager.nameColor(player)
            team.nameTagVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
        } else {
            team.prefix = Text.empty()
            team.suffix = Text.empty()
            team.color = Formatting.WHITE
            team.nameTagVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
        }

        sb.addScoreHolderToTeam(holder, team)
    }

    fun refreshAll(server: MinecraftServer) {
        sortCache.clear()
        server.playerManager.playerList.forEach { applyTeam(it) }
    }
}