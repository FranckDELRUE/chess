# ADR 0003 — Pas de LLM embarqué en v1 : gabarits hors ligne, LLM sur la station PC

- **Statut** : proposé (S0-6), à relire en S0-8
- **Date** : 2026-10-18

## Contexte
Le dossier de conception prévoyait un petit LLM (1 à 2 Md de paramètres) sur la tablette pour
reformuler les faits pédagogiques et répondre aux questions courtes hors ligne. Le spike S0-6 a
mesuré Qwen3-1.7B et EuroLLM-1.7B-Instruct (Q4_K_M, Apache 2.0) via llama.cpp en JNI sur le
Snapdragon 6 Gen 1. Mesures : `docs/spikes/rapport-s0.md`.

## Décision
1. **Aucun LLM sur la tablette en v1.** Le retour du professeur hors ligne repose uniquement sur
   les gabarits de `:core:pedagogy` (rapides, testables, exacts par construction).
2. **Le LLM tourne sur la station PC** (modèle de 8 à 14 Md choisi en S0-7), pour « Explique-moi
   plus » et les questions libres, quand le PC est joignable. Hors ligne, ces actions proposent le
   gabarit détaillé.
3. Quand un LLM est appelé, il reçoit des **faits déjà traduits en français par le code** (coups en
   notation française, noms de pièces, motif expliqué), pas le JSON brut : le spike montre que même
   un petit modèle y gagne nettement.
4. `:spike:llm` reste disponible jusqu'à la clôture de S0 comme référence, puis il est retiré de
   l'application (le pont JNI pourra être repris si la décision change).

## Raisons (mesures S0-6)
- **Latence** : 3,6 à 4,8 s par phrase (premier jeton à 2,4 à 3,3 s, puis 12 à 16 jetons/s), contre
  moins de 200 ms visés pour le retour immédiat.
- **Fiabilité** : 0 réponse correcte sur 30 avec le fait brut ; 5 sur 15 au mieux avec des faits
  pré-traduits, et 40 % de réponses fausses (coups ou prises inventés). Contraire à la règle 3.
- **Mémoire** : 1,8 à 2,4 Go de PSS, au-delà du budget de 3 Go pour toute l'app avec Stockfish et Maia.
- La chauffe n'est pas un obstacle (stable en 5 min) : ce n'est pas elle qui disqualifie.

## Conséquences
- `:core:pedagogy` doit couvrir tous les motifs par des gabarits de qualité dès S4 (déjà prévu).
- Le professeur « riche » dépend du PC : S9 (station) et S11 (professeur v2) portent cette valeur.
- Réévaluer si un modèle de 1 à 3 Md nettement meilleur en français paraît, ou si le NPU Hexagon
  devient utilisable par des outils libres (backlog). Pistes déjà identifiées : cache KV du prompt
  système, Qwen3 plutôt qu'EuroLLM, KleidiAI, Q4_0.
