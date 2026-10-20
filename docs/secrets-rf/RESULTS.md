# Résultats : détection de secrets par Random Forest

Fichier généré par `TrainSecretModel` ; ne pas modifier à la main.

## Données

- 3384 exemples après dédoublonnage : 1652 valeurs normales (littéraux réels du JDK 17 et de Spring), 1732 secrets générés (graine 42).
- Entraînement : 2708 exemples ; test mis de côté : 676 exemples (20 %, stratifié).
- Jeu de contrôle écrit à la main : 42 exemples (19 secrets, 23 valeurs normales).

## Validation croisée (5 plis, entraînement)

| Configuration | Précision | Rappel | F1 | Secrets ratés | Fausses alertes |
| --- | --- | --- | --- | --- | --- |
| Regex actuelles | 0.998 | 0.426 | 0.597 | 796 | 1 |
| 100 arbres, mtry = 3 | 0.989 | 0.989 | 0.989 | 15 | 15 |
| 100 arbres, mtry = 6 | 0.983 | 0.987 | 0.985 | 18 | 24 |
| 300 arbres, mtry = 3 | 0.989 | 0.991 | 0.990 | 13 | 15 |
| 300 arbres, mtry = 6 | 0.983 | 0.986 | 0.985 | 19 | 24 |

Réglage retenu (meilleur F1) : 300 arbres, mtry = 3, profondeur max 20, seuil 0.5.

## Jeu de test (jamais vu pendant le réglage)

| Configuration | Précision | Rappel | F1 | Secrets ratés | Fausses alertes |
| --- | --- | --- | --- | --- | --- |
| Regex actuelles | 0.993 | 0.416 | 0.587 | 202 | 1 |
| Random Forest seul | 0.977 | 1.000 | 0.989 | 0 | 8 |
| Regex + Random Forest (version intégrée) | 0.975 | 1.000 | 0.987 | 0 | 9 |

## Jeu de contrôle écrit à la main

| Configuration | Précision | Rappel | F1 | Secrets ratés | Fausses alertes |
| --- | --- | --- | --- | --- | --- |
| Regex actuelles | 1.000 | 0.316 | 0.480 | 13 | 0 |
| Random Forest seul | 0.800 | 0.632 | 0.706 | 7 | 3 |
| Regex + Random Forest (version intégrée) | 0.833 | 0.789 | 0.811 | 4 | 3 |

### Erreurs de la version intégrée sur le jeu de contrôle

| Variable | Valeur | Attendu | Probabilité RF |
| --- | --- | --- | --- |
| `pwd` | `hunter2` | secret | 0.44 |
| `value` | `Zx9!kLm2#Qr7` | secret | 0.32 |
| `pin` | `4821` | secret | 0.19 |
| `credentials` | `admin:admin123` | secret | 0.40 |
| `commit` | `9fceb02d0ae598e95dc970b74767f19372d61af8` | normal | 0.97 |
| `checksum` | `d41d8cd98f00b204e9800998ecf8427e` | normal | 0.93 |
| `keyAlgorithm` | `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` | normal | 0.99 |

## Importance des caractéristiques

| Caractéristique | Importance (Gini) |
| --- | --- |
| `entropy` | 15.4 % |
| `digitRatio` | 15.3 % |
| `lowerRatio` | 13.8 % |
| `length` | 13.2 % |
| `upperRatio` | 12.1 % |
| `symbolRatio` | 12.1 % |
| `nameHint` | 11.3 % |
| `charClasses` | 2.9 % |
| `knownPrefix` | 2.4 % |
| `hasSpace` | 0.6 % |
| `looksHex` | 0.5 % |
| `hasDotOrSlash` | 0.3 % |
| `looksUuid` | 0.0 % |
