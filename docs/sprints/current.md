# Sprint S3 — Adversaires humains

**Statut** : à planifier. Le plan détaillé des stories sera proposé puis validé avant tout code.

**Objectif** : jouer contre un bot Maia d'un ELO choisi, avec pendule (dossier de conception, section S3).

**Livrable démontrable** : partie contre un bot Maia d'un ELO choisi, avec pendule.

**Critères d'acceptation du sprint** : sur 20 parties tests, le bot 1100 commet nettement plus d'erreurs que le bot 1900 ;
la partie se reprend après fermeture de l'app.

Sprint précédent : `docs/sprints/s8.md` (archive et rétrospective). Ordre d'exécution : ADR 0011 (S7, S8, puis S3).

---

## Contenu prévu (dossier de conception, `docs/dossier-conception.md`)
- Intégrer Maia (selon le résultat du spike S0) avec choix de l'ELO adverse.
- Personas : nom, ELO, style, ouvertures préférées.
- Pendules (rapide, blitz, personnalisée), abandon, nulle, revanche.
- Enregistrement automatique des parties jouées dans la base.
- Choix du palier au premier lancement (ELO saisi ou « je ne sais pas »).
- Partie d'entraînement : Maia joue ton répertoire puis continue hors théorie (venue de S8, ADR 0011).

## Point de départ hérité de S8
- `:core:srs` (FSRS, file quotidienne, `DrillSession`, `LineWalk`) et `:feature:training` ; base Room en version 4.
- Le graphe `Repertoire` et `LineWalk` permettent de jouer un répertoire jusqu'à la sortie de théorie.
- Dette et ajustements : voir la rétrospective de `docs/sprints/s8.md`.
