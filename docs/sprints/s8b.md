# Mini-sprint S8b — Apprentissage des ouvertures (archive)

**Statut** : terminé le 2026-11-20 (4 stories ; demandé par le joueur après S8, placé avant S3, ADR 0011).

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
- [x] Démo sur la tablette (adb, 4 captures dans `s8b-demo/`), rétrospective courte, archive de ce fichier.

| Étape | Résultat |
| --- | --- |
| Liste des répertoires : bouton « Apprendre » à côté de « Réviser » sur « Ouverture Écossaise » | conforme (`01-liste-apprendre.png`) |
| « Apprendre » : ouvre sur la première position non apprise, « Position 5 sur 98 · 10 apprises », ligne `1. e4 e5 2. Cf3 Cc6 3. d4 d6 4. dxe5 dxe5`, flèche verte sur Dxd8+, évaluation Stockfish +0,46, Lichess (niveau Club) 86 % des parties, Blancs 59 % / nulles 6 % / Noirs 33 % | conforme (`02-lecon.png`) |
| Glisser la dame d1 → d8 : « Appris : Dxd8+. Cette position reviendra demain dans tes révisions. », compteur 11 apprises | conforme (`03-coup-appris.png`) |
| « Suivant » : position 6, ligne prolongée de `5. Dxd8+ Cxd8`, Cxe5 (+2,12, 92 % des parties) | conforme (`04-suivant.png`) |

Aucun défaut relevé. La démo a appris une vraie position du répertoire de la tablette (carte due demain).

**Non vérifié sur la tablette** : les champs Idée / Plan / Piège dans l'éditeur et leur affichage dans la leçon
(couverts par les tests), l'ajout d'un trou avec complétion de ligne (jeton Lichess nécessaire), le bouton « Précédent ».

## Rétrospective S8b (2026-11-20)

### Résultat
- **Livrable démontré** : ajout d'un trou avec la suite de la ligne, leçon position par position avec faits (notes,
  Stockfish, Lichess), apprentissage qui alimente les révisions, idée / plan / piège saisis dans l'éditeur.
- Branches `feat/s8b-1…` et `feat/s8b-2-learn-screen` (locales, non poussées) ; base Room en version 5
  (migration 4→5 testée).
- Aucune IA en ligne ; tout coup ajouté passe par le moteur de règles.

### Ce qui a bien marché
- Réutilisation de S8 : `learnedCard`, `DrillInput`, `EvaluationProbe` et l'explorateur ont servi tels quels.
- La leçon reste utilisable hors ligne ou sans moteur (les faits arrivent s'ils sont disponibles).

### Ce qui a coûté
- Contraintes detekt (longueur de ligne, nombre de fonctions par fichier) : deux aller-retours.
- Le schéma Room `5.json` n'est généré qu'au build : le test de migration échoue à la première exécution.
- Un fichier parasite créé par erreur à la racine du disque utilisateur (`x`, dans le dossier court `FRANCK~1`), que la
  protection a empêché de supprimer : à supprimer à la main.

### Dette restante
| Dette | Où | Quand |
| --- | --- | --- |
| Idée / plan / piège absents de l'export et de l'import PGN | S8b-3 | à planifier |
| Pas de test d'écran Compose pour la leçon ni pour les champs de l'éditeur | S8b-2, S8b-3 | à planifier |
| Explications en phrases : seulement les notes saisies ; un texte généré arrivera avec le LLM local (S9) | S8b-2 | S9 |
| Les coups ajoutés à un trou sont les plus joués, pas les meilleurs ; pas d'aperçu avant ajout | S8b-1 | selon l'usage |
| Leçon : pas de saut direct vers une ligne ni de filtre « non apprises » | S8b-2 | selon l'usage |

