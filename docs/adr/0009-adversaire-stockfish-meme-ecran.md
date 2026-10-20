# ADR 0009 — La partie contre Stockfish : le même écran, un contrôleur d'adversaire

- **Statut** : accepté (S2-4)
- **Date** : 2026-10-20

## Contexte
S2-4 ajoute une partie contre Stockfish à force réglable. L'écran de partie de S1 (`PlayViewModel`,
`MoveInput`, `GameSession`) sait déjà jouer, naviguer dans l'arbre de coups, importer et exporter du
PGN. `PlayViewModel` est aussi à la limite de detekt (11 fonctions par classe).

## Décision
1. **Même écran, même `GameSession`** : pas de nouveau module ni de nouvel écran. L'accueil ouvre la
   route `play?vs=engine` ; `PlayViewModel` lit l'argument dans son `SavedStateHandle`.
2. **`OpponentController`** (dans `:feature:play`) porte tout ce qui est propre à l'adversaire :
   réglages, camp du joueur, abandon, recherche du coup de Stockfish. Le ViewModel le branche en
   quatre points : `sync` après chaque changement de la partie, `canHumanMove` avant un geste,
   `accept` pour les actions (nouvelle partie, reprise, abandon) et `finish` à l'export.
3. **Quand Stockfish joue** : à son trait, partie non terminée, joueur n'ayant pas abandonné, et soit
   la position affichée termine la ligne, soit le joueur vient de jouer (`humanMoved`). Naviguer dans
   une ancienne position ne le fait jamais jouer.
4. **Règle 2 respectée** : la recherche reçoit la FEN de départ et tous les coups
   (`position fen … moves …`, S2-1) ; le coup rendu est vérifié par `:core:chess` (légal sur la
   position attendue) avant d'être joué. Sinon la partie ne bouge pas, un message s'affiche et la même
   position n'est pas réessayée en boucle.
5. **Priorité `OPPONENT`** dans l'`EngineScheduler` (ADR 0008) : Stockfish répond même si une analyse
   tourne. L'analyse est masquée pendant la partie (ce serait un conseil gratuit) et revient quand la
   partie est finie (mat, nulle, abandon).
6. **Reprendre un coup** revient au dernier trait du joueur ; les coups repris restent dans l'arbre
   comme variante (aucune perte). **Abandonner** donne le résultat à Stockfish dans le PGN exporté.
7. **Sauvegarde** : réglages, camp et abandon sont gardés avec le PGN dans le `SavedStateHandle` ; si le
   processus est tué pendant la réflexion, Stockfish reprend son coup à la restauration.

## Conséquences
- La force offerte est celle de Stockfish (`UCI_Elo` 1320 à 3190, ou `Skill Level` 0 à 20) : son ELO n'est
  pas celui de Chess.com ou de Lichess. Les adversaires calibrés comme des humains viennent en S3 (Maia).
- Pas de pendule : le temps par coup est une limite de recherche, pas une horloge de partie (S3).
- Le premier coup de la session coûte ≈ 2 s de plus (démarrage de Stockfish et chargement du réseau,
  mesuré sur la tablette) ; un préchauffage au lancement de la partie est au backlog.
- En vérifiant l'annulation sur la tablette, un défaut de `UciEngine` (S2-1) est apparu : une annulation
  arrivée pendant l'envoi de `go` laissait la recherche courir jusqu'à sa fin, en retenant le moteur.
  Corrigé dans la même story, avec un test qui reproduit la course.
