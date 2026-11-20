# Mini-sprint S8b — Apprentissage des ouvertures

**Statut** : en cours (demandé par le joueur après S8 ; placé avant S3, ADR 0011).

**Objectif** : compléter son répertoire à partir des trous trouvés et apprendre une ouverture avec des explications
(faits : notes de l'auteur, évaluation Stockfish, statistiques de l'explorateur, idées et plans saisis).

**Règles tenues** : pas d'IA en ligne ; le moteur de règles valide tout coup ajouté ; un texte généré par un LLM local
(station PC, S9) ne s'ajoutera que plus tard et ne proposera jamais de coup.

## Stories

### S8b-1 · Ajouter un trou au répertoire et compléter la ligne
- [x] Sur l'écran « Trous », un appui sur une ligne ajoute le coup adverse trouvé **puis la suite de la ligne** :
  `LineCompleter` (`:core:data`) suit le coup le plus joué de l'explorateur Lichess pour les deux camps (ta réponse et
  les coups suivants), chaque coup validé par le moteur de règles, puis sauvegarde le répertoire. Limites
  (`GapSettings`) : 12 demi-coups, coups joués dans au moins 50 parties ; la ligne finit toujours sur ton coup ; elle
  s'arrête à une position inconnue de l'explorateur, à une position déjà dans le répertoire ou au premier problème
  réseau (le coup du trou est ajouté quand même, la ligne dit pourquoi elle s'arrête).
- [x] La ligne passe en « Ajouté au répertoire : n coups » ; une ligne ne s'ajoute qu'une fois, un seul ajout à la fois.
- [x] Tests : `LineCompleterTest` (4 : deux camps jusqu'à la limite, sans explorateur, position peu jouée, coup illégal
  écarté) et 3 de plus dans `GapsViewModelTest`. Installé sur la tablette.
- *Notes* : les coups ajoutés sont les plus joués (niveau Club), pas forcément les meilleurs ; vérifier la ligne dans
  l'éditeur. Pas de test d'écran dédié ; parcours réel non vu (jeton Lichess nécessaire).

### S8b-2 · Écran « Apprendre une ouverture »
- [x] Cours position par position (ligne principale d'abord, `LessonPlanner` dans `:core:srs`), progression « position
  n/total, k apprises », Précédent/Suivant, ouverture sur la première position non apprise. Bouton « Apprendre » dans la
  liste des répertoires.
- [x] Jouer le bon coup apprend la position : carte de révision créée, due demain (`learnedCard`, aussi utilisée par
  le mode apprentissage du drill).
- [x] Explications factuelles : notes et source de l'auteur, flèche verte, évaluation Stockfish de chaque coup,
  statistiques Lichess (fréquence, victoires/nulles/défaites). Hors ligne ou sans moteur, la leçon marche quand même.
- [x] Tests : `LessonPlannerTest` (5), `LearnViewModelTest` (11). Installé sur la tablette.
- *Notes* : pas de test d'écran Compose dédié ; statistiques non cachées = jeton Lichess nécessaire ; parcours réel
  sur tablette non vu.

### S8b-3 · Idée, plan, piège
- [x] Trois champs « Idée du coup », « Plan », « Piège » dans l'éditeur (`NodeNotes.idea/plan/trap`, `InsightField`),
  sauvegardés par position ; base Room v5 (migration `V4_TO_V5`, colonnes ajoutées vides).
- [x] Affichés dans l'écran Apprendre (un bloc par champ rempli).
- [x] Tests : stockage, migration 4→5, éditeur, leçon. Installé sur la tablette.
- *Notes* : l'idée, le plan et le piège ne sont pas inclus dans l'export PGN (seul le commentaire l'est) ; pas de test
  d'écran Compose dédié ; parcours réel non vu sur la tablette.

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
