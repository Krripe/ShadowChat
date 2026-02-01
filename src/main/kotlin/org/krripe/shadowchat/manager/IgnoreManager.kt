package org.krripe.shadowchat.manager

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object IgnoreManager {

    private val cache = ConcurrentHashMap<UUID, IgnoreState>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class IgnoreState(
        val ignored: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
        val lastKnownNames: MutableMap<UUID, String> = ConcurrentHashMap()
    )

    private data class IgnoreFile(
        val ignored: List<IgnoreEntry> = emptyList()
    )
    private data class IgnoreEntry(
        val uuid: String,
        val lastKnownName: String? = null
    )

    private fun fileFor(viewer: UUID) =
        FabricLoader.getInstance().configDir
            .resolve("shadowchat")
            .resolve("data")
            .resolve("player")
            .resolve("ignore")
            .resolve("${viewer}.json")

    fun ensureLoaded(viewer: UUID) {
        cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }
    }

    fun unload(viewer: UUID) {
        cache.remove(viewer)
    }

    fun isIgnoring(viewer: UUID, target: UUID): Boolean {
        val st = cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }
        return st.ignored.contains(target)
    }

    fun getIgnored(viewer: UUID): Set<UUID> {
        val st = cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }
        return st.ignored.toSet()
    }

    fun getLastKnownName(viewer: UUID, target: UUID): String? {
        val st = cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }
        return st.lastKnownNames[target]
    }

    fun toggleIgnore(viewer: UUID, target: UUID, targetName: String? = null): Boolean {
        val st = cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }

        val nowIgnored = if (st.ignored.contains(target)) {
            st.ignored.remove(target)
            st.lastKnownNames.remove(target)
            false
        } else {
            st.ignored.add(target)
            if (!targetName.isNullOrBlank()) st.lastKnownNames[target] = targetName
            true
        }

        if (st.ignored.isEmpty()) {
        }

        return nowIgnored
    }

    fun clear(viewer: UUID) {
        val st = cache.computeIfAbsent(viewer) { loadFromDisk(viewer) }
        st.ignored.clear()
        st.lastKnownNames.clear()
    }

    fun save(viewer: UUID) {
        val st = cache[viewer] ?: return

        val out = IgnoreFile(
            ignored = st.ignored
                .map { uuid ->
                    IgnoreEntry(
                        uuid = uuid.toString(),
                        lastKnownName = st.lastKnownNames[uuid]
                    )
                }
                .sortedWith(compareBy({ it.lastKnownName ?: "" }, { it.uuid }))
        )

        val file = fileFor(viewer)
        try {
            file.parent?.createDirectories()
            Files.writeString(file, gson.toJson(out))
        } catch (_: Throwable) {
        }
    }

    fun saveAsync(server: MinecraftServer, viewer: UUID) {
        server.execute { save(viewer) }
    }

    private fun loadFromDisk(viewer: UUID): IgnoreState {
        val file = fileFor(viewer)
        try {
            file.parent?.createDirectories()
            if (!file.exists()) return IgnoreState()

            val json = Files.readString(file)
            val parsed = gson.fromJson(json, IgnoreFile::class.java) ?: IgnoreFile()

            val st = IgnoreState()
            for (e in parsed.ignored) {
                val uuid = runCatching { UUID.fromString(e.uuid) }.getOrNull() ?: continue
                st.ignored.add(uuid)
                if (!e.lastKnownName.isNullOrBlank()) st.lastKnownNames[uuid] = e.lastKnownName
            }
            return st
        } catch (_: JsonSyntaxException) {
            return IgnoreState()
        } catch (_: Throwable) {
            return IgnoreState()
        }
    }
}