# Sprint S2 — Moteur Stockfish

**Durée** : 2 semaines · **Objectif** : Stockfish au service de l'application : analyse en direct,
adversaire réglable et analyses en arrière-plan, sur un moteur unique partagé.

**Livrable démontrable** : sur la tablette, une partie contre Stockfish à un niveau choisi, puis
l'analyse libre de cette partie (barre d'évaluation et 3 meilleures lignes) et son analyse
complète lancée en arrière-plan, annulable.

**Critères d'acceptation du sprint** : analyse à profondeur 18 en moins de 5 s sur les positions
de test du corpus Heidoji (tablette, 4 threads) ; aucun blocage de l'UI (aucun ANR, pas de saccade
visible de l'échiquier pendant une analyse).

Sprint précédent : `docs/sprints/s1.md`.

---

## Décisions de planification (validées le 2026-10-19)
- **Un seul Stockfish** dans l'application (ADR 0004 : une session par processus). L'analyse en
  direct, l'adversaire et la file en arrière-plan le partagent via un ordonnanceur à priorités :
  adversaire > analyse en direct > arrière-plan. → ADR 0008.
- **Force de l'adversaire** : deux réglages proposés, `Skill Level` (0 à 20) et limite d'ELO
  (`UCI_Elo`, 1320 à 3190), plus le temps par coup.
- **Écart de périmètre assumé** : la base Room (`:core:data`) reçoit dès S2 une table
  `position_analysis` pour stocker les résultats de la file d'analyses ; la revue de parties
  complète reste en S6.
- Nouvelle dépendance : `androidx.work` (Apache 2.0).
- Leçons de S1 appliquées : tablette branchée et déverrouillée avant chaque story d'interface,
  démo courte en fin de story ; écrans et ViewModels découpés tôt (detekt) ; toute valeur de
  référence vient d'un outil (Stockfish, `:core:chess`), jamais de la mémoire.

## Stories

### S2-1 · API moteur et partage du moteur unique
- [x] `ChessEngine` : MultiPV, `go movetime`, `go nodes`, `searchmoves` ; options de force
  (`Skill Level`, `UCI_LimitStrength`, `UCI_Elo`).
- [x] Score ramené au point de vue des blancs ; conversion en probabilité de gain pour la barre.
- [x] `EngineScheduler` : tâches à priorité ; une tâche plus prioritaire interrompt (`stop`) une
  recherche moins prioritaire, qui reprend ensuite ; chaque tâche applique ses propres options.
- [x] ADR 0008.

**Acceptation** : tests JVM avec le faux transport (MultiPV, préemption, reprise, options
rétablies entre deux tâches, annulation) ; test instrumenté de MultiPV 3 sur la tablette.

### S2-2 · Positions de test du corpus Heidoji et nouvelles mesures
- [x] Environ 20 positions de milieu de partie tirées de `testdata/pgn/chesscom-heidoji-100.pgn`
  par `PgnReader`, selon une règle écrite et reproductible ; elles remplacent les 10 positions
  de S0-4 (`BenchmarkPositions`).
- [x] Mesures P16, P18, P20 sur la tablette (4 threads) et sur la station PC (`station-pc/bench`).

**Acceptation** : P18 sous 5 s sur chaque position (sinon, écart signalé avant tout réglage) ;
mesures consignées dans `docs/spikes/rapport-s0.md` ou un rapport S2.

### S2-3 · Analyse libre : barre d'évaluation et 3 lignes
- [x] Interrupteur « Analyse » dans `:feature:play` : barre d'évaluation verticale, 3 lignes en
  SAN française avec profondeur et score.
- [x] L'analyse suit la navigation dans les coups ; la recherche en cours est annulée quand la
  position change.
- [x] Toucher une ligne la joue dans l'arbre de variantes.

**Acceptation** : tests du ViewModel (Turbine) ; test Compose UI du câblage ; vérifié sur la
tablette en paysage et en portrait, clair et sombre, sans saccade.

### S2-4 · Adversaire Stockfish réglable
- [x] Écran « Nouvelle partie » : couleur, niveau (`Skill Level` ou limite d'ELO), temps par coup.
- [x] `GameSession` et l'écran de partie réutilisés, un joueur étant le moteur ; le coup du moteur
  est validé par `:core:chess` avant d'être joué (règle 2).
- [x] Analyse masquée pendant la partie, disponible à la fin ; reprendre un coup, abandonner.

**Acceptation** : tests du ViewModel (coup du moteur, reprise, abandon, fin de partie) ; test UI
d'une partie courte contre un faux moteur ; vérifié sur la tablette.

### S2-5 · File d'analyses en arrière-plan
- [ ] « Analyser la partie » : chaque coup analysé à profondeur 18 par un worker WorkManager au
  premier plan (notification de progression, continue écran éteint), annulable.
- [ ] Résultats dans Room (`:core:data`, table `position_analysis` indexée par clé Zobrist et
  profondeur), réutilisés par l'analyse en direct.
- [ ] La file cède le moteur à l'analyse en direct et à l'adversaire (S2-1).

**Acceptation** : tests Room in-memory ; tests du worker (progression, annulation, reprise) ;
une partie de Heidoji analysée sur la tablette, annulation vérifiée.

### S2-6 · Écrans de debug masqués en release
- [ ] Écrans « Debug moteur » et Maia visibles en debug seulement.

**Acceptation** : `assembleRelease` passe ; test de la navigation sans les entrées de debug.

### S2-7 · Documentation et démo
- [ ] ADR et notes de sprint à jour ; idées hors périmètre reportées dans `docs/backlog.md`.
- [ ] Démo sur la tablette ; capture `docs/sprints/s2-demo.png` ; rétrospective ; archivage en
  `docs/sprints/s2.md`.

**Acceptation** : sprint relu ; `ktlintCheck detekt test assembleDebug` verts.

---

## Notes de sprint
_(à compléter en fin de story : fait, dette, ajustements)_

**S2-1** — Fait : `SearchRequest` (FEN + coups UCI, limite `Depth`/`MoveTime`/`Nodes`, `searchmoves`, MultiPV, force, `newGame`) remplace la surcharge `analyze(fen, depth, searchMoves)` ; `EngineStrength` (`Full`, `Skill` 0–20, `Elo` 1320–3190, bornes relues dans `engine.cpp` et `search.h` de Stockfish 19). `Score.forWhite`, `winningChances`, `winProbability` : formule de Lichess relue dans `ui/lib/src/ceval/winningChances.ts` (lila) et `eval.scala` (scalachess). `UciEngine` envoie `position fen … moves …`, n'envoie que les options qui changent et ne coupe plus jamais un échange au milieu : lectures non annulables, `stop` envoyé dès l'annulation par un observateur, sortie lue jusqu'au `bestmove`. `EngineScheduler` (priorités `OPPONENT > LIVE > BACKGROUND`, préemption par relance avec `EngineEvent.Restarted`, démarrage paresseux) fourni par Hilt (`EngineModule`, Hilt ajouté à `:core:engine`) ; `EngineProvider` supprimé, écran de debug et benchmark Maia migrés en priorité `BACKGROUND`. `MaiaEngine` accepte une `SearchRequest` (coups joués par `:core:chess`). ADR 0008, `CLAUDE.md` mis à jour. Tests : `:core:engine` passe de 31 à 56 tests JVM (faux moteur à recherches longues, préemption, ordre, annulations, options rétablies ; une mutation qui désactive la préemption fait échouer 2 tests) ; premier `androidTest` de `:core:engine`, vert sur la tablette : MultiPV 3 à profondeur 12 en 2,0 s (3 premiers coups distincts à chaque profondeur), coup adverse de 500 ms (`UCI_Elo` 1500) rendu en 0,69 s en interrompant une recherche de fond de 30 s. Dépendances de test : `androidx.test:runner` 1.6.2 et `androidx.test.ext:junit` 1.2.1 (Apache 2.0). `ktlintCheck detekt test assembleDebug` passent.
Hors story : `PlayScreenTest` (S1-7) échouait sur ce poste parce que git y extrait le corpus PGN en CRLF (`core.autocrlf=true`) ; le test normalise maintenant les fins de ligne.
Dette : une préemption juste après l'attribution du moteur, avant le lancement de la recherche, émet un `Restarted` sans recherche réellement interrompue (sans effet visible) ; `.gitattributes` pour fixer les fins de ligne de `testdata/` à voir ; pas de changement visible dans l'app, écran de debug non revérifié à la main sur la tablette (couvert par le test instrumenté du planificateur).

**S2-2** — Fait : 20 milieux de partie de Heidoji choisis par une règle reproductible en deux étapes (`testdata/positions/`, README). Étape 1 : `HeidojiSelection` (tests de `:app`), 261 candidates. Étape 2 : `station-pc/bench/select_positions.py`, Stockfish à 1 thread, ±150 cp à profondeur 18. Les tests vérifient que les fichiers correspondent à la règle et que `BenchmarkPositions.FENS` est identique au fichier. Le fichier est partagé par la tablette (`BenchmarkPositions`, assets du `androidTest` de `:core:engine`) et le PC (`bench/common.py`, `sf_fens()`). Le benchmark tablette est un test instrumenté `StockfishBenchmark`, exclu par défaut et lancé avec `-PengineBenchmark` ; il passe par l'`EngineScheduler`, avec 3 passes à P18 et un échauffement non chronométré. Écran de debug : lot de 20×3. Mesures et verdict dans `docs/spikes/rapport-s2.md` :
- P18 en médiane 2,05 s ; toutes les médianes par position restent sous 5 s, mais la pire vaut **4,96 s**, et 2 passes isolées sur 60 dépassent 5 s (jusqu'à 6,1 s). La deuxième série donne le même résultat.
- PC : P30 en médiane 17,4 s.

Critère du sprint tenu au sens validé (médiane par position), **sans marge**. Pour S2-3, afficher les lignes au fil des profondeurs sans attendre P18. Entrée du backlog issue de S0-8 rayée. `ktlintCheck detekt test assembleDebug` et pytest de la station passent.
Écart validé : l'étape 1 ne gardait qu'un demi-coup par partie et ne donnait que 19 positions équilibrées ; elle liste maintenant tous les demi-coups admissibles de 30 à 40, avec toujours une position par partie.
Leçon : le tampon logcat de la tablette (256 Kio) perd les premières lignes d'un lot ; il faut lancer `adb logcat -G 4M` avant (c'est noté dans le KDoc du benchmark).
Dette : 4 et 6 threads non mesurés ; le benchmark ne publie ses résultats que dans logcat (pas de fichier).

**S2-3** — Fait et vérifié sur la tablette le 2026-10-20. `AnalysisState` et `AnalysisReducer` purs (une ligne ne remplace celle de son rang que si elle est au moins aussi profonde, `Restarted` garde les lignes affichées, SAN français par `:core:chess` limité aux 8 demi-coups affichés, variante coupée au premier coup illégal, scores ramenés aux blancs, format `+0,4` / `M3`). `AnalysisController` : MultiPV 3, profondeur 24, priorité `LIVE` via `PositionAnalyzer` (module Hilt `AnalysisModule`), annulation à chaque nouvelle position, attente de 150 ms pendant la navigation rapide, pas de recherche si la partie est terminée, erreur du moteur affichée sans planter ; les résultats sont assemblés hors du thread principal (`workDispatcher`) et n'arrivent à l'écran qu'au plus toutes les 250 ms. `PlayViewModel` : actions `ToggleAnalysis` et `PlayAnalysisMove` (le coup d'une ligne n'est joué que s'il est légal sur la position pour laquelle la ligne a été calculée) ; l'état de l'analyse est un flux à part (`analysisState`), pour ne pas reconstruire l'échiquier et la liste des coups à chaque mise à jour. Interface : `EvalBar` à gauche de l'échiquier (de sa hauteur, blanc en bas, inversée si l'échiquier est retourné), `AnalysisPanel` (interrupteur, profondeur, 3 lignes à toucher). `:feature:play` dépend maintenant de `:core:engine`. Tests : `:feature:play` passe de 31 à 60 tests (reducer 10, contrôleur 10, ViewModel avec faux moteur 8, parcours Robolectric : interrupteur, barre, ligne jouée). `ktlintCheck detekt test assembleDebug` passent.
Démo sur la tablette (build debug, paysage sombre, portrait sombre et clair, échiquier retourné) : la barre a la hauteur de l'échiquier dans les deux orientations et s'inverse avec lui ; 3 lignes en SAN français qui s'affinent (profondeur 16 en ≈ 5 s, 21 en ≈ 10 s, 24 en ≈ 20 s) ; toucher une ligne joue son premier coup (la ligne jouée est celle affichée à cet instant, le classement évolue pendant la recherche) ; l'analyse repart sur la nouvelle position après un coup ou une navigation et survit à la rotation. Aucun ANR ; la seule pause de 0,85 à 1 s (`Davey`) est celle du démarrage de l'application, pas de l'analyse.
Fluidité mesurée (`dumpsys gfxinfo`, glisser e2-e4 puis 6 s d'analyse, build debug) : médiane 32 à 34 ms par image avec l'analyse, contre 27 ms pour le même glisser sans analyse ; 70 % des images dépassent 16 ms avec l'analyse (12 à 20 % sans), surtout dans la première seconde qui suit un changement de position. Le moteur occupé ne ralentit pas l'interface (toucher l'interrupteur « Annoter » : médiane 9 ms, moteur occupé ou non). Trois essais d'optimisation (assemblage hors du thread principal, flux séparé, 250 ms entre deux mises à jour) n'ont pas changé le résultat de façon nette : le coût dominant est le redessin de l'échiquier d'S1 (≈ 27 ms par image de glisser), pas l'analyse.
Dette : mesurer en build release (le build debug n'est pas optimisé) et alléger le redessin de l'échiquier, avant S13 (backlog) ; seul le premier coup d'une ligne se joue ; l'interrupteur n'est pas sauvegardé (éteint à chaque lancement) ; la position est envoyée sans l'historique des coups, donc Stockfish ignore les répétitions en analyse ; l'analyse s'arrête à la profondeur 24.

**S2-4** — Fait et vérifié sur la tablette le 2026-10-20. Même écran et même `GameSession` que la partie à deux joueurs (ADR 0009) : bouton d'accueil « Partie contre Stockfish » (route `play?vs=engine`), boîte de réglages (Blancs / Noirs / Au hasard ; limite d'ELO 1320 à 3190 ou niveau 0 à 20 ; 0,5 / 1 / 3 / 10 s par coup ; défaut ELO 1500 et 1 s ; note sur l'échelle d'ELO de Stockfish). `OpponentController` : Stockfish joue à son trait, partie non terminée, quand la position affichée termine la ligne ou que le joueur vient de jouer (naviguer dans une ancienne position ne le fait jamais jouer) ; la recherche reçoit la FEN de départ et tous les coups ; son coup est vérifié par `:core:chess` avant d'être joué (illégal : message, pas de nouvel essai en boucle) ; priorité `OPPONENT`. Le joueur ne peut pas jouer sur le trait du moteur. Reprendre un coup (retour au dernier trait du joueur, les coups repris restent en variante), abandon (résultat à Stockfish dans le PGN, bouton grisé ensuite), analyse masquée pendant la partie et de retour à la fin, « Stockfish réfléchit… » pendant la recherche, plateau retourné quand on joue les Noirs, joueurs et force dans les étiquettes PGN (`Event`, `White`, `Black`). Réglages, camp et abandon sont sauvegardés avec la partie : après la mort du processus, la partie revient avec ses réglages (vérifié sur la tablette).
**Défaut trouvé et corrigé dans `UciEngine` (S2-1)** : une annulation arrivée pendant l'envoi de `go` ne démarrait pas l'observateur du `stop` ; la recherche courait jusqu'à sa fin naturelle (30 s dans un test instrumenté, après ≈ 5 % des essais), en retenant le moteur et donc l'adversaire. La boucle de lecture vérifie maintenant `isActive`. Test JVM qui reproduit la course (il échoue sans la correction), faux moteur qui échoue au lieu de bloquer ; le test instrumenté de préemption est revenu de 30,7 s à 0,68 s (3 essais).
Tests : `:feature:play` passe de 60 à 82 tests (adversaire 17 avec un faux moteur, réglages 4, parcours Robolectric : réglages, coup, reprise, abandon) ; `:core:engine` 57 ; test instrumenté : un coup de Stockfish à ELO 1500 est légal et rendu dans le temps imparti. `ktlintCheck detekt test assembleDebug` passent.
Écart au plan : « Stockfish se prépare… » n'est pas affiché (le premier coup de la session coûte ≈ 2 s de plus, démarrage de Stockfish et chargement du réseau : le texte « Stockfish réfléchit… » couvre ce cas). Ajouts non prévus : retournement automatique du plateau côté Noirs.
Dette : préchauffer Stockfish à l'ouverture de la partie ; pas de pendule (S3) ; `Hasard` n'est pas testé de façon déterministe ; le niveau 0 à 20 et l'ELO ne sont pas calibrés sur Chess.com ou Lichess.
