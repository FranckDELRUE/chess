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
- [ ] Compiler Stockfish (dernière version stable) pour `arm64-v8a` via NDK + CMake.
- [ ] Mini-wrapper UCI (`uci`, `position fen`, `go depth N`, `stop`) derrière une interface `ChessEngine`.
- [ ] Mesurer sur la tablette : temps pour profondeur 16, 18, 20 sur 10 positions de milieu de partie ; mémoire ; température après 5 min.

**Acceptation** : mesures consignées dans `docs/spikes/rapport-s0.md`.

### S0-5 · Spike Maia sur la tablette
- [ ] Obtenir les réseaux Maia (1100, 1500, 1900) et les convertir en ONNX (ou utiliser un export existant).
- [ ] Inférence ONNX Runtime Mobile : encodage d'une position → distribution de coups → coup légal.
- [ ] Mesurer : temps par coup (politique seule), mémoire, taille des fichiers.
- [ ] Vérifier sur 20 positions que le coup proposé est légal et plausible pour le niveau.

**Acceptation** : solution Maia viable sur la tablette, ou plan B documenté (Maia sur PC, Stockfish « humanisé »).

### S0-6 · Spike LLM embarqué
- [ ] Tester 2 modèles open source de 1–2 Md de paramètres, quantifiés 4 bits, via llama.cpp (JNI) ou MediaPipe LLM.
- [ ] Prompt test : reformuler un « fait pédagogique » en une phrase de professeur en français.
- [ ] Mesurer : jetons/s, temps jusqu'au premier jeton, mémoire, chauffe ; noter la qualité du français.
- [ ] Vérifier la licence de chaque modèle (compatibilité usage personnel et GPL v3).

**Acceptation** : recommandation chiffrée (garder, réserver au PC, ou abandonner le LLM embarqué).

### S0-7 · Spike station PC
- [ ] Dossier `station-pc/` : environnement Python, FastAPI, route `/health`.
- [ ] Stockfish x86-64 multi-cœurs : temps pour profondeur 30.
- [ ] lc0 avec backend CUDA + réseau Maia : temps par coup.
- [ ] Ollama avec 2 LLM de 8–14 Md (4 bits) : jetons/s, VRAM, qualité du français sur le même prompt que S0-6.

**Acceptation** : mesures consignées ; modèle LLM PC présélectionné.

### S0-8 · Documentation et décisions
- [ ] ADR 0001 (architecture) relu et accepté.
- [ ] ADR 0002 (moteur humain) et ADR 0003 (LLM embarqué) rédigés à partir des spikes.
- [ ] `docs/spikes/rapport-s0.md` complet ; ajustements proposés pour les sprints suivants.
- [ ] Corpus de test : 100 parties de **Heidoji** exportées en PGN dans `testdata/pgn/` (export manuel depuis Lichess et Chess.com).

**Acceptation** : rapport et ADR versionnés ; le plan S1–S13 est confirmé ou ajusté.

---

## Notes de sprint
_(à compléter en fin de story : fait, dette, ajustements)_

**S0-1** — Fait : 5 modules, AGP 9.4.0 / Gradle 9.6.0 / Kotlin 2.4.21 / KSP 2.3.12 / Hilt 2.60.1 / Room 2.8.5 / Compose BOM 2026.09.00 ; `assembleDebug` OK ; écran d'accueil adaptatif (WindowSizeClass). compileSdk/targetSdk 37.
Vérifié sur la tablette Honor (HEY2-W09, Android 14) : `installDebug` OK, l'app s'ouvre en paysage (échiquier à gauche, panneau à droite), sans crash.
Dette : pas de `jvmToolchain` (seul le JDK 25 d'Android Studio est installé, cible Java 17 via compileOptions) ; Room et `ChessEngine` sont des squelettes ; le mapping `Side` de :core:chess est provisoire ; `local.properties` pointe vers `C:/Users/Public/Android`.

**S0-2** — Fait : ktlint (plugin 14.2.0, `.editorconfig`) et detekt 2.0.0-alpha.6 (`config/detekt/detekt.yml`) sur tous les sous-projets, sans erreur (3 reformatages dans `Theme.kt`) ; JUnit 5.13.4 sur les 5 modules, 1 test d'exemple chacun (5 exécutés) ; workflow `.github/workflows/ci.yml` ; `.gitignore` et README mis à jour. `ktlintCheck detekt test assembleDebug` passent en local.
Dette : detekt 2 est en alpha (1.23.x incompatible avec Kotlin 2.4) ; la CI n'a pas encore tourné sur GitHub (critère « verte sur `main` » à vérifier après push/merge) ; tests d'exemple triviaux, remplacés au fil des sprints.
