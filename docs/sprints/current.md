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
- [x] Import par fichier ou texte collé ; chaque partie passe par `:core:chess` avant stockage (règle 2).
- [x] Fusion dans le répertoire choisi, rapport : lignes ajoutées, déjà présentes, rejetées avec la raison.
- [x] Import hors du fil principal, progression visible.

**Acceptation** : PGN de 500 variantes importé sans perte ; transpositions vers le même nœud ; temps
d'import mesuré sur la tablette.

### S7-6 · Import d'une étude Lichess (réseau, facultatif)
- [x] Saisie de l'identifiant ou de l'adresse d'une étude publique ; récupération du PGN ; même chemin
  que S7-5.
- [x] Hors ligne ou erreur réseau : message clair, reste de l'application intact.

**Acceptation** : étude réelle importée avec ses flèches et commentaires ; test hors ligne ; test de
l'analyse des erreurs HTTP avec un faux transport.

### S7-7 · Explorateur
- [x] Spike court : accès à l'explorateur Lichess (jeton requis ou non), décision consignée.
- [x] Statistiques de la position (coups, parties, résultats) filtrées par niveau, mises en cache dans Room.
- [x] Évaluation Stockfish de la position via `EngineScheduler` (priorité analyse en direct).
- [x] Ajouter un coup de l'explorateur au répertoire.

**Acceptation** : une position déjà consultée s'affiche sans réseau ; l'analyse ne bloque pas l'UI (aucun
ANR) ; test avec faux transport réseau.

### S7-8 · Démo de bout en bout et corrections
- [x] Parcours complet sur la tablette : créer, éditer, importer PGN et étude, explorer, fermer et rouvrir ;
  enchaînement des modes et relecture du PGN exporté.
- [x] Chaque défaut trouvé reçoit sa story de correction (S7-8a, S7-8b…) avant la clôture.
- [x] S7-8a : séparateurs de milliers dans les nombres de parties de l'explorateur.
- [x] S7-8b : explorateur en fin de panneau, pour ne plus repousser notes et source.

> **Note (S7-8)** : démo consignée dans `s7.md`, 4 captures dans `s7-demo/`. Deux défauts corrigés (S7-8a, S7-8b).
> Dette : ouverture lente d'un répertoire de ~10 000 coups (~5 s) et export PGN absent de l'interface,
> notés dans `backlog.md`. L'export n'a été vérifié que par le test d'aller-retour.

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

**S7-5** (2026-11-20) : `Repertoire.importPgn` (`:core:chess`) fusionne toutes les parties d'un PGN en une seule
passe (`mergeAll`, une copie du graphe) et rend un `ImportReport` : parties fusionnées, coups ajoutés ou déjà
présents, rejets avec raison (`UNREADABLE` avec la ligne, `DIFFERENT_START`) ; une partie rejetée n'arrête pas
les autres. Dans l'éditeur, bouton « Importer un PGN » : fichier (`GetContent`, 32 Mo maximum) ou texte collé,
import sur `Dispatchers.Default` avec progression, un seul `save` à la fin, position affichée conservée,
modifications bloquées pendant l'import. Tests : 500 lignes sans perte (comparées à une construction coup par
coup), transpositions, rejets, réimport, progression, écran. **Mesure sur la tablette** : 500 parties de
20 demi-coups (63 Ko) = 9262 coups ajoutés, 738 déjà présents, **4,5 s** import et sauvegarde compris, coups
conservés après arrêt forcé ; répertoire et PGN de test supprimés. Dette : le `save` à remplacement complet
domine probablement ces 4,5 s (non isolé) ; un fichier de plusieurs milliers de parties demandera un `save`
incrémental ou par lots ; encodage lu en UTF-8 uniquement.

**S7-6** (2026-11-20) : import d'une étude publique Lichess. `StudyRef.parse` (identifiant ou adresse
lichess.org/study/…, autres hôtes refusés), `StudyFetcher` / `KtorStudyFetcher` (`GET /api/study/{id}.pgn`, sans
jeton, 32 Mo maximum) qui classe les réponses (`Found`, `NotFound`, `Private`, `RateLimited`, `Offline`,
`ServerError`, `TooLarge`) ; le PGN suit le chemin de S7-5 (validation par `:core:chess`, règle 2). Champ
« Étude Lichess » dans le dialogue d'import, message clair par erreur, rien n'est modifié en cas d'échec.
Première permission réseau (`INTERNET`) ; Ktor 3.5.2 + OkHttp (Apache 2.0). `PgnMarks` retire désormais les
commandes `[%clk]`, `[%eval]`… du commentaire. Tests : MockEngine (chemin, absence d'identifiants, toutes les
erreurs HTTP, hors ligne, taille), ViewModel, écran. **Vérifié sur la tablette** avec l'étude Y1yXP80U :
56 parties sur 56, 9734 coups ajoutés, 385 déjà présents, 5,6 s réseau et sauvegarde compris ; répertoire de
test supprimé. Non vérifié sur la tablette : affichage des flèches/commentaires importés, mode avion (couvert
par les tests JVM). Dette : une étude dont des chapitres partent d'une autre position est rejetée en
`DIFFERENT_START` ; pas de jeton ni de nouvelle tentative sur 429 ; UTF-8 uniquement.

**S7-7** (2026-11-20) : explorateur Lichess dans l'éditeur. **Spike** : l'API répond 401 sans jeton, d'où l'ADR 0013
(jeton personnel facultatif, sans permission, chiffré par une clé AES-GCM de l'Android Keystore, envoyé au seul
`explorer.lichess.ovh`). `:core:data.explorer` : `KtorExplorerFetcher` (aucune requête sans jeton, erreurs classées :
pas de jeton, jeton refusé, limite, hors ligne, serveur), `CachedExplorer` (cache Room `explorer_position`,
base v3 avec `Migrations.V2_TO_V3` testée, 7 jours, repli sur le cache si le réseau échoue), `KeystoreTokenStore`.
Éditeur : `ExplorerController` (statistiques par niveau Débutant/Club/Fort/Maître, évaluation Stockfish via
`EngineScheduler` en priorité `LIVE`, 2 s, annulée à chaque changement de position), panneau avec parties,
pourcentages et barre de résultats, bouton Ajouter/Suivre qui passe par `Repertoire.add` (coup illégal refusé,
SAN recalculé en français, jamais celui de Lichess). Tests : faux transport, cache, migration, contrôleur via le
ViewModel. **Tablette** : migration v2→v3 sans perte, évaluation affichée (+0,28 au départ, +0,30 après 1. e4,
vue des Blancs), enchaînement rapide de positions sans blocage, message « pas de jeton », puis stockage chiffré
et requête réelle avec un faux jeton → 401 → « Lichess a refusé le jeton » ; faux jeton et répertoire de test
retirés. **Non vérifié** : l'affichage des statistiques réelles (aucun jeton valide saisi) ; elles ne sont
couvertes que par les tests JVM. Dette : jeton saisi dans le panneau (pas de réglages), évaluation à durée fixe,
niveau non mémorisé.
