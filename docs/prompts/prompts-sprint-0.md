# Prompts Claude Code — Sprint 0

Une story à la fois. Lance `claude` à la racine du dépôt, colle le prompt, valide le plan,
puis laisse Claude Code implémenter. Passe à la story suivante quand la précédente est cochée.

---

## S0-1 · Socle du projet Android

```
Sprint S0, story S0-1 : socle du projet Android.
Lis CLAUDE.md, docs/dossier-conception.md (section 3) et docs/sprints/current.md.
Crée le projet Android (Kotlin, Gradle Kotlin DSL, version catalog) avec les modules
:app, :core:chess, :core:ui, :core:data, :core:engine. :core:chess est un module Kotlin/JVM
pur. Configure Hilt, Compose Material 3, Navigation Compose, Room, thème clair/sombre et
WindowSizeClass. minSdk 26, Java 17.
Commence par un plan court (arborescence, dépendances et versions) et attends ma validation.
Critère : ./gradlew assembleDebug réussit et l'app s'ouvre sur la tablette.
```

## S0-2 · Qualité et intégration continue

```
Sprint S0, story S0-2 : qualité et CI.
Ajoute ktlint et detekt (config raisonnable, pas de règles bloquantes inutiles),
JUnit 5 sur les modules Kotlin purs avec un test d'exemple par module, et un workflow
GitHub Actions (build, test, ktlintCheck, detekt) sur push et pull request.
Ajoute .gitignore et mets à jour README.md (prérequis, commandes).
Plan court d'abord. Critère : toutes les tâches passent en local.
```

## S0-3 · Échiquier Compose minimal

```
Sprint S0, story S0-3 : échiquier Compose minimal dans :core:ui.
Composant ChessBoard(fen: String, flipped: Boolean) : 8×8, coordonnées sur les bords,
pièces vectorielles d'un jeu libre de droits (indique la licence dans docs/licences.md).
Pour parser la FEN, crée dans :core:chess un parseur minimal avec tests (pas encore les règles).
Écran d'accueil : échiquier à gauche et panneau vide à droite en paysage, empilés en portrait.
Plan court d'abord. Critère : position initiale et 3 FEN de test correctement affichées.
```

## S0-4 · Spike Stockfish

```
Sprint S0, story S0-4 : spike Stockfish sur la tablette.
Compile la dernière version stable de Stockfish pour arm64-v8a via NDK + CMake et
expose-la derrière une interface ChessEngine dans :core:engine (protocole UCI :
uci, isready, position fen, go depth, stop, quit) avec lecture asynchrone par Flow.
Ajoute un écran de debug qui lance une analyse sur une FEN et affiche profondeur,
évaluation, meilleure ligne et temps.
Puis crée docs/spikes/rapport-s0.md avec un tableau à remplir : 10 positions,
temps pour profondeur 16/18/20, mémoire, température après 5 min.
Plan court d'abord. Indique-moi précisément comment faire les mesures sur la tablette.
```

## S0-5 · Spike Maia

```
Sprint S0, story S0-5 : spike Maia sur la tablette.
Propose la meilleure voie pour exécuter les réseaux Maia (1100, 1500, 1900) sur Android :
conversion des poids lc0 en ONNX (script Python dans tools/), ou export existant.
Implémente l'encodage d'une position en tenseur d'entrée, l'inférence ONNX Runtime Mobile
(politique seule) et le décodage en coup légal, derrière ChessEngine.
Vérifie la licence des poids. Ajoute les mesures (temps par coup, mémoire, taille)
au rapport docs/spikes/rapport-s0.md, et un plan B si ce n'est pas viable.
Plan court d'abord, en signalant les incertitudes techniques.
```

## S0-6 · Spike LLM embarqué

```
Sprint S0, story S0-6 : spike LLM embarqué sur la tablette (Snapdragon 6 Gen 1, 8 Go).
Propose 2 modèles open source de 1 à 2 Md de paramètres, bons en français, quantifiés
4 bits, avec leur licence. Intègre-les via llama.cpp (JNI) ou MediaPipe LLM Inference,
dans un module expérimental isolé.
Prompt de test : reformuler ce fait pédagogique en une phrase de professeur pour un
joueur de 1300 : {"motif":"hanging_piece","piece":"knight","square":"f3","best":"Nd2"}.
Mesure jetons/s, temps au premier jeton, mémoire, chauffe ; note la qualité du français.
Ajoute les résultats et ta recommandation au rapport.
Plan court d'abord.
```

## S0-7 · Spike station PC

```
Sprint S0, story S0-7 : spike de la station PC (Windows, RTX 4080 Laptop 12 Go, i9, 32 Go).
Crée station-pc/ : projet Python (venv, requirements.txt), FastAPI avec /health,
script start.bat. Ajoute un script de benchmark qui mesure :
Stockfish x86-64 (tous les cœurs) jusqu'à profondeur 30 ; lc0 CUDA avec un réseau Maia ;
Ollama avec 2 LLM de 8 à 14 Md (4 bits) sur le même prompt que S0-6 (jetons/s, VRAM).
Explique-moi les installations à faire (lc0, Ollama, modèles). Résultats dans le rapport.
Plan court d'abord.
```

## S0-8 · Documentation et décisions

```
Sprint S0, story S0-8 : clôture du sprint.
À partir de docs/spikes/rapport-s0.md, rédige ADR 0002 (moteur humain) et ADR 0003
(LLM embarqué), mets à jour l'ADR 0001 en « accepté » si rien ne le contredit,
et propose les ajustements des sprints S1 à S13 dans docs/sprints/retro-s0.md.
Prépare testdata/pgn/ avec un README expliquant comment j'y dépose mes parties.
Ne modifie pas le dossier de conception sans me montrer le diff.
```
