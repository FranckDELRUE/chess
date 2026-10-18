# ADR 0004 — Stockfish embarqué : bibliothèque JNI en processus, réseau en assets

- **Statut** : accepté (S0-4)
- **Date** : 2026-10-18

## Contexte
Il faut exécuter Stockfish (GPL v3) sur la tablette derrière `ChessEngine`. Deux voies :
exécutable lancé par `ProcessBuilder`, ou bibliothèque partagée appelée en JNI.

## Décision
1. **Bibliothèque partagée `libstockfish_jni.so`** compilée par CMake (`arm64-v8a` uniquement),
   packagée normalement par AGP. `main()` de Stockfish est renommé `stockfish_main`
   (`-Dmain=…` sur `main.cpp` seulement) ; un pont C++ relie `std::cin`/`std::cout` à deux tubes
   POSIX via des `streambuf`, sans toucher au stdio de l'application.
2. **Une session par processus** : Stockfish garde un état global, `quit` est définitif. Le moteur
   vit donc dans un singleton Hilt (`EngineProvider`).
3. **Transport UCI abstrait** (`UciTransport`) : `UciEngine` est testé en JVM avec un faux moteur ;
   seul `StockfishEngine` dépend d'Android.
4. **Réseau NNUE en assets** (`NNUE_EMBEDDING_OFF`), copié dans `filesDir` au premier lancement et
   passé via l'option `EvalFile`. Il n'est pas versionné dans git (98 Mo) :
   `scripts/fetch-stockfish-net.ps1`.

## Conséquences
- Aucune dépendance au droit d'exécuter des fichiers depuis `filesDir` (Android 10+).
- Redémarrer le moteur demande de relancer le processus ; changer d'option passe par `setoption`.
- APK debug de 112 Mo ; à revoir si l'application est un jour publiée (livraison du réseau à part).
- Pas d'x86-64 : l'émulateur n'est pas supporté pour le moteur.
