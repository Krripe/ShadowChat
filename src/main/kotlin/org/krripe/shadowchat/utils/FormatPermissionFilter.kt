package org.krripe.shadowchat.utils

import net.minecraft.server.command.ServerCommandSource

object FormatPermissionFilter {
    private val perms = PermissionsSystem()

    private val AMP_CODES   = Regex("&[0-9a-fk-orA-FK-OR]")
    private val HEX_TAG     = Regex("<#[0-9A-Fa-f]{6}>")
    private val COLOR_TAGS  = Regex("</?(b|i|u|strike|obf|gradient:[^>]+|click:[^>]+|hover:[^>]+|link:[^>]+)>")

    fun filter(text: String, src: ServerCommandSource): String {
        if (perms.hasPermission(src, "shadowchat.color")) {
            return text
        }
        return text
            .replace(AMP_CODES, "")
            .replace(HEX_TAG, "")
            .replace(COLOR_TAGS, "")
    }
}
