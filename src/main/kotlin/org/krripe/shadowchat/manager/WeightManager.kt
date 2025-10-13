package org.krripe.shadowchat.manager

import net.minecraft.scoreboard.AbstractTeam
import net.minecraft.scoreboard.Team
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.PermissionsSystem

object WeightManager {
    private const val TEAM_PREFIX = "scw_"

    fun weightOf(player: ServerPlayerEntity): Int {
        for (entry in ConfigManager.chatConfig.weights) {
            if (PermissionsSystem().hasPermission(player.commandSource, entry.weightPerm)) {
                return entry.weight
            }
        }
        return 9_999
    }

    private fun teamNameFor(player: ServerPlayerEntity): String {
        val w = weightOf(player)
        val short = player.uuid.toString().substring(0, 8)
        return TEAM_PREFIX + String.format("%06d", w) + "_" + short
    }

    fun applyTeam(player: ServerPlayerEntity) {
        val sb = player.server.scoreboard
        val holder = player.gameProfile.name

        sb.getScoreHolderTeam(holder)?.let { current: AbstractTeam ->
            if (current.name.startsWith(TEAM_PREFIX)) {
                sb.removeScoreHolderFromTeam(holder, current as Team?)

                val strong = sb.getTeam(current.name)
                if (strong != null && strong.playerList.isEmpty()) {
                    sb.removeTeam(strong)
                }
            }
        }

        val teamName = teamNameFor(player)
        val team = sb.getTeam(teamName) ?: sb.addTeam(teamName).apply {
            color = Formatting.WHITE
            setShowFriendlyInvisibles(false)
            nameTagVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
            deathMessageVisibilityRule = AbstractTeam.VisibilityRule.ALWAYS
        }

        team.prefix = NameTagManager.prefix(player)
        team.suffix = NameTagManager.suffix(player)

        sb.addScoreHolderToTeam(holder, team)
    }

    fun refreshAll(server: MinecraftServer) {
        server.playerManager.playerList.forEach { applyTeam(it) }
    }
}
