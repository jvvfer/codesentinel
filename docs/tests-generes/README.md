# Tests JUnit générés par Gemini : compilation et exécution

Expérience du 20 octobre 2026 (rapport, partie 5.2).

- Modèle : `gemini-3.1-flash-lite`, sans vérificateur, via `POST /api/analyze` sur les 6 échantillons du benchmark.
- Environnement : JUnit 5.10, Mockito, Java 21, Windows 11.
- Dans chaque dossier : `*Test.java` = le test tel que Gemini l'a généré (non modifié) ; l'autre fichier = l'extrait original du benchmark, non modifié, placé dans la classe que le test attend, avec le minimum de code autour (constructeur, interfaces simulées). Les lignes sous `// --- extrait original ---` sont exactement celles de `BenchmarkService`.

Ces fichiers sont hors de `src/`, donc Maven et la CI ne les compilent pas.

## Étape A : tests tels quels

0 sur 6 compilent. Cause unique : la classe testée et ses dépendances n'existent pas (l'agent reçoit un extrait de méthode et invente la classe autour).

## Étape B : après intégration minimale

| Échantillon | Tests | Réussis | Cause des échecs |
|---|---|---|---|
| 1 Injection SQL | 5 | 5 | Aucune, mais `testFind_SqlInjectionVulnerability` valide la faille au lieu de la refuser |
| 2 Secret en dur | 5 | 2 | Le test attend `"[MASKED_SECRET]"` : le marqueur du masquage a fui dans le test |
| 3 NullPointerException | 5 | 5 | Aucune |
| 4 Fuite de ressource | 5 | 2 | Le fichier jamais fermé empêche JUnit de supprimer `@TempDir` sous Windows : l'exécution révèle la fuite |
| 5 Injection de commande | 4 | 2 | `ping -c` et sortie au format Linux ; le test d'injection ne vérifie rien |
| 6 Code sain | 4 | 4 | Aucune |
| **Total** | **28** | **20 (71 %)** | |

## Reproduire

```
./mvnw dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=cp.txt
javac -cp "$(cat cp.txt)" -d out docs/tests-generes/1/*.java
java -jar junit-platform-console-standalone-1.10.5.jar execute -cp "out;$(cat cp.txt)" --scan-classpath out
```

Sous Windows, le classpath doit utiliser des chemins Windows (`C:\...`), sinon le lanceur ne trouve aucun test.
