# Chess Companion — guide pour Claude Code

## Projet
Application Android d'entraînement aux échecs, centrée sur un **Professeur** en mode
apprentissage qui conseille avant le coup, analyse chaque coup joué et explique pourquoi.
Kotlin, Jetpack Compose, Material 3. Usage personnel (pas de publication pour l'instant).

- Référence unique : `docs/dossier-conception.md`
- Sprint en cours : `docs/sprints/current.md`
- Décisions d'architecture : `docs/adr/`

## Contexte utilisateur
- Joueur ELO ~1300 (palier Club), comptes **Heidoji** sur Lichess et Chess.com.
- Appareil cible unique pour les performances : tablette Honor, Snapdragon 6 Gen 1,
  8 Go de RAM réelle (ignorer la « RAM turbo »), écran 2560 × 1600, paysage prioritaire.
- Station PC Windows : RTX 4080 Laptop 12 Go VRAM, i9-13980HX, 32 Go RAM.

## Règles non négociables
1. **Aucune API d'IA en ligne** (pas d'API Claude, OpenAI, etc.). Toute l'IA tourne sur la
   tablette ou sur la station PC du réseau local.
2. **Le moteur de règles fait foi** : toute donnée d'échecs entrante (PGN importé, ligne lue
   sur photo, coup proposé par un LLM) est validée par `:core:chess` avant stockage.
3. **L'IA commente, le moteur calcule** : un LLM ne reçoit que les faits pédagogiques et les
   lignes Stockfish ; il n'invente jamais de coup.
4. **Hors ligne d'abord** : jeu, puzzles, répertoire, livres et révisions fonctionnent sans
   réseau ni PC. Le PC améliore, il n'est jamais requis.
5. **Licences** : n'ajouter que des dépendances compatibles GPL v3 (Apache 2.0, MIT, BSD,
   CC0, GPL). Signaler toute licence douteuse avant de l'ajouter.

## Architecture
- Clean Architecture multi-modules, MVVM + UDF, Hilt, Room, Coroutines/Flow, WorkManager.
- `:core:chess` et `:core:pedagogy` sont en **Kotlin pur** (aucune dépendance Android).
- Les moteurs (Stockfish via NDK, Maia via ONNX Runtime) passent par l'interface `ChessEngine`.
- La station PC est un projet séparé dans `station-pc/` (Python, FastAPI).

Modules : `:app`, `:core:chess`, `:core:engine`, `:core:data`, `:core:ui`, `:core:srs`,
`:core:pedagogy`, `:core:compute`, `:feature:play`, `:feature:puzzles`,
`:feature:repertoire`, `:feature:training`, `:feature:review`, `:feature:teacher`,
`:feature:endgames`, `:feature:vision`, `:feature:books`, `:feature:progress`.
Ne créer un module que lorsque son sprint commence.

## Commandes (Windows : `gradlew.bat`)
- Build : `./gradlew assembleDebug`
- Tests : `./gradlew test`
- Qualité : `./gradlew ktlintCheck detekt`
- Installer sur la tablette : `./gradlew installDebug` (débogage USB activé)

## Conventions
- Code, identifiants et commits en anglais ; textes d'interface en ressources, français par défaut.
- Un ViewModel par écran, état exposé en `StateFlow` immuable ; pas de logique métier dans les composables.
- Tests obligatoires pour toute règle d'échecs, tout cas d'usage et tout algorithme (FSRS, Glicko-2, motifs).
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`) ; une branche par user story.

## Façon de travailler
1. Avant de coder : lis `docs/sprints/current.md`, propose un plan court, **attends ma validation**.
2. Une story à la fois. Lance build, tests et qualité avant de déclarer une story terminée.
3. En fin de story : coche la case dans `docs/sprints/current.md` et ajoute une note courte
   (ce qui a été fait, dette technique éventuelle).
4. Toute décision d'architecture nouvelle → un ADR court dans `docs/adr/`.
5. Signale tout écart au périmètre plutôt que de l'absorber ; les idées nouvelles vont dans
   `docs/backlog.md`.
