# ADR 0010 — File d'analyses en arrière-plan : Room par position, WorkManager au premier plan

- **Statut** : accepté (S2-5)
- **Date** : 2026-11-20

## Contexte
S2-5 analyse une partie entière en tâche de fond (profondeur 18 par position, environ 2 s chacune
sur la tablette d'après S2-2, soit près de 3 min pour 80 positions). L'analyse doit pouvoir être
annulée, survivre à l'écran éteint et à la mort du processus, et ses résultats doivent resservir à
l'analyse en direct (S2-3) et plus tard à la revue de parties (S6).

## Décision
1. **Stockage par position** dans Room (`:core:data`, base `ChessDatabase` v1, schéma exporté) :
   table `position_analysis` indexée par la clé Zobrist, une seule ligne par position, **la plus
   profonde gagne**. Les quatre premiers champs de la FEN sont gardés pour écarter une collision de
   clé (une autre position de même clé n'est jamais renvoyée, elle remplace l'ancienne).
2. **Table `analysis_job`** pour la file : PGN, état, positions faites. WorkManager ne reçoit que
   l'identifiant du job (ses données d'entrée sont limitées à 10 Ko, un PGN avec horloges peut
   dépasser). La progression écrite après chaque position permet de **reprendre** où l'analyse
   s'est arrêtée.
3. **`GameAnalyzer`** (cas d'usage testable en JVM) parcourt la ligne principale, saute les
   positions terminées et celles déjà analysées aussi profond, cherche en priorité `BACKGROUND`
   (ADR 0008 : l'adversaire et l'analyse en direct passent devant, la recherche interrompue
   repart seule) et valide coup et ligne par `:core:chess` avant stockage (règle 2).
4. **`GameAnalysisWorker`** (`CoroutineWorker` Hilt) au premier plan, type `specialUse` (aucun type
   standard ne décrit une analyse d'échecs locale), notification de progression avec « Annuler ».
   Les parties s'enchaînent dans un travail unique (`APPEND_OR_REPLACE`) : annuler l'analyse annule
   aussi les parties en attente, et leurs jobs passent à « annulé ». Arrêté par le système, le job
   repasse en attente et reprend quand WorkManager le relance.
5. **Code dans `:feature:play`** en attendant `:feature:review` (S6) : un module ne se crée qu'avec
   son sprint. `AnalysisStore` (interface) et Room restent dans `:core:data`.
6. **Réutilisation** : l'analyse en direct affiche d'abord la ligne stockée (rang 1), remplacée
   seulement par une ligne en direct au moins aussi profonde (règle de S2-3).

## Conséquences
- WorkManager est initialisé par l'application (fabrique Hilt) : l'initialiseur par défaut est
  retiré du manifeste.
- Permissions : service au premier plan (`specialUse`) et notifications (demandée au premier
  « Analyser la partie » ; refusée, l'analyse tourne quand même, sans notification visible).
- Dépendances : `androidx.work` 2.12.0, `androidx.hilt:hilt-work` 1.4.0 (Apache 2.0).
- À déplacer dans `:feature:review` en S6 (cas d'usage, worker, file).
