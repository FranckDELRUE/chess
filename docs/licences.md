# Licences des ressources tierces

## Jeu de pièces d'échecs « Cburnett »

- **Auteur** : Colin M.L. Burnett (Wikimedia Commons, « Chess_klt45.svg » et équivalents).
- **Licence** : CC BY-SA 3.0 (également disponible sous GFDL et GPL v2+), compatible GPL v3.
- **Source** : https://commons.wikimedia.org/wiki/Category:SVG_chess_pieces
- **Fichiers** : `core/ui/src/main/res/drawable/piece_{w,b}{k,q,r,b,n,p}.xml`.
- **Modifications** : conversion mécanique des SVG 45×45 en `VectorDrawable` Android
  (mêmes tracés, couleurs et traits). Les dérivés restent sous CC BY-SA 3.0.
- **Attribution** : « Chess pieces by Colin M.L. Burnett, CC BY-SA 3.0 », à afficher dans
  l'écran « À propos » dès qu'il existera (voir `docs/backlog.md`).

## Stockfish 19

- **Auteurs** : The Stockfish developers (voir `core/engine/src/main/cpp/stockfish/AUTHORS`).
- **Licence** : GPL v3, texte dans `core/engine/src/main/cpp/stockfish/Copying.txt`.
- **Source** : https://github.com/official-stockfish/Stockfish, étiquette `sf_19`. Sources non modifiées :
  `main()` est seulement renommé à la compilation (voir ADR 0004).
- **Réseau NNUE** `nn-1a298aa575a0.nnue` : https://tests.stockfishchess.org/api/nn/nn-1a298aa575a0.nnue,
  distribué par le projet Stockfish sous GPL v3 ; non versionné (`scripts/fetch-stockfish-net.ps1`).
- **Conséquence** : publier l'application impose de publier son code source sous GPL v3.

## kotlinx.coroutines 1.10.2

- **Licence** : Apache 2.0, compatible GPL v3.

## Maia-1 (réseaux 1100, 1500, 1900)

- **Auteurs** : CSSLab, université de Toronto (McIlroy-Young, Sen, Kleinberg, Anderson, 2020).
- **Licence** : GPL v3, celle du dépôt https://github.com/CSSLab/maia-chess. Le dépôt ne donne pas
  de licence distincte pour les poids : on les considère couverts par la GPL v3 (à revérifier avant
  toute publication).
- **Source** : release `v1.0`, fichiers `maia-{1100,1500,1900}.pb.gz` ; non versionnés
  (`scripts/fetch-maia-nets.ps1`, sommes SHA-256 figées dans le script).
- **Dérivés** : `core/engine/src/main/assets/maia-*.onnx`, conversion mécanique par
  `lc0 leela2onnx` (non versionnés) ; `core/engine/src/test/resources/maia-reference.tsv`
  (politiques calculées par lc0 avec ces poids).

## Leela Chess Zero (lc0) v0.32.1

- **Licence** : GPL v3. Outil du PC uniquement (conversion ONNX et référence de test), non embarqué.
- **Source** : https://github.com/LeelaChessZero/lc0, binaire `lc0-v0.32.1-windows-cpu-openblas.zip`.
- **Repris** : la table des 1858 coups de la politique (`kMoveStrs`, `src/neural/encoder.cc`), en
  fixture de test (`lc0-policy-moves.txt`), et la logique d'encodage (`EncodePositionForNN`)
  réécrite en Kotlin (`Lc0Encoder`, `Lc0PolicyMap`).

## ONNX Runtime

- **Licence** : MIT, compatible GPL v3.
- `com.microsoft.onnxruntime:onnxruntime-android` 1.30.0 (application) ;
  `com.microsoft.onnxruntime:onnxruntime` 1.18.0 (tests JVM seulement).
