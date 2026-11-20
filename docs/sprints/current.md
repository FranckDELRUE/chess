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

### S8-4 · Écran de drill (`:feature:training`)
- [x] Module `:feature:training` : `DrillViewModel` (UDF, un point d'entrée `onAction`), `DrillScreen` (échiquier + panneau,
  paysage et portrait), textes en français ; route `drill/{id}` ; bouton « Réviser » sur chaque répertoire non vide.
- [x] Question : position au trait avec la ligne jouée jusque-là, indice (compte comme « difficile »), saisie par
  toucher ou glisser, promotion. Réponse : flèches vertes (coups du répertoire) et rouge (coup joué).
- [x] Retour factuel sur une déviation : coup joué, coup(s) attendu(s), ligne du répertoire, évaluation Stockfish après
  chacun (priorité `LIVE`, 1 s, point de vue des Blancs) ; sans moteur, le retour s'affiche sans évaluation.
  Chaque réponse notée est écrite dans `review_card` ; la position ratée revient en fin de séance, sans être renotée.
- [x] Fin de séance (bilan, positions encore disponibles) ; « rien à réviser » si la file est vide.
- [x] 14 tests (ViewModel 11, écran 3) ; `DrillSession.isRetry` ajouté (test) ; `Clock` fourni par Hilt (`DataModule`).
- [x] Installé sur la tablette (`installDebug`) : l'application démarre ; migration Room 3 → 4 à l'ouverture d'un répertoire.
- *Dette* : `DrillInput` et `formatEvaluation`/`lineText` dupliquent ceux de `:feature:repertoire` (à extraire dans
  `:core:ui` / `:core:engine` quand un troisième écran les utilisera) ; pas de vérification visuelle automatisée sur la
  tablette (à faire par le joueur) ; l'ordre des nouvelles positions est celui du graphe, sans regroupement par ligne ;
  `RepertoireListScreen` : le bouton « Réviser » n'a pas de test d'écran dédié.

### S8-5 · Mode apprentissage
- [x] `DrillSession.isLearning` / `learn` / `LearnResult` (`:core:srs`) : une position nouvelle n'est jamais interrogée à
  froid ; on la **découvre** (coup du répertoire et flèche affichés, commentaire et source de la position) puis on
  joue le coup montré. Un autre coup ne change rien et n'est pas noté. La carte est créée avec la mémoire d'un premier
  succès (Good) mais échéance **le lendemain** : le premier vrai rappel arrive vite.
- [x] `DrillStatus.LEARNING` dans `:feature:training` : panneau « Nouvelle position à apprendre », message si mauvais
  coup, confirmation « Appris », bilan « n nouvelles positions apprises » (`DrillSummary.learned`).
- [x] Tests : 2 de plus dans `DrillSessionTest`, 5 de plus dans `DrillViewModelTest` (réécrit : les révisions partent de
  cartes déjà vues), 1 de plus dans `DrillScreenTest`. Installé sur la tablette.
- *Notes* : le mode « déviation » du plan est le retour factuel de S8-4 (pas de mode distinct). Pas d'option « sauter
  l'apprentissage » ni de réglage de la limite de nouvelles positions (10 par séance, `QueueLimits`) : à ajouter avec
  les réglages si le besoin se confirme. Les flèches et marques dessinées par l'auteur sur la position ne sont pas
  encore montrées pendant l'apprentissage (seuls commentaire et source le sont).
### S8-6 · Trous du répertoire et rappel quotidien
- [x] `GapFinder` (`:core:data`) : parcourt les positions où l'adversaire joue (de la racine vers les feuilles), interroge
  l'explorateur Lichess (cache d'abord) et liste les réponses fréquentes (≥ 5 % et ≥ 100 parties) sans réponse dans le
  répertoire ; coups validés par le moteur de règles. Écran « Trous » (`GapsViewModel`/`GapsScreen`), bouton dans la
  liste des répertoires. 10 + 5 tests.
- [x] Rappel quotidien optionnel (`ReviewReminderWorker`, WorkManager périodique 24 h, ~18 h locale) : notification
  « n positions à réviser » seulement s'il y en a et si les notifications sont autorisées ; lit la base locale, aucun
  réseau. Interrupteur sur l'accueil (`ReminderSwitch`) qui demande la permission POST_NOTIFICATIONS (Android 13+) ;
  désactivé par défaut. 3 tests de calcul du délai. Installé sur la tablette.
- *Notes / dette* : analyse limitée à 40 positions par passage (les plus proches de la racine) ; nécessite le jeton
  Lichess ou des positions en cache ; le rappel ne compte que les révisions échues (pas les nouvelles positions) ; le
  worker et `DueCounter` n'ont pas de test dédié (le fake de store renvoie un flux vide) ; l'heure (18 h) n'est pas
  réglable ; WorkManager n'est pas exact à la minute.
- [x] Correctif (retour du joueur) : bouton « Quitter la séance » toujours visible dans le drill, « Terminer » à la fin ; la
  progression est déjà enregistrée à chaque réponse. 1 test d'écran de plus.
- [x] Révision par lignes (demande du joueur, ADR 0014) : le drill part de la position de départ, l'application joue les
  réponses adverses, le joueur répond à tous ses coups jusqu'à la fin de la ligne. `LineWalk`, `DrillSession.focus/drop`,
  `practiceResult` dans `:core:srs` ; `DrillViewModel` réécrit autour des lignes (statut `REPLYING`, entraînement non
  noté). Tests : `LineWalkTest` (4), `DrillSessionTest` (+2), `DrillViewModelTest` (+3, 1 adapté). Installé sur la tablette.
- *Notes* : pas de réglage de la pause ni de l'arrêt « à la position visée » ; non vérifié visuellement sur la tablette.
### S8-7 · Export PGN d'un répertoire
- [x] Bouton « Exporter » sur chaque répertoire non vide de la liste : `RepertoireListViewModel.exportPgn(id)` écrit le
  répertoire en une partie PGN (`Repertoire.toGame`, variations pour les autres coups, notes en commentaires) dans un
  fichier choisi par le joueur (sélecteur de documents Android, sans permission de stockage), avec un message de
  réussite ou d'échec. Nom proposé : `Nom-du-repertoire.pgn`.
- [x] Tests : aller-retour export puis relecture (mêmes positions et mêmes coups), répertoire inconnu, nom de fichier
  sûr. `RepertoireListContent` reçoit désormais ses callbacks groupés (`RepertoireRowActions`). Installé sur la tablette.
- *Notes* : les notes de la position de départ ne sont pas exportées (limite de `toGame`) ; l'écriture du fichier n'a pas
  de test automatisé (sélecteur système), à vérifier à la main sur la tablette.
### S8-8
Voir le plan validé : démo de bout en bout et clôture du sprint (rétrospective, archive `docs/sprints/s8.md`).

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
