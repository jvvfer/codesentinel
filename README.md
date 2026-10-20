# CodeSentinel

Application web de revue de code assistée par IA, réalisée pour le mini-projet IA for Software Engineering (ESPRIT, A.U 2026-2027).

On colle du code Java dans la page. L'application renvoie les bugs et vulnérabilités trouvés, des pistes de refactoring, des tests JUnit générés et un score de qualité sur 100.

## Prérequis

- Java 17
- Une clé API Gemini gratuite : aistudio.google.com, puis **Get API key**
- Pas besoin d'installer Maven : le wrapper `mvnw` est dans le dépôt

## Installation

1. Cloner le dépôt :

```
git clone https://github.com/jvvfer/codesentinel.git
cd codesentinel
```

2. **Crée ton `secrets.properties` avec ta clé Gemini**, à la racine du projet (à côté de `pom.xml`), avec une seule ligne :

```
llm.api-key=TA_CLE_GEMINI
```

Pas de guillemets, pas d'espaces autour du `=`. Ce fichier est ignoré par Git : ne le commite jamais. À la place, on peut aussi définir la variable d'environnement `GEMINI_API_KEY`.

## Lancer l'application

**Depuis IntelliJ** : ouvrir le projet et lancer `CodeSentinelApplication`. Le dossier de travail (Working directory) doit être la racine du projet, sinon `secrets.properties` n'est pas trouvé.

**Depuis le terminal** :

```
./mvnw spring-boot:run
```

Sous Windows (PowerShell), il faut d'abord définir `JAVA_HOME` :

```
$env:JAVA_HOME="C:\chemin\vers\jdk-17"
.\mvnw.cmd spring-boot:run
```

L'application démarre sur http://localhost:8080.

## Utilisation

- http://localhost:8080 : page d'analyse (code, choix du modèle, vérificateur)
- http://localhost:8080/benchmark.html : benchmark sur 6 échantillons (5 défauts connus et 1 code sain)

### API

| Méthode | URL | Rôle |
|---|---|---|
| POST | `/api/analyze` | Analyse un code |
| GET | `/api/benchmark/samples` | Liste les échantillons du benchmark |
| GET | `/api/benchmark/run?id=1&model=...&verify=true` | Lance un échantillon |

Exemple de requête pour `/api/analyze` :

```json
{
  "code": "String q = \"SELECT * FROM users WHERE id=\" + id;",
  "model": "gemini-3.1-flash-lite",
  "verify": true
}
```

La réponse contient `findings`, `generatedTests`, `score`, `latencyMs`, `cached`, `model` et `verified`.

## Modèles et quotas

Le projet utilise l'offre gratuite de Google AI Studio, qui a des quotas par minute et par jour. Le modèle par défaut est défini dans `src/main/resources/application.yml` (`llm.model`).

Si tu obtiens une erreur 429 (quota dépassé) ou 503 (modèle surchargé), choisis un autre modèle dans le menu de la page, par exemple `gemini-3.1-flash-lite`, ou attends quelques minutes. Les quotas se vérifient dans AI Studio.

## Structure

```
src/main/java/com/codesentinel/
├── AnalysisController.java    API REST /api/analyze
├── AnalysisService.java       masquage, cache, agents, vérificateur, score
├── SecretMasker.java          masque les secrets avant l'envoi au modèle
├── GeminiClient.java          appel à l'API Gemini, relances, erreurs
├── Prompts.java               les 4 prompts système
├── BenchmarkService.java      échantillons et mesures
├── BenchmarkController.java   API /api/benchmark
├── Finding.java               un problème détecté
└── AnalysisResponse.java      réponse de l'analyse
src/main/resources/
├── application.yml
└── static/                    index.html et benchmark.html
```

## Limites

- Les tests générés ne sont pas exécutés et doivent être relus.
- Le cache est en mémoire : il est perdu au redémarrage, et il n'y a pas d'historique.
- Ne pas analyser de code confidentiel : le code est envoyé à un service externe.

## Auteurs

Jaafer Abidi et Aziz Chouikha, ESPRIT.