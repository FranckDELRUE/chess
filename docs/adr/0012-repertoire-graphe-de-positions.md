# ADR 0012 — Le répertoire est un graphe de positions

- **Statut** : accepté
- **Date** : 2026-11-20

## Contexte
Le répertoire (S7) se saisit sur l'échiquier ou s'importe de PGN et d'études, puis S8 le fait réviser.
Le critère du sprint demande que « les transpositions pointent vers le même nœud ». L'arbre de coups de
`:core:chess` (`MoveNode`, ADR 0006) ne le permet pas : un même coup atteint par deux ordres différents
y apparaît deux fois, avec deux commentaires et, en S8, deux cartes de révision pour la même position.

## Décision
- Un **répertoire** est un graphe orienté : les **nœuds** sont des positions, les **arêtes** des coups
  (`RepertoireEdge` : origine, destination, coup, SAN). Il est immuable et en Kotlin pur, dans
  `:core:chess.repertoire` pour que `:core:srs` s'en serve sans dépendre d'Android.
- **Identité d'un nœud** (`PositionId`) : clé Zobrist, vérifiée par une FEN canonique (placement, trait,
  roques, case en passant seulement si la prise est possible, sans compteurs de coups). Deux chemins
  qui transposent donnent le même identifiant ; deux positions différentes n'en partagent jamais un,
  même si leurs clés entraient en collision.
- **Identité d'une arête** : (nœud d'origine, coup). Rejouer un coup existant ne crée rien.
- Le graphe **peut contenir des cycles** (une position qui se répète) : tout parcours retient les nœuds
  déjà vus.
- **Fusion** (`merge`) : toutes les lignes d'une `Game` (ligne principale et variantes) sont ajoutées sans
  doublon ; la partie doit partir de la position du répertoire. L'ordre des coups d'une position est
  l'ordre d'ajout.
- Les coups sont déjà validés par `PgnReader` (règle 2) ; l'ajout sur l'échiquier (`add`) les valide lui-même.
- **Annotations** (commentaire, flèches, source) : portées par le **nœud**, pas par l'arête, pour que
  les chemins qui transposent partagent la même note. S7-2 les stocke, S7-4 les édite. Le commentaire
  d'un coup dans un PGN est rattaché à la position qu'il atteint.

## Conséquences
- Une position a une seule carte de révision en S8, quel que soit le chemin pour y arriver.
- Une ligne du répertoire n'est plus une suite de nœuds unique : l'affichage en liste de coups parcourt
  le graphe et choisit un chemin.
- Le stockage Room (S7-2) aura une table de nœuds clés par `PositionId` et une table d'arêtes.
- Le nœud garde la `Position` atteinte la première fois ; ses compteurs (demi-coups, numéro de coup)
  dépendent de ce chemin et ne servent pas.
- Fusion et ajout recopient les index (coût proportionnel à la taille) ; acceptable pour des répertoires
  de quelques milliers de positions, à mesurer lors de l'import de S7-5.
