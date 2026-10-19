# Chess Companion

Application Android d'entraînement aux échecs, centrée sur un professeur qui conseille,
analyse et explique chaque coup. IA 100 % locale (tablette + station PC).

## Démarrage

1. **Prérequis PC (Windows)**
   - Android Studio (dernière version stable) avec, dans *SDK Manager → SDK Tools* :
     **NDK (Side by side)** et **CMake**.
   - JDK 17 (fourni avec Android Studio).
   - Git et Claude Code.
   - Python 3.11+ (pour la station PC, à partir du S0-7).
2. **Tablette** : activer les *Options pour les développeurs* puis le *Débogage USB*,
   brancher en USB et accepter l'empreinte du PC.
3. **Projet Claude** : créer le projet « Chess Companion », coller
   `docs/prompts/instructions-projet-claude.md` dans ses instructions et ajouter
   `docs/dossier-conception.md` à ses connaissances.
4. **Dépôt** : dans ce dossier, `git init`, premier commit du kit.
5. **Sprint 0** : lancer `claude` à la racine, puis suivre `docs/prompts/prompts-sprint-0.md`
   story par story.

## Arborescence du kit

```
CLAUDE.md                          Guide lu automatiquement par Claude Code
README.md                          Ce fichier
.gitignore
docs/
  dossier-conception.md            Référence : vision, architecture, sprints, risques
  backlog.md                       Idées hors sprint
  sprints/current.md               Sprint en cours, stories et critères
  sprints/s0.md                    Sprint terminé (S1 le rejoint à sa clôture)
  adr/                             Décisions d'architecture (0001 à 0007)
  spikes/rapport-s0.md             Mesures des spikes de S0
  prompts/                         Instructions du projet Claude, prompts du S0
testdata/pgn/                      Corpus de parties de Heidoji pour les tests
```

## Modules

| Module | Rôle | État |
| --- | --- | --- |
| `:core:chess` | Règles, FEN, SAN, PGN, arbre de coups (Kotlin pur) | S1 : perft exact, 100 parties réimportées |
| `:core:ui` | Échiquier Compose interactif, annotations | S1 |
| `:core:engine` | `ChessEngine`, Stockfish (JNI), Maia (ONNX) | coups légaux par les règles depuis S1-5 |
| `:core:data` | Room, dépôts | squelette |
| `:feature:play` | Écran de partie à deux joueurs | S1 (créé avant S3, voir ADR 0007) |
| `:app` | Navigation, thème, écran « Debug moteur » | |

## Moteur Stockfish (à partir de S0-4)

Le réseau NNUE (98 Mo) n'est pas versionné : avant le premier `installDebug`, lancer
`powershell scripts/fetch-stockfish-net.ps1`. Le NDK 29.0.14206865 et CMake 3.31.6 sont requis.
Le moteur n'existe que pour `arm64-v8a` (la tablette, pas l'émulateur x86).

## Réseaux Maia (à partir de S0-5)

Les réseaux Maia-1 et leur conversion ONNX ne sont pas versionnés. Une fois :

1. `powershell scripts/fetch-maia-nets.ps1` (poids Maia 1100/1500/1900 et lc0 pour Windows dans `build/maia/`) ;
2. `python tools/maia2onnx/maia2onnx.py` (écrit `core/engine/src/main/assets/maia-*.onnx` et régénère la
   référence de test `maia-reference.tsv`).

Sans les modèles, `MaiaPipelineTest` est ignoré (cas de la CI).

## Commandes

| Action | Commande |
| --- | --- |
| Compiler | `gradlew.bat assembleDebug` |
| Installer sur la tablette | `gradlew.bat installDebug` |
| Tests (JUnit 5, dont l'UI sur la JVM) | `gradlew.bat test` |
| Perft profond (≈ 750 M nœuds, environ 1 min) | `gradlew.bat :core:chess:test -PslowTests` |
| Qualité | `gradlew.bat ktlintCheck detekt` |
| Corriger le style automatiquement | `gradlew.bat ktlintFormat` |

Sous Windows, `JAVA_HOME` doit pointer vers un JDK 17+ (par exemple
`C:\Program Files\Android Studio\jbr`).

## Qualité et intégration continue

- **ktlint** (plugin Gradle) : règles de `.editorconfig`.
- **detekt** 2.x : configuration par défaut complétée par `config/detekt/detekt.yml`.
- **JUnit 5** sur tous les modules ; un test d'exemple par module.
- **Tests UI Compose sur la JVM** (à partir de S1-6) : Robolectric, règle JUnit 4 exécutée par
  JUnit Vintage. Sous Windows, si le dossier personnel contient une espace (`C:\Users\Prénom Nom`),
  Robolectric ne charge pas sa bibliothèque native : ajouter dans `local.properties` la ligne
  `robolectric.userHome=C\:/Users/Public/robolectric-home` (dossier sans espace ; le premier lancement
  y télécharge le jar Android de Robolectric).
- **GitHub Actions** (`.github/workflows/ci.yml`) : qualité, tests et `assembleDebug`
  à chaque push sur `main` et chaque pull request.
