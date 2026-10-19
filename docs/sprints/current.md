# Sprint S1 — Échiquier et règles

**Durée** : 2 semaines · **Objectif** : des règles d'échecs exactes dans `:core:chess`, une
partie à deux joueurs sur l'écran et l'import/export PGN et FEN.

**Livrable démontrable** : sur la tablette, deux joueurs font une partie complète (roque, prise en
passant, promotion, fin de partie détectée), naviguent dans les coups en SAN, exportent la partie
en PGN et réimportent une partie de Heidoji.

**Critères d'acceptation du sprint** : la suite perft passe sur les positions de référence ; les
100 parties de Heidoji (`testdata/pgn/chesscom-heidoji-100.pgn`) sont réimportées à l'identique.

Sprint précédent : `docs/sprints/s0.md`.

---

## Décisions de planification (validées le 2026-10-19)
- `:feature:play` est créé dès S1 (S1-6) pour l'écran de partie à deux joueurs, au lieu de S3.
- Plateau en tableau 0x88 (pas de bitboards), hachage Zobrist ; partie modélisée en **arbre de
  coups** (variantes) dès S1. → ADR 0006.
- Nulles **automatiques** à la triple répétition et aux 50 coups (comme Lichess), sans
  réclamation ; le matériel insuffisant est ajouté aux fins de partie.
- « Réimporté à l'identique » : mêmes en-têtes, mêmes coups, mêmes commentaires après
  normalisation ; la mise en forme du fichier d'origine (espaces, retours à la ligne) ne compte pas.

## Stories

### S1-1 · Modèle de position et FEN complète
- [x] `Square`, `Move` (UCI), `CastlingRights`, `Position` immuable (plateau, trait, roques,
  case en passant, compteurs) dans `:core:chess`.
- [x] Lecture et écriture de la FEN complète (6 champs ; 4 champs acceptés, compteurs par défaut).
- [x] Validation : un roi par camp, pas de pion en 1re ou 8e rangée, droits de roque cohérents
  avec le roi et la tour, case en passant cohérente avec le trait et le pion qui vient d'avancer.
- [x] Clé Zobrist de la position (base de la triple répétition).
- [x] ADR 0006.

**Acceptation** : FEN → `Position` → FEN identique sur un jeu de positions ; FEN invalides
rejetées avec un message clair ; tests unitaires.

### S1-2 · Coups légaux et fins de partie
- [x] Générateur de coups légaux : promotions, roque (cases traversées non attaquées), prise en
  passant (y compris le clouage horizontal), échec et parade.
- [x] Jouer un coup → nouvelle `Position` (compteurs, droits de roque, clé Zobrist incrémentale).
- [x] Mat, pat, triple répétition, règle des 50 coups, matériel insuffisant.
- [x] Validation FEN complétée : le camp qui n'a pas le trait n'est pas en échec.

**Acceptation** : perft exact sur les 6 positions de référence de chessprogramming.org
(profondeurs rapides en CI, profondeurs longues dans un test marqué « lent ») ; tests de chaque
fin de partie.

### S1-3 · Notation SAN et UCI
- [x] SAN : écriture avec levée d'ambiguïté (colonne, rangée, case), `+` et `#`, `O-O`/`O-O-O`,
  promotion `=Q`.
- [x] SAN : lecture tolérante (`0-0`, `+` ou `#` absents ou faux, `e8Q`, annotations `!?`).
- [x] UCI : lecture et écriture, coup validé contre la liste des coups légaux.

**Acceptation** : aller-retour SAN sur des positions pièges (deux cavaliers, trois dames, prise
en passant avec échec, promotion avec mat) ; tests unitaires.

### S1-4 · Import et export PGN
- [x] Modèle de partie : en-têtes ordonnés, position de départ (`SetUp`/`FEN`), arbre de coups
  avec commentaires, NAG et variantes, résultat.
- [x] Lecteur PGN multi-parties (commentaires `{}` et `;`, `[%clk]` conservés, variantes
  imbriquées, échappements) ; chaque coup validé par les règles (règle 2), erreur localisée sinon.
- [x] Écriture PGN canonique (Seven Tag Roster en tête, lignes de 80 caractères au plus).

**Acceptation** : les 100 parties de Heidoji sont toutes légales et
import → export → import donne une partie identique ; tests de PGN avec variantes et erreurs.

### S1-5 · Moteurs branchés sur les règles
- [ ] `LegalMoveProvider` implémenté par `:core:chess` au lieu de `go perft 1` (dette S0-5).
- [ ] `Lc0Encoder` lit la position par `:core:chess` (fin de `FenFields`, règle 2).
- [ ] Mesure du coût d'un coup Maia complet sur la tablette (≈ 35 ms aujourd'hui, ≈ 4 ms visés).

**Acceptation** : tests Maia et moteur verts ; mesure consignée dans la note de story.

### S1-6 · Jouer sur l'échiquier
- [ ] Module `:feature:play` : écran « Partie » à deux joueurs, `GameViewModel` en `StateFlow`.
- [ ] Échiquier interactif dans `:core:ui` : toucher-toucher et glisser-déposer, coups légaux en
  surbrillance (désactivable), dernier coup, roi en échec.
- [ ] Choix de la pièce de promotion ; fin de partie affichée (mat, pat, nulles).

**Acceptation** : tests du ViewModel ; test Compose UI (mat du berger joué au toucher) ; vérifié
sur la tablette en paysage et en portrait, clair et sombre.

### S1-7 · Navigation, SAN et import/export dans l'application
- [ ] Liste des coups en SAN (coup courant surligné, toucher pour y aller), boutons début,
  précédent, suivant, fin ; jouer un coup depuis une position passée crée une variante.
- [ ] Retourner l'échiquier.
- [ ] Import et export PGN et FEN par le presse-papiers et par fichier (Storage Access Framework).

**Acceptation** : tests UI du parcours (importer une partie de Heidoji, naviguer, exporter) ;
vérifié sur la tablette.

### S1-8 · Flèches et cases colorées
- [ ] Mode « annoter » : toucher une case la colore, glisser d'une case à une autre trace une
  flèche ; trois couleurs ; un nouveau toucher efface.
- [ ] Annotations attachées à la position courante, effacées en changeant de coup.

**Acceptation** : tests de la logique d'annotation ; vérifié sur la tablette.

### S1-9 · Documentation et démo
- [ ] ADR et notes de sprint à jour ; idées hors périmètre reportées dans `docs/backlog.md`.
- [ ] Démo sur la tablette (partie complète, import Heidoji, export) ; capture `docs/sprints/s1-demo.png`.

**Acceptation** : sprint relu ; `ktlintCheck detekt test assembleDebug` verts.

---

## Notes de sprint
_(à compléter en fin de story : fait, dette, ajustements)_

**S1-1** — Fait : `Square` (classe valeur 0..63), `Move` (UCI), `CastlingRights` (4 bits, FEN canonique `KQkq`), `Position` immuable validée à la construction, `Zobrist` (SplitMix64 à graine fixe, case en passant comptée seulement si la prise est possible), objet `Fen` (lecture stricte 6 ou 4 champs, écriture canonique) qui remplace `FenParser` ; appelants migrés (`:core:ui`, `:core:engine`, `:app`), l'écran « Debug moteur » et le test des positions de benchmark valident maintenant la FEN complète. ADR 0006. 72 tests dans `:core:chess` (aller-retour FEN dont les 6 positions perft, 27 FEN invalides, Zobrist). `ktlintCheck detekt test assembleDebug` passent.
Dette : « le camp sans le trait n'est pas en échec » reporté à S1-2 (détection d'attaque) ; `Board` reste une liste de 64 cases (le 0x88 arrive avec le générateur) ; `FenFields` de `:core:engine` lit encore trait et roques lui-même (S1-5) ; clé Zobrist recalculée en entier (incrémentale en S1-2) ; pas de changement visible, APK non réinstallé sur la tablette.

**S1-2** — Fait : `Attacks` (cases attaquées et échec en 0x88), `MoveGenerator` (pseudo-légaux puis filtre « roi non attaqué après le coup », roque avec cases traversées), `Position.play` (immuable, lève `IllegalMoveException`, clé Zobrist incrémentale), `Position.legalMoves` mis en cache, `GameRules.status` (mat, pat, matériel insuffisant, 50 coups, triple répétition depuis le dernier coup irréversible ; le mat au 100e demi-coup gagne), `Perft` (`count`, `divide`). Nouveau contrôle FEN : le camp sans le trait n'est pas en échec. `Position` n'est plus une `data class` (constructeur privé, `Position.of` valide, `copy()` disparaît). Perft exact sur les 6 positions de chessprogramming.org et les 14 cas de Martin Sedlak à leur profondeur de référence : en CI (≈ 20 M nœuds, quelques secondes) et en test `slow` (`gradlew :core:chess:test -PslowTests`, 750 M nœuds en 73 s, 8 à 17 M nœuds/s sur le PC). Clé incrémentale vérifiée contre le recalcul complet sur 5 arbres de profondeur 3. 136 tests dans `:core:chess` ; `ktlintCheck detekt test assembleDebug` passent.
Décision : la case en passant est **toujours** écrite dans la FEN après une poussée de deux cases (norme FEN/PGN) ; Lichess et Stockfish ne l'écrivent que si la prise est possible, à normaliser lors des comparaisons (S1-4, S1-5).
Dette : une copie du plateau (64 cases) par coup essayé, assez rapide pour l'interface et Maia, vitesse sur la tablette non mesurée (S1-5) ; l'historique des clés est fourni par l'appelant (l'objet `Game` arrive en S1-4) ; pas de changement visible, APK non réinstallé.

**S1-3** — Fait : `San.write` (désambiguïsation calculée sur les seuls coups légaux, donc pas pour une pièce clouée ; `x`, `=Q`, `O-O`/`O-O-O`, `+`/`#`) avec `PieceLetters` anglais par défaut et français (R, D, T, F, C) pour l'affichage ; `San.parse` tolérant (`0-0`, `+`/`#` absents ou faux, annotations, `e.p.`, `e8Q`/`e8=q`/`e8(N)`, `Ng1f3`/`Ng1-f3`/`e2e4`, `x` absent ou `:`), lettres anglaises sensibles à la casse ; `Uci.parse` validé contre les coups légaux ; raccourcis `Position.toSan`, `playSan`, `playUci`. Erreurs distinctes : `InvalidMoveNotationException` (mal formé, ambigu, avec les coups candidats et la FEN) et `IllegalMoveException` (aucun coup légal). 64 tests, dont l'aller-retour SAN de tous les coups légaux sur 7 arbres de profondeur 2 (unicité vérifiée). 200 tests dans `:core:chess` ; `ktlintCheck detekt test assembleDebug` passent.
Dette : la lecture française n'existe pas (choix validé : affichage seulement) ; notation figurine (♘) non prévue ; pas de changement visible, APK non réinstallé.

**S1-4** — Fait : paquet `core.chess.pgn` : `Game` (en-têtes ordonnés, position de départ, arbre immuable de `MoveNode` dont le premier enfant est la ligne principale, résultat, `mainline`, `finalPosition`, `status()`), `PgnReader` (BOM, CRLF, échappements, commentaires `{}` et `;`, lignes `%`, numéros collés `1.e4`, NAG et suffixes `!?` → $1–$6, variantes imbriquées ; chaque coup validé par `San.parse`), `PgnWriter` (Seven Tag Roster en tête, `$n`, `N...` après commentaire ou variante, lignes ≤ 80 caractères). Fichier multi-parties : une partie fautive est écartée (`PgnImport.errors` : n° de partie, ligne, coup, motif) et la lecture continue ; avertissements pour résultat absent ou contradictoire et en-tête en double. Choix validés : en-têtes obligatoires complétés à la lecture, marqueur de fin qui l'emporte sur `Result`, `[%clk]` gardés en texte brut. **Critère du sprint tenu** : les 100 parties de Heidoji (4 495 demi-coups, lus en ≈ 75 ms sur le PC) sont légales, leur position finale égale l'en-tête `CurrentPosition` de Chess.com (4 premiers champs, case en passant comprise) et import → export → import redonne des parties identiques avec un texte stable. 223 tests dans `:core:chess` ; `ktlintCheck detekt test assembleDebug` passent.
Dette : un commentaire placé avant un coup au milieu d'une ligne ne peut venir que d'un début de partie ou d'après une variante (sinon il serait relu comme commentaire du coup précédent) : à respecter dans les éditions de S1-7 ; `}` dans un commentaire est remplacé par `)` à l'export ; un commentaire de plus de 80 caractères dépasse la limite de ligne ; horloges non structurées (S6) ; chaque nœud garde sa `Position` (mémoire à surveiller sur de très gros fichiers) ; pas de changement visible, APK non réinstallé.
