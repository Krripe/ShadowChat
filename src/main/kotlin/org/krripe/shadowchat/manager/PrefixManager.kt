package org.krripe.shadowchat.manager

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.config.PrefixConfigManager
import org.krripe.shadowchat.config.PrefixDefinition
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.utils.PermissionsSystem
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object PrefixManager {

    private val cache = ConcurrentHashMap<UUID, PrefixState>()
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val perms = PermissionsSystem()

    private data class PrefixState(
        val owned: MutableSet<String> = ConcurrentHashMap.newKeySet(),
        val active: MutableSet<String> = ConcurrentHashMap.newKeySet(),
        var initializedDefaults: Boolean = false
    )

    private data class PrefixFile(
        val owned: List<String> = emptyList(),
        val active: List<String> = emptyList(),
        val initializedDefaults: Boolean = false
    )

    private fun fileFor(uuid: UUID) =
        FabricLoader.getInstance().configDir
            .resolve("shadowchat")
            .resolve("data")
            .resolve("player")
            .resolve("prefixes")
            .resolve("$uuid.json")

    private fun normalizeId(id: String): String =
        id.trim().lowercase()

    fun ensureLoaded(player: ServerPlayerEntity) {
        stateFor(player)
    }

    fun unload(player: ServerPlayerEntity) {
        save(player.uuid)
        cache.remove(player.uuid)
    }

    fun getDefinition(id: String): PrefixDefinition? {
        val normalized = normalizeId(id)

        return PrefixConfigManager.prefixConfig.prefixes.firstOrNull {
            normalizeId(it.id) == normalized
        }
    }

    fun getAllDefinitions(): List<PrefixDefinition> {
        return PrefixConfigManager.prefixConfig.prefixes
            .filter { it.id.isNotBlank() }
            .sortedWith(
                compareByDescending<PrefixDefinition> { it.priority }
                    .thenBy { it.id.lowercase() }
            )
    }

    fun getOwned(player: ServerPlayerEntity): Set<String> {
        return stateFor(player).owned.toSet()
    }

    fun getActive(player: ServerPlayerEntity): List<PrefixDefinition> {
        val state = stateFor(player)

        return state.active
            .mapNotNull(::getDefinition)
            .filter { it.enabled }
            .sortedWith(
                compareByDescending<PrefixDefinition> { it.priority }
                    .thenBy { it.id.lowercase() }
            )
    }

    fun getAvailable(player: ServerPlayerEntity): List<PrefixDefinition> {
        val state = stateFor(player)

        return getAllDefinitions()
            .filter { it.enabled }
            .filter { it.selectable }
            .filter { hasAccess(player, it, state) }
    }

    fun isActive(player: ServerPlayerEntity, id: String): Boolean {
        return stateFor(player).active.contains(normalizeId(id))
    }

    fun grant(player: ServerPlayerEntity, id: String): Boolean {
        val definition = getDefinition(id) ?: return false
        val state = stateFor(player)
        val normalized = normalizeId(definition.id)

        if (!state.owned.add(normalized)) {
            return false
        }

        if (definition.autoEquip && definition.enabled) {
            equipInternal(
                player = player,
                definition = definition,
                state = state,
                force = true
            )
        }

        save(player.uuid)
        refreshPlayer(player)
        return true
    }

    fun revoke(player: ServerPlayerEntity, id: String): Boolean {
        val definition = getDefinition(id) ?: return false
        val state = stateFor(player)
        val normalized = normalizeId(definition.id)

        val removedOwned = state.owned.remove(normalized)
        val removedActive = state.active.remove(normalized)

        if (!removedOwned && !removedActive) {
            return false
        }

        save(player.uuid)
        refreshPlayer(player)
        return true
    }

    fun equip(
        player: ServerPlayerEntity,
        id: String,
        force: Boolean = false
    ): PrefixActionResult {
        val definition = getDefinition(id)
            ?: return PrefixActionResult.NOT_FOUND

        val state = stateFor(player)

        val result = equipInternal(
            player = player,
            definition = definition,
            state = state,
            force = force
        )

        if (result == PrefixActionResult.SUCCESS) {
            save(player.uuid)
            refreshPlayer(player)
        }

        return result
    }

    fun unequip(
        player: ServerPlayerEntity,
        id: String
    ): PrefixActionResult {
        val definition = getDefinition(id)
            ?: return PrefixActionResult.NOT_FOUND

        val state = stateFor(player)
        val normalized = normalizeId(definition.id)

        if (!state.active.remove(normalized)) {
            return PrefixActionResult.NOT_ACTIVE
        }

        save(player.uuid)
        refreshPlayer(player)
        return PrefixActionResult.SUCCESS
    }

    fun toggle(player: ServerPlayerEntity, id: String): PrefixActionResult {
        return if (isActive(player, id)) {
            unequip(player, id)
        } else {
            equip(player, id)
        }
    }

    fun setActive(
        player: ServerPlayerEntity,
        id: String,
        force: Boolean = false
    ): PrefixActionResult {
        val definition = getDefinition(id)
            ?: return PrefixActionResult.NOT_FOUND

        val state = stateFor(player)
        val previous = state.active.toSet()

        state.active.clear()

        val result = equipInternal(
            player = player,
            definition = definition,
            state = state,
            force = force
        )

        if (result != PrefixActionResult.SUCCESS) {
            state.active.addAll(previous)
            return result
        }

        save(player.uuid)
        refreshPlayer(player)
        return PrefixActionResult.SUCCESS
    }

    fun clearActive(player: ServerPlayerEntity): Boolean {
        val state = stateFor(player)

        if (state.active.isEmpty()) {
            return false
        }

        state.active.clear()
        save(player.uuid)
        refreshPlayer(player)
        return true
    }

    fun render(player: ServerPlayerEntity): Text {
        if (!PrefixConfigManager.prefixConfig.enabled) {
            return Text.empty()
        }

        val active = getActive(player)
        if (active.isEmpty()) {
            return Text.empty()
        }

        val output = Text.literal("")
        val separator = parse(PrefixConfigManager.prefixConfig.separator, player)

        active.forEachIndexed { index, definition ->
            if (index > 0) {
                output.append(separator.copy())
            }

            output.append(parse(definition.display, player))
        }

        return output
    }

    fun renderPrimary(player: ServerPlayerEntity): Text {
        if (!PrefixConfigManager.prefixConfig.enabled) {
            return Text.empty()
        }

        val first = getActive(player).firstOrNull()
            ?: return Text.empty()

        return parse(first.display, player)
    }

    fun parseWithPrefixes(
        player: ServerPlayerEntity,
        raw: String
    ): Text {
        val tokens = listOf(
            "\${custom_prefixes}" to false,
            "\\\${custom_prefixes}" to false,
            "\${custom_prefix}" to true,
            "\\\${custom_prefix}" to true
        )

        val output = Text.literal("")
        var remaining = raw

        while (remaining.isNotEmpty()) {
            var foundIndex = -1
            var foundToken: Pair<String, Boolean>? = null

            for (candidate in tokens) {
                val index = remaining.indexOf(candidate.first)

                if (
                    index >= 0 &&
                    (foundIndex == -1 || index < foundIndex)
                ) {
                    foundIndex = index
                    foundToken = candidate
                }
            }

            val token = foundToken

            if (token == null) {
                output.append(parse(remaining, player))
                break
            }

            if (foundIndex > 0) {
                output.append(
                    parse(
                        remaining.substring(0, foundIndex),
                        player
                    )
                )
            }

            output.append(
                if (token.second) {
                    renderPrimary(player)
                } else {
                    render(player)
                }
            )

            remaining = remaining.substring(
                foundIndex + token.first.length
            )
        }

        return output
    }

    fun onConfigReload(server: MinecraftServer) {
        for (player in server.playerManager.playerList) {
            val state = stateFor(player)

            if (cleanActive(player, state)) {
                save(player.uuid)
            }
        }
    }

    fun saveAll() {
        cache.keys.forEach(::save)
    }

    private fun stateFor(player: ServerPlayerEntity): PrefixState {
        val state = cache.computeIfAbsent(player.uuid) {
            loadFromDisk(player.uuid)
        }

        var changed = false

        if (!state.initializedDefaults) {
            initializeDefaults(player, state)
            state.initializedDefaults = true
            changed = true
        }

        if (cleanActive(player, state)) {
            changed = true
        }

        if (changed) {
            save(player.uuid)
        }

        return state
    }

    private fun initializeDefaults(
        player: ServerPlayerEntity,
        state: PrefixState
    ) {
        val defaults = getAllDefinitions()
            .filter { it.enabled }
            .filter { it.defaultOwned }
            .filter { it.autoEquip }

        for (definition in defaults) {
            equipInternal(
                player = player,
                definition = definition,
                state = state,
                force = true
            )
        }
    }

    private fun equipInternal(
        player: ServerPlayerEntity,
        definition: PrefixDefinition,
        state: PrefixState,
        force: Boolean
    ): PrefixActionResult {
        if (!PrefixConfigManager.prefixConfig.enabled) {
            return PrefixActionResult.SYSTEM_DISABLED
        }

        if (!definition.enabled) {
            return PrefixActionResult.DISABLED
        }

        if (!force && !definition.selectable) {
            return PrefixActionResult.NOT_SELECTABLE
        }

        if (!hasAccess(player, definition, state)) {
            return PrefixActionResult.NO_ACCESS
        }

        val normalized = normalizeId(definition.id)

        if (state.active.contains(normalized)) {
            return PrefixActionResult.ALREADY_ACTIVE
        }

        val conflicting = if (definition.exclusiveGroup.isBlank()) {
            emptySet()
        } else {
            state.active.filterTo(mutableSetOf()) { activeId ->
                val activeDefinition = getDefinition(activeId)

                activeDefinition != null &&
                        activeDefinition.exclusiveGroup.isNotBlank() &&
                        activeDefinition.exclusiveGroup.equals(
                            definition.exclusiveGroup,
                            ignoreCase = true
                        )
            }
        }

        val activeAfterConflicts = state.active.size - conflicting.size
        val maxActive = PrefixConfigManager.prefixConfig.maxActivePrefixes

        val bypassLimit =
            force ||
                    perms.hasPermission(
                        player.commandSource,
                        "shadowchat.prefix.bypass.limit"
                    )

        if (
            !bypassLimit &&
            maxActive > 0 &&
            activeAfterConflicts >= maxActive
        ) {
            return PrefixActionResult.LIMIT_REACHED
        }

        state.active.removeAll(conflicting)
        state.active.add(normalized)

        return PrefixActionResult.SUCCESS
    }

    private fun cleanActive(
        player: ServerPlayerEntity,
        state: PrefixState
    ): Boolean {
        if (!PrefixConfigManager.prefixConfig.enabled) {
            return false
        }

        var changed = false

        val invalid = state.active.filter { id ->
            val definition = getDefinition(id)

            definition == null ||
                    !definition.enabled ||
                    !hasAccess(player, definition, state)
        }

        if (invalid.isNotEmpty()) {
            state.active.removeAll(invalid.toSet())
            changed = true
        }

        val grouped = state.active
            .mapNotNull { id ->
                getDefinition(id)?.let { definition -> id to definition }
            }
            .filter { (_, definition) ->
                definition.exclusiveGroup.isNotBlank()
            }
            .groupBy { (_, definition) ->
                definition.exclusiveGroup.lowercase()
            }

        for ((_, entries) in grouped) {
            if (entries.size <= 1) continue

            val keep = entries
                .maxWithOrNull(
                    compareBy<Pair<String, PrefixDefinition>> { it.second.priority }
                        .thenByDescending { it.first }
                )
                ?.first

            for ((id, _) in entries) {
                if (id != keep && state.active.remove(id)) {
                    changed = true
                }
            }
        }

        val maxActive = PrefixConfigManager.prefixConfig.maxActivePrefixes
        val bypassLimit = perms.hasPermission(
            player.commandSource,
            "shadowchat.prefix.bypass.limit"
        )

        if (!bypassLimit && maxActive > 0 && state.active.size > maxActive) {
            val keep = getActiveDefinitionsFromState(state)
                .take(maxActive)
                .map { normalizeId(it.id) }
                .toSet()

            if (state.active.retainAll(keep)) {
                changed = true
            }
        }

        return changed
    }

    private fun getActiveDefinitionsFromState(
        state: PrefixState
    ): List<PrefixDefinition> {
        return state.active
            .mapNotNull(::getDefinition)
            .filter { it.enabled }
            .sortedWith(
                compareByDescending<PrefixDefinition> { it.priority }
                    .thenBy { it.id.lowercase() }
            )
    }

    private fun hasAccess(
        player: ServerPlayerEntity,
        definition: PrefixDefinition,
        state: PrefixState
    ): Boolean {
        if (definition.defaultOwned) {
            return true
        }

        if (state.owned.contains(normalizeId(definition.id))) {
            return true
        }

        if (definition.permission.isNotBlank()) {
            return perms.hasPermission(
                player.commandSource,
                definition.permission
            )
        }

        return false
    }

    private fun refreshPlayer(player: ServerPlayerEntity) {
        WeightManager.applyTeam(player)
        TabListManager.pushDisplayNameUpdate(player)
    }

    private fun parse(
        raw: String,
        player: ServerPlayerEntity
    ): Text {
        var value = raw
            .replace("%player:name%", player.name.string)
            .replace("%name%", player.name.string)
            .replace("%nickname%", NicknameManager.getDisplayName(player))

        value = PlaceholderBridge.apply(player, value)

        return Placeholders.parseText(
            CustomColorParser.processAllFormats(value),
            PlaceholderContext.of(player)
        )
    }

    private fun save(uuid: UUID) {
        val state = cache[uuid] ?: return

        val output = PrefixFile(
            owned = state.owned.sorted(),
            active = state.active.sorted(),
            initializedDefaults = state.initializedDefaults
        )

        val file = fileFor(uuid)

        try {
            file.parent?.createDirectories()
            Files.writeString(file, gson.toJson(output))
        } catch (_: Throwable) {
        }
    }

    private fun loadFromDisk(uuid: UUID): PrefixState {
        val file = fileFor(uuid)

        try {
            file.parent?.createDirectories()

            if (!file.exists()) {
                return PrefixState()
            }

            val json = Files.readString(file)
            val parsed = gson.fromJson(json, PrefixFile::class.java)
                ?: PrefixFile()

            val state = PrefixState(
                initializedDefaults = parsed.initializedDefaults
            )

            parsed.owned
                .map(::normalizeId)
                .filter { it.isNotBlank() }
                .forEach(state.owned::add)

            parsed.active
                .map(::normalizeId)
                .filter { it.isNotBlank() }
                .forEach(state.active::add)

            return state
        } catch (_: JsonSyntaxException) {
            return PrefixState()
        } catch (_: Throwable) {
            return PrefixState()
        }
    }
}

enum class PrefixActionResult {
    SUCCESS,
    SYSTEM_DISABLED,
    NOT_FOUND,
    DISABLED,
    NO_ACCESS,
    NOT_SELECTABLE,
    ALREADY_ACTIVE,
    NOT_ACTIVE,
    LIMIT_REACHED
}