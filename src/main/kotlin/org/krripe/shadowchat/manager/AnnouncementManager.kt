package org.krripe.shadowchat.manager

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.krripe.shadowchat.config.Announcement
import org.krripe.shadowchat.config.AnnouncementsConfig
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

object AnnouncementManager {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val file: Path = FabricLoader.getInstance()
        .configDir.resolve("shadowchat").resolve("announcements.json")

    @Volatile var cfg: AnnouncementsConfig = AnnouncementsConfig()
        private set

    private val exec = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ShadowChat-Announcements").apply { isDaemon = true }
    }
    private val tasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    fun init() {
        load()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            cancelAll()
            if (ConfigManager.chatConfig.chatSystems.announcements) {
                scheduleAll(server)
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { cancelAll() }
    }

    fun load() {
        try {
            if (!file.exists()) {
                file.parent.createDirectories()
                cfg = AnnouncementsConfig()
                save()
            } else {
                val type = object : TypeToken<AnnouncementsConfig>() {}.type
                cfg = gson.fromJson(Files.readString(file), type) ?: AnnouncementsConfig()
            }
        } catch (_: Exception) {
            cfg = AnnouncementsConfig()
        }
    }

    fun save() {
        try {
            file.parent.createDirectories()
            file.writeText(gson.toJson(cfg))
        } catch (_: Exception) { }
    }

    fun onConfigReload(server: MinecraftServer) {
        val systems = ConfigManager.chatConfig.chatSystems
        cancelAll()
        load()
        if (systems.announcements) {
            scheduleAll(server)
        }
    }

    fun scheduleAll(server: MinecraftServer) {
        cfg.announcements
            .filter { it.enabled && it.lines.isNotEmpty() }
            .forEach { schedule(server, it) }
    }

    private fun schedule(server: MinecraftServer, ann: Announcement) {
        val period = maxOf(1L, ann.intervalSeconds.toLong())
        val task = exec.scheduleAtFixedRate({
            server.execute { sendAnnouncement(server, ann) }
        }, period, period, TimeUnit.SECONDS)

        tasks[ann.id] = task
    }

    fun cancelAll() {
        tasks.values.forEach { it.cancel(false) }
        tasks.clear()
    }

    private fun sendAnnouncement(server: MinecraftServer, ann: Announcement) {
        val lines = ann.lines.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        for (p in server.playerManager.playerList) {
            val ctx = PlaceholderContext.of(p)
            for (raw in lines) {
                val colored = CustomColorParser.processAllFormats(raw)
                val parsed: Text = Placeholders.parseText(colored, ctx)
                p.sendMessage(parsed, false)
            }
        }
    }
}