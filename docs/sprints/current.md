# Sprint S8 — Entraînement d'ouvertures

**Statut** : en cours (plan validé le 2026-11-20).

**Objectif** : réviser son répertoire en répétition espacée, avec un retour factuel quand on s'en écarte.

**Livrable démontrable** (dossier de conception, section S8) : drill en répétition espacée sur un
répertoire du joueur, retour factuel sur les déviations.

**Critères d'acceptation du sprint** : une ligne ratée revient le lendemain, une ligne réussie trois fois
s'espace de plusieurs jours.

Sprint précédent : `docs/sprints/s7.md` (archive et rétrospective). Ordre d'exécution : ADR 0011.

---

## Stories

### S8-1 · FSRS dans `:core:srs`
- [x] Module `:core:srs` (Kotlin pur) : `Fsrs`, `FsrsCard`, `FsrsParameters`, `Rating`.
- [x] FSRS-5 avec paramètres par défaut, intervalles en jours entiers, tests JUnit 5 (16 cas, dont les critères
  d'acceptation : ligne ratée → lendemain, trois réussites → intervalles croissants, 3 puis ≥ 7 jours).
- *Note* : un `Again` renvoie toujours au lendemain (règle produit, même pour une ligne déjà apprise où
  FSRS donnerait ~3 jours) ; la stabilité, elle, suit FSRS. Pas de « fuzz » (déterminisme). Les poids par
  défaut sont ceux publiés de FSRS-5 (MIT), non réentraînés sur les révisions de Heidoji : dette éventuelle.

### S8-2 · Cartes de révision et mesure de `save`
- [x] Table `review_card` (Room v4, migration `V3_TO_V4`, schéma `4.json`) : clé (répertoire, clé Zobrist, FEN
  canonique), état FSRS, sans clé étrangère vers les nœuds (ADR 0012) ; `ReviewCardStore` / `RoomReviewCardStore`
  (`get`, `cards`, `put`, `dueCount`, `retainOnly`, `deleteAll`), fourni par Hilt ; `:core:data` dépend de `:core:srs`.
- [x] Tests : aller-retour, remplacement, isolation entre répertoires, `dueCount`, `save` conserve les cartes,
  suppression d'un répertoire supprime ses cartes, `retainOnly`, migration 3 → 4.
- [x] Mesure (JVM/Robolectric, répertoire synthétique de 3 000 coups) : `save` ≈ 150 ms, `load` ≈ 190 ms, `save` avec
  3 000 cartes présentes ≈ 110 ms (cartes non touchées par `save`), lecture de toutes les cartes ≈ 40 ms,
  `put` unitaire ≈ 0,4 ms. Le coût de `save` ne dépend pas des cartes.
- *Notes* : les chiffres JVM ne sont pas ceux de la tablette (~5 s pour 10 000 coups mesurés en S7) ; à remesurer
  sur l'appareil au drill (S8-4). Une carte n'existe qu'après la première révision (position sans carte = nouvelle) ;
  `retainOnly` est à appeler après une suppression de coups (S8-3). Un `put` en lot sera ajouté si l'apprentissage
  (S8-5) crée beaucoup de cartes d'un coup. Sauvegarde incrémentale du répertoire non nécessaire pour les cartes.

### S8-3 · File de révisions et logique du drill (Kotlin pur, `:core:srs`)
- [x] `ReviewQueueBuilder` : une carte par position où le joueur doit répondre (au trait, avec une réponse au
  répertoire) ; révisions dues d'abord (les plus en retard en tête), puis nouvelles positions du tronc vers les
  branches ; limites quotidiennes (`QueueLimits`, 100 révisions / 10 nouvelles par défaut) ; chemin le plus court
  depuis la racine pour rejouer la ligne ; transpositions = une seule carte.
- [x] « Dû aujourd'hui » = échéance avant la fin du jour local (`endOfDay`), pour qu'une ligne vue à 21 h revienne
  le lendemain matin.
- [x] `DrillSession` : toute réponse du répertoire est juste ; bon coup sans aide = Good, avec indice = Hard,
  déviation = Again ; une position ratée revient en fin de séance sans être notée à nouveau (le calendrier garde
  la première note) ; l'appelant stocke chaque `updatedCard` non nul.
- [x] 18 tests (queue 11, drill 7), dont les critères du sprint : ligne ratée due le lendemain mais pas aujourd'hui,
  trois réussites aux écarts croissants. `:core:srs` dépend maintenant de `:core:chess` (api).
- *Notes* : `ReviewCardStore.dueCount` compare une instant brute ; lui passer `endOfDay(...)` pour le compteur du jour.
  La carte d'une position est créée à la première réponse (S8-4 l'enregistre via `put`). Appeler
  `retainOnly` après une suppression de coups. Pas d'indicateur « Facile » ni de « passer » (hors périmètre).
  Les tests du dépôt qui fusionnent plusieurs parties sans en-têtes (`RoomRepertoireStoreTest.sample`) n'en lisent
  qu'une seule (« Text after the result ») : à corriger en passant.

### S8-4 à S8-8
Voir le plan validé : (3 fait), écran de drill (4), apprentissage (5),
trous du répertoire et notifications (6), export PGN (7), démo et clôture (8).

---

## Contenu prévu (dossier de conception, `docs/dossier-conception.md`)
- Implémenter FSRS dans `:core:srs` avec tests.
- Modes apprentissage, drill et déviation ; retour factuel sur une déviation (coup attendu, coup joué,
  ligne du répertoire, évaluation Stockfish).
- File de révisions quotidienne, notifications optionnelles.
- Détection des trous du répertoire (réponses fréquentes non couvertes).

**Hors périmètre** (ADR 0011) : partie d'entraînement contre Maia (S3), explication des déviations par le
professeur (S4), lien revue de parties / répertoire (S6).

## Point de départ hérité de S7
- Graphe `Repertoire` dans `:core:chess.repertoire` (nœuds par `PositionId`, transpositions fusionnées),
  stocké par `RepertoireStore` (Room v3) ; `:core:srs` n'existe pas encore (module créé au début du sprint).
- Une carte de révision par position : clé (répertoire, `PositionId`, FEN), sans clé étrangère vers les nœuds
  car `save` remplace tout le contenu (ADR 0012).
- Explorateur Lichess (cache Room, jeton facultatif) et évaluation Stockfish via `EngineScheduler`
  disponibles pour les détections de trous et le retour sur une déviation.

## Points d'attention issus de la rétrospective de S7
- Mesurer le coût de `save` et du chargement d'un gros répertoire (~5 s pour ~10 000 coups) dès les
  premières cartes ; sauvegarde incrémentale à envisager.
- Démo de bout en bout du livrable avant la story de clôture ; tablette branchée à chaque story d'interface.
- **Décidé le 2026-11-20 (relecture de S7)** : l'export PGN d'un répertoire dans l'interface entre dans S8 ;
  les cartes référencent (répertoire, clé, FEN) sans clé étrangère, et le coût de `save` est mesuré dès les
  premières cartes.
- Dette héritée à trier au plan : réglages (jeton, niveau), marques jaunes, ouverture lente d'un gros
  répertoire (`docs/backlog.md`, sections « Issu de S7-x »).
- **Demande de l'utilisateur (2026-11-20)** : refonte de l'éditeur de répertoire (panneau unique mieux ordonné,
  suites nommables), **planifiée après S8** ; description dans `docs/backlog.md`.
