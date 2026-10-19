# ADR 0006 — Modèle d'échiquier : position immuable, 0x88, Zobrist, partie en arbre

- **Statut** : accepté (S1-1)
- **Date** : 2026-10-19

## Contexte
`:core:chess` devient la source de vérité des règles (règle 2) : FEN, coups légaux, SAN, PGN.
Ses clients sont l'interface (un coup à la fois), Maia (coups légaux d'une position, ≈ 1 ms visé)
et l'import de PGN (des milliers de coups). Le calcul profond reste à Stockfish.

## Décision
1. **API par cases** : `Square` (0..63, a1 = 0, h8 = 63, classe valeur), `Piece`, `Move` (départ,
   arrivée, promotion ; roque noté comme un coup de roi `e1g1`, comme en UCI). Pas de Chess960.
2. **`Position` immuable** : plateau, trait, droits de roque, case en passant, compteurs. Jouer un
   coup renvoie une nouvelle `Position`. Simple à partager entre coroutines et à mettre en
   `StateFlow`.
3. **Générateur en 0x88** (tableau de 128 cases, `index and 0x88 != 0` hors plateau) plutôt que des
   bitboards : plus simple à lire et à tester, largement assez rapide pour l'interface, Maia et
   l'import PGN. Le 0x88 reste interne au générateur (S1-2) ; l'API publique parle en `Square`.
4. **Hachage Zobrist** 64 bits, nombres tirés d'un SplitMix64 à graine fixe (reproductibles). La case
   en passant n'entre dans la clé que si un pion du camp au trait peut la prendre (convention
   Polyglot), pour que la triple répétition ne distingue pas deux positions identiques.
   Les clés ne sont **pas** celles de Polyglot : si un livre `.bin` est un jour lu, on ajoutera la
   table Polyglot à part.
5. **FEN stricte** : une FEN incohérente (deux rois, roque sans tour, case en passant impossible) est
   rejetée avec un message, jamais corrigée en silence. Les 4 champs EPD sont acceptés (compteurs
   0 et 1 par défaut).
6. **Partie en arbre de coups** dès S1 (S1-4) : chaque nœud porte un coup, ses commentaires et NAG ;
   le premier enfant est la ligne principale, les suivants des variantes. Le répertoire (S7) et les
   cours tirés des livres (S10) en ont besoin.

## Conséquences
- Une allocation par coup joué : négligeable à l'échelle de l'interface et de Maia, à mesurer sur
  l'import PGN (S1-4) et les puzzles (S5).
- Si un jour il faut des millions de nœuds par seconde en Kotlin (motifs tactiques du professeur),
  un générateur en bitboards pourra remplacer le 0x88 derrière la même API.
- Les FEN « sales » d'outils tiers (droits de roque erronés) sont refusées : l'import pourra proposer
  plus tard une correction explicite.
