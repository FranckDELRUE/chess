# Sprint S0 — Fondations

**Durée** : 2 semaines · **Objectif** : un socle qui compile, se teste et s'installe sur la
tablette, et des spikes qui confirment ce que la tablette et le PC savent faire.

**Livrable démontrable** : l'application s'installe sur la tablette Honor, affiche un
échiquier vide en paysage, la CI est verte, et `docs/spikes/rapport-s0.md` donne les mesures.

---

## Stories

### S0-1 · Socle du projet Android
- [x] Projet Android Kotlin, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`).
- [x] Modules créés : `:app`, `:core:chess`, `:core:ui`, `:core:data`, `:core:engine`.
- [x] `minSdk` 26, `targetSdk` dernier stable, Java 17.
- [x] Hilt, Compose (Material 3), Navigation Compose, Room configurés.
- [x] Thème clair/sombre, orientation paysage gérée (WindowSizeClass).

**Acceptation** : `./gradlew assembleDebug` réussit ; l'app s'ouvre sur la tablette.

### S0-2 · Qualité et intégration continue
- [x] ktlint et detekt configurés, sans erreur sur le code existant.
- [x] JUnit 5 sur les modules Kotlin purs, un test d'exemple par module.
- [x] GitHub Actions : build + tests + qualité sur chaque push et pull request.
- [x] `.gitignore`, `README.md` à jour.

**Acceptation** : la CI est verte sur `main`.

### S0-3 · Échiquier Compose minimal
- [x] Composant `ChessBoard` dans `:core:ui` : 8×8, coordonnées, pièces affichées depuis une FEN.
- [x] Échiquier retournable ; écran d'accueil qui l'affiche à gauche, panneau vide à droite (paysage).
- [x] Jeu de pièces libre de droits (par exemple Cburnett, CC BY-SA ou équivalent), licence notée.

**Acceptation** : la position initiale et une FEN arbitraire s'affichent correctement en paysage et en portrait.

> Note S0-3 : `FenParser` (placement seul) + `Board` dans `:core:chess` ; `ChessBoard` dans `:core:ui` ; pièces Cburnett (CC BY-SA 3.0) converties en VectorDrawable, voir `docs/licences.md`. Vérifié sur la tablette en paysage et en portrait (position initiale) ; les 3 FEN de test sont dans les `@Preview` et le parseur est testé. Dette : couleurs des cases en constantes (pas encore de thèmes d'échiquier), attribution à afficher dans un futur écran « À propos », bouton « Retourner » non vérifié visuellement (logique testée).

### S0-4 · Spike Stockfish sur la tablette
- [x] Compiler Stockfish (dernière version stable) pour `arm64-v8a` via NDK + CMake.
- [x] Mini-wrapper UCI (`uci`, `position fen`, `go depth N`, `stop`) derrière une interface `ChessEngine`.
- [x] Mesurer sur la tablette : temps pour profondeur 16, 18, 20 sur 10 positions de milieu de partie ; mémoire ; température après 5 min.

**Acceptation** : mesures consignées dans `docs/spikes/rapport-s0.md`.

### S0-5 · Spike Maia sur la tablette
- [x] Obtenir les réseaux Maia (1100, 1500, 1900) et les convertir en ONNX (ou utiliser un export existant).
- [x] Inférence ONNX Runtime Mobile : encodage d'une position → distribution de coups → coup légal.
- [x] Mesurer : temps par coup (politique seule), mémoire, taille des fichiers.
- [x] Vérifier sur 20 positions que le coup proposé est légal et plausible pour le niveau.

**Acceptation** : solution Maia viable sur la tablette, ou plan B documenté (Maia sur PC, Stockfish « humanisé »).

### S0-6 · Spike LLM embarqué
- [x] Tester 2 modèles open source de 1–2 Md de paramètres, quantifiés 4 bits, via llama.cpp (JNI) ou MediaPipe LLM.
- [x] Prompt test : reformuler un « fait pédagogique » en une phrase de professeur en français.
- [x] Mesurer : jetons/s, temps jusqu'au premier jeton, mémoire, chauffe ; noter la qualité du français.
- [x] Vérifier la licence de chaque modèle (compatibilité usage personnel et GPL v3).

**Acceptation** : recommandation chiffrée (garder, réserver au PC, ou abandonner le LLM embarqué).

### S0-7 · Spike station PC
- [x] Dossier `station-pc/` : environnement Python, FastAPI, route `/health`.
- [x] Stockfish x86-64 multi-cœurs : temps pour profondeur 30.
- [x] lc0 avec backend CUDA + réseau Maia : temps par coup.
- [x] Ollama avec 2 LLM de 8–14 Md (4 bits) : jetons/s, VRAM, qualité du français sur le même prompt que S0-6.

**Acceptation** : mesures consignées ; modèle LLM PC présélectionné.

### S0-8 · Documentation et décisions
- [x] ADR 0001 (architecture) relu et accepté.
- [x] ADR 0002 (moteur humain) et ADR 0003 (LLM embarqué) rédigés à partir des spikes.
- [x] `docs/spikes/rapport-s0.md` complet ; ajustements proposés pour les sprints suivants.
- [x] Corpus de test : 100 parties de **Heidoji** exportées en PGN dans `testdata/pgn/` (export manuel depuis Lichess et Chess.com).

**Acceptation** : rapport et ADR versionnés ; le plan S1–S13 est confirmé ou ajusté.

---

## Notes de sprint
_(à compléter en fin de story : fait, dette, ajustements)_

**S0-1** — Fait : 5 modules, AGP 9.4.0 / Gradle 9.6.0 / Kotlin 2.4.21 / KSP 2.3.12 / Hilt 2.60.1 / Room 2.8.5 / Compose BOM 2026.09.00 ; `assembleDebug` OK ; écran d'accueil adaptatif (WindowSizeClass). compileSdk/targetSdk 37.
Vérifié sur la tablette Honor (HEY2-W09, Android 14) : `installDebug` OK, l'app s'ouvre en paysage (échiquier à gauche, panneau à droite), sans crash.
Dette : pas de `jvmToolchain` (seul le JDK 25 d'Android Studio est installé, cible Java 17 via compileOptions) ; Room et `ChessEngine` sont des squelettes ; le mapping `Side` de :core:chess est provisoire ; `local.properties` pointe vers `C:/Users/Public/Android`.

**S0-2** — Fait : ktlint (plugin 14.2.0, `.editorconfig`) et detekt 2.0.0-alpha.6 (`config/detekt/detekt.yml`) sur tous les sous-projets, sans erreur (3 reformatages dans `Theme.kt`) ; JUnit 5.13.4 sur les 5 modules, 1 test d'exemple chacun (5 exécutés) ; workflow `.github/workflows/ci.yml` ; `.gitignore` et README mis à jour. `ktlintCheck detekt test assembleDebug` passent en local.
Dette : detekt 2 est en alpha (1.23.x incompatible avec Kotlin 2.4) ; la CI n'a pas encore tourné sur GitHub (critère « verte sur `main` » à vérifier après push/merge) ; tests d'exemple triviaux, remplacés au fil des sprints.

**S0-4** — Fait : Stockfish 19 compilé pour `arm64-v8a` (NDK 29, CMake 3.31.6) en bibliothèque JNI ; `ChessEngine` (`start`, `setOption`, `newGame`, `analyze` en `Flow`, `close`), `UciParser` et `UciEngine` testés en JVM, `StockfishEngine` ; écran « Debug moteur » (analyse, lot 10×3, chauffe 5 min) ; mesures dans `docs/spikes/rapport-s0.md` (profondeur 16 : médiane 0,6 s ; 18 : 1,7 s ; 20 : 5,5 s ; ≈ 430 Mo ; batterie 22 → 26 °C, aucun bridage). ADR 0004. Vérifié sur la tablette.
Dette : réseau NNUE non versionné (lancer `scripts/fetch-stockfish-net.ps1` avant le build) et APK de 112 Mo ; plusieurs des 10 positions de test sont déséquilibrées (à remplacer par des positions du corpus Heidoji en S0-8) ; écran de debug aussi présent en release (à masquer avant toute diffusion) ; mesures faites à 4 threads seulement ; sonde thermique = batterie ; ni LTO ni PGO essayés ; CI : installation du NDK et de CMake ajoutée mais non éprouvée sur GitHub.

**S0-5** — Fait : Maia-1 1100/1500/1900 convertis par `lc0 leela2onnx` (`scripts/fetch-maia-nets.ps1`, `tools/maia2onnx/maia2onnx.py`) ; `Lc0Encoder`, `Lc0PolicyMap`, `PolicyDecoder`, `MaiaEngine : ChessEngine` (ONNX Runtime Android 1.30.0) dans `:core:engine`, validés contre les priors de lc0 (écart < 0,001 sur 20 positions × 3 réseaux) ; bouton « Lot Maia 20×3 » ; mesures dans `docs/spikes/rapport-s0.md` (1,5–3,6 ms par coup, +21 Mo pour 3 réseaux, 60/60 coups légaux, perte moyenne 17/15/4 cp) ; ADR 0002 (proposé). Vérifié sur la tablette. **Conclusion : Maia viable sur la tablette.**
Écart au périmètre (validé) : coups légaux fournis par Stockfish `go perft 1` derrière `LegalMoveProvider`, faute de générateur dans `:core:chess` (S1).
Dette : perft Stockfish ≈ 30 ms par coup et dépendance à la session Stockfish (S1) ; champs FEN hors placement lus dans `:core:engine` sans validation `:core:chess` (S1) ; historique synthétisé (`fen_only`), à passer en S3 ; coup déterministe à `Temperature` 0 ; APK +41 Mo (ONNX Runtime complet, build minimale à étudier) ; tests JVM sur ONNX Runtime desktop 1.18.0 (le JBR plante avec ≥ 1.19), test de bout en bout ignoré en CI faute de modèles ; écran de debug toujours présent en release.

**S0-6** — Fait : module isolé `:spike:llm` (llama.cpp `v0.7.0` en JNI, récupéré par CMake avec SHA-256 épinglé ; `TextGenerator`, `Utf8StreamDecoder`, `FactFidelity`, `TeacherPrompt`, 22 tests JVM) ; écran « Debug LLM » (réponse en flux, lot, chauffe 5 min) ; Qwen3-1.7B et EuroLLM-1.7B-Instruct en Q4_K_M, tous deux Apache 2.0 (`scripts/fetch-llm-models.ps1 -Push`) ; mesures dans `docs/spikes/rapport-s0.md` : premier jeton 2,4–3,3 s, génération 12,5 (Qwen3) et 15,5 (EuroLLM) jetons/s à 4 threads, PSS 2,4 et 1,8 Go, chauffe 30 → 31 °C sans bridage. Qualité : 0/30 réponse correcte avec le fait JSON brut, 5/15 au mieux (Qwen3) avec des faits pré-traduits. ADR 0003 (proposé). Vérifié sur la tablette. **Conclusion : pas de LLM embarqué en v1, LLM réservé au PC.**
Écarts au périmètre : module `:spike:llm` hors de la liste des modules ; variante de prompt « FRENCH » (faits pré-traduits + exemple) ajoutée au prompt du sprint.
Dette : notes de qualité attribuées par Claude, à relire ; cache du prompt système, KleidiAI et Q4_0 non essayés ; CMake télécharge llama.cpp (38 Mo) à la configuration, aussi en CI (non éprouvé sur GitHub) ; `:spike:llm` à retirer de l'app en S0-8 ; `ndkVersion` ajouté à `:app` (les `.so` sont maintenant strippés) ; écran de debug présent en release.

**S0-7** — Fait : `station-pc/` (venv Python 3.12, `requirements.txt` épinglé, FastAPI `/health` avec GPU, moteurs et modèles Ollama, `start.bat` sur le port 8765, README d'installation, 17 tests pytest) ; `station-pc/scripts/fetch-engines.ps1` (Stockfish 19 et lc0 CUDA 12, SHA-256) ; benchmarks `bench.stockfish`, `bench.lc0`, `bench.llm` (mêmes prompts et contrôle de fidélité que S0-6, portés en Python). Mesures dans `docs/spikes/rapport-s0.md` : Stockfish 32 threads profondeur 20 en 0,9 s (×6 la tablette), profondeur 30 en 39,7 s en médiane (jusqu'à 20 min sur les positions déséquilibrées) ; Maia plus rapide sur CPU (≈ 1,5 ms) que sur CUDA (4–14 ms) ; qwen3:14b 8,9 Go de VRAM, 1,2 s par phrase, 10/15 réponses correctes et 0 fausse avec les faits pré-traduits ; mistral-nemo:12b 0,4 s par phrase, 9/15 et 2 fausses. **LLM présélectionné : qwen3:14b (repli mistral-nemo:12b).**
Dette : mesures sur portable avec d'autres applications actives (≈ 3 Go de VRAM pris, bridage non suivi) ; un seul réglage Stockfish ; prompts et contrôle de fidélité dupliqués Kotlin/Python ; notes de qualité attribuées par Claude, à relire ; avertissement de dépréciation de `fastapi.testclient`.

**S0-8** — Fait : ADR 0001, 0002 et 0003 relus et passés en « accepté » (0001 précise : pas de LLM sur la tablette, lc0 CUDA réservé à l'affinage de Maia) ; ADR 0005 (rôles de la station PC : Stockfish limité par temps, Maia sur la tablette, qwen3:14b avec repli mistral-nemo:12b, gabarit toujours affiché) ; `rapport-s0.md` : synthèse tablette/PC en tête et ajustements S1–S13 en fin, reportés dans la section 6 et les tableaux d'IA de `docs/dossier-conception.md` (S9 : critère « profondeur 30 en 3 s » remplacé par « 3 s, profondeur ≥ 22 », LLM embarqué retiré ; S11 : gabarit détaillé hors ligne) ; écran « Debug LLM » et `:spike:llm` retirés de l'app (module conservé pour mémoire, plus construit), APK debug 186 → 153 Mo. `ktlintCheck detekt test assembleDebug` passent ; APK installé sur la tablette.
Écart (validé) : remplacement des 10 positions de test de S0-4 par des positions du corpus reporté en S2 (backlog).
Corpus : `testdata/pgn/chesscom-heidoji-100.pgn`, 100 parties standard de Chess.com (2019-10 → 2024-03) par l'API publique au lieu d'un export manuel ; le compte Lichess n'a aucune partie. Légalité non vérifiée (test d'import de S1).
Reste : capture de démo de fin de sprint (tablette verrouillée).
