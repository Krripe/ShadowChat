package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.utils.CustomColorParser
import net.minecraft.text.Text
import org.krripe.shadowchat.data.NicknameStorage

object NicknameManager {
    private val nicknames = mutableMapOf<String, String>()

    fun getNickname(player: ServerPlayerEntity): Text {
        val raw = nicknames[player.uuidAsString] ?: player.name.string
        return CustomColorParser.processAllFormats(raw)
    }

    fun setNickname(player: ServerPlayerEntity, nickname: String) {
        nicknames[player.uuidAsString] = nickname
        NicknameStorage.save(nicknames)
        WeightManager.applyTeam(player)
        TabListManager.pushDisplayNameUpdate(player)
    }

    fun resetNickname(player: ServerPlayerEntity) {
        nicknames.remove(player.uuidAsString)
        NicknameStorage.save(nicknames)
        TabListManager.pushDisplayNameUpdate(player)
    }

    fun getRaw(player: ServerPlayerEntity): String? = nicknames[player.uuidAsString]

    fun load() {
        nicknames.putAll(NicknameStorage.load())
    }

    fun getDisplayName(player: ServerPlayerEntity): String =
        getRaw(player) ?: player.name.string
}
