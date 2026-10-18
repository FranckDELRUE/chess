# Corpus de parties réelles

Dépose ici tes parties au format PGN. Elles servent aux tests d'import (S1), puis à la
détection de motifs du professeur (S4).

- **Lichess** : https://lichess.org/@/Heidoji/download (ou Profil → Exporter les parties).
- **Chess.com** : https://www.chess.com/games/archive/Heidoji → sélectionner → Télécharger.

Nommage conseillé : `lichess-2026-10.pgn`, `chesscom-2026-10.pgn`.

## Corpus S0-8

`chesscom-heidoji-100.pgn` : 100 parties de Heidoji sur Chess.com (octobre 2019 → mars 2024),
récupérées le 2026-10-18 par l'API publique (`api.chess.com/pub/player/heidoji/games/{année}/{mois}/pgn`).
Le compte Lichess n'a aucune partie (seulement des puzzles).

Sélection : les 100 plus récentes en échecs standard, sans variante (4 parties Chess960 écartées)
ni partie sans coup (abandons au départ). Fichier non modifié par ailleurs : en-têtes Chess.com,
horloges en commentaires `{[%clk …]}`. Contenu : 1 à 167 demi-coups (médiane 39), 55 parties
avec les Blancs, 39 mats, 33 au temps, 22 abandons, 2 nulles par répétition ; cadences
quotidiennes (52), 10 min (21), 15+10 (17), 60 min (4).

La légalité des coups n'est pas encore vérifiée : c'est le test d'import de S1 (`:core:chess`).
