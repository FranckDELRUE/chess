# Rapport des spikes du sprint S0

Appareil : tablette Honor HEY2-W09, Android 14, Snapdragon 6 Gen 1 (4 × A78 + 4 × A55), 8 Go.
Les sections S0-6 et S0-7 seront ajoutées par leurs stories.

---

## S0-4 · Stockfish sur la tablette

**Date** : 2026-10-18 · **Version** : Stockfish 19 (`sf_19`), réseau `nn-1a298aa575a0.nnue` (98,5 Mo).
**Build** : NDK 29.0.14206865, CMake 3.31.6, `arm64-v8a`, `-O3 -march=armv8.2-a+dotprod`
(`USE_NEON=8`, `USE_NEON_DOTPROD`), sans LTO ni PGO. Bibliothèque `libstockfish_jni.so` : 1,6 Mo.
**Réglages** : 4 threads, `Hash` 64 Mo, `ucinewgame` avant chaque recherche. Build debug de l'app
(le code natif est optimisé de la même façon en debug et en release).

### Temps pour atteindre la profondeur N (10 positions, 4 threads)

| Pos | P16 (ms) | P18 (ms) | P20 (ms) | nps (k) à P20 | Éval. P20 (cp) |
|----:|---------:|---------:|---------:|--------------:|---------------:|
| 1 | 666 | 1 260 | 1 819 | 724 | +2 |
| 2 | 568 | 2 801 | 3 866 | 666 | +64 |
| 3 | 244 | 769 | 2 609 | 655 | −14 |
| 4 | 2 477 | 1 592 | 7 095 | 563 | +728 |
| 5 | 1 937 | 15 273 | 40 400 | 496 | +915 |
| 6 | 4 261 | 7 171 | 31 139 | 594 | +1 009 |
| 7 | 188 | 308 | 384 | 455 | 0 |
| 8 | 417 | 5 779 | 10 242 | 639 | +398 |
| 9 | 2 844 | 1 761 | 12 170 | 569 | +645 |
| 10 | 259 | 1 602 | 2 728 | 705 | +259 |
| **Médiane** | **617** | **1 682** | **5 481** | | |
| **Moyenne** | **1 386** | **3 832** | **11 245** | | |

- Vitesse : 450 à 725 kn/s, moyenne autour de 600 kn/s.
- Le temps varie beaucoup d'une position à l'autre (×100 entre P20 pos. 7 et pos. 5) et n'est pas
  strictement croissant avec la profondeur (pos. 4 et 9) : la recherche multi-thread est non
  déterministe et une position se résout vite ou lentement selon la tactique.
- **Limite des données** : les positions 4, 5, 6 et 9 sont très déséquilibrées (+6 à +10 pions) ;
  ce ne sont pas des milieux de partie équilibrés. Les temps restent valides pour dimensionner,
  mais la série mérite d'être remplacée par des positions équilibrées tirées du corpus Heidoji (S0-8).
- Non mesuré : 1 et 2 threads (le sélecteur existe dans l'écran de debug).

### Mémoire

- `dumpsys meminfo` après le lot complet : **PSS total ≈ 428 Mo** (tas natif ≈ 287 Mo, code ≈ 34 Mo).
- `Debug.getPss()` tout au long du lot : 413 à 420 Mo, stable (pas de fuite visible).
- Cette empreinte inclut le réseau NNUE (98 Mo) et 64 Mo de hash ; elle est très inférieure aux 8 Go.
- APK debug : 112 Mo. Le réseau est embarqué en assets puis copié dans `filesDir` au premier
  lancement, ce qui ajoute ≈ 100 Mo d'espace disque.

### Température après 5 minutes

Recherches de profondeur 20 enchaînées pendant 5 min (28 rondes), 4 threads :

| Instant | Température batterie |
|---------|---------------------:|
| Début | 22 °C |
| ≈ 2 min | 24 °C |
| ≈ 3 min | 26 °C |
| Fin (5 min) | 26 °C |

- `Thermal Status` Android : 0 (aucune limitation) à la fin.
- Vitesse soutenue : 485 à 740 kn/s sur toute la durée, sans baisse visible liée à la chauffe.
- Limite : la sonde est la température de la **batterie** (résolution 1 °C), pas celle du SoC ;
  `dumpsys thermalservice` ne donne pas les capteurs sur cet appareil.

### Conclusion

Stockfish embarqué est **confortable** pour le rôle décrit dans la conception : la profondeur 16
prend typiquement moins d'une seconde (médiane 0,6 s, 4,3 s au pire observé), la profondeur 18
quelques secondes, la profondeur 20 (médiane 5,5 s, jusqu'à 40 s) convient à l'analyse en tâche
de fond. Mémoire et chauffe ne posent aucun problème.

**Recommandation** : profondeur 16 pour le conseil avant coup, 18 pour l'analyse d'un coup joué,
20 et plus en tâche de fond ou sur la station PC.

### Reproduire les mesures

1. `scripts/fetch-stockfish-net.ps1` (une fois), puis `./gradlew installDebug`.
2. Dans l'app : bouton « Debug moteur », puis « Lot 10×3 » ou « Chauffe 5 min ».
3. Résultats : `adb logcat -s SF_BENCH`. Mémoire : `adb shell dumpsys meminfo com.chesscompanion`.

---

## S0-5 · Maia sur la tablette

**Date** : 2026-10-18 · **Réseaux** : Maia-1 1100, 1500, 1900 (CSSLab, release `v1.0`, GPL v3),
6 blocs SE × 64 filtres, entrée `INPUT_CLASSICAL_112_PLANE`, politique convolutive, valeur WDL.
**Conversion** : `lc0 leela2onnx` (lc0 v0.32.1, opset 17, float32), entrée `/input/planes`
(N × 112 × 8 × 8), sortie `/output/policy` (N × 1858). **Exécution** : ONNX Runtime Android 1.30.0,
politique seule (équivalent de lc0 `go nodes 1`), décodage par masquage des coups illégaux + softmax.

### Fidélité de la chaîne

L'encodeur (`Lc0Encoder`), la table des coups (`Lc0PolicyMap`) et le décodeur sont réécrits en Kotlin
d'après `encoder.cc` de lc0. `MaiaPipelineTest` (JVM, ONNX Runtime desktop) compare leurs priors à
ceux que lc0 calcule lui-même (`tools/maia2onnx/maia2onnx.py`) : **écart < 0,001 sur les 1 674 coups
légaux** des 20 positions × 3 réseaux, même coup choisi partout. Cas couverts : trait aux noirs
(miroir), roque des deux côtés (codé « roi prend tour » par lc0), prise en passant, promotions et
sous-promotions (la promotion en cavalier partage l'indice du coup simple), position initiale
(historique vide), règle des 50 coups.

### Temps par coup (20 positions × 5 passes, après une inférence à froid)

| Réseau | Config. | Chargement (ms) | 1re inférence (ms) | Médiane (ms) | p90 (ms) |
|-------:|---------|----------------:|-------------------:|-------------:|---------:|
| 1100 | CPU, 1 thread | 60,2* | 7,6 | 3,63 | 3,80 |
| 1100 | CPU, 4 threads | 17,3 | 7,6 | 1,57 | 4,68 |
| 1100 | XNNPACK, 1 thread | 26,0 | 3,7 | 2,88 | 2,95 |
| 1500 | CPU, 1 thread | 19,7 | 4,1 | 3,61 | 3,70 |
| 1500 | CPU, 4 threads | 18,2 | 10,5 | 1,53 | 1,72 |
| 1500 | XNNPACK, 1 thread | 22,4 | 3,7 | 2,89 | 2,97 |
| 1900 | CPU, 1 thread | 19,9 | 4,2 | 3,59 | 3,85 |
| 1900 | CPU, 4 threads | 17,3 | 4,8 | 1,54 | 1,70 |
| 1900 | XNNPACK, 1 thread | 27,4 | 3,9 | 2,91 | 3,33 |

\* inclut la création de l'environnement ONNX Runtime (une fois par processus).

- Encodage FEN → tenseur : 0,2 à 0,3 ms.
- **Coups légaux par Stockfish (`go perft 1`) : médiane 28 à 38 ms**, soit dix fois l'inférence.
  C'est le poste dominant (aller-retour UCI par tubes JNI) ; il disparaît avec le générateur de S1.
- Un coup Maia complet coûte donc ≈ 35 ms aujourd'hui, ≈ 4 ms après S1.

### Mémoire et taille

- Trois réseaux chargés en même temps : **+21 Mo de PSS** (466 → 487 Mo, Stockfish déjà chargé).
  PSS total de l'app après le lot : 498 Mo (428 Mo en S0-4 avec Stockfish seul).
- Fichiers : `.pb.gz` 1,25 à 1,31 Mo ; `.onnx` 3,48 Mo chacun (float32), 2,6 Mo compressés dans l'APK.
- `libonnxruntime.so` (arm64) : 33 Mo. **APK debug : 111,7 → 152,7 Mo (+41 Mo)**, dont 33 Mo pour
  ONNX Runtime et 7,8 Mo pour les trois modèles. Filtre `arm64-v8a` ajouté à `:app` (sinon les
  quatre ABI d'ONNX Runtime seraient embarquées).
- Température batterie : 22 → 23 °C sur le lot (≈ 2 min, Stockfish compris).

### Légalité et plausibilité (20 positions, `Temperature` 0 = coup le plus probable)

Perte = score Stockfish profondeur 16 du meilleur coup − score du coup Maia (`searchmoves`),
plafonnée à 1 000 cp ; « accord » = perte nulle.

| Réseau | Légaux | Arg-max brut légal | Accord Stockfish | Perte moyenne | Perte médiane |
|-------:|-------:|-------------------:|-----------------:|--------------:|--------------:|
| 1100 | 20/20 | 20/20 | 9/20 | 17 cp | 3 cp |
| 1500 | 20/20 | 20/20 | 11/20 | 15 cp | 0 cp |
| 1900 | 20/20 | 20/20 | 11/20 | 4 cp | 0 cp |

- La perte décroît avec le niveau, comme attendu. Les écarts typés « humain » apparaissent là où on
  les attend : grand roque risqué (pos. 6, 1100 : −147 cp ; 1500 joue Fxe6 : −112 cp), prise en
  passant tentante mais faible (pos. 13 : 1100 et 1500 jouent exf6 e.p., −115/−121 cp ; 1900 joue d4).
- Les trois niveaux prennent la dame en prise (pos. 10), trouvent le mat en un (pos. 14) et promeuvent
  en dame (pos. 17-18, prior 0,82 à 0,98).
- L'arg-max brut des 1858 sorties est toujours un coup légal : le masquage ne change rien ici,
  mais reste indispensable (règle « le moteur de règles fait foi »).
- Limites : une seule recherche Stockfish par coup (bruit de quelques cp), 20 positions seulement,
  plausibilité jugée sur la perte, pas sur la fréquence humaine réelle des coups.

### Écart au périmètre et dette

- **Coups légaux** : `:core:chess` n'a pas encore de générateur (prévu en S1). Le spike passe par
  `LegalMoveProvider`, implémenté par Stockfish `go perft 1` : Maia dépend donc d'une session
  Stockfish et paie ≈ 30 ms par coup. À remplacer par `:core:chess` en S1.
- **Champs FEN** hors placement (trait, roques, en passant, demi-coups) lus par `FenFields` dans
  `:core:engine`, sans validation par `:core:chess` (règle 2) : à migrer en S1.
- **Historique** : `ChessEngine.analyze` ne reçoit qu'une FEN ; l'historique est synthétisé comme
  lc0 (`fen_only`). En partie réelle, passer les 7 positions précédentes (évolution de l'interface en S3).
- **Coup déterministe** à `Temperature` 0 (même coup à chaque partie, comme les bots Lichess Maia) :
  prévoir variété d'ouvertures ou échantillonnage en S3 (`Temperature` existe déjà).
- **Taille** : ONNX Runtime complet (33 Mo) ; une build minimale (format ORT, opérateurs réduits)
  ramènerait quelques Mo. Float16/int8 non essayés (inutile vu les temps).
- **Tests JVM** : ONNX Runtime desktop 1.18.0 au lieu de 1.30.0, car les versions ≥ 1.19 plantent
  avec le `msvcp140.dll` 14.29 du JDK d'Android Studio. Sur la CI (pas de modèles), le test de bout
  en bout est ignoré.

### Conclusion

**Maia est viable sur la tablette** : 1,5 à 3,6 ms par coup en politique seule, +21 Mo de mémoire
pour trois niveaux, coups toujours légaux et pertes cohérentes avec le niveau. Le seul coût notable
est la taille de l'APK (+41 Mo).

**Recommandation** : Maia-1 en ONNX Runtime CPU, 1 thread (laisse les cœurs à Stockfish ; 4 threads
ou XNNPACK n'apportent que 1 à 2 ms). Charger le réseau à la demande (≈ 20 ms). Les autres tranches
(1200 à 1800) s'ajoutent sans changement de code.

**Plan B (non nécessaire, gardé pour mémoire)** : (1) Maia sur la station PC via lc0 CUDA (S0-7),
appelée en réseau local quand le PC est joignable ; (2) Stockfish « humanisé » hors ligne :
`UCI_LimitStrength` + `UCI_Elo`, et tirage pondéré parmi les coups MultiPV selon la perte.

### Reproduire les mesures

1. `scripts/fetch-maia-nets.ps1`, `python tools/maia2onnx/maia2onnx.py`, puis `./gradlew installDebug`.
2. Dans l'app : « Debug moteur », puis « Lot Maia 20×3 » (≈ 2 min).
3. Résultats : `adb logcat -s MAIA_BENCH`. Mémoire : `adb shell dumpsys meminfo com.chesscompanion`.
