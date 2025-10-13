package org.krripe.shadowchat.compat

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.MinecraftServer
import java.lang.reflect.Method

object TriviaCompat {
    private val hasTrivia = FabricLoader.getInstance().isModLoaded("trivia")

    private var quizManager: Any? = null
    private var quizInProgress: Method? = null
    private var isRightAnswer: Method? = null
    private var processWinner: Method? = null

    private fun initIfNeeded() {
        if (!hasTrivia || quizManager != null) return
        try {
            val triviaCls     = Class.forName("dev.roanoke.trivia.Trivia")
            val getInstance   = triviaCls.getMethod("getInstance")
            val triviaInst    = getInstance.invoke(null)

            quizManager       = triviaCls.getField("quiz").get(triviaInst)
            val qmCls         = quizManager!!.javaClass

            quizInProgress    = qmCls.getMethod("quizInProgress")
            isRightAnswer     = qmCls.getMethod("isRightAnswer", String::class.java)
            processWinner     = qmCls.getMethod(
                "processQuizWinner",
                ServerPlayerEntity::class.java,
                MinecraftServer::class.java
            )
        } catch (_: Throwable) {
            quizManager = null
        }
    }

    fun tryAnswerTrivia(player: ServerPlayerEntity, raw: String) {
        if (!hasTrivia) return
        initIfNeeded()
        val qm = quizManager ?: return

        try {
            if (!(quizInProgress!!.invoke(qm) as Boolean)) return
            if (!(isRightAnswer!!.invoke(qm, raw.trim()) as Boolean)) return
            processWinner!!.invoke(qm, player, player.server)
        } catch (_: Throwable) {  }
    }
}