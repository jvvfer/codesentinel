# Détection de secrets par Random Forest

Le masquage des secrets (`SecretMasker`) se fait en deux passes avant tout envoi à Gemini :

1. les expressions régulières d'origine (noms connus comme `password`, `token`, valeur de 6 caractères au moins) ;
2. un Random Forest (bibliothèque Smile 2.6.0) qui juge chaque chaîne littérale `nom = "valeur"` laissée par les regex. Si c'est un secret, la valeur devient `[MASKED_SECRET]` et le nom de variable est gardé, comme avant, pour que la règle CWE-798 du prompt fonctionne.

Les résultats chiffrés sont dans [RESULTS.md](RESULTS.md), généré par l'entraînement.

## Fichiers

| Fichier | Rôle |
|---|---|
| `src/main/java/com/codesentinel/secrets/SecretFeatures.java` | 13 caractéristiques : longueur, entropie, proportions de caractères, préfixes connus, nom de variable… |
| `src/main/java/com/codesentinel/secrets/SecretClassifier.java` | Charge le modèle au démarrage, renvoie une probabilité (seuil 0,5) |
| `src/main/java/com/codesentinel/secrets/SecretDataset.java` | Lit les CSV, génère les faux secrets (graine fixe) |
| `src/main/java/com/codesentinel/secrets/TrainSecretModel.java` | Validation croisée, choix du réglage, test final, écrit le modèle et `RESULTS.md` |
| `src/main/resources/secrets/secret-rf.model` | Le modèle entraîné, chargé par l'application |
| `data/secrets/negatives.csv` | 1 732 littéraux réels (JDK 17, Spring AMQP), tous non secrets |
| `data/secrets/manual.csv` | 42 cas écrits à la main, jamais utilisés pour l'entraînement ni le réglage |
| `tools/secrets/extract_negatives.py` | Reconstruit `negatives.csv` à partir des sources Java |

Aucune vraie clé n'est stockée : les secrets d'entraînement sont générés à l'exécution d'après des formats publics (Stripe, AWS, GitHub, Slack, Google, JWT, hexadécimal, base64, mots de passe humains, secrets courts). Le jeu manuel n'utilise que des exemples publics de documentation (clé d'exemple AWS, jeton d'exemple de jwt.io) ou des valeurs inventées.

## Reproduire

```
python tools/secrets/extract_negatives.py <JDK>/lib/src.zip <spring-rabbit-sources.jar> > data/secrets/negatives.csv
./mvnw compile
./mvnw dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes;$(cat target/cp.txt)" com.codesentinel.secrets.TrainSecretModel
```

Sous Linux ou macOS, le séparateur de classpath est `:` au lieu de `;`. Avec les mêmes fichiers, l'entraînement redonne le même modèle (graines fixes).

## Limites connues

- **Le jeu de test est trop facile.** Les secrets générés suivent des formats que le modèle apprend par cœur : F1 de 0,99 sur le test, contre 0,81 sur le jeu manuel. C'est le chiffre du jeu manuel qu'il faut retenir.
- **Fausses alertes sur les empreintes.** Un hash Git, un MD5 et un nom d'algorithme long sont pris pour des secrets : ils ressemblent à des clés aléatoires. Masquer une empreinte est sans danger, mais cela retire une information au modèle Gemini.
- **Secrets humains courts ratés.** `pwd = "hunter2"`, `pin = "4821"` ou `credentials = "admin:admin123"` restent visibles.
- **Seules les affectations `nom = "valeur"` sont analysées.** Un secret passé directement en argument (`connect("admin", "hunter2")`) n'est pas vu.
- **Le jeu manuel est petit** (42 exemples) : un exemple de plus ou de moins change le rappel d'environ 5 points.
- **Licence.** Smile 2.6.0 est sous LGPL-3 ; les versions 3.x sont sous GPL-3, d'où le choix de la 2.6.0.
