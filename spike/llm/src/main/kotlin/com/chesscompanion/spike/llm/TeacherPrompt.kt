package com.chesscompanion.spike.llm

/** One pedagogical fact: the engine's JSON and the same facts already put into French words. */
data class TeacherFact(
    val json: String,
    val french: String,
)

/** [JSON]: the raw fact, as in the sprint prompt. [FRENCH]: facts pre-translated, plus one worked example. */
enum class PromptVariant { JSON, FRENCH }

/**
 * S0-6 test prompt: turn one pedagogical fact (computed by the engine) into one teacher sentence
 * in French for a ~1300 player. The model only rewords: it receives no board and must not add moves.
 */
object TeacherPrompt {
    /** The main fact is the one named in the sprint (`docs/prompts/prompts-sprint-0.md`). */
    val FACTS =
        listOf(
            TeacherFact(
                """{"motif":"hanging_piece","piece":"knight","square":"f3","best":"Nd2"}""",
                "Motif : pièce en prise, non défendue. Pièce : ton cavalier en f3. Meilleur coup : Cd2.",
            ),
            TeacherFact(
                """{"motif":"fork","piece":"knight","square":"c7","targets":["king e8","rook a8"],"best":"Nc7+"}""",
                "Motif : fourchette. Pièce : ton cavalier, qui va en c7. Cibles : le roi en e8 et la tour en a8. " +
                    "Meilleur coup : Cc7+.",
            ),
            TeacherFact(
                """{"motif":"pin","piece":"bishop","square":"g5",""" +
                    """"pinned":"knight f6","behind":"queen d8","best":"Bxf6"}""",
                "Motif : clouage. Pièce : ton fou en g5. Pièce clouée : le cavalier en f6, devant la dame en d8. " +
                    "Meilleur coup : Fxf6.",
            ),
            TeacherFact(
                """{"motif":"back_rank_mate","piece":"rook","square":"e8","best":"Re8#"}""",
                "Motif : mat du couloir. Pièce : ta tour, qui va en e8. Meilleur coup : Te8#.",
            ),
            TeacherFact(
                """{"motif":"free_capture","piece":"bishop","square":"d4","best":"Nxd4"}""",
                "Motif : pièce à prendre gratuitement. Pièce : le fou adverse en d4. Meilleur coup : Cxd4.",
            ),
        )

    val MAIN_FACT: TeacherFact = FACTS.first()

    const val SYSTEM_JSON =
        "Tu es un professeur d'échecs bienveillant. Ton élève a environ 1300 Elo. " +
            "On te donne un fait pédagogique au format JSON, calculé par un moteur d'échecs. " +
            "Reformule-le en une seule phrase en français, claire et encourageante, en tutoyant l'élève. " +
            "N'invente aucun coup, aucune case et aucune pièce : utilise seulement les informations du JSON. " +
            "Écris les coups en notation française (R roi, D dame, T tour, F fou, C cavalier). " +
            "Lexique : hanging_piece = pièce en prise non défendue ; fork = fourchette ; pin = clouage ; " +
            "back_rank_mate = mat du couloir ; free_capture = pièce à prendre gratuitement ; " +
            "best = meilleur coup."

    const val SYSTEM_FRENCH =
        "Tu es un professeur d'échecs bienveillant. Ton élève a environ 1300 Elo. " +
            "On te donne des faits calculés par un moteur d'échecs. " +
            "Écris une seule phrase en français, claire et encourageante, en tutoyant l'élève. " +
            "N'ajoute aucun coup, aucune case et aucune pièce : utilise seulement les faits donnés. " +
            "Exemple. Faits : Motif : échec à la découverte. Pièce : ton fou en c4. Meilleur coup : Fxf7+. " +
            "Phrase : Ton fou en c4 peut prendre en f7 avec échec : joue Fxf7+ !"

    fun messages(
        fact: TeacherFact,
        variant: PromptVariant = PromptVariant.JSON,
    ): List<ChatMessage> =
        when (variant) {
            PromptVariant.JSON ->
                listOf(
                    ChatMessage(ChatMessage.Role.SYSTEM, SYSTEM_JSON),
                    ChatMessage(ChatMessage.Role.USER, "Fait : ${fact.json}"),
                )
            PromptVariant.FRENCH ->
                listOf(
                    ChatMessage(ChatMessage.Role.SYSTEM, SYSTEM_FRENCH),
                    ChatMessage(ChatMessage.Role.USER, "Faits : ${fact.french}\nPhrase :"),
                )
        }
}
