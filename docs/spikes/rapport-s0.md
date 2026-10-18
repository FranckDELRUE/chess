# Rapport des spikes du sprint S0

Appareil : tablette Honor HEY2-W09, Android 14, Snapdragon 6 Gen 1 (4 × A78 + 4 × A55), 8 Go.
Les sections S0-5 à S0-7 seront ajoutées par leurs stories.

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
