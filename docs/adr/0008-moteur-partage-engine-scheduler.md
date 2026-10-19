# ADR 0008 — Un seul Stockfish partagé par un planificateur à priorités

- **Statut** : accepté (S2-1)
- **Date** : 2026-10-19

## Contexte
Stockfish ne tourne qu'en un exemplaire par processus (ADR 0004). En S2, trois usages se
le disputent : l'adversaire (S2-4), l'analyse en direct (S2-3) et la file d'analyses en
arrière-plan (S2-5). Ils n'ont pas les mêmes options (force limitée, MultiPV 3, pleine force) et
l'adversaire ne doit jamais attendre une analyse de fond. UCI ne sait pas suspendre une
recherche : on ne peut que l'arrêter (`stop`).

## Décision
1. **`EngineScheduler`** (`:core:engine`, singleton Hilt via `EngineModule`) est le seul accès à
   Stockfish. `EngineProvider` (dans `:app` depuis S0-4) disparaît.
2. **Priorités** `OPPONENT > LIVE > BACKGROUND` ; une recherche à la fois, la plus prioritaire
   d'abord, la plus ancienne à priorité égale.
3. **Préemption par relance** : une recherche plus prioritaire fait arrêter celle en cours (`stop`,
   son `bestmove` est lu puis jeté). La recherche interrompue repart du début quand le moteur se
   libère et émet `EngineEvent.Restarted` ; la table de hachage, conservée, limite le travail perdu.
4. **Options par recherche** : `SearchRequest` porte MultiPV et force (`EngineStrength` : pleine,
   `Skill Level`, `UCI_Elo`). `UciEngine` n'envoie que les options qui changent d'une recherche à
   l'autre ; `Threads` et `Hash` restent globales (`EngineScheduler.setOption`).
5. **Position avec historique** : `position fen … moves …`, pour que Stockfish voie les
   répétitions.
6. **Échanges UCI jamais coupés au milieu** : lectures et attentes de `readyok` non annulables ; une
   annulation envoie `stop` aussitôt (sans attendre la ligne suivante du moteur) puis lit la sortie
   jusqu'au `bestmove`. La recherche suivante part ainsi d'un flux propre.

## Conséquences
- L'adversaire répond dans son temps par coup même pendant une analyse de fond (mesuré sur la
  tablette : coup de 500 ms rendu en 0,69 s en interrompant une recherche de 30 s).
- Une analyse en direct interrompue reprend à la profondeur 1 : l'écran garde ses lignes tant que
  la nouvelle profondeur ne les dépasse pas (S2-3).
- Le moteur démarre au premier besoin (copie du réseau NNUE comprise), pas au lancement de l'app.
- Maia (ONNX) n'est pas concerné : il ne partage pas Stockfish et sert ses requêtes directement.
