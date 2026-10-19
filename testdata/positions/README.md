# Positions de test

Fichiers générés, à ne pas modifier à la main (des tests vérifient qu'ils correspondent à leur règle).

- `heidoji-candidates.txt` : étape 1 de S2-2, sans moteur (`HeidojiSelection`, tests de `:app`).
  Pour chaque partie de `../pgn/chesscom-heidoji-100.pgn`, dans l'ordre du fichier, les positions des
  demi-coups 30 à 40 sans échec au roi, à matériel égal à un pion près (1/3/3/5/9), avec une dame ou
  deux tours dans chaque camp. Régénérer :
  `WRITE_HEIDOJI_CANDIDATES=1 gradlew :app:testDebugUnitTest --tests "*HeidojiPositionsTest*"`.
- `heidoji-middlegames.txt` : étape 2, Stockfish 19 sur le PC (`station-pc`,
  `python -m bench.select_positions`, 1 thread, déterministe). La première candidate de chaque partie
  évaluée à ±150 cp au plus à profondeur 18, pour les 20 premières parties. Positions de mesure de
  Stockfish sur la tablette (`StockfishBenchmark`, `BenchmarkPositions.FENS`) et sur le PC
  (`python -m bench.stockfish`).

Format : `FEN ; game <n> ply <p>`, où `n` est le rang de la partie dans le PGN et `p` le nombre de
demi-coups joués ; les lignes qui commencent par `#` sont des commentaires.
