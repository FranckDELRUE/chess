# ADR 0001 — Architecture générale

- **Statut** : accepté (S0-8, révisé d'après les spikes)
- **Date** : 2026-10-17

## Contexte
Chess Companion est une application Android d'entraînement aux échecs, centrée sur un
Professeur qui analyse et explique les coups. Elle doit fonctionner hors ligne sur une
tablette de milieu de gamme (Snapdragon 6 Gen 1, 8 Go), tirer parti d'un PC équipé d'une
RTX 4080 quand il est joignable, et n'utiliser aucun service d'IA en ligne.

## Décision
1. **Application Kotlin native**, Jetpack Compose et Material 3, en Clean Architecture
   multi-modules (`:app`, `:core:*`, `:feature:*`), MVVM + UDF, Hilt, Room.
2. **Cœur échecs en Kotlin pur** (`:core:chess`) qui fait foi pour toute donnée d'échecs.
3. **Moteurs embarqués derrière une interface unique** `ChessEngine` : Stockfish (NDK)
   pour l'analyse, Maia (ONNX Runtime Mobile) pour les adversaires humains.
4. **Professeur en trois couches** : analyse moteur → détection de motifs
   (`:core:pedagogy`, Kotlin pur) → rédaction (gabarits hors ligne ; LLM de la station PC en
   option, jamais sur la tablette en v1, voir ADR 0003).
5. **Trois niveaux de calcul** : tablette (toujours disponible, Stockfish et Maia), station PC
   en réseau local (FastAPI, Stockfish multi-cœurs, Ollama ; lc0 CUDA seulement pour l'affinage
   de Maia, voir ADR 0005), aucun cloud. Routage et file d'attente dans
   `:core:compute`.
6. **Licences** : dépendances compatibles GPL v3 uniquement, pour garder ouverte une
   publication future.

## Conséquences
- Le cœur échecs et la pédagogie sont testables rapidement sans émulateur.
- Ajouter un moteur ou un LLM ne touche pas aux écrans.
- La qualité du Professeur hors ligne repose sur la détection de motifs et les gabarits,
  pas sur un LLM : ils doivent être solides dès le S4.
- La station PC est un second projet à maintenir (Python), séparé du dépôt Android ou dans
  `station-pc/`.

## Révision S0-8
Les spikes S0-4 à S0-7 confirment l'architecture, avec deux précisions : pas de LLM sur la
tablette (ADR 0003) et Maia sur la tablette plutôt que sur le PC (ADR 0002, ADR 0005).
Stockfish est intégré en JNI dans le processus de l'app (ADR 0004).

## Alternatives écartées
- **Application multiplateforme (Flutter, React Native)** : intégration NDK/ONNX plus
  complexe, aucun besoin iOS.
- **API d'IA en ligne** : écartée par choix de l'utilisateur (coût, confidentialité, hors ligne).
- **Stockfish bridé seul comme adversaire** : style trop « robotique » ; Maia retenu.
