package org.krripe.shadowchat.config

data class BadWordsConfig(
    val enabled: Boolean = true,
    val censorWord: String = "*****",
    val badWords: List<String> = listOf("noob", "suka", "nigga", "sex", "nigger", "retard"),
    val warnings: WarningConfig = WarningConfig(),
    val messages: BadWordMessages = BadWordMessages()
)

data class WarningConfig(
    val offensesPerWarning: Int = 2,
    val maxWarnings: Int = 3,
    val punishmentCommand: String = "kick %player% You have used offensive language too many times!"
)

data class BadWordMessages(
    val warningMessage: String = "<#ffe033>⚠</#> <#ff7253>Watch your language, %player%! You received a warning.</#>",
    val punishmentBroadcast: String = "<#ffe033>⚠</#> <#fffacf>%player%</#> <#ff7253>has been punished for offensive language.</#>"
)

