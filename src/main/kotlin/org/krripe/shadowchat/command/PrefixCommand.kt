package org.krripe.shadowchat.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.config.PrefixConfigManager
import org.krripe.shadowchat.config.PrefixDefinition
import org.krripe.shadowchat.manager.PrefixActionResult
import org.krripe.shadowchat.manager.PrefixManager
import org.krripe.shadowchat.manager.TabListManager
import org.krripe.shadowchat.manager.WeightManager
import org.krripe.shadowchat.utils.PermissionsSystem

object PrefixCommand {

    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            val aliases = PrefixConfigManager.prefixConfig.aliases
                .map {
                    it.trim()
                        .removePrefix("/")
                        .lowercase()
                }
                .filter {
                    it.isNotBlank() && !it.contains(" ")
                }
                .distinct()
                .ifEmpty {
                    listOf("prefix")
                }

            for (alias in aliases) {
                registerCommands(dispatcher, alias)
            }
        }
    }

    private fun registerCommands(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        alias: String
    ) {

        // /prefix
        dispatcher.register(
            literal(alias)
                .executes { ctx ->
                    if (!perms.hasPermission(
                            ctx.source,
                            "shadowchat.prefix.list"
                        )
                    ) {
                        ctx.source.sendFeedback(
                            {
                                MessageManager.get(
                                    ctx.source,
                                    "noPermission"
                                )
                            },
                            false
                        )
                        return@executes 0
                    }

                    val player = ctx.source.player

                    if (player == null) {
                        ctx.source.sendFeedback(
                            {
                                MessageManager.get(
                                    ctx.source,
                                    "onlyPlayers"
                                )
                            },
                            false
                        )
                        return@executes 0
                    }

                    showAvailable(player)
                }
        )

        // /prefix list
        dispatcher.register(
            literal(alias)
                .then(
                    literal("list")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.list"
                            )
                        }
                        .executes { ctx ->
                            val player = ctx.source.player

                            if (player == null) {
                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "onlyPlayers"
                                        )
                                    },
                                    false
                                )
                                return@executes 0
                            }

                            showAvailable(player)
                        }
                )
        )

        // /prefix active
        dispatcher.register(
            literal(alias)
                .then(
                    literal("active")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.active"
                            )
                        }
                        .executes { ctx ->
                            val player = ctx.source.player

                            if (player == null) {
                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "onlyPlayers"
                                        )
                                    },
                                    false
                                )
                                return@executes 0
                            }

                            showActive(player)
                        }
                )
        )

        // /prefix toggle <id> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("toggle")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.toggle"
                            )
                        }
                        .then(
                            argument(
                                "id",
                                StringArgumentType.word()
                            )
                                .suggests { ctx, builder ->
                                    val player = ctx.source.player

                                    val ids = if (player != null) {
                                        PrefixManager
                                            .getAvailable(player)
                                            .map { it.id }
                                    } else {
                                        emptyList()
                                    }

                                    CommandSource.suggestMatching(
                                        ids,
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    val player = ctx.source.player

                                    if (player == null) {
                                        ctx.source.sendFeedback(
                                            {
                                                MessageManager.get(
                                                    ctx.source,
                                                    "onlyPlayers"
                                                )
                                            },
                                            false
                                        )
                                        return@executes 0
                                    }

                                    toggle(
                                        player,
                                        StringArgumentType.getString(
                                            ctx,
                                            "id"
                                        ),
                                        false
                                    )
                                }
                                .then(
                                    literal("silent")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                                ?: return@executes 0

                                            toggle(
                                                player,
                                                StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                true
                                            )
                                        }
                                )
                        )
                )
        )

        // /prefix equip <id> [silent]
        // /prefix equip <player> <id> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("equip")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.equip"
                            ) ||
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.equip.other"
                                    )
                        }

                        // Self
                        .then(
                            argument(
                                "id",
                                StringArgumentType.word()
                            )
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.equip"
                                    )
                                }
                                .suggests { ctx, builder ->
                                    val player = ctx.source.player

                                    val ids = if (player != null) {
                                        PrefixManager
                                            .getAvailable(player)
                                            .map { it.id }
                                    } else {
                                        emptyList()
                                    }

                                    CommandSource.suggestMatching(
                                        ids,
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    val player = ctx.source.player

                                    if (player == null) {
                                        ctx.source.sendFeedback(
                                            {
                                                MessageManager.get(
                                                    ctx.source,
                                                    "onlyPlayers"
                                                )
                                            },
                                            false
                                        )
                                        return@executes 0
                                    }

                                    equipSelf(
                                        player,
                                        StringArgumentType.getString(
                                            ctx,
                                            "id"
                                        ),
                                        false
                                    )
                                }
                                .then(
                                    literal("silent")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                                ?: return@executes 0

                                            equipSelf(
                                                player,
                                                StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                true
                                            )
                                        }
                                )
                        )

                        // Other
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.equip.other"
                                    )
                                }
                                .then(
                                    argument(
                                        "id",
                                        StringArgumentType.word()
                                    )
                                        .suggests { _, builder ->
                                            CommandSource.suggestMatching(
                                                PrefixManager
                                                    .getAllDefinitions()
                                                    .map { it.id },
                                                builder
                                            )
                                        }
                                        .executes { ctx ->
                                            equipOther(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                id = StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                silent = false
                                            )
                                        }
                                        .then(
                                            literal("silent")
                                                .executes { ctx ->
                                                    equipOther(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        silent = true
                                                    )
                                                }
                                        )
                                )
                        )
                )
        )

        // /prefix unequip <id> [silent]
        // /prefix unequip <player> <id> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("unequip")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.unequip"
                            ) ||
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.unequip.other"
                                    )
                        }

                        // Self
                        .then(
                            argument(
                                "id",
                                StringArgumentType.word()
                            )
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.unequip"
                                    )
                                }
                                .suggests { ctx, builder ->
                                    val player = ctx.source.player

                                    val ids = if (player != null) {
                                        PrefixManager
                                            .getActive(player)
                                            .map { it.id }
                                    } else {
                                        emptyList()
                                    }

                                    CommandSource.suggestMatching(
                                        ids,
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    val player = ctx.source.player

                                    if (player == null) {
                                        ctx.source.sendFeedback(
                                            {
                                                MessageManager.get(
                                                    ctx.source,
                                                    "onlyPlayers"
                                                )
                                            },
                                            false
                                        )
                                        return@executes 0
                                    }

                                    unequipSelf(
                                        player,
                                        StringArgumentType.getString(
                                            ctx,
                                            "id"
                                        ),
                                        false
                                    )
                                }
                                .then(
                                    literal("silent")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                                ?: return@executes 0

                                            unequipSelf(
                                                player,
                                                StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                true
                                            )
                                        }
                                )
                        )

                        // Other
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.unequip.other"
                                    )
                                }
                                .then(
                                    argument(
                                        "id",
                                        StringArgumentType.word()
                                    )
                                        .suggests { ctx, builder ->
                                            val target = runCatching {
                                                EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                )
                                            }.getOrNull()

                                            val ids = target
                                                ?.let {
                                                    PrefixManager.getActive(it)
                                                }
                                                ?.map { it.id }
                                                ?: PrefixManager
                                                    .getAllDefinitions()
                                                    .map { it.id }

                                            CommandSource.suggestMatching(
                                                ids,
                                                builder
                                            )
                                        }
                                        .executes { ctx ->
                                            unequipOther(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                id = StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                silent = false
                                            )
                                        }
                                        .then(
                                            literal("silent")
                                                .executes { ctx ->
                                                    unequipOther(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        silent = true
                                                    )
                                                }
                                        )
                                )
                        )
                )
        )

        // /prefix clear [silent]
        // /prefix clear <player> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("clear")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.clear"
                            ) ||
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.clear.other"
                                    )
                        }
                        .executes { ctx ->
                            if (!perms.hasPermission(
                                    ctx.source,
                                    "shadowchat.prefix.clear"
                                )
                            ) {
                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "noPermission"
                                        )
                                    },
                                    false
                                )
                                return@executes 0
                            }

                            val player = ctx.source.player

                            if (player == null) {
                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "onlyPlayers"
                                        )
                                    },
                                    false
                                )
                                return@executes 0
                            }

                            clearSelf(
                                player,
                                false
                            )
                        }
                        .then(
                            literal("silent")
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.clear"
                                    )
                                }
                                .executes { ctx ->
                                    val player = ctx.source.player
                                        ?: return@executes 0

                                    clearSelf(
                                        player,
                                        true
                                    )
                                }
                        )
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .requires {
                                    perms.hasPermission(
                                        it,
                                        "shadowchat.prefix.clear.other"
                                    )
                                }
                                .executes { ctx ->
                                    clearOther(
                                        source = ctx.source,
                                        target = EntityArgumentType.getPlayer(
                                            ctx,
                                            "player"
                                        ),
                                        silent = false
                                    )
                                }
                                .then(
                                    literal("silent")
                                        .executes { ctx ->
                                            clearOther(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                silent = true
                                            )
                                        }
                                )
                        )
                )
        )

        // /prefix grant <player> <id> [equip] [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("grant")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.grant"
                            )
                        }
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .then(
                                    argument(
                                        "id",
                                        StringArgumentType.word()
                                    )
                                        .suggests { _, builder ->
                                            CommandSource.suggestMatching(
                                                PrefixManager
                                                    .getAllDefinitions()
                                                    .map { it.id },
                                                builder
                                            )
                                        }
                                        .executes { ctx ->
                                            grant(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                id = StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                equip = false,
                                                silent = false
                                            )
                                        }
                                        .then(
                                            literal("silent")
                                                .executes { ctx ->
                                                    grant(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        equip = false,
                                                        silent = true
                                                    )
                                                }
                                        )
                                        .then(
                                            literal("equip")
                                                .executes { ctx ->
                                                    grant(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        equip = true,
                                                        silent = false
                                                    )
                                                }
                                                .then(
                                                    literal("silent")
                                                        .executes { ctx ->
                                                            grant(
                                                                source = ctx.source,
                                                                target = EntityArgumentType.getPlayer(
                                                                    ctx,
                                                                    "player"
                                                                ),
                                                                id = StringArgumentType.getString(
                                                                    ctx,
                                                                    "id"
                                                                ),
                                                                equip = true,
                                                                silent = true
                                                            )
                                                        }
                                                )
                                        )
                                )
                        )
                )
        )

        // /prefix revoke <player> <id> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("revoke")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.revoke"
                            )
                        }
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .then(
                                    argument(
                                        "id",
                                        StringArgumentType.word()
                                    )
                                        .suggests { ctx, builder ->
                                            val target = runCatching {
                                                EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                )
                                            }.getOrNull()

                                            val ids = target
                                                ?.let {
                                                    PrefixManager.getOwned(it)
                                                }
                                                ?.toList()
                                                ?: PrefixManager
                                                    .getAllDefinitions()
                                                    .map { it.id }

                                            CommandSource.suggestMatching(
                                                ids,
                                                builder
                                            )
                                        }
                                        .executes { ctx ->
                                            revoke(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                id = StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                silent = false
                                            )
                                        }
                                        .then(
                                            literal("silent")
                                                .executes { ctx ->
                                                    revoke(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        silent = true
                                                    )
                                                }
                                        )
                                )
                        )
                )
        )

        // /prefix set <player> <id> [silent]
        dispatcher.register(
            literal(alias)
                .then(
                    literal("set")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.set"
                            )
                        }
                        .then(
                            argument(
                                "player",
                                EntityArgumentType.player()
                            )
                                .then(
                                    argument(
                                        "id",
                                        StringArgumentType.word()
                                    )
                                        .suggests { _, builder ->
                                            CommandSource.suggestMatching(
                                                PrefixManager
                                                    .getAllDefinitions()
                                                    .map { it.id },
                                                builder
                                            )
                                        }
                                        .executes { ctx ->
                                            setOther(
                                                source = ctx.source,
                                                target = EntityArgumentType.getPlayer(
                                                    ctx,
                                                    "player"
                                                ),
                                                id = StringArgumentType.getString(
                                                    ctx,
                                                    "id"
                                                ),
                                                silent = false
                                            )
                                        }
                                        .then(
                                            literal("silent")
                                                .executes { ctx ->
                                                    setOther(
                                                        source = ctx.source,
                                                        target = EntityArgumentType.getPlayer(
                                                            ctx,
                                                            "player"
                                                        ),
                                                        id = StringArgumentType.getString(
                                                            ctx,
                                                            "id"
                                                        ),
                                                        silent = true
                                                    )
                                                }
                                        )
                                )
                        )
                )
        )

        // /prefix info <id>
        dispatcher.register(
            literal(alias)
                .then(
                    literal("info")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.info"
                            )
                        }
                        .then(
                            argument(
                                "id",
                                StringArgumentType.word()
                            )
                                .suggests { _, builder ->
                                    CommandSource.suggestMatching(
                                        PrefixManager
                                            .getAllDefinitions()
                                            .map { it.id },
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    showInfo(
                                        ctx.source,
                                        StringArgumentType.getString(
                                            ctx,
                                            "id"
                                        )
                                    )
                                }
                        )
                )
        )

        // /prefix reload
        dispatcher.register(
            literal(alias)
                .then(
                    literal("reload")
                        .requires {
                            perms.hasPermission(
                                it,
                                "shadowchat.prefix.reload"
                            )
                        }
                        .executes { ctx ->
                            PrefixConfigManager.load()

                            if (!PrefixConfigManager.lastLoadOk) {
                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "prefixReloadFailed"
                                        )
                                    },
                                    false
                                )
                                return@executes 0
                            }

                            PrefixManager.onConfigReload(
                                ctx.source.server
                            )

                            WeightManager.refreshAll(
                                ctx.source.server
                            )

                            TabListManager.refreshAll(
                                ctx.source.server
                            )

                            ctx.source.sendFeedback(
                                {
                                    MessageManager.get(
                                        ctx.source,
                                        "prefixReloaded"
                                    )
                                },
                                false
                            )

                            Command.SINGLE_SUCCESS
                        }
                )
        )
    }

    private fun showAvailable(
        player: ServerPlayerEntity
    ): Int {
        if (!PrefixConfigManager.prefixConfig.enabled) {
            player.sendMessage(
                MessageManager.get(
                    player,
                    "prefixSystemDisabled"
                ),
                false
            )
            return 0
        }

        val available = PrefixManager.getAvailable(player)

        if (available.isEmpty()) {
            player.sendMessage(
                MessageManager.get(
                    player,
                    "prefixListEmpty"
                ),
                false
            )
            return Command.SINGLE_SUCCESS
        }

        val list = buildDefinitionList(available)

        player.sendMessage(
            MessageManager.get(
                player,
                "prefixListHeader",
                "count" to available.size.toString(),
                "list" to list
            ),
            false
        )

        return Command.SINGLE_SUCCESS
    }

    private fun showActive(
        player: ServerPlayerEntity
    ): Int {
        val active = PrefixManager.getActive(player)

        if (active.isEmpty()) {
            player.sendMessage(
                MessageManager.get(
                    player,
                    "prefixActiveEmpty"
                ),
                false
            )
            return Command.SINGLE_SUCCESS
        }

        player.sendMessage(
            MessageManager.get(
                player,
                "prefixActiveHeader",
                "count" to active.size.toString(),
                "list" to buildDefinitionList(active)
            ),
            false
        )

        return Command.SINGLE_SUCCESS
    }

    private fun buildDefinitionList(
        definitions: List<PrefixDefinition>
    ): String {
        val separator =
            MessageManager.getRaw("prefixListSeparator")

        return definitions.joinToString(separator) { definition ->
            MessageManager.getRaw(
                "prefixListEntry",
                "display" to definition.display,
                "id" to definition.id
            )
        }
    }

    private fun toggle(
        player: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val wasActive =
            PrefixManager.isActive(player, id)

        val result =
            PrefixManager.toggle(player, id)

        if (result == PrefixActionResult.SUCCESS) {
            if (!silent) {
                player.sendMessage(
                    MessageManager.get(
                        player,
                        if (wasActive) {
                            "prefixUnequipped"
                        } else {
                            "prefixEquipped"
                        },
                        "id" to id
                    ),
                    false
                )
            }

            return Command.SINGLE_SUCCESS
        }

        return sendPlayerError(
            player,
            result,
            id,
            silent
        )
    }

    private fun equipSelf(
        player: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val result =
            PrefixManager.equip(player, id)

        if (result == PrefixActionResult.SUCCESS) {
            if (!silent) {
                player.sendMessage(
                    MessageManager.get(
                        player,
                        "prefixEquipped",
                        "id" to id
                    ),
                    false
                )
            }

            return Command.SINGLE_SUCCESS
        }

        return sendPlayerError(
            player,
            result,
            id,
            silent
        )
    }

    private fun unequipSelf(
        player: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val result =
            PrefixManager.unequip(player, id)

        if (result == PrefixActionResult.SUCCESS) {
            if (!silent) {
                player.sendMessage(
                    MessageManager.get(
                        player,
                        "prefixUnequipped",
                        "id" to id
                    ),
                    false
                )
            }

            return Command.SINGLE_SUCCESS
        }

        return sendPlayerError(
            player,
            result,
            id,
            silent
        )
    }

    private fun clearSelf(
        player: ServerPlayerEntity,
        silent: Boolean
    ): Int {
        val changed =
            PrefixManager.clearActive(player)

        if (!silent) {
            player.sendMessage(
                MessageManager.get(
                    player,
                    if (changed) {
                        "prefixCleared"
                    } else {
                        "prefixNothingToClear"
                    }
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun grant(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        id: String,
        equip: Boolean,
        silent: Boolean
    ): Int {
        val definition =
            PrefixManager.getDefinition(id)

        if (definition == null) {
            return sendSourceError(
                source,
                PrefixActionResult.NOT_FOUND,
                id,
                silent
            )
        }

        val alreadyOwned =
            PrefixManager.getOwned(target).any {
                it.equals(
                    definition.id,
                    ignoreCase = true
                )
            }

        val granted = if (alreadyOwned) {
            false
        } else {
            PrefixManager.grant(
                target,
                definition.id
            )
        }

        var equipResult: PrefixActionResult? = null

        if (equip) {
            equipResult = PrefixManager.equip(
                player = target,
                id = definition.id,
                force = true
            )
        }

        if (
            equipResult != null &&
            equipResult != PrefixActionResult.SUCCESS &&
            equipResult != PrefixActionResult.ALREADY_ACTIVE
        ) {
            return sendSourceError(
                source,
                equipResult,
                definition.id,
                silent
            )
        }

        if (!silent) {
            if (!granted && !equip) {
                source.sendFeedback(
                    {
                        MessageManager.get(
                            source,
                            "prefixAlreadyOwned",
                            "target" to target.name.string,
                            "id" to definition.id
                        )
                    },
                    false
                )
                return 0
            }

            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixGranted",
                        "target" to target.name.string,
                        "id" to definition.id
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixGrantedTarget",
                    "id" to definition.id
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun revoke(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val definition =
            PrefixManager.getDefinition(id)

        if (definition == null) {
            return sendSourceError(
                source,
                PrefixActionResult.NOT_FOUND,
                id,
                silent
            )
        }

        val removed =
            PrefixManager.revoke(
                target,
                definition.id
            )

        if (!removed) {
            if (!silent) {
                source.sendFeedback(
                    {
                        MessageManager.get(
                            source,
                            "prefixNotOwned",
                            "target" to target.name.string,
                            "id" to definition.id
                        )
                    },
                    false
                )
            }

            return 0
        }

        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixRevoked",
                        "target" to target.name.string,
                        "id" to definition.id
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixRevokedTarget",
                    "id" to definition.id
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun equipOther(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val result = PrefixManager.equip(
            player = target,
            id = id,
            force = true
        )

        if (result != PrefixActionResult.SUCCESS) {
            return sendSourceError(
                source,
                result,
                id,
                silent
            )
        }

        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixEquippedOther",
                        "target" to target.name.string,
                        "id" to id
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixEquippedByStaff",
                    "id" to id
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun unequipOther(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val result =
            PrefixManager.unequip(target, id)

        if (result != PrefixActionResult.SUCCESS) {
            return sendSourceError(
                source,
                result,
                id,
                silent
            )
        }

        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixUnequippedOther",
                        "target" to target.name.string,
                        "id" to id
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixUnequippedByStaff",
                    "id" to id
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun setOther(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        id: String,
        silent: Boolean
    ): Int {
        val result = PrefixManager.setActive(
            player = target,
            id = id,
            force = true
        )

        if (result != PrefixActionResult.SUCCESS) {
            return sendSourceError(
                source,
                result,
                id,
                silent
            )
        }

        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixSetOther",
                        "target" to target.name.string,
                        "id" to id
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixSetByStaff",
                    "id" to id
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun clearOther(
        source: ServerCommandSource,
        target: ServerPlayerEntity,
        silent: Boolean
    ): Int {
        PrefixManager.clearActive(target)

        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixClearedOther",
                        "target" to target.name.string
                    )
                },
                false
            )

            target.sendMessage(
                MessageManager.get(
                    target,
                    "prefixClearedByStaff"
                ),
                false
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun showInfo(
        source: ServerCommandSource,
        id: String
    ): Int {
        val definition =
            PrefixManager.getDefinition(id)

        if (definition == null) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        "prefixNotFound",
                        "id" to id
                    )
                },
                false
            )
            return 0
        }

        source.sendFeedback(
            {
                MessageManager.get(
                    source,
                    "prefixInfo",
                    "id" to definition.id,
                    "description" to definition.description.ifBlank {
                        "-"
                    },
                    "priority" to definition.priority.toString(),
                    "permission" to definition.permission.ifBlank {
                        "-"
                    }
                )
            },
            false
        )

        return Command.SINGLE_SUCCESS
    }

    private fun sendPlayerError(
        player: ServerPlayerEntity,
        result: PrefixActionResult,
        id: String,
        silent: Boolean
    ): Int {
        if (!silent) {
            player.sendMessage(
                MessageManager.get(
                    player,
                    resultMessageKey(result),
                    "id" to id
                ),
                false
            )
        }

        return 0
    }

    private fun sendSourceError(
        source: ServerCommandSource,
        result: PrefixActionResult,
        id: String,
        silent: Boolean
    ): Int {
        if (!silent) {
            source.sendFeedback(
                {
                    MessageManager.get(
                        source,
                        resultMessageKey(result),
                        "id" to id
                    )
                },
                false
            )
        }

        return 0
    }

    private fun resultMessageKey(
        result: PrefixActionResult
    ): String {
        return when (result) {
            PrefixActionResult.SYSTEM_DISABLED ->
                "prefixSystemDisabled"

            PrefixActionResult.NOT_FOUND ->
                "prefixNotFound"

            PrefixActionResult.DISABLED ->
                "prefixDisabled"

            PrefixActionResult.NO_ACCESS ->
                "prefixNoAccess"

            PrefixActionResult.NOT_SELECTABLE ->
                "prefixNotSelectable"

            PrefixActionResult.ALREADY_ACTIVE ->
                "prefixAlreadyActive"

            PrefixActionResult.NOT_ACTIVE ->
                "prefixNotActive"

            PrefixActionResult.LIMIT_REACHED ->
                "prefixLimitReached"

            PrefixActionResult.SUCCESS ->
                "prefixEquipped"
        }
    }
}