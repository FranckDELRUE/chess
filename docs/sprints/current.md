# Mini-sprint S8b — Apprentissage des ouvertures

**Statut** : en cours (demandé par le joueur après S8 ; placé avant S3, ADR 0011).

**Objectif** : compléter son répertoire à partir des trous trouvés et apprendre une ouverture avec des explications
(faits : notes de l'auteur, évaluation Stockfish, statistiques de l'explorateur, idées et plans saisis).

**Règles tenues** : pas d'IA en ligne ; le moteur de règles valide tout coup ajouté ; un texte généré par un LLM local
(station PC, S9) ne s'ajoutera que plus tard et ne proposera jamais de coup.

## Stories

### S8b-1 · Ajouter un trou au répertoire
- [x] Sur l'écran « Trous », un appui sur une ligne ajoute le coup adverse trouvé à l'ouverture (`Repertoire.add`, coup
  validé par le moteur de règles) et sauvegarde le répertoire ; la ligne passe en « Ajouté au répertoire », une ligne
  ne s'ajoute qu'une fois. Il reste à choisir sa propre réponse dans l'éditeur.
- [x] Tests : 3 de plus dans `GapsViewModelTest` (ajout et sauvegarde, une seule fois, ligne inconnue).
- *Note* : seul le coup de l'adversaire est ajouté (suggestion de réponse et ouverture de l'éditeur sur la position :
  S8b-2 ou plus tard).

### S8b-2 · Écran « Apprendre une ouverture » (à planifier avec le joueur)
- Cours ligne par ligne (ligne principale d'abord), progression (positions apprises sur total), positions apprises
  envoyées dans la file de révisions (mode apprentissage de S8-5).
- Explications factuelles : notes, flèches et sources de l'auteur ; évaluation Stockfish du coup et alternatives ;
  statistiques de l'explorateur (fréquence, résultats, réponses courantes).

### S8b-3 · Idée, plan, piège
- Invites dans l'éditeur pour écrire l'idée du coup, le plan et le piège d'une position ; affichage dans l'écran Apprendre.

### S8b-4 · Démo et clôture
- Démo sur la tablette, rétrospective courte, archive `docs/sprints/s8b.md`.

---

# Sprint S3 — Adversaires humains (après S8b)

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
