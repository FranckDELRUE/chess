# Rapport S2 — mesures

## S2-2 · Stockfish sur les milieux de partie du corpus Heidoji

**Date** : 2026-10-19 · **Version** : Stockfish 19 (`sf_19`), réseau `nn-1a298aa575a0.nnue`, même build
qu'en S0-4 (`docs/spikes/rapport-s0.md`).

### Positions

Les 10 positions de S0-4 étaient choisies à la main, et quatre d'entre elles étaient très
déséquilibrées (+6 à +10 pions). Elles sont remplacées par **20 milieux de partie de Heidoji**,
choisis en deux étapes reproductibles (`testdata/positions/README.md`) :

1. **Sans moteur** (`HeidojiSelection`, tests de `:app`) : pour chaque partie du corpus, les
   demi-coups 30 à 40 sans échec au roi, à matériel égal à un pion près, avec une dame ou deux tours
   dans chaque camp. On obtient 261 candidates.
2. **Stockfish sur le PC** (`python -m bench.select_positions`, 1 thread, profondeur 18,
   déterministe) : on garde la première candidate de chaque partie évaluée à ±150 cp au plus, pour les
   20 premières parties qui en ont une. 73 candidates ont été évaluées, 53 écartées.

Écart validé en cours de story : la règle d'origine ne gardait qu'un demi-coup par partie (le premier
admissible). Elle ne donnait que 19 positions équilibrées. On a donc élargi l'étape 1 à tous les
demi-coups admissibles, en gardant toujours une seule position par partie et le même seuil.

Les 20 positions viennent des parties 3 à 57, demi-coups 30 à 35. L'évaluation à profondeur 18
(1 thread) va de −140 à +149 cp.

### Tablette : temps jusqu'au `bestmove` (4 threads, Hash 64 Mo)

Méthode de S0-4 : `ucinewgame` avant chaque recherche, une recherche par profondeur, temps écoulé
jusqu'au `bestmove`. Différences avec S0-4 :
- la profondeur 18 est jouée 3 fois par position, parce que la recherche multi-thread n'est pas
  déterministe ;
- le moteur est démarré par une recherche d'échauffement non chronométrée ;
- les recherches passent par l'`EngineScheduler` (S2-1) en priorité `BACKGROUND`, sans aucune
  préemption pendant le lot.

Test instrumenté `StockfishBenchmark` (`gradlew :core:engine:connectedDebugAndroidTest
-PengineBenchmark`, après `adb logcat -G 4M`), tablette branchée en USB.

| Pos | P16 (ms) | P18 passes (ms) | P18 médiane (ms) | P20 (ms) | nps (k) à P20 | Éval. P20 |
|----:|---------:|----------------:|-----------------:|---------:|--------------:|----------:|
| 1 | 1 404 | 1 255 / 1 127 / 2 126 | 1 255 | 2 208 | 726 | cp 104 |
| 2 | 354 | 799 / 740 / 653 | 740 | 1 581 | 452 | cp 20 |
| 3 | 1 146 | 2 850 / 3 813 / 1 751 | 2 850 | 5 302 | 542 | cp 58 |
| 4 | 857 | 935 / 1 108 / 1 379 | 1 108 | 2 202 | 662 | cp -16 |
| 5 | 574 | 1 123 / 1 011 / 1 563 | 1 123 | 919 | 800 | cp 24 |
| 6 | 4 237 | 3 260 / 4 960 / 6 030 | 4 960 | 7 179 | 616 | cp -46 |
| 7 | 960 | 1 789 / 2 011 / 1 878 | 1 878 | 3 566 | 623 | cp 90 |
| 8 | 1 544 | 3 487 / 6 094 / 1 969 | 3 487 | 5 871 | 587 | cp 165 |
| 9 | 1 518 | 1 858 / 2 001 / 2 713 | 2 001 | 6 186 | 632 | cp -50 |
| 10 | 776 | 1 023 / 1 261 / 863 | 1 023 | 2 262 | 639 | cp -38 |
| 11 | 1 089 | 2 433 / 1 509 / 1 447 | 1 509 | 3 158 | 746 | cp 9 |
| 12 | 1 073 | 1 905 / 1 334 / 2 465 | 1 905 | 8 060 | 627 | cp -161 |
| 13 | 777 | 1 278 / 1 499 / 1 430 | 1 430 | 4 021 | 812 | cp 13 |
| 14 | 2 747 | 3 654 / 4 148 / 3 305 | 3 654 | 4 284 | 607 | cp -82 |
| 15 | 1 923 | 2 656 / 2 338 / 1 676 | 2 338 | 5 075 | 678 | cp -44 |
| 16 | 2 104 | 2 571 / 2 376 / 3 258 | 2 571 | 6 818 | 681 | cp 29 |
| 17 | 1 351 | 4 583 / 2 965 / 2 717 | 2 965 | 5 234 | 682 | cp 71 |
| 18 | 1 212 | 2 908 / 2 612 / 2 673 | 2 673 | 4 704 | 757 | cp -150 |
| 19 | 1 317 | 1 571 / 3 205 / 2 101 | 2 101 | 6 347 | 696 | cp 67 |
| 20 | 1 025 | 2 393 / 2 854 / 4 121 | 2 854 | 6 879 | 637 | cp -109 |
| **Médiane** | **1 179** | | **2 051** | **4 889** | | |
| **Moyenne** | **1 399** | | **2 221** | **4 592** | | |

- **P18** : la médiane des 20 positions vaut **2,05 s**. Le 90e centile des 60 passes vaut 4,1 s.
- **Pire cas P18** : la médiane la plus haute vaut **4,96 s** (position 6). Deux passes isolées sur 60
  dépassent 5 s (6,0 s sur la position 6 et 6,1 s sur la position 8).
- **Première série** (la même, sans échauffement) : médiane P18 de 2,15 s, médiane la plus haute de
  4,90 s (position 14), et là aussi 2 passes sur 60 au-dessus de 5 s (6,4 s et 5,8 s). Le résultat se
  reproduit.
- **Vitesse** : 450 à 810 kn/s, comme en S0-4.
- **Durée et chauffe** : le lot dure 4 min 20 s. La batterie reste à 27 °C du début à la fin
  (25 °C pendant la première série), et `Thermal Status` vaut 0.
- **Mémoire** : PSS de 370 Mo en fin de lot, contre 428 Mo en S0-4. L'écart vient sans doute de
  l'absence d'interface (APK de test).

### Comparaison avec S0-4

| Médianes (ms) | P16 | P18 | P20 |
|---|---:|---:|---:|
| S0-4, 10 positions choisies à la main | 617 | 1 682 | 5 481 |
| S2-2, 20 positions Heidoji équilibrées | 1 179 | 2 051 | 4 889 |

Les positions équilibrées ne sont pas plus lentes en moyenne. Les très longues recherches de S0-4
(15 à 40 s) venaient des positions gagnantes, qui ne figurent plus dans le lot. P16 augmente un peu :
S0-4 comptait deux positions qui se résolvaient presque instantanément (positions 7 et 10).

### PC : Stockfish 19, 32 threads, Hash 2 Go, profondeur 30

`python -m bench.stockfish`, même portable qu'en S0-7, avec les applications habituelles ouvertes.

| Pos | P20 (ms) | P25 (ms) | P30 (ms) | nps (M) | Éval, P30 |
|----:|---------:|---------:|---------:|--------:|----------:|
| 1 | 784 | 3 914 | 8 793 | 15,3 | cp 113 |
| 2 | 287 | 1 372 | 3 598 | 17,2 | cp 4 |
| 3 | 1 813 | 8 156 | 23 166 | 14,5 | cp 67 |
| 4 | 1 110 | 2 405 | 7 859 | 14,3 | cp -11 |
| 5 | 771 | 2 482 | 5 204 | 16,2 | cp 1 |
| 6 | 1 867 | 5 381 | 15 991 | 15,7 | cp -14 |
| 7 | 1 719 | 7 853 | 23 532 | 14,5 | cp 66 |
| 8 | 1 537 | 9 855 | 29 982 | 13,5 | cp 172 |
| 9 | 1 154 | 3 755 | 14 990 | 13,9 | cp -27 |
| 10 | 697 | 4 406 | 19 052 | 13,5 | cp -23 |
| 11 | 750 | 1 958 | 5 289 | 15,3 | cp 0 |
| 12 | 1 751 | 8 172 | 21 915 | 13,8 | cp -200 |
| 13 | 695 | 2 483 | 5 028 | 16,1 | cp 1 |
| 14 | 1 647 | 7 705 | 20 489 | 14,6 | cp -35 |
| 15 | 829 | 2 578 | 6 421 | 15,2 | cp -12 |
| 16 | 1 754 | 6 053 | 13 728 | 13,2 | cp 7 |
| 17 | 4 513 | 12 012 | 22 991 | 13,6 | cp 32 |
| 18 | 2 331 | 12 671 | 37 110 | 14,2 | cp -243 |
| 19 | 1 593 | 5 886 | 18 829 | 13,9 | cp 15 |
| 20 | 2 612 | 5 374 | 22 157 | 12,7 | cp -101 |

- **Médianes** : P20 1,6 s, P25 5,4 s, **P30 17,4 s** (39,7 s en S0-7). La recherche la plus longue
  dure 37 s ; il n'y a plus de recherche de 6 à 20 min comme sur les positions gagnantes de S0-7.
- **Vitesse** : 12,7 à 17,2 M nœuds/s.
- **Rapport avec la tablette à P20** : 1,6 s contre 4,9 s (×3, contre ×6 en S0-7). Le PC n'est plus
  ralenti par les positions déséquilibrées.

### Verdict sur le critère du sprint

**Critère « profondeur 18 en moins de 5 s sur chaque position » tenu**, au sens retenu en
planification : la médiane des 3 passes de chaque position est sous 5 s, dans les deux séries. **La
marge est quasi nulle** (4,96 s au pire), et une passe isolée peut aller jusqu'à 6 s (2 sur 60 par
série).

Conséquences pour la suite :
- **S2-3 (analyse en direct)** : ne pas attendre la profondeur 18. Afficher les lignes dès les
  premières profondeurs, ce qui donne une analyse utile en moins d'1 s (P16 médiane 1,2 s), et la
  laisser s'affiner.
- **S2-5 (file en arrière-plan)** : la profondeur 18 coûte environ 2 s par coup, soit environ 1,5 min
  pour une partie de 40 coups.
- **Réglages** : aucun changement fait. Piste si le critère devait devenir strict : mesurer 6 threads
  et un Hash plus grand.

### Reproduire

1. `scripts/fetch-stockfish-net.ps1` (une fois).
2. Tablette : `adb logcat -G 4M`, puis
   `gradlew :core:engine:connectedDebugAndroidTest -PengineBenchmark`, puis
   `adb logcat -d -s SF_BENCH`.
3. PC (dans `station-pc/`) : `.venv/Scripts/python -m bench.stockfish`. Le résultat JSON est écrit
   dans `build/station/results/`.
4. Régénérer les positions : voir `testdata/positions/README.md`.
