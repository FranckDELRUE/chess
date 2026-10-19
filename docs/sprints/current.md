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
- [ ] `ChessEngine` : MultiPV, `go movetime`, `go nodes`, `searchmoves` ; options de force
  (`Skill Level`, `UCI_LimitStrength`, `UCI_Elo`).
- [ ] Score ramené au point de vue des blancs ; conversion en probabilité de gain pour la barre.
- [ ] `EngineScheduler` : tâches à priorité ; une tâche plus prioritaire interrompt (`stop`) une
  recherche moins prioritaire, qui reprend ensuite ; chaque tâche applique ses propres options.
- [ ] ADR 0008.

**Acceptation** : tests JVM avec le faux transport (MultiPV, préemption, reprise, options
rétablies entre deux tâches, annulation) ; test instrumenté de MultiPV 3 sur la tablette.

### S2-2 · Positions de test du corpus Heidoji et nouvelles mesures
- [ ] Environ 20 positions de milieu de partie tirées de `testdata/pgn/chesscom-heidoji-100.pgn`
  par `PgnReader`, selon une règle écrite et reproductible ; elles remplacent les 10 positions
  de S0-4 (`BenchmarkPositions`).
- [ ] Mesures P16, P18, P20 sur la tablette (4 threads) et sur la station PC (`station-pc/bench`).

**Acceptation** : P18 sous 5 s sur chaque position (sinon, écart signalé avant tout réglage) ;
mesures consignées dans `docs/spikes/rapport-s0.md` ou un rapport S2.

### S2-3 · Analyse libre : barre d'évaluation et 3 lignes
- [ ] Interrupteur « Analyse » dans `:feature:play` : barre d'évaluation verticale, 3 lignes en
  SAN française avec profondeur et score.
- [ ] L'analyse suit la navigation dans les coups ; la recherche en cours est annulée quand la
  position change.
- [ ] Toucher une ligne la joue dans l'arbre de variantes.

**Acceptation** : tests du ViewModel (Turbine) ; test Compose UI du câblage ; vérifié sur la
tablette en paysage et en portrait, clair et sombre, sans saccade.

### S2-4 · Adversaire Stockfish réglable
- [ ] Écran « Nouvelle partie » : couleur, niveau (`Skill Level` ou limite d'ELO), temps par coup.
- [ ] `GameSession` et l'écran de partie réutilisés, un joueur étant le moteur ; le coup du moteur
  est validé par `:core:chess` avant d'être joué (règle 2).
- [ ] Analyse masquée pendant la partie, disponible à la fin ; reprendre un coup, abandonner.

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
