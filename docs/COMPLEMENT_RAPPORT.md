# Complément au rapport technique – CodeSentinel

Mini-projet IA for Software Engineering · ESPRIT · A.U 2026-2027

**Membres du groupe :** Jaafer Abidi, Heni Slejmi, Aziz Chouikha

Ce document complète le rapport : membres du groupe, répartition des tâches (critère « Collaboration, répartition des tâches et dépôt Git »), améliorations apportées après la première version et documentations individuelles de Heni et Aziz.

## 1. Répartition des tâches

| Membre | Rôle | Code | Sections du rapport |
|---|---|---|---|
| Jaafer Abidi | Moteur IA | `GeminiClient`, `Prompts`, `AnalysisService` (agents, vérificateur, cache, score), création du dépôt | §3, §4.1, §6.4, §8, annexe A |
| Heni Slejmi | Sécurité et interface | `SecretMasker`, `AnalysisController` (validation), `index.html` | §2, §4.2, §5.1, §6.5, annexe C |
| Aziz Chouikha | Tests, évaluation et fiabilité | `BenchmarkService`, `benchmark.html`, tests unitaires, gestion des réponses invalides, mesure des tokens, CI | §5.2, §6.1–6.3, §7, §9, annexe B |

Chaque membre committe sous son propre nom sur le dépôt : https://github.com/jvvfer/codesentinel

## 2. Améliorations après la première version

Ces changements répondent à des limites citées dans le rapport (§6.5 et §9).

| Limite du rapport | Correction |
|---|---|
| « JSON invalide : il est traité comme une liste vide, donc un échec d'analyse ressemble à un code sans problème » | Une réponse invalide d'un agent est maintenant signalée (`failedAgents` dans la réponse de l'API). La page affiche « Analyse incomplète » et le benchmark compte les analyses avec réponse invalide. Un résultat partiel n'est plus mis en cache. |
| « On n'a pas mesuré la consommation de tokens » | Chaque appel lit `usageMetadata.totalTokenCount` de Gemini. Le total par analyse est affiché sur la page et dans le benchmark (moyenne par analyse). |
| Température laissée à la valeur par défaut | Paramètre optionnel `llm.temperature` dans `application.yml`. Sans valeur, le modèle garde sa température par défaut. |
| Aucun test automatisé de l'application | Tests JUnit 5 avec Mockito : `SecretMaskerTest` (masquage des secrets, des emails, des jetons Bearer et des URL de base de données) et `AnalysisServiceTest` (score, réponse invalide, vérificateur invalide, cache). Ils n'appellent pas Gemini et tournent sans clé. |
| Pas de CI/CD (cité dans « Valeurs ajoutées » de la grille) | Workflow GitHub Actions (`.github/workflows/ci.yml`) qui compile le projet et lance les tests à chaque push. |

---

## Annexe B : documentation individuelle (Aziz Chouikha)

### B.1 Outils utilisés

J'ai utilisé **Claude Code** (l'agent de Claude dans VS Code), qui peut lire le projet, lancer des commandes et modifier les fichiers. Gemini (API) reste le modèle du module IA de l'application.

### B.2 Interactions avec le LLM

| Ma demande (extraits) | Ce que Claude a produit | Ce que j'ai fait ou vérifié |
|---|---|---|
| « cloner repo https://github.com/jvvfer/codesentinel.git » | Clonage du dépôt et identification d'un projet Spring Boot (Maven) | J'ai récupéré le projet de Jaafer sur ma machine |
| « run le projet » | Lecture du README, lancement avec `mvnw spring-boot:run`, constat qu'aucune clé Gemini n'était configurée | L'application tournait sur http://localhost:8080 |
| « Crée le fichier et fais le nécessaire » (avec ma clé) | Création de `secrets.properties`, vérification qu'il est ignoré par Git, redémarrage, analyse de test | L'analyse a détecté l'injection SQL (CWE-89), score 85/100 en environ 4 s |
| « changer la clé » | Remplacement de la clé et nouveau test | Analyse fonctionnelle avec la nouvelle clé |
| Le rapport et « partitionner ce travail en 3 personnes » | Proposition de répartition en trois rôles et liste des limites du rapport à corriger | Répartition retenue dans la partie 1 |
| La grille d'évaluation et « donner la note que je peux obtenir » | Estimation critère par critère et points qui rapportent le plus | Choix des améliorations de la partie 2 |
| « compléter le nécessaire et faire push » | Gestion des réponses invalides, mesure des tokens, température configurable, tests unitaires, CI, ce document | Relecture, tests exécutés, push |

**Erreur relevée sur une proposition du LLM :** une première version de la gestion des réponses invalides contenait deux erreurs de syntaxe JavaScript : une apostrophe (« l'agent ») dans une chaîne entre apostrophes, et un retour à la ligne à la place de `\n`. Elles auraient cassé les deux pages. Elles ont été corrigées avant le commit.

### B.3 Justification des modèles

- **Claude Code pour le développement** : contrairement à une interface de chat, il travaille directement dans le dépôt. Il lance l'application, exécute les tests et lit les erreurs lui-même, sans copier-coller.
- **Gemini Flash Lite pour l'application** : je garde le choix de l'équipe (rapide et quota gratuit plus large, voir §3). La mesure des tokens permet maintenant de comparer aussi les modèles sur leur consommation.

### B.4 Ma contribution

- Mise en route du projet sur ma machine (clonage, configuration de la clé, lancement).
- Analyse du rapport face à la grille d'évaluation et proposition de la répartition des tâches.
- Fiabilité : une réponse invalide de Gemini n'est plus confondue avec un code sain.
- Optimisation : mesure des tokens par analyse et température configurable.
- Tests unitaires de `SecretMasker` et `AnalysisService`, et intégration continue GitHub Actions.
- Rédaction de ce complément au rapport.

### B.5 Gain de productivité

La mise en route (clonage, clé, lancement et test), qui avait demandé plusieurs échanges à Jaafer à cause de JAVA_HOME et de PowerShell, a pris quelques minutes : l'agent a trouvé lui-même le JDK installé et lancé l'application. Je n'ai pas mesuré le temps qu'aurait pris le même travail sans IA.

---

## Annexe C : documentation individuelle (Heni Slejmi)

> **À compléter par Heni** avec ses propres échanges avec un LLM. Ne pas recopier ceux d'un autre membre : l'enseignant peut demander à voir la conversation.

### C.1 Outils utilisés

_Quels outils d'IA (Claude, ChatGPT, Copilot…) et pour quoi faire._

### C.2 Interactions avec le LLM

| Ma demande (extraits) | Ce que le LLM a produit | Ce que j'ai fait ou corrigé |
|---|---|---|
| | | |

### C.3 Justification des modèles

### C.4 Ma contribution

_Par exemple : `SecretMasker`, validation dans `AnalysisController`, `index.html`, sections §2, §4.2, §5.1 et §6.5._

### C.5 Gain de productivité
