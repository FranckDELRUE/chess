# ADR 0011 — Avancer le répertoire (S7) et l'entraînement d'ouvertures (S8) avant Maia

- **Statut** : accepté (option B, validée le 2026-11-20)
- **Date** : 2026-11-20

## Contexte
S2 est clos (`docs/sprints/s2.md`). La feuille de route du dossier de conception place l'import
d'ouvertures en S7 et leur entraînement en S8, soit 4 et 5 sprints plus tard (de 10 à 12 semaines au
rythme nominal, davantage au rythme réel de S2). L'utilisateur veut pouvoir importer ses ouvertures et
s'entraîner dessus plus tôt. Au palier Club (ELO ≈ 1300), le « premier répertoire » fait partie des
objectifs du dossier.

Ce qu'il faut pour S7 et S8 :

| Élément | Dépend de | Disponible ? |
| --- | --- | --- |
| Arbre de répertoire, saisie sur l'échiquier, commentaires, flèches | `:core:chess` (arbre de coups, ADR 0006), `:core:ui` | oui (S1) |
| Import PGN, fusion des variantes, transpositions par FEN | parseur PGN, clé Zobrist et FEN (S1, S2-5) | oui |
| Stockage | Room dans `:core:data` | oui (S2-5) |
| Explorateur : statistiques Lichess, évaluation moteur | API Lichess (réseau, facultatif), `EngineScheduler` | oui (S2) |
| Import d'une étude Lichess | API Lichess | oui (réseau, facultatif) |
| « Tu es sorti de ton répertoire au coup 8 » | bibliothèque de parties et revue | **non : S6** |
| FSRS, modes apprentissage, drill, déviation, file quotidienne | `:core:srs` (Kotlin pur), WorkManager | oui |
| Détection des trous du répertoire | explorateur Lichess (S7) | oui après S7 |
| Partie d'entraînement : Maia joue ton répertoire | Maia | **non : S3** |
| Professeur qui explique une déviation | `:core:pedagogy`, panneau du professeur | **non : S4** |

Seuls trois éléments dépendent d'un sprint non fait. Aucun n'est au cœur de l'import ni du drill.

## Options

### A. Insérer S7 et S8 juste après S3
Ordre : S3 → **S7 → S8** → S4 → S5 → S6 → S9 …
- L'import arrive après Maia (≈ 4 semaines), l'entraînement ≈ 2 semaines plus tard.
- La partie d'entraînement contre Maia reste dans S8.
- Le Professeur v1 (S4), premier livrable « professeur jouable » du dossier, recule de deux sprints.

### B. Commencer par S7 et S8 (recommandée)
Ordre : **S7 → S8** → S3 → S4 → S5 → S6 → S9 …
- L'import arrive au prochain sprint (≈ 2 semaines), l'entraînement ≈ 2 semaines plus tard.
- Maia (S3) et le Professeur v1 (S4) reculent de deux sprints.
- Trois éléments changent de sprint pour qu'aucun sprint n'attende un sprint postérieur :
  1. « Partie d'entraînement : Maia joue ton répertoire » passe de S8 à **S3**. C'est une petite
     story une fois Maia branché, puisque le répertoire existe déjà.
  2. En S8, une déviation reçoit un retour factuel (coup attendu, coup joué, ligne du répertoire,
     évaluation Stockfish). L'explication pédagogique de la déviation passe en **S4**.
  3. « Tu es sorti de ton répertoire au coup 8 » passe de S7 à **S6**, avec la bibliothèque de
     parties.

C'est l'option qui répond le plus vite au besoin, sans fonctionnalité bancale : chaque élément
déplacé tombe dans le sprint qui apporte sa dépendance.

## Décision
- Retenir **l'option B**.
- **Garder les numéros de sprint** (S7 reste « Répertoire », S8 « Entraînement d'ouvertures ») et ne
  changer que l'ordre d'exécution. Les archives (`s1.md`, `s2.md`), l'ADR 0006 et le backlog citent
  ces numéros par leur contenu. Renuméroter réécrirait l'historique pour rien. Les fichiers d'archive
  garderont les numéros (`s7.md`, `s8.md` après `s2.md`).
- Créer `:feature:repertoire` au début de S7, puis `:core:srs` et `:feature:training` au début de
  S8, selon la règle « un module quand son sprint commence ».

## Conséquences
- Le dossier de conception est mis à jour : un ordre d'exécution sous le tableau des sprints et les
  trois éléments déplacés (S3, S4, S6, S7, S8). Le livrable de S8 devient « drill en répétition
  espacée, retour factuel sur les déviations ».
- `docs/sprints/current.md` désigne S7 comme prochain sprint à planifier, au lieu de S3.
- Les ajustements proposés pour S3 à la fin de `s2.md` restent valables, deux sprints plus tard : le
  préchauffage de Stockfish et la calibration de son ELO (dette de S2-4) attendent S3.
- L'application garde un adversaire Stockfish (non calibré comme un humain) jusqu'à S3, et aucun
  professeur jusqu'à S4.
- Hors ligne d'abord : l'explorateur et l'import d'étude Lichess sont des compléments réseau. L'import
  PGN, l'arbre et les révisions fonctionnent sans réseau. Les statistiques consultées sont mises en
  cache dans Room.
- Règle 2 : tout PGN ou étude importé passe par `:core:chess` avant stockage, comme en S1.

## Bilan à la fin de S7 (2026-11-20)
- L'ordre S7 puis S8 a tenu : S7 est livré (répertoire, imports PGN et étude, explorateur, démo sur la
  tablette) et S8 démarre sur un graphe de répertoire déjà stocké et clé par position.
- Aucun élément de S3, S4 ou S6 n'a été touché ; les trois éléments déplacés (Maia qui joue le répertoire,
  explication des déviations, lien revue/répertoire) restent à leur sprint d'arrivée.
