# Sprint S7 — Répertoire d'ouvertures

**Durée** : 2 semaines · **Objectif** : un répertoire d'ouvertures Blancs et Noirs, saisi sur l'échiquier
ou importé, consultable avec les statistiques Lichess et une évaluation moteur.

**Livrable démontrable** : sur la tablette, créer un répertoire Blancs et un répertoire Noirs, y ajouter
des lignes en jouant sur l'échiquier, importer un PGN et une étude Lichess, puis consulter l'explorateur.

**Critères d'acceptation du sprint** : import d'un PGN de 500 variantes sans perte ; les transpositions
pointent vers le même nœud.

Sprint précédent : `docs/sprints/s2.md`. Ordre d'exécution : ADR 0011 (S7 puis S8, avant S3).

---

## Décisions de planification (validées le 2026-11-20)
- **Graphe, pas arbre** : les nœuds sont des positions (clé Zobrist + FEN), les arêtes des coups. Une
  position atteinte par deux chemins reste un seul nœud. → ADR 0012.
- **Logique en Kotlin pur dans `:core:chess`** (package `repertoire`), réutilisable par `:core:srs` en S8.
- **Module `:feature:repertoire`** créé en S7-3 (règle « un module quand son sprint commence »).
- **Réseau facultatif** : import d'étude et explorateur sont des compléments ; l'arbre, l'import PGN et
  l'édition fonctionnent hors ligne. Statistiques consultées mises en cache dans Room.
- **Dépendances proposées** (licences à noter dans `docs/licences.md`) : `kotlinx.serialization`
  (Apache 2.0) ; `HttpURLConnection` du JDK, sans OkHttp.
- **Explorateur Lichess et jeton** : l'API peut exiger un jeton (non vérifié). Spike court au début de
  S7-7 ; si un jeton est requis, jeton personnel en lecture seule, stocké localement, après accord de
  l'utilisateur.
- **Écarts de périmètre assumés** : lecture/écriture de `[%cal]` et `[%csl]` (backlog S1-8) et suppression
  d'une branche en S7-4. Les autres idées d'édition (promotion en ligne principale, NAG) restent au backlog.
- **Hors périmètre** (ADR 0011) : trous du répertoire (S8), « tu es sorti de ton répertoire » (S6),
  Maia qui joue le répertoire (S3), explication des déviations par le professeur (S4).
- Leçons de S2 appliquées : tablette branchée à chaque story d'interface ; pour chaque mode, un test qui
  enchaîne les autres actions (import, export) et relit le PGN produit ; démo de bout en bout **avant**
  la story de documentation ; compter en demi-coups dès l'estimation ; toute valeur de référence vient
  d'un outil, jamais de la mémoire.

## Stories

### S7-1 · Modèle du répertoire (Kotlin pur)
- [x] Graphe de répertoire dans `:core:chess.repertoire` : nœuds par clé Zobrist (FEN d'identification),
  arêtes par coup (UCI + SAN), côté du répertoire (Blancs ou Noirs), position de départ.
- [x] Fusion d'une `Game` (avec variantes) dans le graphe, sans doublon ; transpositions détectées par clé.
- [x] ADR 0012 (graphe plutôt qu'arbre, commentaire et flèches portés par le nœud ou l'arête).

**Acceptation** : tests JVM : deux ordres de coups qui transposent donnent un seul nœud ; fusionner deux
fois le même PGN ne change rien ; collision de clé Zobrist départagée par la FEN.

### S7-2 · Stockage Room
- [x] Base en version 2 avec migration depuis la v1 ; schéma exporté dans `core/data/schemas`.
- [x] Tables répertoire, nœud, arête ; commentaire, flèches et source (livre, chapitre, page) par nœud.
- [x] DAO et dépôt (`RepertoireStore`) derrière une interface.

**Acceptation** : test de migration v1 → v2 sur la tablette ou en JVM ; aller-retour graphe → Room →
graphe sans perte.

### S7-3 · Module `:feature:repertoire` et écrans de base
- [x] Création du module (Hilt, navigation, dépendances limitées à `:core:chess`, `:core:data`, `:core:ui`).
- [x] Liste des répertoires, création Blancs ou Noirs, renommage, suppression avec confirmation.
- [x] Entrée depuis l'accueil.

**Acceptation** : un répertoire créé survit à la fermeture de l'app (vérifié sur la tablette).

### S7-4 · Éditeur sur l'échiquier
- [x] Jouer un coup l'ajoute au répertoire ; navigation dans le graphe ; retour à la position précédente.
- [x] Commentaire, flèches (couche de `:core:ui`) et source (livre, chapitre, page) sur chaque nœud.
- [x] Suppression d'une branche, avec confirmation.
- [x] `:core:chess` lit et écrit `[%cal]` et `[%csl]` dans les commentaires PGN.

**Acceptation** : test d'enchaînement : éditer, exporter en PGN, relire, annotations conservées ; test
d'interface JVM de l'éditeur ; vérifié sur la tablette.

### S7-5 · Import PGN
- [ ] Import par fichier ou texte collé ; chaque partie passe par `:core:chess` avant stockage (règle 2).
- [ ] Fusion dans le répertoire choisi, rapport : lignes ajoutées, déjà présentes, rejetées avec la raison.
- [ ] Import hors du fil principal, progression visible.

**Acceptation** : PGN de 500 variantes importé sans perte ; transpositions vers le même nœud ; temps
d'import mesuré sur la tablette.

### S7-6 · Import d'une étude Lichess (réseau, facultatif)
- [ ] Saisie de l'identifiant ou de l'adresse d'une étude publique ; récupération du PGN ; même chemin
  que S7-5.
- [ ] Hors ligne ou erreur réseau : message clair, reste de l'application intact.

**Acceptation** : étude réelle importée avec ses flèches et commentaires ; test hors ligne ; test de
l'analyse des erreurs HTTP avec un faux transport.

### S7-7 · Explorateur
- [ ] Spike court : accès à l'explorateur Lichess (jeton requis ou non), décision consignée.
- [ ] Statistiques de la position (coups, parties, résultats) filtrées par niveau, mises en cache dans Room.
- [ ] Évaluation Stockfish de la position via `EngineScheduler` (priorité analyse en direct).
- [ ] Ajouter un coup de l'explorateur au répertoire.

**Acceptation** : une position déjà consultée s'affiche sans réseau ; l'analyse ne bloque pas l'UI (aucun
ANR) ; test avec faux transport réseau.

### S7-8 · Démo de bout en bout et corrections
- [ ] Parcours complet sur la tablette : créer, éditer, importer PGN et étude, explorer, fermer et rouvrir ;
  enchaînement des modes et relecture du PGN exporté.
- [ ] Chaque défaut trouvé reçoit sa story de correction (S7-8a, S7-8b…) avant la clôture.

**Acceptation** : démo consignée dans `s7.md` (tableau des étapes et défauts), capture d'écran.

### S7-9 · Documentation et clôture
- [ ] Cases cochées, notes de fin de story, dette technique, `docs/backlog.md` à jour.
- [ ] ADR de sprint annotés, licences ajoutées, rétrospective.
- [ ] Archive en `docs/sprints/s7.md` ; `current.md` désigne S8 (entraînement d'ouvertures).

**Acceptation** : sprint relu avec l'utilisateur ; `./gradlew test ktlintCheck detekt` verts.

---

## Notes de fin de story
**S7-1** (2026-11-20) : `Repertoire`, `PositionId`, `RepertoireEdge`, `add` et `merge` dans
`:core:chess.repertoire` ; ADR 0012. 15 tests dans `RepertoireTest`, dont transposition, fusion
idempotente, cycle de répétition, case en passant inutilisable et collision de clé simulée.
Choix : identité d'un nœud = clé Zobrist + FEN canonique ; annotations portées par le nœud (stockées en
S7-2). Dette : `add` et `merge` recopient les index (O(taille)), à mesurer à l'import de S7-5.

**S7-2** (2026-11-20) : base Room en version 2 (`repertoire`, `repertoire_node`, `repertoire_edge`), migration
`Migrations.V1_TO_V2` testée avec `MigrationTestHelper` sous Robolectric (analyses de la v1 conservées),
`RepertoireStore` / `RoomRepertoireStore` (créer, lister, charger, enregistrer, renommer, supprimer).
Écart de périmètre : `NodeNotes` (commentaire, flèches, cases, source) et `Repertoire.restore` ajoutés à
`:core:chess.repertoire`, nécessaires pour stocker les annotations. Nouvelle dépendance de test :
`androidx.room:room-testing` (Apache 2.0). Dette : `save` remplace tout le contenu du répertoire à chaque
appel (O(taille) écritures), à mesurer à l'import de S7-5 ; les cartes de S8 référenceront
(repertoireId, key, fen) sans clé étrangère vers les nœuds, pour survivre à ce remplacement.

**S7-3** (2026-11-20) : module `:feature:repertoire` (liste, création Blancs/Noirs, renommage, suppression
avec confirmation), `RepertoireListViewModel` à point d'entrée unique, bouton sur l'accueil et route
`repertoires`. 9 tests de ViewModel et 6 tests UI Robolectric (en français pour les pluriels). Vérifié sur
la tablette : création, redémarrage de l'app (le répertoire reste), suppression ; répertoire de test supprimé.
Les lignes ne sont pas encore ouvrables (`onOpen` nul) : l'éditeur arrive en S7-4. Observation : la tablette
affiche « 0 coups » (pluriel selon la langue du système, hors français), les tests JVM attendent « 0 coup ».

**S7-4** (2026-11-20) : éditeur sur l'échiquier (`RepertoireEditorViewModel`, modèle immuable `EditorModel`,
écran à deux panneaux). Jouer ajoute le coup, rejouer un coup connu le suit, un autre coup crée une variante ;
navigation Début / Reculer / suites ; commentaire, flèches et cases (G/R/B) et source par position ; suppression
du dernier coup avec confirmation ; sauvegarde différée (600 ms) et forcée à ON_STOP / onCleared.
`:core:chess` : `PgnMarks` (`[%cal]`, `[%csl]`), `Repertoire.removeMove`, `merge` conserve les commentaires,
`Repertoire.toGame` (export PGN, aller-retour testé). Vérifié sur la tablette : coups, variante (e4 / d4),
commentaire, redémarrage forcé (4 coups et commentaire conservés) ; répertoire de test supprimé.
Dette : `EditorInput` duplique `MoveInput` de `:feature:play` (à mutualiser) ; les notes de la position de
départ ne sont pas exportées en PGN ; les marques jaunes sont ignorées (backlog) ; `save` remplace tout le contenu.
