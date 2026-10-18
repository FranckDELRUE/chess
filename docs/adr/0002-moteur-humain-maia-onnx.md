# ADR 0002 — Moteur humain : Maia-1 en ONNX Runtime, politique seule

- **Statut** : accepté (S0-8), proposé en S0-5
- **Date** : 2026-10-18

## Contexte
Les adversaires « humains » par niveau reposent sur Maia (réseaux lc0, GPL v3). Il faut les exécuter
hors ligne sur la tablette derrière `ChessEngine`, sans embarquer lc0. Mesures : `docs/spikes/rapport-s0.md`.

## Décision
1. **Maia-1** (une tranche d'Elo par réseau, 1100 à 1900), converti une fois sur le PC par
   `lc0 leela2onnx` (`tools/maia2onnx/`). Les poids et les `.onnx` ne sont pas versionnés.
2. **ONNX Runtime Android** (MIT), fournisseur CPU, 1 thread ; modèles lus depuis les assets.
3. **Politique seule** : une inférence par coup, sans recherche (comme lc0 `go nodes 1`, l'usage
   prévu par les auteurs de Maia).
4. Encodage d'entrée et table des 1858 coups **réécrits en Kotlin** d'après lc0 (`Lc0Encoder`,
   `Lc0PolicyMap`), validés contre lc0 lui-même par un test de bout en bout.
5. Les coups illégaux sont masqués avant le softmax, à partir d'un `LegalMoveProvider` :
   Stockfish `go perft 1` pendant le spike, le générateur de `:core:chess` à partir de S1.
6. `MaiaEngine` implémente `ChessEngine` ; `depth` est ignoré ; option `Temperature`
   (0 = coup le plus probable, sinon tirage).

## Conséquences
- 1,5 à 3,6 ms par coup, +7 Mo de mémoire par réseau : aucune contrainte de performance.
- APK +41 Mo (ONNX Runtime 33 Mo) : à réduire par une build minimale avant toute diffusion.
- Maia-2 (Elo continu, PyTorch) reste possible plus tard avec le même runtime, mais demanderait un
  autre encodage ; non retenu pour la v1.
- Plan B documenté (Maia sur le PC via lc0 CUDA, ou Stockfish « humanisé »), non nécessaire.
  S0-7 l'a mesuré : lc0 CUDA met 4 à 14 ms par coup sur la RTX 4080 portable, plus lent que le CPU
  (≈ 1,5 ms) ; le réseau est trop petit pour le GPU. Maia reste donc sur la tablette.
