# ADR 0005 — Rôles de la station PC : Stockfish profond et LLM, Maia sur la tablette

- **Statut** : accepté (S0-8)
- **Date** : 2026-10-18

## Contexte
La station PC (`station-pc/`, FastAPI) doit décharger la tablette quand elle est joignable, sans
jamais être requise (règle 4). Le spike S0-7 a mesuré Stockfish 19, lc0 + Maia et deux LLM via
Ollama sur la station (i9-13980HX, RTX 4080 Laptop 12 Go). Mesures : `docs/spikes/rapport-s0.md`.

## Décision
1. **Stockfish multi-cœurs** pour les analyses profondes (revue de partie, analyse de nuit),
   **limitées par temps** (de l'ordre de 3 à 10 s par position) plutôt que par profondeur : la
   profondeur 30 prend de 0,5 s à 20 min selon la position.
2. **Maia reste sur la tablette** (ADR 0002). lc0 CUDA n'est pas utilisé pour jouer ; il ne sert
   qu'à l'affinage de Maia sur les parties du joueur (S12).
3. **LLM du professeur : qwen3:14b** (Q4_K_M, Apache 2.0) via Ollama, raisonnement désactivé.
   **Repli : mistral-nemo:12b** (Apache 2.0) si la VRAM libre est inférieure à ≈ 9 Go.
4. Le LLM reçoit uniquement des **faits pré-traduits en français par le code** et des lignes
   moteur (règle 3). Sa phrase est toujours affichée **à côté du gabarit exact**, jamais à sa place.
5. Le modèle est chargé au démarrage de la station (`keep_alive`) pour éviter 7 à 9 s d'attente au
   premier appel.

## Raisons (mesures S0-7)
- Stockfish : profondeur 20 en 0,9 s en médiane, 6 fois plus vite que la tablette.
- Maia : 4 à 14 ms par coup en CUDA contre ≈ 1,5 ms sur CPU ; la tablette fait déjà 1,5 à 3,6 ms.
- qwen3:14b : 10 réponses correctes sur 15, aucune fausse, 1,2 s par phrase, 8,9 Go de VRAM.
  mistral-nemo:12b : 9 correctes et 2 fausses, 0,4 s par phrase, 6,8 Go.
- Même le meilleur modèle reste imprécis 1 fois sur 3 : il ne peut être qu'un complément.

## Conséquences
- Le critère S9 « profondeur 30 en moins de 3 s » est irréaliste ; il est remplacé par une analyse
  limitée par temps (voir les ajustements du rapport S0).
- La station dépend d'Ollama (MIT) en plus de Python ; à installer à part.
- Les mesures ont été faites sur un portable avec d'autres applications ouvertes : à refaire en S9
  avant de fixer les limites de temps.
