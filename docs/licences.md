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
