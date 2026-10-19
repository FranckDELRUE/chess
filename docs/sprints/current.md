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
- [x] `LegalMoveProvider` implémenté par `:core:chess` au lieu de `go perft 1` (dette S0-5).
- [x] `Lc0Encoder` lit la position par `:core:chess` (fin de `FenFields`, règle 2).
- [ ] Mesure du coût d'un coup Maia complet sur la tablette (≈ 35 ms aujourd'hui, ≈ 4 ms visés).

**Acceptation** : tests Maia et moteur verts ; mesure consignée dans la note de story.

### S1-6 · Jouer sur l'échiquier
- [x] Module `:feature:play` : écran « Partie » à deux joueurs, `GameViewModel` en `StateFlow`.
- [x] Échiquier interactif dans `:core:ui` : toucher-toucher et glisser-déposer, coups légaux en
  surbrillance (désactivable), dernier coup, roi en échec.
- [x] Choix de la pièce de promotion ; fin de partie affichée (mat, pat, nulles).

**Acceptation** : tests du ViewModel ; test Compose UI (mat du berger joué au toucher) ; vérifié
sur la tablette en paysage et en portrait, clair et sombre.

### S1-7 · Navigation, SAN et import/export dans l'application
- [x] Liste des coups en SAN (coup courant surligné, toucher pour y aller), boutons début,
  précédent, suivant, fin ; jouer un coup depuis une position passée crée une variante.
- [x] Retourner l'échiquier.
- [x] Import et export PGN et FEN par le presse-papiers et par fichier (Storage Access Framework).

**Acceptation** : tests UI du parcours (importer une partie de Heidoji, naviguer, exporter) ;
vérifié sur la tablette.

### S1-8 · Flèches et cases colorées
- [x] Mode « annoter » : toucher une case la colore, glisser d'une case à une autre trace une
  flèche ; trois couleurs ; un nouveau toucher efface.
- [x] Annotations attachées à la position courante, effacées en changeant de coup.

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

**S1-5** — Fait (sauf la mesure sur la tablette) : `RulesLegalMoveProvider` (coups légaux de `:core:chess` en UCI, FEN invalide refusée), valeur par défaut de `MaiaEngine` ; `Lc0Encoder.encode(Position)` (surcharge `encode(fen)` qui passe par `Fen.parse`) et `PolicyDecoder` sur `Position` : `FenFields` supprimé (roques via `CastlingRights`, case en passant, `halfmoveClock`) ; `go perft 1` retiré de `UciEngine` et `UciParser` (décision validée) ; `MaiaBenchmark` contrôle la légalité par les règles et affiche `legal_median`. Contrôle croisé : sur les 20 positions de `maia-reference.tsv`, nos coups légaux sont exactement ceux de lc0 ; `MaiaPipelineTest` (réseaux présents en local) retrouve les priors de lc0. Coups légaux ≈ 9 µs par position sur le PC (FEN lue comprise), contre ≈ 30 ms avec Stockfish. `ktlintCheck detekt test assembleDebug` passent ; APK installé sur la tablette.
Dette : **mesure du coût d'un coup Maia complet sur la tablette non faite** (tablette verrouillée) : lancer « Lot Maia 20×3 » dans « Debug moteur », lire `adb logcat -s MAIA_BENCH` (`legal_median`, temps d'inférence), consigner ici et dans `docs/spikes/rapport-s0.md`, puis cocher la case — prévue le 2026-10-19 au soir. La FEN est lue deux fois par coup Maia (moteur puis fournisseur), négligeable.

**S1-6** — Fait : `GameSession` dans `:core:chess` (partie + chemin du coup courant ; `play` suit un coup existant ou l'ajoute en variante, `status()` avec répétition, `toGame()` qui fixe le résultat pour le PGN). `:core:ui` : `ChessBoard` interactif sans état métier (toucher, glisser-déposer avec la pièce sous le doigt, case sélectionnée, cibles en point ou en anneau, dernier coup, roi en échec, description accessible « e4, cavalier blanc » et `testTag` par case) et `PromotionDialog` ; l'ancien `ChessBoard(fen)` reste pour l'accueil. Nouveau module `:feature:play` (décision validée) : `MoveInput` (machine à états immuable : sélection, coup, promotion en attente, plus rien après la fin), `PlayViewModel` (Hilt, `StateFlow<PlayUiState>`, partie gardée en PGN dans le `SavedStateHandle`), `PlayScreen` paysage/portrait (statut : trait, échec, mat, pat, trois nulles ; « Nouvelle partie », « Retourner », interrupteur « Montrer les coups légaux ») ; bouton « Partie à deux joueurs » sur l'accueil. Tests : 7 `GameSession`, 11 ViewModel, 1 test UI Compose sous Robolectric (mat du berger joué au toucher, message de mat vérifié), `squareAtOffset`. Dépendances de test ajoutées (choix validé) : Robolectric 4.16 (MIT), `compose-ui-test-junit4` (Apache 2.0), JUnit 4.13.2 (EPL 1.0, tests seulement, jamais dans l'APK) et JUnit Vintage. `ktlintCheck detekt test assembleDebug` passent ; APK installé sur la tablette.
Contournement : Robolectric ne charge pas sa bibliothèque native depuis un chemin contenant une espace (`C:\Users\Franck DELRUE`) ; `robolectric.userHome` dans `local.properties` (propre à la machine, non versionné) déplace son cache, voir README. Robolectric tourne en SDK 35 (`@Config`), le SDK 37 n'étant pas garanti.
Dette : **vérification sur la tablette non faite** (tablette verrouillée) : paysage, portrait, mode sombre, glisser-déposer, promotion — à faire ce soir avec la mesure de S1-5 ; préférences non persistées et pas d'animation (backlog).

**S1-7** — Fait : `GameSession` navigue (`toStart`, `back`, `forward`, `toEnd`, `goTo`, `nextMoves`) ; `toGame()` garde le résultat lu dans le PGN quand la ligne ne finit ni par un mat ni par une nulle (abandon, temps). `:feature:play` : `MoveListBuilder` (arbre → liste affichable, **notation française** validée, numéros « 12... » après une variante ou un commentaire, variantes imbriquées, NAG en `!?`, commandes `[%clk]` masquées) ; liste des coups cliquable avec coup courant surligné et variantes en plus petit ; boutons ⏮ ◀ ▶ ⏭ ; jouer depuis une position passée crée une variante. Import : « Coller (PGN ou FEN) » (une FEN démarre une partie depuis la position, sinon lecture PGN), « Ouvrir… » par le Storage Access Framework ; fichier multi-parties → choix de la partie, message « n parties importées, e en erreur ». Export : « Copier le PGN », « Copier la FEN », « Enregistrer… » (SAF). `PlayViewModel` passe à un point d'entrée unique `onAction(PlayAction)` (UDF, actions groupées échiquier / navigation / documents) ; fichiers via `GameDocuments` (Hilt, `ContentResolver`, à déplacer dans `:core:data` en S6). Tests : `GameSession` (10), `MoveListBuilder` (5), ViewModel (19, dont navigation, variante, import simple / multiple / FEN / texte illisible, fichiers ouverts et enregistrés), 2 tests UI Robolectric dont le parcours d'acceptation (coller une partie de Heidoji, naviguer, « Copier le PGN » → partie identique). `ktlintCheck detekt test assembleDebug` passent ; APK installé sur la tablette.
Choix validés : notation française dans la liste ; édition des variantes, balayage et clavier au backlog.
Dette : **vérification sur la tablette non faite** (à faire avec S1-5 et S1-6 : ouvrir et enregistrer un vrai `.pgn`) ; détail des erreurs PGN en anglais ; pas de défilement automatique vers le coup courant (backlog).

**S1-8** — Fait : `:core:ui` : `BoardAnnotations` (cases colorées et flèches, immuable ; même couleur = efface, autre couleur = remplace ; flèche vers sa propre case = case marquée), trois couleurs vert, rouge, bleu (choix validé) ; `ChessBoard` dessine les cases marquées sous les pièces et les flèches au-dessus (droites, cavalier compris), `DragMode.ARROW` trace depuis n'importe quelle case avec une flèche d'aperçu sous le doigt ; description accessible « e4, marquée en rouge » ; gestes extraits dans `BoardGestures` (seul état gardé : le glissement en cours). `:feature:play` : mode « Annoter » explicite (choix validé, pas d'appui long) avec interrupteur, trois pastilles de couleur et « Effacer les annotations » ; `AnnotationInput` (machine à états immuable) intercepte toucher et dépôt quand le mode est actif ; annotations éphémères (choix validé), effacées dès que la position affichée change. Tests : `BoardAnnotations` (3), centre des cases (1), ViewModel (4 : marquer sans sélectionner, flèche sans jouer, sortie du mode, effacement au changement de position), 1 test UI Robolectric (case marquée en rouge puis effacée). `ktlintCheck detekt test assembleDebug` passent ; APK installé sur la tablette.
Dette : **tracé des flèches au doigt non vérifié sur la tablette** (verrouillée), à ajouter à la vérification de ce soir ; annotations non sauvegardées dans le PGN (backlog).
