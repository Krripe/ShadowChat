package org.krripe.shadowchat.config

data class AnnouncementsConfig(
    val announcements: MutableList<Announcement> = mutableListOf(
        Announcement(
            id = "welcome",
            enabled = true,
            intervalSeconds = 300,
            lines = listOf(
                "<#55ffff>[ShadowChat]</#> <#aaaaaa>Welcome to the server!</#>",
                "<#aaaaaa>Use </#><#ffe856>/rules</#><#aaaaaa> and </#><#ffe856>/discord</#><#aaaaaa>.</#>"
            )
        ),
        Announcement(
            id = "tips",
            enabled = true,
            intervalSeconds = 600,
            lines = listOf(
                "<#55ffff>Advice:</#> <#aaaaaa>You can open the menu with </#><#ffe856>M</#><#aaaaaa>.</#>"
            )
        )
    )
)

data class Announcement(
    val id: String,
    val enabled: Boolean = true,
    val intervalSeconds: Int = 300,
    val lines: List<String> = emptyList()
)
