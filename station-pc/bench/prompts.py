"""The S0-6 teacher prompts, copied word for word from `spike/llm/.../TeacherPrompt.kt` so both spikes compare."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class TeacherFact:
    json: str
    french: str


FACTS = [
    TeacherFact(
        '{"motif":"hanging_piece","piece":"knight","square":"f3","best":"Nd2"}',
        "Motif : pièce en prise, non défendue. Pièce : ton cavalier en f3. Meilleur coup : Cd2.",
    ),
    TeacherFact(
        '{"motif":"fork","piece":"knight","square":"c7","targets":["king e8","rook a8"],"best":"Nc7+"}',
        "Motif : fourchette. Pièce : ton cavalier, qui va en c7. Cibles : le roi en e8 et la tour en a8. "
        "Meilleur coup : Cc7+.",
    ),
    TeacherFact(
        '{"motif":"pin","piece":"bishop","square":"g5","pinned":"knight f6","behind":"queen d8","best":"Bxf6"}',
        "Motif : clouage. Pièce : ton fou en g5. Pièce clouée : le cavalier en f6, devant la dame en d8. "
        "Meilleur coup : Fxf6.",
    ),
    TeacherFact(
        '{"motif":"back_rank_mate","piece":"rook","square":"e8","best":"Re8#"}',
        "Motif : mat du couloir. Pièce : ta tour, qui va en e8. Meilleur coup : Te8#.",
    ),
    TeacherFact(
        '{"motif":"free_capture","piece":"bishop","square":"d4","best":"Nxd4"}',
        "Motif : pièce à prendre gratuitement. Pièce : le fou adverse en d4. Meilleur coup : Cxd4.",
    ),
]

SYSTEM_JSON = (
    "Tu es un professeur d'échecs bienveillant. Ton élève a environ 1300 Elo. "
    "On te donne un fait pédagogique au format JSON, calculé par un moteur d'échecs. "
    "Reformule-le en une seule phrase en français, claire et encourageante, en tutoyant l'élève. "
    "N'invente aucun coup, aucune case et aucune pièce : utilise seulement les informations du JSON. "
    "Écris les coups en notation française (R roi, D dame, T tour, F fou, C cavalier). "
    "Lexique : hanging_piece = pièce en prise non défendue ; fork = fourchette ; pin = clouage ; "
    "back_rank_mate = mat du couloir ; free_capture = pièce à prendre gratuitement ; "
    "best = meilleur coup."
)

SYSTEM_FRENCH = (
    "Tu es un professeur d'échecs bienveillant. Ton élève a environ 1300 Elo. "
    "On te donne des faits calculés par un moteur d'échecs. "
    "Écris une seule phrase en français, claire et encourageante, en tutoyant l'élève. "
    "N'ajoute aucun coup, aucune case et aucune pièce : utilise seulement les faits donnés. "
    "Exemple. Faits : Motif : échec à la découverte. Pièce : ton fou en c4. Meilleur coup : Fxf7+. "
    "Phrase : Ton fou en c4 peut prendre en f7 avec échec : joue Fxf7+ !"
)

VARIANTS = ("JSON", "FRENCH")


def messages(fact: TeacherFact, variant: str) -> list[dict[str, str]]:
    if variant == "JSON":
        return [{"role": "system", "content": SYSTEM_JSON}, {"role": "user", "content": f"Fait : {fact.json}"}]
    if variant == "FRENCH":
        return [
            {"role": "system", "content": SYSTEM_FRENCH},
            {"role": "user", "content": f"Faits : {fact.french}\nPhrase :"},
        ]
    raise ValueError(variant)
