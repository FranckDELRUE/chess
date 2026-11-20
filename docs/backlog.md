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

## Issu de S0-7
- Station : précharger le LLM au démarrage (`keep_alive`) et exposer son état dans `/health`.
- Analyse de partie sur la station limitée par temps (≈ 10 s par coup) plutôt que par profondeur ;
  mesurer 16/24/32 threads et le bridage du portable.
- Revoir les jetons/s de qwen3:14b (≈ 20 jetons/s, attendu plus haut sur une RTX 4080) ; essayer
  Q4_K_M contre Q5, `num_gpu`, application seule.

## Issu de S0-8
- ~~S2 : remplacer les 10 positions de test Stockfish de S0-4 (plusieurs déséquilibrées) par des
  positions de milieu de partie tirées du corpus Heidoji, puis refaire les mesures tablette et PC.~~
  Fait en S2-2 (`testdata/positions/`, `docs/spikes/rapport-s2.md`).
- Supprimer `spike/llm/` du dépôt si le LLM embarqué n'est pas réévalué d'ici S13.

## Issu de S1-6
- Sauvegarde durable des préférences d'échiquier (« Montrer les coups légaux », orientation) avec
  DataStore ; aujourd'hui en mémoire et dans le `SavedStateHandle`.
- Animation du déplacement des pièces et retour animé de la pièce après un dépôt illégal.
- Préférence « promotion automatique en dame ».

## Issu de S1-7
- Éditer les variantes : supprimer, promouvoir en ligne principale, ajouter un commentaire ou un NAG
  (en respectant la limite de S1-4 sur les commentaires placés avant un coup).
- Navigation par balayage sur l'échiquier et par les flèches d'un clavier physique.
- Faire défiler automatiquement la liste des coups jusqu'au coup courant (longues parties).
- Messages d'erreur PGN en français (aujourd'hui le détail vient de `:core:chess`, en anglais).

## Issu de S1-8
- Écrire et relire les annotations dans le PGN (`[%csl Gd4]`, `[%cal Re2e4]`, format Lichess et
  Chess.com) pour les garder par coup et les exporter ; utile aux cours tirés des livres (S10).
- Flèches de cavalier en L (aujourd'hui droites) ; plus de couleurs (jaune).

## Issu de S2-3
- Analyse libre : jouer ou prévisualiser toute la variante d'une ligne (aujourd'hui le premier coup seulement), flèche du meilleur coup sur l'échiquier.
- Garder l'interrupteur d'analyse et envoyer l'historique des coups à Stockfish (répétitions).
- Fluidité : le glisser d'une pièce coûte ≈ 27 ms par image en build debug (S2-3) et jusqu'à ≈ 34 ms avec l'analyse ; mesurer en release et alléger le redessin de l'échiquier (cases et pièces) avant S13.

## Issu de S2-4
- Préchauffer Stockfish (réseau NNUE compris) à l'ouverture de la partie contre Stockfish : le premier coup coûte ≈ 2 s de plus.
- Mesurer la force réelle des réglages de Stockfish (ELO et niveau) contre les parties de Heidoji ou contre Maia (S3), pour afficher un ELO plus parlant que `UCI_Elo`.
- Partie contre Stockfish : pendule (S3), nulle proposée, revanche avec les mêmes réglages, enregistrement automatique des parties (S3).
