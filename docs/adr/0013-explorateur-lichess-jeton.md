# ADR 0013 — Explorateur Lichess : jeton personnel, cache et évaluation locale

- **Statut** : accepté
- **Date** : 2026-11-20

## Contexte
S7-7 ajoute à l'éditeur de répertoire les statistiques de la base Lichess (coups, parties, résultats par
niveau) et une évaluation du moteur. Spike du 2026-11-20 : un appel anonyme à
`https://explorer.lichess.ovh/lichess?fen=…` répond **401 Authorization Required**. L'explorateur exige un
jeton d'API personnel (gratuit, sans permission particulière).

## Décision
- **Jeton personnel facultatif.** L'utilisateur le crée sur lichess.org (Préférences, Clés d'API, aucune
  permission cochée) et le saisit dans le panneau Explorateur. Sans jeton, le panneau l'explique et le reste
  de l'éditeur fonctionne normalement (règle 4, hors ligne d'abord).
- **Stockage** : chiffré par une clé AES-GCM de l'Android Keystore (non exportable) ; seul le texte chiffré
  est écrit dans les préférences de l'application. Il n'est jamais journalisé ni commité.
- **Destination** : le jeton n'est envoyé qu'à `explorer.lichess.ovh`, dans l'en-tête `Authorization: Bearer`.
  Aucune autre requête (import d'étude comprise) n'en porte.
- **Requête** : `GET /lichess?fen=…&ratings=…&speeds=blitz,rapid,classical&moves=12&topGames=0&recentGames=0`.
  Le niveau est un ensemble de tranches `ratings` ; par défaut « Club » (1200, 1400, 1600), proche du
  palier de l'utilisateur.
- **Cache Room** (base v3, table `explorer_position`, clé = FEN sans compteurs + niveau) : une position déjà
  consultée s'affiche sans réseau. Une entrée de moins de 7 jours n'est pas redemandée ; en cas d'échec réseau
  une entrée plus ancienne est servie et signalée comme provenant du cache.
- **Le moteur reste l'autorité (règle 2)** : un coup de l'explorateur n'entre dans le répertoire que par
  `Repertoire.add`, qui le valide ; le SAN de Lichess n'est jamais stocké, il est recalculé.
- **Évaluation** : Stockfish via `EngineScheduler` en priorité `LIVE`, profondeur bornée, annulée dès que la
  position change. Aucune IA en ligne (règle 1).

## Conséquences
- Première donnée secrète de l'application : à revoir si d'autres jetons arrivent (station PC, S12).
- La limite de débit Lichess est respectée par le cache et par une seule requête à la fois (la précédente est
  annulée quand on change de position).
- `core:data` dépend de `android.security`/Keystore uniquement dans `KeystoreTokenStore` ; le reste passe par
  l'interface `TokenStore`, remplaçable dans les tests.
