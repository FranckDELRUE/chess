# Rapport des spikes du sprint S0

Appareil : tablette Honor HEY2-W09, Android 14, Snapdragon 6 Gen 1 (4 × A78 + 4 × A55), 8 Go.
Station PC (S0-7) : i9-13980HX, 32 Go, RTX 4080 Laptop 12 Go.

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

---

## S0-6 · LLM embarqué sur la tablette

**Date** : 2026-10-18 · **Moteur** : llama.cpp `v0.7.0` (MIT), CPU seul, lié en statique dans
`libllm_jni.so` (6,3 Mo après strip), module isolé `:spike:llm`.
**Build** : NDK 29, `-O3`, `GGML_CPU_ARM_ARCH=armv8.2-a+dotprod+fp16`, repack ARM et llamafile actifs,
pool de threads de ggml (sans OpenMP), sans KleidiAI. Code natif compilé en Release même en build debug.
**Réglages** : contexte 1024 jetons, poids en `mmap`, `temperature` 0,7, `top_k` 40, `top_p` 0,9
(graines 1 à 3), 96 jetons au plus ; décodage glouton pour la réponse à froid et le balayage des threads.

### Modèles testés

| Modèle | Quantification (source) | Fichier | Licence |
|--------|-------------------------|--------:|---------|
| Qwen3-1.7B (Alibaba) | Q4_K_M (unsloth) | 1 056 Mo | Apache 2.0 |
| EuroLLM-1.7B-Instruct (projet UTTER) | Q4_K_M (mradermacher) | 996 Mo | Apache 2.0 |

Apache 2.0 est compatible GPL v3 et permet l'usage personnel comme la redistribution. Les licences
sont celles des dépôts officiels (`Qwen/Qwen3-1.7B`, `utter-project/EuroLLM-1.7B-Instruct`) ; les GGUF
sont des conversions communautaires, vérifiées par SHA-256. Rien n'est embarqué dans l'APK.
Qwen3 est utilisé sans raisonnement (bloc `<think>` vide, réglage officiel `enable_thinking=False`).

### Prompts

- **JSON** (prompt du sprint) : consigne en français, lexique des motifs, puis le fait brut,
  par exemple `{"motif":"hanging_piece","piece":"knight","square":"f3","best":"Nd2"}`.
- **FRENCH** (variante ajoutée, voir « Écarts ») : les mêmes faits déjà traduits par le code
  (« Motif : pièce en prise, non défendue. Pièce : ton cavalier en f3. Meilleur coup : Cd2. »)
  et un exemple résolu dans la consigne.
- 5 faits (pièce en prise, fourchette, clouage, mat du couloir, prise gratuite) × 3 essais × 2 prompts.
  Les prompts font 190 à 237 jetons.

### Vitesse (4 threads, médianes sur 15 réponses par prompt)

| Modèle | Prompt | Premier jeton | Lecture du prompt | Génération | Jetons générés |
|--------|--------|--------------:|------------------:|-----------:|---------------:|
| Qwen3-1.7B | JSON | 3,27 s | 67 jetons/s | 12,3 jetons/s | 61 |
| Qwen3-1.7B | FRENCH | 3,14 s | 66 jetons/s | 12,5 jetons/s | 21 |
| EuroLLM-1.7B | JSON | 2,74 s | 83 jetons/s | 15,5 jetons/s | 34 |
| EuroLLM-1.7B | FRENCH | 2,38 s | 81 jetons/s | 15,6 jetons/s | 19 |

- **Une phrase** (≈ 20 jetons) demande donc **3,6 s (EuroLLM) à 4,8 s (Qwen3)** de bout en bout.
  Le temps est dominé par la lecture du prompt (≈ 200 jetons), pas par la génération.
- Chargement du modèle : 2,7 à 3,0 s.

Threads (glouton, fait principal, prompt JSON) :

| Threads | Qwen3 lecture | Qwen3 génération | EuroLLM lecture | EuroLLM génération |
|--------:|--------------:|-----------------:|----------------:|-------------------:|
| 2 | 39 jetons/s | 11,8 jetons/s | 49 jetons/s | 14,9 jetons/s |
| **4** | **75 jetons/s** | **12,6 jetons/s** | **88 jetons/s** | **15,5 jetons/s** |
| 6 | 53 jetons/s | 10,6 jetons/s | 61 jetons/s | 12,5 jetons/s |
| 8 | 64 jetons/s | 9,1 jetons/s | 68 jetons/s | 11,4 jetons/s |

4 threads (les 4 cœurs A78) est optimal ; ajouter les A55 ralentit. La génération est limitée par la
bande passante mémoire (presque la même à 2 et 4 threads).

### Mémoire

| Modèle | PSS avant → modèle chargé | Après 30 réponses | Après fermeture |
|--------|--------------------------:|------------------:|----------------:|
| Qwen3-1.7B | 159 → 2 366 Mo | 2 391 Mo | 209 Mo |
| EuroLLM-1.7B | 209 → 1 817 Mo | 1 832 Mo | 191 Mo |

- Le PSS dépasse la taille du fichier de 0,6 à 1,3 Go : tampon de calcul (318 Mo pour Qwen3, dont le
  vocabulaire compte 151 k jetons), cache KV, et probablement la copie « repack » des poids pour les
  noyaux ARM en plus des pages `mmap`. Non investigué (voir dette).
- Avec Stockfish (≈ 430 Mo) et Maia, Qwen3 ferait dépasser l'objectif de **3 Go pour toute l'app** ;
  EuroLLM tiendrait de justesse. La mémoire est bien rendue à la fermeture.

### Chauffe (Qwen3, 4 threads, 5 min, 58 réponses enchaînées, prompt FRENCH)

| Instant | Batterie | Lecture du prompt | Génération |
|---------|---------:|------------------:|-----------:|
| Début | 30 °C | 75 jetons/s | 12,8 jetons/s |
| ≈ 1 min | 30 °C | 67 jetons/s | 12,7 jetons/s |
| ≈ 2,5 min | 31 °C | 64 jetons/s | 12,6 jetons/s |
| Fin (5 min) | 31 °C | 64 jetons/s | 12,5 jetons/s |

La tablette sortait du lot (déjà à 30 °C). La lecture du prompt perd ≈ 15 % dans la première minute
puis se stabilise ; la génération ne bouge pas. Aucun bridage gênant, comme pour Stockfish.

### Qualité du français et fidélité aux faits

Chaque réponse est notée à la main : **correcte** (exacte, une phrase, utilisable telle quelle),
**approximative** (exacte sur l'essentiel, mais formulation fautive ou imprécise), **fausse**
(fait inventé ou contredit, hors sujet). Les 60 textes sont dans le journal (`text=`).

| Modèle | Prompt | Correctes | Approximatives | Fausses | Une seule phrase | Sans invention (contrôle auto) |
|--------|--------|----------:|---------------:|--------:|-----------------:|-------------------------------:|
| Qwen3-1.7B | JSON | 0 | 1 | 14 | 0/15 | 8/15 |
| Qwen3-1.7B | FRENCH | 5 | 4 | 6 | 15/15 | 9/15 |
| EuroLLM-1.7B | JSON | 0 | 0 | 15 | 10/15 | 7/15 |
| EuroLLM-1.7B | FRENCH | 0 | 1 | 14 | 10/15 | 8/15 |

Exemples réels :
- Qwen3, JSON (fait principal) : « Tu as capturé la tour en f3 avec un cavalier, ce qui est un coup
  très bien ! » : contresens (le cavalier est **en prise**, il n'a rien capturé), pièce inventée,
  trois phrases ; ailleurs « bishou », « le dame », « bon sens de la jeu », un émoji, du Markdown.
- Qwen3, FRENCH, bonnes réponses : « Ton cavalier en c7 peut attaquer le roi en e8 et la tour en a8
  avec échec : joue Cc7+ ! », « Ta tour en e8 va achever le mat : joue Te8# ! ».
- Qwen3, FRENCH, fautes : « Ton cavalier en f3 peut prendre en c2 avec échec : joue Cd2 ! ». Le
  modèle **recopie la structure de l'exemple** et invente une prise avec échec. Les 3 essais sont
  identiques : le modèle est très sûr de lui, même en échantillonnant.
- EuroLLM, JSON : fuite de gabarit (« {{motif.motif}} est une pièce en prise… »), une partie entière
  inventée (« 1. e4 e6 2. f4 exf4 3. Rf3… »), « chevalier », « reine ». En FRENCH, il recopie
  aussi l'exemple (« peut prendre en c4 avec échec ») ou part hors sujet.

Contrôle automatique (`FactFidelity`) : il repère cases, coups et pièces absents du fait, et compte
les phrases. Il attrape la plupart des inventions (c2, c4, « dame »…), mais il lit des mots, pas le
sens : « Ton fou en d4 » (le fou est adverse) ou « tu as capturé » au lieu de « tu es en prise »
passent. **Il ne suffit pas comme garde-fou.**

### Écarts au périmètre et dette

- **Écart** : module `:spike:llm` hors de la liste de `CLAUDE.md` (le prompt du sprint demande un
  « module expérimental isolé »). L'app en dépend seulement pour l'écran « Debug LLM ».
- **Écart** : prompt FRENCH ajouté au prompt JSON du sprint, pour savoir si des faits pré-traduits
  rendent un petit modèle utilisable (réponse : partiellement, pour Qwen3 seulement).
- **Évaluation** : notes de qualité attribuées par Claude sur 60 réponses ; à relire par Franck.
- **Non essayé** : cache KV du prompt système (réutiliser les ≈ 180 jetons fixes ramènerait le premier
  jeton vers 0,5 s), KleidiAI, Q4_0 (repack plus efficace), `mmap` désactivé (mémoire), autres
  modèles (Gemma 3 1B, Llama 3.2 1B : licences non libres, à signaler avant usage), MediaPipe LLM.
- **Build** : CMake télécharge l'archive llama.cpp (38 Mo, SHA-256 épinglé) à la première
  configuration, aussi en CI ; la CI compile llama.cpp en plus (non éprouvé sur GitHub).
- **APK** : +6 Mo. Le module de l'app déclare maintenant `ndkVersion`, donc les `.so` sont enfin
  strippés (Stockfish passe de 18 à 1,6 Mo dans l'APK).
- L'écran « Debug LLM » est aussi dans la build release (comme « Debug moteur »).

### Conclusion et recommandation

**Le LLM embarqué ne convient pas au professeur hors ligne : le réserver au PC.**

1. **Trop lent pour le retour immédiat** : 3,6 à 4,8 s par phrase contre moins de 200 ms visés ; les
   gabarits restent indispensables. Même avec le cache du prompt système, on resterait vers 2 s.
2. **Pas fiable** : avec le fait brut, 0 réponse correcte sur 30 et des contresens nets (pièce capturée
   au lieu de pièce en prise). Même avec des faits pré-mâchés, le meilleur modèle (Qwen3) donne 40 %
   de réponses fausses, dont des coups inventés : cela contredit la règle 3, et le contrôle
   automatique ne rattrape pas les erreurs de sens.
3. **Trop gourmand** : 1,8 à 2,4 Go de PSS, incompatible avec le budget de 3 Go pour toute l'app
   quand Stockfish et Maia sont chargés.

Conséquences proposées (à trancher en S0-8, ADR 0003) :
- Le retour du professeur hors ligne repose **uniquement sur les gabarits** de `:core:pedagogy`.
- « Explique-moi plus » et les questions libres passent par la **station PC** (S0-7 teste le même
  prompt sur des modèles de 8 à 14 Md). Hors ligne, le bouton propose le gabarit détaillé.
- Garder `:spike:llm` jusqu'à S0-8 comme référence (pont JNI et mesures), puis le retirer de l'app.
- Réévaluer si un modèle de 1 à 3 Md nettement meilleur en français sort, ou si le NPU devient
  exploitable (backlog). Si l'essai reprend : Qwen3 plutôt qu'EuroLLM, prompt FRENCH, cache du
  prompt système.

### Reproduire les mesures

1. `scripts/fetch-llm-models.ps1 -Push` (télécharge les deux GGUF dans `build/llm/` et les copie dans
   `/sdcard/Android/data/com.chesscompanion/files/models/`), puis `./gradlew installDebug`.
2. Lancer `adb logcat -v brief -s LLM_BENCH:I > llm.log` **avant** le lot (le tampon de logcat est
   trop petit pour tout garder).
3. Dans l'app : « Debug LLM », puis « Lot LLM » (≈ 10 min) ; « Chauffe 5 min » avec le modèle choisi.

---

## S0-7 · Station PC

**Date** : 2026-10-18 · **Machine** : PC portable, i9-13980HX (8 P + 16 E, 32 threads), 32 Go,
RTX 4080 Laptop 12 Go (pilote 595.79), Windows 11, sur secteur, avec les applications habituelles
ouvertes (navigateurs, DaVinci Resolve, ComfyUI : ≈ 3 Go de VRAM déjà pris).
**Logiciel** : `station-pc/` (Python 3.12, FastAPI 0.146, route `/health`, `start.bat`) ;
Stockfish 19 `x86-64-universal` ; lc0 v0.32.1 CUDA 12 ; Ollama 0.40.6. Scripts dans `station-pc/bench/`,
résultats JSON dans `build/station/results/`.

### Stockfish 19, 32 threads, hash 2 Go, profondeur 30 (10 positions de S0-4)

| Pos | P20 (s) | P25 (s) | P30 (s) | nps (M) à P30 | Éval. P30 (cp) |
|----:|--------:|--------:|--------:|--------------:|---------------:|
| 1 | 0,7 | 1,7 | 5,2 | 14,1 | 0 |
| 2 | 0,9 | 4,0 | 12,4 | 15,1 | +39 |
| 3 | 0,6 | 2,0 | 5,5 | 14,3 | −11 |
| 4 | 0,9 | 6,5 | 33,7 | 12,2 | +719 |
| 5 | 11,7 | 63,2 | 350,2 | 10,4 | +816 |
| 6 | 4,0 | 71,0 | 1 185,9 | 10,5 | +1 074 |
| 7 | 0,1 | 0,2 | 0,5 | 14,4 | 0 |
| 8 | 4,8 | 29,1 | 115,4 | 11,8 | +384 |
| 9 | 1,2 | 20,9 | 93,8 | 11,2 | +659 |
| 10 | 0,5 | 4,6 | 45,7 | 13,6 | +341 |
| **Médiane** | **0,9** | **5,6** | **39,7** | **12,9** | |

- **Profondeur 20 : 0,9 s contre 5,5 s sur la tablette (×6).** La profondeur 30 prend 0,5 à 40 s sur
  les positions équilibrées, mais 6 à 20 min sur les positions très déséquilibrées (5, 6) : la recherche
  creuse longtemps les lignes gagnantes. Pour l'analyse de partie, limiter par temps (par exemple
  10 s par coup) plutôt que par profondeur.
- 10 à 15 M nœuds/s, en baisse au fil des recherches longues : le portable se bride (puissance et
  chauffe). Un PC de bureau ferait environ le double ; non mesuré ici.

### lc0 + Maia (politique seule, `go nodes 1`, 20 positions de S0-5 × 5 passes)

| Backend | Maia 1100 | Maia 1500 | Maia 1900 | Démarrage jusqu'au 1er coup | VRAM |
|---------|----------:|----------:|----------:|----------------------------:|-----:|
| CUDA (`cuda-auto`) | 4,8 / 14,0 ms | 12,7 / 14,2 ms | 13,9 / 4,4 ms | 0,4–0,7 s | ≈ 0,7 Go |
| CUDA fp16 | 11,3 / 5,1 ms | 4,6 / 14,2 ms | 5,4 / 14,1 ms | 0,4–0,6 s | ≈ 0,7 Go |
| CPU OpenBLAS (S0-5) | **1,7 ms** | **1,5 ms** | **1,6 ms** | < 0,1 s | 0 |

(Médianes de deux passages, « première / seconde ».)

- **Le GPU est plus lent que le CPU pour Maia** : 4 à 14 ms par coup (deux paliers selon l'état de
  fréquence du GPU portable) contre ≈ 1,5 ms, temps de dialogue UCI compris. Le réseau de Maia est
  minuscule ; la latence de lancement des noyaux CUDA domine.
- Même coup sur les trois backends dans 60 positions sur 60.
- Maia tourne déjà en 1,5 à 3,6 ms sur la tablette (S0-5) : **la station n'apporte rien pour Maia**.
  lc0 CUDA ne servirait qu'à des réseaux plus gros ou à une recherche (`nodes` > 1), hors périmètre.

### LLM « professeur » via Ollama (mêmes prompts, faits et échantillonnage que S0-6)

| Modèle (Q4_K_M) | Licence | VRAM | Chargement à froid | Premier jeton | Génération | Phrase complète (FRENCH) |
|-----------------|---------|-----:|-------------------:|--------------:|-----------:|-------------------------:|
| qwen3:14b | Apache 2.0 | 8,9 Go | 9,1 s | 76 ms | 19–22 jetons/s | 1,2 s |
| mistral-nemo:12b | Apache 2.0 | 6,8 Go | 7,5 s | 40–45 ms | 57 jetons/s | 0,4 s |

- Premier jeton très court : Ollama garde en cache le prompt système d'une requête à l'autre.
- **VRAM juste pour qwen3:14b** : avec ≈ 3 Go déjà pris par d'autres applications, il tient (10,8 Go
  sur 12,3). Lors d'un premier passage où la VRAM n'était pas encore libérée, seule la moitié du
  modèle était sur le GPU : 10 à 13 jetons/s et 47 s pour la première réponse. Mistral-Nemo laisse
  plus de marge.
- La génération de qwen3:14b (≈ 20 jetons/s) est sous ce qu'on attend d'une RTX 4080 ; non creusé
  (GPU portable partagé avec l'affichage et les autres applications).

### Qualité (même grille que S0-6, 15 réponses par ligne, notes de Claude)

| Modèle | Prompt | Correctes | Approximatives | Fausses | Une seule phrase | Sans invention (contrôle auto) |
|--------|--------|----------:|---------------:|--------:|-----------------:|-------------------------------:|
| qwen3:14b | JSON | 4 | 8 | 3 | 15/15 | 15/15 |
| qwen3:14b | FRENCH | **10** | 5 | **0** | 15/15 | 15/15 |
| mistral-nemo:12b | JSON | 4 | 5 | 6 | 11/15 | 14/15 |
| mistral-nemo:12b | FRENCH | 9 | 4 | 2 | 13/15 | 14/15 |
| *rappel tablette : Qwen3-1.7B* | *FRENCH* | *5* | *4* | *6* | *15/15* | *9/15* |

Exemples réels :
- qwen3:14b, FRENCH : « Ton cavalier en f3 est en prise et non défendu : joue Cd2 pour le sauver ! » ;
  « Ton cavalier peut prendre gratuitement le fou adverse en d4 : joue Cxd4 ! ».
- qwen3:14b, approximations : « menacer à la fois ton roi et ta tour » (ce sont les pièces adverses),
  « cloué par la dame en d8 » (cloué *devant* la dame) ; en JSON, notation anglaise (Nc7+, Bxf6)
  malgré la consigne, et deux fois « en déplaçant ton roi sur e8 » pour le mat de la tour.
- mistral-nemo:12b, fautes : « tu vas être mat en e8 avec le Roque », « ton fou sur e8 »,
  « Ton cavalier est en danger, tu peux le sauver en jouant Ta8! » (mat du couloir) ; « épinglant »
  (anglicisme).
- Le contrôle automatique laisse passer presque toutes ces erreurs de sens (« ton roi » au lieu de la
  tour, mauvais possessif) : il confirme qu'il ne suffit pas comme garde-fou.

### Écarts et dette

- Mesures sur un portable avec les applications habituelles ouvertes : nœuds/s de Stockfish et
  jetons/s de Qwen3 sont sans doute sous-estimés ; le bridage n'a pas été suivi (pas de sonde CPU).
- Stockfish : un seul réglage (32 threads, 2 Go) ; pas de comparaison 16/24 threads ni de limite par temps.
- lc0 : les mesures de VRAM sont bruitées (mémoire GPU totale, autres applications actives).
- Prompts et contrôle de fidélité copiés de Kotlin vers Python (`bench/prompts.py`, `bench/fidelity.py`) :
  deux copies à garder alignées tant que les deux spikes vivent.
- `fastapi.testclient` signale une dépréciation (httpx → httpx2 côté Starlette) : sans effet pour l'instant.

### Conclusion et présélection

- **Analyse profonde** : Stockfish sur la station, par temps (≈ 10 s par coup donne au moins la
  profondeur 25 à 30 en milieu de partie équilibré), soit 6 fois plus vite que la tablette.
- **Maia** : reste sur la tablette (ONNX, S0-5). lc0 CUDA sur la station n'est pas nécessaire.
- **LLM présélectionné : qwen3:14b** (Apache 2.0), raisonnement désactivé, avec des faits **pré-traduits
  en français par le code** (prompt FRENCH) : 10 réponses correctes sur 15, aucune fausse, 1,2 s par
  phrase. **Repli : mistral-nemo:12b** si la VRAM manque (3 fois plus rapide, 2 réponses fausses sur 15).
- Le LLM du PC reste un **complément** (« Explique-moi plus », questions libres) : même à ce niveau,
  5 réponses sur 15 sont imprécises. Il faut donc toujours afficher le gabarit exact à côté, et garder
  la règle 3 (le LLM ne reçoit que des faits et des lignes moteur).
- Charger le modèle au démarrage de la station (`keep_alive`) pour éviter 7 à 9 s d'attente au premier appel.

### Reproduire les mesures

Voir `station-pc/README.md` (installation), puis depuis `station-pc/` :
`python -m bench.stockfish` (≈ 30 min, surtout les positions 5 et 6), `python -m bench.lc0` (≈ 1 min),
`python -m bench.llm` (≈ 3 min). Fermer les applications gourmandes en VRAM avant `bench.llm`.
