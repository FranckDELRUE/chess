# ADR 0014 — Révision par lignes depuis le début

**Statut** : accepté (demande du joueur pendant S8).

## Contexte
Le drill de S8-4/S8-5 posait chaque position isolément : le plateau sautait directement à la position à réviser.
Le joueur veut réviser comme à la partie : plusieurs coups de suite depuis la position de départ, l'application
jouant les réponses de l'adversaire.

## Décision
- La file quotidienne (cartes FSRS, limites, notation) ne change pas. La séance est découpée en **lignes**.
- Une ligne part de la racine, suit le chemin (`ReviewItem.path`) vers la position en tête de file, puis continue
  **jusqu'à la fin de la ligne du répertoire** (feuille du graphe).
- Les coups du joueur sont tous demandés, y compris ceux déjà bien sus. Une position de la file est notée (ou apprise
  si elle est nouvelle) ; une position connue mais non échue est un **entraînement non noté** ; une position jamais
  vue hors file est jouée à la place du joueur.
- Les réponses adverses sont jouées par l'application : celles du chemin tant que le joueur le suit, sinon tirées au
  hasard parmi celles du répertoire (`LineWalk`, pur, dans `:core:srs`).
- Après une erreur, le coup attendu est joué et la ligne continue ; la position revient plus tard, non notée.
- Si le joueur quitte deux fois le chemin avant la position visée, celle-ci est abandonnée pour la séance (elle reste
  échue).

## Conséquences
Séances plus longues et plus proches de la partie ; une même position peut être demandée plusieurs fois dans la
séance (notée une seule fois). La pause avant un coup adverse (700 ms) est réglable dans `DrillViewModel`.
