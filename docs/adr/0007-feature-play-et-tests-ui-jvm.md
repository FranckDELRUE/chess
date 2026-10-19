# ADR 0007 — `:feature:play` dès S1, tests UI Compose sur la JVM avec Robolectric

- **Statut** : accepté (S1-6)
- **Date** : 2026-10-19

## Contexte
Deux décisions prises pendant le sprint S1 s'écartent du plan initial.

1. Le module `:feature:play` devait apparaître en S3 (parties contre un bot). L'écran de partie à deux
   joueurs de S1 (jouer, naviguer, importer, annoter) a besoin d'un ViewModel, d'écrans et de
   ressources : le mettre dans `:app` obligeait à le déplacer trois sprints plus tard.
2. La Definition of Done exige des tests UI Compose sur le parcours principal de chaque écran
   modifié. La CI GitHub n'a ni émulateur ni tablette, et le parcours doit rester testable à chaque
   pull request.

## Décision
1. **`:feature:play` est créé en S1-6** (validé en planification). Il dépend de `:core:chess` et de
   `:core:ui` seulement ; `:app` ne fait que l'inclure dans la navigation.
   - Un ViewModel par écran, état exposé en `StateFlow` immuable, **un seul point d'entrée
     `onAction(PlayAction)`** (UDF) ; les gestes (sélection, promotion, annotation) sont des machines à
     états immuables (`MoveInput`, `AnnotationInput`) testées sans Android.
   - Fichiers PGN lus et écrits par le Storage Access Framework derrière l'interface `GameDocuments`
     (aucune permission de stockage) ; elle rejoindra `:core:data` quand les imports Lichess et
     Chess.com arriveront (S6).
2. **Tests UI sur la JVM avec Robolectric** (`./gradlew test`), donc aussi en CI, sans émulateur.
   Dépendances de test seulement : Robolectric 4.16 (MIT), `compose-ui-test-junit4` (Apache 2.0),
   JUnit 4.13.2 (EPL 1.0) exécuté par JUnit Vintage. Rien de tout cela n'est livré dans l'APK ; JUnit 5
   (EPL 2.0) est déjà dans le même cas.
   - Robolectric tourne en SDK 35 (`@Config`), le SDK 37 de l'application n'étant pas garanti.
   - Les écrans exposent des `testTag` stables (`square-e4`, `move-0-0-1`, `nav-forward`…) et des
     descriptions accessibles en français : les tests s'en servent, les lecteurs d'écran aussi.
3. **Contournement Windows** : Robolectric ne charge pas sa bibliothèque native depuis un jar dont le
   chemin contient une espace (`C:\Users\Prénom Nom`). La ligne `robolectric.userHome=…` de
   `local.properties` (propre à la machine, non versionnée) déplace le `user.home` des JVM de test vers
   un dossier sans espace. La CI Linux n'est pas concernée. Voir le `README.md`.

## Conséquences
- Les tests UI ne remplacent pas la vérification sur la tablette : le rendu réel, la fluidité du
  glisser-déposer et le tracé des flèches au doigt se voient sur l'appareil (démo de fin de sprint).
- `local.properties` porte maintenant un réglage de test en plus de `sdk.dir` ; un nouveau poste
  Windows dont le dossier personnel contient une espace doit l'ajouter.
- Si Robolectric tarde à suivre un nouveau SDK Android, la version simulée reste figée en 35 sans
  gêner l'application.
- Alternative écartée : tests instrumentés (`androidTest`) seulement. Ils exigent un appareil branché,
  donc pas de CI, et chaque story reposerait sur un lancement manuel.
