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

### S8-2 à S8-8
Voir le plan validé : cartes et mesure de `save` (2), file et cas d'usage (3), écran de drill (4), apprentissage (5),
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
