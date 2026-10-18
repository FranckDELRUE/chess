# Backlog

Idées hors sprint, à trier en planification. Ne jamais les ajouter au sprint en cours.

- Reconnaissance d'un échiquier réel par photo (après la v1).
- Exploitation du NPU Hexagon pour le LLM embarqué (si les outils le permettent).
- Publication Play Store (à réévaluer après la v1, impose GPL v3 + code public).

## Issu de S0-3
- Interaction tactile sur l'échiquier (sélection, déplacement), surbrillance des coups, animations.
- Thèmes d'échiquier et de pièces configurables.
- Écran « À propos » avec l'attribution Cburnett (CC BY-SA 3.0).

## Issu de S0-5
- Build minimale d'ONNX Runtime (format ORT, opérateurs de Maia seulement) pour réduire l'APK de ≈ 30 Mo.
- Ajouter les autres tranches Maia-1 (1200 à 1800) : même code, un fichier `.onnx` de plus par niveau.
- Évaluer Maia-2 (Elo continu) face à Maia-1, sur le PC d'abord.

## Issu de S0-6
- LLM embarqué, si l'essai reprend : cache KV du prompt système (premier jeton de ≈ 3 s vers ≈ 0,5 s),
  KleidiAI et Q4_0 dans llama.cpp, `mmap` désactivé pour réduire le PSS, nouveaux modèles de 1 à 3 Md.
- Gabarits « détaillés » de `:core:pedagogy` pour « Explique-moi plus » hors ligne (le LLM reste sur le PC).
