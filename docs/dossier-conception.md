# Chess Companion — Dossier de conception & plan de sprints

17 octobre 2026 · Franck

## 1. Vision produit

Chess Companion est une application Android (téléphone et tablette) qui fait progresser un joueur d'échecs de son niveau actuel au suivant, quel que soit ce niveau. Elle réunit dans un seul outil ce que l'on trouve aujourd'hui dispersé : adversaires au style humain calibrés en ELO, puzzles, répertoire d'ouvertures, revue de parties et plan d'entraînement.

**Le cœur de l'application est un professeur.** En mode apprentissage, il accompagne chaque partie : il conseille avant le coup quand on le lui demande, analyse chaque coup joué, explique pourquoi en langage clair et propose de rejouer. Tous les autres modules (puzzles, ouvertures, revue, finales) s'appuient sur ce même professeur et sur ce qu'il sait de tes erreurs.

**Objectifs mesurables**

- Gain d'ELO mesurable du joueur sur 3 mois d'usage régulier (suivi depuis Lichess ou Chess.com).
- Chaque séance d'entraînement cible une faiblesse détectée dans les parties réelles du joueur, pas un contenu générique.
- Fonctionnement hors ligne pour le jeu, les puzzles et l'entraînement d'ouvertures ; le réseau n'est requis que pour l'import de parties et pour joindre la station PC. Aucun service d'IA en ligne n'est utilisé : toute l'IA tourne sur la tablette et sur ton PC.

**Principe « utile à tous les niveaux »**

L'application détecte le niveau du joueur (ELO déclaré, importé, ou estimé par un test de placement) et adapte contenu, vocabulaire et exigences. Cinq paliers structurent cette adaptation :

| Palier | ELO indicatif | Priorité d'entraînement | Style du coach |
| --- | --- | --- | --- |
| Découverte | < 800 | Règles, mats élémentaires, ne plus laisser de pièces en prise | Pédagogique, une idée à la fois |
| Fondations | 800–1200 | Tactique de base (fourchette, clouage), principes d'ouverture | Concret, exemples courts |
| Club | 1200–1600 | Calcul, plans de milieu de partie, finales de base, premier répertoire | Explique les plans |
| Compétition | 1600–2000 | Répertoire approfondi, finales techniques, gestion du temps | Technique, variantes |
| Expert | > 2000 | Préparation ciblée, nuances positionnelles, finales théoriques | Concis, langage de maître |

Le palier n'enferme pas : il fixe les réglages par défaut (difficulté des puzzles, profondeur des variantes, ELO des adversaires), que le joueur peut toujours modifier.

## 2. Périmètre fonctionnel

L'application compte neuf modules organisés autour du professeur (M6), qui est le module central : il intervient en jeu, en puzzles, en ouvertures et en revue de parties.

### M1 — Jouer contre des adversaires calibrés

- Adversaires « humains » par tranche d'ELO, basés sur Maia : ils commettent les erreurs typiques de leur niveau au lieu de gaffes aléatoires.
- Adversaires « moteur » Stockfish (niveaux 0–20, limitation UCI_Elo) pour l'entraînement technique et les niveaux au-delà de Maia.
- Cadences : sans pendule, rapide, blitz, et parties à la cadence choisie ; partie à partir d'une position (FEN) ou d'une ouverture du répertoire.
- Personas : chaque adversaire a un nom, un ELO et un style (agressif, solide, ouvertures préférées).
- Par palier : aide visuelle (pièces en prise, coups légaux) activée en Découverte, désactivée par défaut au-delà.

### M2 — Puzzles

- Base de puzzles Lichess notés et classés par thème (mat en N, fourchette, clouage, attaque à la découverte, finale…).
- Classement puzzle propre au joueur (Glicko-2) ; la difficulté suit ce classement.
- Modes : entraînement libre, par thème, série chronométrée (type Puzzle Rush), révision des puzzles ratés.
- Puzzles personnels générés à partir des erreurs de tes propres parties (voir M5).

### M3 — Bibliothèque d'ouvertures

- Répertoire sous forme d'arbre (Blancs et Noirs séparés), avec commentaires, flèches et sources.
- Import PGN, import depuis une étude Lichess, saisie au clavier sur l'échiquier.
- Import par photo de pages de livres : la photo est lue par l'IA locale (vision), convertie en variantes, chaque coup est validé par le moteur de règles, puis un écran de correction permet de corriger les coups illisibles avant intégration.
- Explorateur : statistiques des coups joués par niveau (base Lichess) et évaluation moteur à chaque nœud.

### M4 — Entraînement aux ouvertures et variantes

- Répétition espacée (algorithme FSRS) : l'application te fait rejouer tes lignes ; celles que tu oublies reviennent plus souvent.
- Modes : apprentissage (coups montrés puis rejoués), drill (de mémoire), « déviation » (l'adversaire sort de la théorie et tu dois trouver le bon plan).
- Partie d'entraînement : un adversaire Maia joue ton répertoire, puis continue au-delà de la théorie.
- Détection des trous : coups adverses fréquents à ton niveau non couverts par ton répertoire.

**Mode « Jouer le livre »**

Chaque livre importé devient un cours jouable, chapitre par chapitre, dans l'ordre de l'auteur. Le professeur s'appuie sur les commentaires du livre plutôt que sur des explications génériques.

1. **Lecture guidée** : les coups du chapitre se jouent sur l'échiquier un par un, avec le commentaire de l'auteur affiché à chaque coup et les flèches des idées clés.
2. **À toi de jouer** : l'échiquier s'arrête à chaque coup de ton camp ; tu dois retrouver le coup du livre. Un indice reprend l'idée du commentaire.
3. **Variantes du livre** : quand l'auteur donne une alternative, l'application te fait jouer la ligne principale puis chaque variante, en revenant au point de bifurcation.
4. **Quiz des positions clés** : les diagrammes du livre deviennent des questions (« quel est le plan ici ? », « trouve le coup thématique »).
5. **Partie d'application** : un adversaire Maia à ton niveau entre dans l'ouverture du chapitre, puis la partie continue librement ; le professeur signale quand tu quittes la théorie du livre.
6. **Révision** : les lignes du livre entrent dans la répétition espacée ; la progression par chapitre (lu, joué, maîtrisé) s'affiche dans la bibliothèque.

### M5 — Revue de parties et mémoire

- Import automatique depuis Lichess et Chess.com (pseudos configurés), import PGN manuel, saisie d'une partie jouée sur échiquier réel.
- Analyse Stockfish en arrière-plan ; classement des coups (meilleur, bon, imprécision, erreur, gaffe) et des erreurs par phase (ouverture, milieu, finale) et par cause (tactique ratée, plan, temps).
- Comparaison avec ton répertoire : où tu en es sorti et si c'était un oubli.
- Base de parties consultable et filtrable ; chaque erreur grave devient un puzzle personnel.

### M6 — Le Professeur : mode apprentissage (module central)

Le professeur est un compagnon qui joue « à côté de toi » pendant une partie contre un adversaire calibré (M1). Il ne joue jamais à ta place : il te fait réfléchir, puis il analyse et explique.

**Avant ton coup (sur demande)**

- Indices gradués en trois niveaux : l'idée (« une pièce adverse n'est pas défendue »), la zone ou la pièce concernée, puis le coup.
- Alerte de menace : « ton cavalier en f3 est attaqué deux fois et défendu une fois ».
- Liste de vérification adaptée au palier : échecs, prises, menaces pour les débutants ; plans, pions faibles, cases clés au-delà.
- Mode « je réfléchis à voix haute » : tu annonces ton plan ou ton coup candidat, le professeur le commente avant que tu joues.

**Après ton coup (automatique)**

- Verdict immédiat : meilleur, bon, imprécision, erreur, gaffe, avec la perte de chances de gain.
- Explication du « pourquoi » à partir des motifs détectés : pièce laissée en prise, fourchette manquée, clouage ignoré, roi exposé, développement en retard, structure de pions abîmée.
- Ce qu'il fallait jouer et l'idée derrière, avec flèches sur l'échiquier.
- Proposition de reprendre le coup sur une erreur ou une gaffe (désactivable), pour trouver soi-même la bonne idée.
- Commentaire aussi sur les bons coups (« bien vu, tu as anticipé la fourchette »), pour renforcer ce qui fonctionne.

**Après la partie**

- Bilan en trois points : ce qui a bien marché, l'erreur la plus coûteuse, le thème à travailler.
- Exercices générés automatiquement à partir de la partie (puzzles personnels, ligne d'ouverture à revoir).

**Réglages et personnalisation**

- Intensité : silencieux (bilan seulement), ponctuel (erreurs et gaffes), bavard (chaque coup).
- Ton et vocabulaire adaptés au palier : imagé et simple en Découverte, technique et concis en Expert.
- Mémoire pédagogique : le professeur retient tes erreurs récurrentes et les relie (« troisième fois cette semaine que tu oublies le clouage sur la colonne e »).
- Questions libres à tout moment sur la position (« quel est le plan ici ? », « pourquoi pas Fxf7 ? »).

### M7 — Finales

- Parcours de finales essentielles par palier : mats de base, roi et pion, opposition, Lucena, Philidor, finales de tours courantes.
- Jeu contre le moteur jusqu'à conversion du gain ou tenue de la nulle ; vérification par tables de finales.

### M8 — Calcul et visualisation

- Exercices à l'aveugle progressifs (repérage de cases, diagonales, puis parties à l'aveugle).
- « Calcule sans bouger » : visualiser une suite de N coups puis répondre à une question sur la position finale.

### M9 — Progression et plan adaptatif

- Tableau de bord : courbes d'ELO (Lichess, Chess.com, puzzles, adversaires), précision par phase, thèmes tactiques faibles, couverture du répertoire.
- Séance du jour générée à partir de tes faiblesses (par exemple 10 puzzles de clouage, 5 lignes du répertoire, 1 finale).
- Test de placement initial pour estimer le palier sans ELO connu.

## 3. Architecture technique

Application Kotlin native en Clean Architecture multi-modules, avec un cœur échecs pur Kotlin testable sans Android, des moteurs et une IA embarqués, et une station d'analyse sur ton PC qui prend le relais dès qu'elle est joignable.

```
┌──────────────────────────────────────────────────────────────┐
│ :app — navigation, thème, mise en page téléphone et tablette │
└──────────────────────────────┬───────────────────────────────┘
┌──────────────────────────────▼───────────────────────────────┐
│ Modules fonctionnels (:feature:*)                            │
│ Jouer · Puzzles · Répertoire · Entraînement · Revue          │
│ Professeur · Finales · Livres/Photo · Progression            │
└──────────────────────────────┬───────────────────────────────┘
┌──────────────────────────────▼───────────────────────────────┐
│ Modules cœur (:core:*)                                       │
│ chess (règles, PGN, FEN) · engine (UCI) · data (Room, API)   │
│ ui (échiquier) · srs (FSRS) · pedagogy (motifs) · compute    │
└───────────────┬──────────────────────────┬───────────────────┘
┌───────────────▼──────────────────────────▼───────────────────┐
│ Moteurs embarqués et services                                │
│ Stockfish (NDK) · Maia (ONNX) · LLM 1–2 Md · Station PC      │
│ API Lichess · API Chess.com                                  │
└──────────────────────────────────────────────────────────────┘
```

Les écrans ne parlent qu'aux modules cœur ; seuls `:core:engine`, `:core:data` et `:core:compute` touchent aux moteurs et aux services, et `:core:chess` valide toute donnée d'échecs.

### Stack

| Couche | Choix | Rôle |
| --- | --- | --- |
| Langage / UI | Kotlin, Jetpack Compose, Material 3 | Écrans adaptatifs, paysage prioritaire sur tablette |
| Architecture | MVVM + UDF, cas d'usage, Hilt | Séparation UI / domaine / données, injection |
| Asynchrone | Coroutines, Flow, WorkManager | Analyses longues et imports en tâche de fond |
| Règles d'échecs | Module maison `:core:chess` (0x88, Zobrist ; ADR 0006) | Coups légaux, FEN, PGN, SAN |
| Moteur fort | Stockfish compilé pour arm64 (NDK, CMake) | Analyse, évaluation, adversaires techniques |
| Moteur humain | Maia via ONNX Runtime Mobile | Adversaires au style humain par ELO |
| IA embarquée | llama.cpp ou MediaPipe LLM (modèle 1–2 Md), ML Kit | Reformulation du professeur hors ligne, lecture de texte |
| IA locale PC | FastAPI, Stockfish, lc0 CUDA, Ollama, modèle vision | Analyses profondes, LLM professeur, lecture des livres |
| Données | Room (SQLite), DataStore | Parties, répertoire, livres, puzzles, révisions |
| Réseau | Ktor client, kotlinx.serialization | APIs Lichess, Chess.com, station PC |
| Qualité | JUnit 5, Turbine, Compose UI Test sur la JVM avec Robolectric (ADR 0007), ktlint, detekt, GitHub Actions | Tests et intégration continue |

### Modules Gradle

- `:app` — point d'entrée, navigation, thème.
- `:core:chess` — modèle d'échiquier, règles, PGN/FEN, arbre de variantes (pur Kotlin, sans dépendance Android).
- `:core:engine` — interface `ChessEngine` (UCI) et ses implémentations Stockfish et Maia ; file d'attente d'analyses.
- `:core:data` — Room, DAO, dépôts, clients API.
- `:core:ui` — composant échiquier Compose, flèches, surbrillances, design system.
- `:core:srs` — algorithme de répétition espacée FSRS.
- `:core:pedagogy` — détection de motifs, profil d'apprentissage, gabarits de phrases (pur Kotlin).
- `:core:compute` — routage tablette / station PC, découverte, appairage, file d'attente.
- `:feature:play`, `:feature:puzzles`, `:feature:repertoire`, `:feature:training`, `:feature:review`, `:feature:teacher`, `:feature:endgames`, `:feature:vision`, `:feature:books`, `:feature:progress` — un module par domaine fonctionnel.

### Principes clés

- **Le moteur de règles fait foi.** Rien n'entre en base (coup importé, ligne lue sur photo, réponse du professeur) sans validation par `:core:chess`.
- **L'IA commente, le moteur calcule.** Le professeur reçoit l'évaluation et les meilleures lignes Stockfish et ne fait que les expliquer, pour éviter les coups inventés.
- **Hors ligne d'abord.** Jeu, puzzles déjà importés, répertoire et révisions fonctionnent sans réseau ; les appels réseau passent par une file synchronisée via WorkManager.
- **Interface moteur unique.** Stockfish et Maia implémentent la même interface ; on peut ajouter un autre moteur sans toucher aux écrans.

### Fonctionnement du Professeur

Le professeur combine trois couches : le moteur établit les faits, un détecteur de motifs les traduit en notions d'échecs, puis la rédaction les explique. La rédaction hors ligne est instantanée et gratuite ; l'IA générative n'intervient que pour les explications riches et les questions libres.

1. **Analyse moteur** : Stockfish en MultiPV (3 lignes) sur la position avant et après ton coup ; l'écart est converti en perte de chances de gain (pourcentage), plus parlant que les centipions.
2. **Détection de motifs** (`:core:pedagogy`, Kotlin pur) : pièces en prise (échange statique), fourchettes, clouages, enfilades, mat menacé, roi exposé, développement, structure de pions. Le résultat est une liste de « faits pédagogiques » structurés.
3. **Choix du message** : selon l'intensité réglée et le palier, le professeur retient un ou deux faits, jamais tous.
4. **Rédaction** : gabarits de phrases en français pour le retour immédiat (hors ligne, moins de 200 ms) ; LLM local sur demande (« Explique-moi plus ») ou pour les questions libres — sur le PC, ou en version courte sur la tablette, avec uniquement les faits et lignes moteur en entrée.
5. **Mémoire pédagogique** : chaque fait est enregistré dans le profil d'apprentissage (thème, fréquence, dernière occurrence), qui alimente les rappels, la séance du jour et les puzzles personnels.

### IA locale : deux niveaux de calcul, aucun cloud

Chaque tâche est envoyée au meilleur niveau disponible : la tablette seule suffit pour jouer et apprendre, ton PC (RTX 4080 12 Go, i9, 32 Go) multiplie la puissance dès qu'il est joignable. Aucun service d'IA en ligne n'est utilisé, et aucune fonction n'exige le PC : il ne fait qu'améliorer la qualité et la vitesse.

| Tâche | Tablette (hors ligne) | Station PC (réseau local) |
| --- | --- | --- |
| Analyse d'un coup | Stockfish arm64, profondeur 16–20 | Stockfish multi-cœurs, limité par temps (profondeur 20 en 0,9 s) |
| Adversaire humain | Maia ONNX complet (réseaux légers, 1,5–3,6 ms par coup) | — (lc0 CUDA plus lent que la tablette, mesuré en S0-7) |
| Retour du professeur | Gabarits (pas de LLM embarqué en v1, ADR 0003) | Gabarit + phrase de qwen3:14b via Ollama, à côté |
| Question libre au professeur | Non disponible hors ligne (gabarit détaillé) | LLM local, réponses riches |
| Lecture de pages de livres | ML Kit (texte) + validation par les règles | Modèle vision local (texte et diagrammes → FEN) |
| Revue de toute la base de parties | À la demande, partie par partie | Analyse de nuit de toute la bibliothèque |
| Modèle personnel (« Maia de toi ») | Exécution du modèle | Entraînement sur tes parties |

**Appareil cible : tablette Honor, Snapdragon 6 Gen 1, 8 Go de RAM, écran 2560 × 1600.** Ce profil fixe ce qui tourne en local :

| Composant | Verdict sur la tablette | Détail |
| --- | --- | --- |
| Stockfish | Confortable | 4 threads : profondeur 16 en 0,6 s, 18 en 1,7 s, 20 en 5,5 s (médianes S0-4) |
| Maia | Confortable | Réseaux Maia très petits ; un coup en politique seule prend quelques millisecondes |
| Détection de motifs et gabarits | Instantané | Kotlin pur, aucune contrainte |
| LLM embarqué 1–2 Md (4 bits) | Écarté en v1 (S0-6) | 3,6–4,8 s par phrase, 40 % de réponses fausses, 1,8–2,4 Go ; voir ADR 0003 |
| LLM 3 Md et plus | Déconseillé | Trop lent et trop gourmand pour rester fluide ; réservé au PC |
| Modèle vision pour les livres | Non | Lecture de texte simple par ML Kit ; diagrammes et pages complexes sur le PC |

La « RAM turbo » d'Honor (8 Go supplémentaires) est de la mémoire d'échange sur le stockage : elle n'accélère pas un LLM et ne doit pas être comptée. L'application vise moins de 3 Go de mémoire au total. Le NPU Hexagon n'est pas exploité en v1, son support par les outils open source étant incertain sur cette puce.

**Conséquence pour l'écran** : à 2560 × 1600, l'interface principale est en paysage, avec l'échiquier à gauche et le panneau du professeur (verdict, explication, lignes moteur) à droite ; le portrait empile les deux.

**Règles de routage**

1. Le retour immédiat après un coup est toujours calculé sur la tablette (latence < 1 s), puis enrichi si le PC répond.
2. Les analyses longues (revue de base, modèle personnel, lecture de livre) partent en file vers le PC ; si le PC est éteint, elles attendent ou s'exécutent localement en version réduite.
3. Aucun appel à un service d'IA externe : si ni la tablette ni le PC ne peuvent faire une tâche, elle est mise en attente et signalée.
4. Le résultat indique toujours sa provenance (« analyse PC, profondeur 32 ») pour juger sa fiabilité.

### Station d'analyse PC

Un serveur léger installé sur ton PC Windows, sur le modèle déjà utilisé pour tes autres applications.

- **Serveur** : Python + FastAPI, lancé par un `.bat` ou en service Windows ; points d'API `/analyze`, `/play`, `/explain`, `/read-page`, `/train`, `/health`.
- **Moteurs** : Stockfish x86-64 avec tous les cœurs, lc0 avec backend CUDA pour Maia et les réseaux forts.
- **IA** : Ollama (ou llama.cpp) avec un LLM 8–14 Md quantifié en 4 bits (environ 6–9 Go de VRAM) et un modèle vision pour les pages de livres ; les modèles exacts sont choisis au spike.
- **Connexion** : même Wi-Fi, découverte automatique (mDNS / NSD Android), appairage par QR code affiché sur le PC, jeton et chiffrement TLS ; en option USB via `adb reverse`, ou accès hors domicile par un VPN maillé de type Tailscale.
- **Partage du GPU** : file de priorité pour ne pas entrer en conflit avec tes travaux ComfyUI ; le serveur peut se mettre en pause quand le GPU est occupé.

### Idées d'IA avancées

- **Maia personnel** : affiner Maia sur tes propres parties pour obtenir un bot qui joue comme toi. Il sert à prédire tes erreurs typiques, à t'entraîner contre « toi d'il y a trois mois » et à mesurer tes progrès.
- **Professeur qui cite tes livres** : les pages importées sont indexées (recherche sémantique) ; le professeur peut renvoyer à « ton livre, chapitre 3, page 42 » quand une position y ressemble.
- **Reconnaissance de diagrammes** : un modèle vision transforme un diagramme de livre en position FEN, pour en faire un puzzle ou une étude.
- **Échiquier réel** (après la v1) : photographier une position sur un vrai échiquier pour l'analyser ou reprendre une partie.

## 4. Sources de données, APIs et licences

Toutes les briques sont gratuites ; la seule contrainte forte est la licence GPL de Stockfish et de Maia/lc0, sans conséquence pour un usage personnel mais structurante en cas de publication. Les détails d'accès sont à revérifier au sprint 0, ces services évoluant.

| Brique | Usage dans l'app | Licence / conditions |
| --- | --- | --- |
| [Stockfish](https://stockfishchess.org) | Analyse, adversaires moteur | GPL v3 : app publiée = code source publié |
| [Maia Chess](https://www.maiachess.com) (Maia-1 par tranche, Maia-2 continu) | Adversaires humains | Poids sous GPL v3 ; export ONNX à valider |
| [Base de puzzles Lichess](https://database.lichess.org) | Puzzles notés et thématiques | CC0 (domaine public) |
| [Base de parties Lichess](https://database.lichess.org) | Statistiques d'ouvertures hors ligne (optionnel) | CC0 |
| [Noms d'ouvertures Lichess](https://github.com/lichess-org/chess-openings) | Nommage ECO des positions | CC0 |
| [API Lichess](https://lichess.org/api) | Import de parties, explorateur d'ouvertures, tables de finales | Gratuite ; jeton personnel conseillé, limites de débit |
| [API publique Chess.com](https://www.chess.com/news/view/published-data-api) | Import de parties par mois | Lecture seule, gratuite, sans authentification |
| LLM et modèle vision open source (choisis au spike) | Professeur, lecture des livres | Licence du modèle à vérifier (usage personnel) |
| chesslib | Règles et formats | Apache 2.0 |
| FSRS | Répétition espacée | MIT |

**Droits sur les livres.** L'import par photo est conçu pour un usage personnel des livres que tu possèdes : le contenu reste sur l'appareil et n'est jamais redistribué.

## 5. Modèle de données

La base Room s'organise autour de la position (FEN normalisée) : parties, répertoire, puzzles et erreurs s'y rattachent, ce qui permet de relier une erreur de partie à la ligne de répertoire concernée.

| Entité | Champs principaux | Relations |
| --- | --- | --- |
| PlayerProfile | pseudo Lichess, pseudo Chess.com, palier, ELO déclaré, préférences | 1 seul profil |
| Game | source, date, cadence, couleur, adversaire, ELO, résultat, PGN | → Move (n), → Analysis (1) |
| Move | ply, SAN, FEN après, temps restant, évaluation, classification | → Game |
| Analysis | moteur, profondeur, précision par phase, statut | → Game |
| Mistake | ply, gravité, phase, cause, thème tactique, meilleur coup | → Game, → Puzzle personnel |
| Repertoire | nom, couleur, description | → RepertoireNode (n) |
| RepertoireNode | FEN, coup SAN, parent, commentaire, flèches, source (livre, page) | arbre ; → ReviewCard (1) |
| ReviewCard | stabilité, difficulté, échéance, historique FSRS | → RepertoireNode ou Puzzle |
| Puzzle | id Lichess ou perso, FEN, solution, note, thèmes | → PuzzleAttempt (n) |
| PuzzleAttempt | date, réussite, temps, classement avant/après | → Puzzle |
| BookImport | photo, texte extrait, lignes proposées, statut de validation | → RepertoireNode (n) |
| RatingSnapshot | date, source (Lichess, Chess.com, puzzle, bots), valeur | série temporelle |
| TrainingSession | date, exercices proposés et réalisés, score | → exercices |

Les millions de puzzles Lichess ne sont pas tous embarqués : un sous-ensemble filtré (par exemple 200 000 puzzles répartis par note et par thème) est livré dans une base préremplie, le reste téléchargeable par tranches.

Le professeur ajoute deux entités : **LearnerProfile** (palier, intensité choisie, préférences de ton) et **WeaknessStat** (thème, nombre d'occurrences, dernière occurrence, tendance), reliée aux entités Mistake et Puzzle.

Le mode « Jouer le livre » ajoute **Book** (titre, auteur, notation utilisée) et **BookChapter** (ordre, titre, pages, progression : lu, joué, maîtrisé), reliées aux pages importées (BookImport) et aux nœuds de répertoire (RepertoireNode), dont chacun garde le commentaire de l'auteur et la page d'origine.

## 6. Plan de sprints

Quatorze sprints de deux semaines (environ sept mois en solo avec Claude Code) livrent un premier professeur jouable dès le sprint 4, puis l'enrichissent module par module. Chaque sprint se termine par une démo sur appareil réel.

| Sprint | Objectif | Modules | Livrable démontrable |
| --- | --- | --- | --- |
| S0 | Fondations | socle | Projet compilé, CI verte, échiquier affiché, spikes IA conclus |
| S1 | Échiquier et règles | core:chess, core:ui | Partie à deux joueurs sur l'écran, import/export PGN |
| S2 | Moteur Stockfish | core:engine | Analyse en direct, partie contre Stockfish |
| S3 | Adversaires humains | M1 | Partie contre un bot Maia d'un ELO choisi, avec pendule |
| S4 | Professeur v1 : mode apprentissage | M6, core:pedagogy | Partie commentée coup par coup, indices, reprise de coup |
| S5 | Puzzles | M2 | Puzzles par thème avec classement Glicko-2, professeur intégré |
| S6 | Revue de parties | M5 | Parties Lichess/Chess.com importées, analysées et commentées |
| S7 | Répertoire | M3 | Arbre d'ouvertures, import PGN, explorateur |
| S8 | Entraînement d'ouvertures | M4 | Drill en répétition espacée, professeur sur les déviations |
| S9 | Station PC et IA locale | serveur PC, core:compute | Tablette appairée au PC, analyses et LLM déportés |
| S10 | Livres : import photo et cours jouables | M3, M4 | Livre photographié transformé en cours jouable |
| S11 | Professeur v2 et finales | M6, M7 | Explications par LLM local, questions libres, parcours de finales |
| S12 | Progression, plan adaptatif, Maia personnel | M8, M9 | Tableau de bord, séance du jour, bot qui joue comme toi |
| S13 | Finitions et version 1.0 | tous | Version stable sur la tablette |

### S0 — Fondations

Détaillé story par story dans `docs/sprints/s0.md`.

- [ ] Créer le projet Android multi-modules (structure de la section 3), version catalog Gradle.
- [ ] Configurer Hilt, Room, Compose, navigation, thème clair/sombre.
- [ ] Mettre en place ktlint, detekt, tests unitaires et GitHub Actions.
- [ ] Rédiger CLAUDE.md et les premiers ADR (décisions d'architecture).
- [ ] Spikes techniques : Stockfish et Maia ONNX sur la tablette cible (temps par coup, mémoire) ; petit LLM embarqué 1–2 Md sur Snapdragon 6 Gen 1 (vitesse en jetons/s, mémoire, chauffe) ; LLM 8–14 Md et lc0 CUDA sur le PC.

Critères d'acceptation : `./gradlew build` et les tests passent en CI ; le spike conclut à une solution Maia viable ou à un plan B documenté.

### S1 — Échiquier et règles

Détaillé story par story dans `docs/sprints/current.md` (archivé en `s1.md` à la clôture du sprint).

- [x] En tant que joueur, je déplace les pièces par glisser ou par toucher, avec coups légaux en surbrillance (désactivable).
- [x] Promotion, roque, prise en passant, mat, pat, répétition, règle des 50 coups (et matériel insuffisant).
- [x] Retourner l'échiquier, naviguer dans les coups, notation SAN affichée (en français dans la liste).
- [x] Import et export PGN et FEN.
- [x] Flèches et cases colorées dessinées au doigt.

Critères d'acceptation : suite de tests perft validée sur les positions de référence (tenu) ; 100 PGN réels (parties de Heidoji) réimportés à l'identique (tenu).

### S2 — Moteur Stockfish

- [ ] Intégrer Stockfish via le NDK derrière l'interface `ChessEngine` (protocole UCI).
- [ ] Barre d'évaluation et 3 meilleures lignes en analyse libre.
- [ ] Adversaire Stockfish réglable (niveau, limite d'ELO, temps par coup).
- [ ] File d'analyses en arrière-plan avec WorkManager, annulable.

Critères d'acceptation : analyse à profondeur 18 d'une position en moins de 5 s sur la tablette ; aucun blocage de l'UI.

### S3 — Adversaires humains

- [ ] Intégrer Maia (selon le résultat du spike S0) avec choix de l'ELO adverse.
- [ ] Personas : nom, ELO, style, ouvertures préférées.
- [ ] Pendules (rapide, blitz, personnalisée), abandon, nulle, revanche.
- [ ] Enregistrement automatique des parties jouées dans la base.
- [ ] Choix du palier au premier lancement (ELO saisi ou « je ne sais pas »).

Critères d'acceptation : sur 20 parties tests, le bot 1100 commet nettement plus d'erreurs que le bot 1900 ; la partie se reprend après fermeture de l'app.

### S4 — Professeur v1 : mode apprentissage

- [ ] Créer `:core:pedagogy` : détection des pièces en prise, fourchettes, clouages, enfilades, mat menacé, roi exposé, retard de développement.
- [ ] Calcul de la perte de chances de gain à partir de l'analyse MultiPV avant et après le coup.
- [ ] Panneau du professeur (`:feature:teacher`) à droite de l'échiquier : verdict, explication courte, flèche du meilleur coup.
- [ ] Gabarits de phrases en français par motif et par palier (rédaction hors ligne), à partir de faits déjà rédigés en français (notation française, noms de pièces).
- [ ] Indices gradués en trois niveaux et alerte de menace avant le coup.
- [ ] Reprise de coup proposée après une erreur ou une gaffe.
- [ ] Réglage de l'intensité (silencieux, ponctuel, bavard).
- [ ] Bilan de fin de partie en trois points et enregistrement des faits dans WeaknessStat.

Critères d'acceptation : sur un corpus de 100 positions annotées à la main, le motif principal est correctement identifié dans au moins 85 % des cas ; le retour après un coup s'affiche en moins de 1 s ; le professeur ne propose jamais un coup absent de l'analyse moteur.

### S5 — Puzzles

- [ ] Script d'extraction : sous-ensemble de la base Lichess filtré par note et thème, livré en base préremplie.
- [ ] Écran de puzzle : coup adverse joué, recherche de la solution, indice, solution.
- [ ] Classement Glicko-2 du joueur, sélection de puzzles autour de ce classement.
- [ ] Modes par thème, série chronométrée, révision des ratés.

Critères d'acceptation : le classement converge après environ 50 puzzles ; les variantes alternatives qui matent aussi sont acceptées.

### S6 — Revue de parties

- [ ] Import Lichess et Chess.com par pseudo (Heidoji), incrémental (seulement les nouvelles parties).
- [ ] Analyse automatique, classification des coups, précision par phase.
- [ ] Écran de revue : courbe d'évaluation, sauts vers les erreurs, meilleur coup, commentaire du professeur.
- [ ] Génération de puzzles personnels à partir des gaffes et erreurs.
- [ ] Bibliothèque de parties avec filtres (ouverture, résultat, cadence, adversaire).

Critères d'acceptation : 200 parties importées et analysées sans intervention ; chaque gaffe produit un puzzle jouable.

### S7 — Répertoire d'ouvertures

- [ ] Créer des répertoires Blancs et Noirs, ajouter des lignes en jouant sur l'échiquier.
- [ ] Commentaires, flèches, source (livre, chapitre, page) sur chaque nœud.
- [ ] Import PGN et étude Lichess, fusion de variantes sans doublons (transpositions détectées par FEN).
- [ ] Explorateur : statistiques de la base Lichess filtrées par niveau, évaluation moteur.
- [ ] Liaison avec la revue : « tu es sorti de ton répertoire au coup 8 ».

Critères d'acceptation : import d'un PGN de 500 variantes sans perte ; les transpositions pointent vers le même nœud.

### S8 — Entraînement d'ouvertures

- [ ] Implémenter FSRS dans `:core:srs` avec tests.
- [ ] Modes apprentissage, drill et déviation.
- [ ] File de révisions quotidienne, notifications optionnelles.
- [ ] Partie d'entraînement : Maia joue ton répertoire puis continue hors théorie.
- [ ] Détection des trous du répertoire (réponses fréquentes non couvertes).

Critères d'acceptation : une ligne ratée revient le lendemain, une ligne réussie trois fois s'espace de plusieurs jours.

### S9 — Station PC et IA locale

- [ ] Serveur FastAPI sur le PC : `/health`, `/analyze`, `/play`, `/explain`, lancé par un `.bat`.
- [ ] Stockfish multi-cœurs sur le PC, analyses limitées par temps (Maia reste sur la tablette, ADR 0005).
- [ ] LLM local via Ollama (qwen3:14b, repli mistral-nemo:12b), préchargé au démarrage, avec un prompt « professeur » qui ne reçoit que les faits pédagogiques pré-traduits et les lignes moteur.
- [ ] Module `:core:compute` côté Android : découverte mDNS, appairage par QR code, jeton, TLS, état de connexion visible.
- [ ] Routage des tâches selon les règles de la section 3 ; file d'attente persistante quand le PC est éteint.
- [ ] Analyse de nuit de toute la bibliothèque de parties depuis le PC.

Critères d'acceptation : appairage en moins de 30 s ; bascule automatique tablette ↔ PC sans perte de tâche ; une analyse limitée à 3 s revient du PC avec au moins la profondeur 22 en milieu de partie ; l'app reste pleinement utilisable PC éteint.

### S10 — Livres : import photo et cours jouables

- [ ] Prise de photo ou sélection en galerie, recadrage, plusieurs pages à la suite.
- [ ] Envoi au modèle vision de la station PC (ML Kit sur la tablette pour le texte simple) avec un prompt qui renvoie un JSON structuré : lignes, coups, commentaires, notation détectée (française R/D/T/F/C, anglaise, figurine).
- [ ] Validation coup par coup par `:core:chess` ; les coups illégaux ou illisibles sont signalés.
- [ ] Écran de correction : position affichée à chaque coup douteux, correction en un geste.
- [ ] Intégration dans un répertoire avec la source (titre, page).
- [ ] Reconnaissance des diagrammes imprimés en position FEN, transformables en puzzle ou en étude.
- [ ] Organisation en livre et chapitres, dans l'ordre de l'auteur, avec les commentaires rattachés à chaque coup.
- [ ] Mode « Jouer le livre » : lecture guidée, à toi de jouer, parcours des variantes, quiz des positions clés.
- [ ] Partie d'application contre Maia depuis l'ouverture du chapitre, avec signal de sortie de la théorie du livre.
- [ ] Progression par chapitre (lu, joué, maîtrisé) et entrée des lignes dans la répétition espacée.

Critères d'acceptation : sur 10 pages tests, plus de 95 % des coups sont lus correctement avant correction ; aucun coup illégal n'entre en base.

### S11 — Professeur v2 et finales

- [ ] Bouton « Explique-moi plus » partout où le professeur parle (jeu, puzzles, revue) : le LLM de la station PC (hors ligne : gabarit détaillé) reçoit FEN, coup joué, faits pédagogiques et lignes moteur, et rédige une explication approfondie selon le palier, affichée à côté du gabarit exact.
- [ ] Questions libres sur une position et mode « je réfléchis à voix haute » (le professeur commente ton plan avant que tu joues).
- [ ] Parcours de finales par palier, joué contre Stockfish, vérifié par tables de finales.

Critères d'acceptation : le professeur ne propose jamais un coup absent de l'analyse moteur fournie ; chaque finale du parcours a une condition de réussite claire.

### S12 — Progression, plan adaptatif et Maia personnel

- [ ] Tableau de bord : courbes d'ELO, précision par phase, thèmes faibles, couverture du répertoire.
- [ ] Détection des faiblesses à partir des erreurs classées.
- [ ] Séance du jour générée (puzzles ciblés, lignes à réviser, une finale, un exercice de visualisation).
- [ ] Exercices de calcul et de visualisation (M8).
- [ ] Test de placement pour les joueurs sans ELO.
- [ ] Maia personnel : affinage de Maia sur tes parties (sur le PC, lc0 CUDA), puis bot « toi » jouable sur la tablette et prédiction de tes erreurs typiques.
- [ ] Indexation sémantique des pages de livres importées, pour que le professeur cite tes sources.

Critères d'acceptation : la séance du jour change quand les faiblesses changent ; le test de placement estime un palier en moins de 10 minutes.

### S13 — Finitions et version 1.0

- [ ] Mise en page tablette soignée (échiquier et analyse côte à côte), portrait et téléphone fonctionnels.
- [ ] Accessibilité (contraste, TalkBack sur les coups), performance, consommation batterie.
- [ ] Sauvegarde et restauration de la base (export fichier, copie vers le PC).
- [ ] Correction des anomalies, tests de bout en bout des parcours principaux.
- [ ] Mentions des licences dans l'application (Stockfish, Maia, modèles) ; préparation Play Store reportée.
- [ ] Téléchargement des modèles (réseaux Maia ; pas de LLM sur la tablette) au premier lancement ou copie depuis la station PC.
- [ ] APK signé installé directement sur la tablette, avec procédure de mise à jour depuis le PC.

Critères d'acceptation : aucun crash sur une semaine d'usage ; démarrage à froid sous 2 s.

## 7. Definition of Done et qualité

Une user story est terminée quand elle remplit tous les points ci-dessous ; un sprint est terminé quand toutes ses stories le sont et que la démo a été faite sur la tablette.

**Definition of Done**

1. Code compilé sans avertissement, ktlint et detekt sans erreur.
2. Tests unitaires écrits pour la logique de domaine (couverture visée : 80 % sur `:core:*`).
3. Tests UI Compose pour les écrans modifiés (parcours principal).
4. Aucune régression : toute la suite passe en CI.
5. Fonctionne en mode sombre, en portrait et en paysage, sur la tablette.
6. Textes de l'interface en ressources (français par défaut, anglais prévu).
7. CLAUDE.md et ADR mis à jour si une décision d'architecture a changé.
8. Commit au format Conventional Commits, branche fusionnée dans `main`.

**Stratégie de tests**

| Niveau | Outil | Cible |
| --- | --- | --- |
| Règles d'échecs | JUnit 5, perft | Génération de coups exacte sur positions de référence |
| Formats | JUnit 5, corpus PGN réels | Import/export sans perte |
| Domaine | JUnit 5, Turbine | Cas d'usage, FSRS, Glicko-2, classification d'erreurs, motifs |
| Moteurs | Tests instrumentés | Réponse UCI, temps par coup, arrêt propre |
| Données | Room in-memory | Migrations de schéma, requêtes |
| UI | Compose UI Test | Parcours : jouer, puzzle, revue, drill, livre |
| IA | Jeu de photos et de positions de référence | Taux de lecture des livres, cohérence du professeur |

## 8. Organisation du projet Claude

Le projet Claude sert de salle de conception (cadrage, décisions, revues) ; Claude Code exécute chaque sprint dans le dépôt, guidé par `CLAUDE.md`. Les deux partagent ce dossier comme référence unique.

### Mise en place

1. Créer un projet Claude nommé « Chess Companion ».
2. Coller les instructions de `docs/prompts/instructions-projet-claude.md` dans ses instructions.
3. Ajouter ce dossier aux connaissances du projet, puis au fil de l'eau les ADR et le compte rendu de chaque sprint.
4. Créer le dépôt Git avec `CLAUDE.md` à la racine et `docs/` (ce dossier, `docs/adr/`, `docs/sprints/`, `docs/prompts/`).

### Déroulé d'un sprint

1. **Planification (projet Claude)** : relire le sprint dans ce dossier, l'ajuster, générer `docs/sprints/current.md` avec les stories et critères.
2. **Exécution (Claude Code)** : une story à la fois, sur sa branche, avec le prompt type ci-dessous.
3. **Revue** : tests verts, démo sur la tablette, cases cochées dans `current.md`.
4. **Rétrospective (projet Claude)** : compte rendu ajouté aux connaissances du projet (ce qui a marché, dette technique, ajustements du sprint suivant).

**Prompt type pour une story dans Claude Code**

```markdown
Sprint S<n>, story : <titre de la story>
Module(s) : <:feature:… / :core:…>
Contexte : <ce qui existe déjà, fichiers clés>
À faire : <résultat attendu côté utilisateur>
Critères d'acceptation : <liste>
Tests attendus : <unitaires / UI>
Commence par un plan court, puis implémente, lance ./gradlew test et résume les changements.
```

## 9. Risques et décisions

Le risque principal est technique : faire tourner Maia et un petit LLM sur la tablette avec un temps de réponse acceptable. Il est traité dès les spikes du sprint 0, avant d'engager la suite.

| Risque | Impact | Parade |
| --- | --- | --- |
| Maia trop lent ou trop lourd sur mobile | Pas d'adversaires humains hors ligne | Spike S0 ; Maia complet sur le PC ; plan B tablette : Maia-1 en politique seule (1 nœud) |
| LLM embarqué trop lent ou trop gourmand en RAM | Professeur limité hors ligne | Gabarits hors ligne toujours disponibles ; LLM réservé au PC |
| PC éteint ou hors réseau | Analyses profondes indisponibles | File d'attente persistante, version réduite sur la tablette |
| Conflit GPU avec d'autres usages (ComfyUI) | Lenteur des deux côtés | File de priorité, pause automatique du serveur |
| Sécurité du serveur local | Accès non autorisé au PC | Appairage par QR code, jeton, TLS, écoute sur le réseau local uniquement |
| Licence GPL (Stockfish, Maia, lc0) | Publication future contrainte | Usage personnel ; dépendances compatibles GPL v3 pour garder la porte ouverte |
| Lecture des photos imprécise | Répertoire faux | Validation par le moteur de règles et écran de correction obligatoire |
| Professeur qui invente des coups | Perte de confiance | Le LLM ne reçoit que les faits et l'analyse moteur, et ne les contredit jamais |
| Changement des APIs Lichess / Chess.com | Import cassé | Clients isolés dans `:core:data`, tests de contrat |
| Taille de la base de puzzles | Application trop lourde | Sous-ensemble embarqué, téléchargement par tranches |
| Dérive du périmètre | Sprints qui glissent | Toute nouvelle idée va au backlog, jamais dans le sprint en cours |

**Décisions prises**

| Sujet | Décision | Conséquence |
| --- | --- | --- |
| Niveau de départ | ELO 1300 | Palier Club : calcul, plans de milieu de partie, finales de base, premier répertoire ; adversaires Maia de 1100 à 1500 par défaut |
| Comptes | Heidoji sur Lichess et Chess.com | Imports du sprint 6 ; parties utilisées comme corpus de test dès le S0 |
| Appareil cible | Tablette Honor (Snapdragon 6 Gen 1, 8 Go, 2560 × 1600) | Référence unique des performances ; le téléphone reste supporté sans être optimisé |
| Distribution | Usage personnel pour l'instant, pas de publication | APK installé directement ; dépendances gardées compatibles GPL v3 |
| IA | 100 % locale, aucune API en ligne | Pas de LLM embarqué en v1 (spike S0-6) ; LLM qwen3:14b sur la station PC |

**Si une publication sur le Play Store est décidée plus tard.** C'est possible : plusieurs applications d'échecs publiées embarquent Stockfish. La condition est que l'application entière soit sous licence GPL v3, avec son code source public. Il faudra alors vérifier les licences de toutes les dépendances et des modèles, ne jamais embarquer de contenu de livre, télécharger les gros modèles hors de l'APK (Play Asset Delivery), et fournir une politique de confidentialité. Décision à réévaluer après la version 1.0.
