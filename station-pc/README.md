# Station PC

Serveur du réseau local qui décharge la tablette : analyses Stockfish profondes, Maia via lc0, et
LLM « professeur » via Ollama. **Jamais requis** : l'application fonctionne sans lui (règle 4).
Projet Python séparé de l'app Android. S0-7 ne fournit que `/health` et les benchmarks ; les routes
d'analyse arrivent avec le sprint S9.

## Installation (Windows)

1. **Python 3.12** (python.org), puis, depuis ce dossier :
   ```bash
   python -m venv .venv
   ```
   ```bash
   .venv/Scripts/pip install -r requirements.txt
   ```
2. **Moteurs** (Stockfish 19 x86-64 et lc0 v0.32.1 CUDA 12, sommes SHA-256 vérifiées), dans
   `build/station/` à la racine du dépôt :
   ```bash
   powershell -File scripts/fetch-engines.ps1
   ```
   La version de lc0 embarque les DLL CUDA : aucun toolkit CUDA n'est nécessaire, seulement un
   pilote NVIDIA récent (testé avec 595.79).
3. **Réseaux Maia** : `scripts/fetch-maia-nets.ps1` à la racine du dépôt (déjà utilisé en S0-5),
   qui les place dans `build/maia/`.
4. **Ollama** (ollama.com, installateur Windows), puis les modèles du professeur :
   ```bash
   ollama pull qwen3:14b
   ```
   ```bash
   ollama pull mistral-nemo:12b
   ```

Chaque chemin peut être remplacé par une variable d'environnement : `STATION_STOCKFISH`,
`STATION_LC0`, `STATION_MAIA_DIR`, `STATION_OLLAMA_URL`, `STATION_RESULTS` (voir `station/config.py`).

## Lancer

`start.bat` démarre le serveur sur le port 8765, à l'écoute du réseau local (Windows demande
d'autoriser Python dans le pare-feu au premier lancement : réseau privé seulement).
Vérification : `http://<ip-du-pc>:8765/health` affiche le GPU, les moteurs présents et les modèles Ollama.

## Tests et benchmarks

Depuis ce dossier, avec le venv :

```bash
.venv/Scripts/python -m pytest
```
```bash
.venv/Scripts/python -m bench.stockfish
```
```bash
.venv/Scripts/python -m bench.lc0
```
```bash
.venv/Scripts/python -m bench.llm
```

Résultats JSON dans `build/station/results/` (non versionnés) ; synthèse dans
`docs/spikes/rapport-s0.md`, section S0-7.

## Licences

FastAPI (MIT), Starlette (BSD), Uvicorn (BSD), httpx (BSD), pytest (MIT), Pydantic (MIT) ;
Stockfish et lc0 (GPL v3) ; modèles Qwen3-14B et Mistral-Nemo-12B (Apache 2.0). Voir `docs/licences.md`.
