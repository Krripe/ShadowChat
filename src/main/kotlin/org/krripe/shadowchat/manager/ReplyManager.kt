package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ReplyManager {
    private val last = ConcurrentHashMap<UUID, UUID>()

    fun setLast(a: ServerPlayerEntity, b: ServerPlayerEntity) {
        last[a.uuid] = b.uuid
    }

    fun getLastUuid(p: ServerPlayerEntity): UUID? = last[p.uuid]

    fun clearPlayer(uuid: UUID) {
        last.remove(uuid)
        val it = last.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.value == uuid) it.remove()
        }
    }
}
