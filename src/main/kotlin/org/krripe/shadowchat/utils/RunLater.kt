package org.krripe.shadowchat.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object RunLater {
    private val tasks = ConcurrentHashMap<Int, Pair<AtomicInteger, () -> Unit>>()
    private val idGen = AtomicInteger(0)
    @Volatile private var hooked = false

    fun init() {
        if (hooked) return
        hooked = true

        ServerTickEvents.END_SERVER_TICK.register { _ ->
            val it = tasks.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val counter = e.value.first
                if (counter.decrementAndGet() <= 0) {
                    try { e.value.second.invoke() } catch (_: Throwable) { }
                    it.remove()
                }
            }
        }
    }

    fun run(server: MinecraftServer, delayTicks: Int, action: () -> Unit) {
        init()

        val id = idGen.incrementAndGet()
        tasks[id] = AtomicInteger(delayTicks.coerceAtLeast(0)) to {
            server.execute {
                action()
            }
        }
    }
}
