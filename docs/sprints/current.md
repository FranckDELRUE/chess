# Sprint S8 — Entraînement d'ouvertures

**Statut** : à planifier. Le plan détaillé des stories sera proposé puis validé avant tout code.

**Objectif** : réviser son répertoire en répétition espacée, avec un retour factuel quand on s'en écarte.

**Livrable démontrable** (dossier de conception, section S8) : drill en répétition espacée sur un
répertoire du joueur, retour factuel sur les déviations.

**Critères d'acceptation du sprint** : une ligne ratée revient le lendemain, une ligne réussie trois fois
s'espace de plusieurs jours.

Sprint précédent : `docs/sprints/s7.md` (archive et rétrospective). Ordre d'exécution : ADR 0011.

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
- Dette héritée à trier au plan : export PGN dans l'interface, réglages (jeton, niveau), marques jaunes
  (`docs/backlog.md`, sections « Issu de S7-x »).
