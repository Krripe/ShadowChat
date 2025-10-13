package org.krripe.shadowchat.manager

import java.util.*
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.utils.PermissionsSystem

object SpyManager {
    private val spies = mutableSetOf<UUID>()
    private val perms = PermissionsSystem()

    fun toggle(p: ServerPlayerEntity): Boolean =
        if (spies.remove(p.uuid)) false else { spies += p.uuid; true }

    fun isSpying(p: ServerPlayerEntity) = p.uuid in spies

    fun canBeSpied(sender: ServerPlayerEntity?): Boolean =
        sender == null ||
                !perms.hasPermission(sender.commandSource, "shadowchat.spy.bypass")
}