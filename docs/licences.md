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

## llama.cpp v0.7.0 (spike S0-6)

- **Auteurs** : The ggml authors. **Licence** : MIT, compatible GPL v3.
- **Source** : https://github.com/ggml-org/llama.cpp, archive de l'étiquette `v0.7.0`, téléchargée par
  CMake (`spike/llm/src/main/cpp/CMakeLists.txt`, SHA-256 épinglé) ; non versionnée, non modifiée.
- Liée en statique dans `libllm_jni.so` (module `:spike:llm`, à retirer après S0, voir ADR 0003).

## Modèles LLM du spike S0-6 (non embarqués)

- **Qwen3-1.7B** (Alibaba, équipe Qwen) : Apache 2.0 (https://huggingface.co/Qwen/Qwen3-1.7B).
  Fichier `Qwen3-1.7B-Q4_K_M.gguf`, quantification d'unsloth (https://huggingface.co/unsloth/Qwen3-1.7B-GGUF).
- **EuroLLM-1.7B-Instruct** (projet UTTER) : Apache 2.0 (https://huggingface.co/utter-project/EuroLLM-1.7B-Instruct).
  Fichier `EuroLLM-1.7B-Instruct.Q4_K_M.gguf`, quantification de mradermacher
  (https://huggingface.co/mradermacher/EuroLLM-1.7B-Instruct-GGUF).
- Apache 2.0 : usage personnel et redistribution permis, compatible GPL v3. Fichiers non versionnés,
  copiés sur la tablette par `scripts/fetch-llm-models.ps1 -Push` (SHA-256 figées dans le script).

## Station PC (S0-7)

- **Dépendances Python** (`station-pc/requirements.txt`) : FastAPI 0.146.0 (MIT), Starlette (BSD),
  Uvicorn 0.54.0 (BSD), httpx 0.28.1 (BSD), Pydantic (MIT), pytest 9.1.1 (MIT). Toutes compatibles GPL v3.
- **Stockfish 19** Windows `x86-64-universal` (GPL v3) et **lc0 v0.32.1** CUDA 12 (GPL v3, avec les
  bibliothèques d'exécution CUDA redistribuables de NVIDIA livrées par le projet lc0) : téléchargés par
  `station-pc/scripts/fetch-engines.ps1` (SHA-256 publiées par GitHub), non versionnés, outils du PC seulement.
- **Ollama** (MIT) : application installée à part, pas une dépendance du code.
- **Modèles** : qwen3:14b (Qwen3-14B, Apache 2.0) et mistral-nemo:12b (Mistral-Nemo-Instruct-2407,
  Apache 2.0), via le catalogue Ollama ; non versionnés.
