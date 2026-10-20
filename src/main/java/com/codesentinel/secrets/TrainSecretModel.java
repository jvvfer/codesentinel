package com.codesentinel.secrets;

import com.codesentinel.SecretMasker;
import com.codesentinel.secrets.SecretDataset.Example;
import smile.base.cart.SplitRule;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.IntVector;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.LongStream;

/**
 * Trains and evaluates the secret classifier, then writes the model and a results report.
 *
 * <p>Run from the project root after {@code ./mvnw compile}:
 * {@code java -cp "target/classes;<dependencies>" com.codesentinel.secrets.TrainSecretModel}
 * (see docs/secrets-rf/README.md).
 */
public final class TrainSecretModel {

    private static final long SEED = 42;
    private static final int FOLDS = 5;
    private static final int[][] GRID = {{100, 3}, {100, 6}, {300, 3}, {300, 6}}; // {trees, mtry}

    record Metrics(int tp, int fp, int fn, int tn) {
        double precision() { return tp + fp == 0 ? 0 : (double) tp / (tp + fp); }
        double recall() { return tp + fn == 0 ? 0 : (double) tp / (tp + fn); }
        double f1() { double p = precision(), r = recall(); return p + r == 0 ? 0 : 2 * p * r / (p + r); }
        Metrics plus(Metrics o) { return new Metrics(tp + o.tp, fp + o.fp, fn + o.fn, tn + o.tn); }
    }

    public static void main(String[] args) throws IOException {
        Path negativesCsv = Path.of("data/secrets/negatives.csv");
        Path manualCsv = Path.of("data/secrets/manual.csv");
        Path modelOut = Path.of("src/main/resources/secrets/secret-rf.model");
        Path reportOut = Path.of("docs/secrets-rf/RESULTS.md");

        List<Example> negatives = SecretDataset.readCsv(negativesCsv);
        List<Example> all = new ArrayList<>(negatives);
        all.addAll(SecretDataset.generateSecrets(negatives.size(), SEED));
        all = dedupeByValue(all);
        Collections.shuffle(all, new Random(SEED));
        List<Example> manual = SecretDataset.readCsv(manualCsv);

        // stratified 80 / 20 split, the test part is only read once at the end
        List<Example> train = new ArrayList<>(), test = new ArrayList<>();
        for (int label = 0; label <= 1; label++) {
            int l = label;
            List<Example> cls = all.stream().filter(e -> e.label() == l).toList();
            int cut = (int) Math.round(cls.size() * 0.8);
            train.addAll(cls.subList(0, cut));
            test.addAll(cls.subList(cut, cls.size()));
        }
        Collections.shuffle(train, new Random(SEED));

        SecretMasker regexOnly = new SecretMasker(null);
        Predicate<Example> regex = e -> masked(regexOnly, e);

        // 5-fold cross-validation on the training part, for each setting of the grid
        StringBuilder cv = new StringBuilder();
        int[] best = GRID[0];
        double bestF1 = -1;
        Metrics regexCv = evaluate(train, regex);
        for (int[] g : GRID) {
            Metrics sum = new Metrics(0, 0, 0, 0);
            for (int k = 0; k < FOLDS; k++) {
                List<Example> fit = new ArrayList<>(), held = new ArrayList<>();
                for (int i = 0; i < train.size(); i++) (i % FOLDS == k ? held : fit).add(train.get(i));
                SecretClassifier c = new SecretClassifier(fit(fit, g[0], g[1]), SecretClassifier.DEFAULT_THRESHOLD);
                sum = sum.plus(evaluate(held, e -> c.isSecret(e.variable(), e.value())));
            }
            cv.append(row(g[0] + " arbres, mtry = " + g[1], sum));
            if (sum.f1() > bestF1) { bestF1 = sum.f1(); best = g; }
        }

        RandomForest forest = fit(train, best[0], best[1]);
        SecretClassifier classifier = new SecretClassifier(forest, SecretClassifier.DEFAULT_THRESHOLD);
        SecretMasker combined = new SecretMasker(classifier);
        Predicate<Example> rf = e -> classifier.isSecret(e.variable(), e.value());
        Predicate<Example> both = e -> masked(combined, e);

        Files.createDirectories(modelOut.getParent());
        try (OutputStream os = Files.newOutputStream(modelOut); ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(forest);
        }

        Map<String, Predicate<Example>> configs = new LinkedHashMap<>();
        configs.put("Regex actuelles", regex);
        configs.put("Random Forest seul", rf);
        configs.put("Regex + Random Forest (version intégrée)", both);

        StringBuilder r = new StringBuilder();
        r.append("# Résultats : détection de secrets par Random Forest\n\n");
        r.append("Fichier généré par `TrainSecretModel` ; ne pas modifier à la main.\n\n");
        r.append("## Données\n\n");
        r.append(String.format(Locale.ROOT, "- %d exemples après dédoublonnage : %d valeurs normales (littéraux réels du JDK 17 et de Spring), %d secrets générés (graine %d).%n",
                all.size(), count(all, 0), count(all, 1), SEED));
        r.append(String.format(Locale.ROOT, "- Entraînement : %d exemples ; test mis de côté : %d exemples (20 %%, stratifié).%n", train.size(), test.size()));
        r.append(String.format(Locale.ROOT, "- Jeu de contrôle écrit à la main : %d exemples (%d secrets, %d valeurs normales).%n%n",
                manual.size(), count(manual, 1), count(manual, 0)));

        r.append("## Validation croisée (5 plis, entraînement)\n\n");
        r.append(header());
        r.append(row("Regex actuelles", regexCv));
        r.append(cv);
        r.append(String.format(Locale.ROOT, "%nRéglage retenu (meilleur F1) : %d arbres, mtry = %d, profondeur max 20, seuil %.1f.%n%n",
                best[0], best[1], SecretClassifier.DEFAULT_THRESHOLD));

        r.append("## Jeu de test (jamais vu pendant le réglage)\n\n").append(header());
        configs.forEach((name, p) -> r.append(row(name, evaluate(test, p))));
        r.append("\n## Jeu de contrôle écrit à la main\n\n").append(header());
        configs.forEach((name, p) -> r.append(row(name, evaluate(manual, p))));

        r.append("\n### Erreurs de la version intégrée sur le jeu de contrôle\n\n");
        r.append("| Variable | Valeur | Attendu | Probabilité RF |\n| --- | --- | --- | --- |\n");
        for (Example e : manual) {
            if (both.test(e) != (e.label() == 1)) {
                r.append(String.format(Locale.ROOT, "| `%s` | `%s` | %s | %.2f |%n", e.variable(), e.value(),
                        e.label() == 1 ? "secret" : "normal", classifier.probability(e.variable(), e.value())));
            }
        }

        r.append("\n## Importance des caractéristiques\n\n| Caractéristique | Importance (Gini) |\n| --- | --- |\n");
        double[] imp = forest.importance();
        double total = 0;
        for (double v : imp) total += v;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < imp.length; i++) order.add(i);
        order.sort((a, b) -> Double.compare(imp[b], imp[a]));
        for (int i : order) {
            r.append(String.format(Locale.ROOT, "| `%s` | %.1f %% |%n", SecretFeatures.NAMES.get(i), 100 * imp[i] / total));
        }

        Files.createDirectories(reportOut.getParent());
        Files.writeString(reportOut, r.toString(), StandardCharsets.UTF_8);
        System.out.println(r);
    }

    static RandomForest fit(List<Example> examples, int trees, int mtry) {
        double[][] x = new double[examples.size()][];
        int[] y = new int[examples.size()];
        for (int i = 0; i < examples.size(); i++) {
            Example e = examples.get(i);
            x[i] = SecretFeatures.of(e.variable(), e.value());
            y[i] = e.label();
        }
        DataFrame df = DataFrame.of(x, SecretFeatures.NAMES.toArray(String[]::new)).merge(IntVector.of("label", y));
        return RandomForest.fit(Formula.lhs("label"), df, trees, mtry, SplitRule.GINI, 20, 1000, 2, 1.0,
                new int[] {1, 1}, LongStream.range(SEED, SEED + trees));
    }

    private static boolean masked(SecretMasker masker, Example e) {
        return masker.mask(e.variable() + " = \"" + e.value() + "\";").contains("[MASKED_SECRET]");
    }

    private static Metrics evaluate(List<Example> examples, Predicate<Example> secret) {
        int tp = 0, fp = 0, fn = 0, tn = 0;
        for (Example e : examples) {
            boolean predicted = secret.test(e), actual = e.label() == 1;
            if (predicted && actual) tp++;
            else if (predicted) fp++;
            else if (actual) fn++;
            else tn++;
        }
        return new Metrics(tp, fp, fn, tn);
    }

    private static List<Example> dedupeByValue(List<Example> examples) {
        Map<String, Example> byValue = new LinkedHashMap<>();
        for (Example e : examples) byValue.putIfAbsent(e.value(), e);
        return new ArrayList<>(byValue.values());
    }

    private static long count(List<Example> examples, int label) {
        return examples.stream().filter(e -> e.label() == label).count();
    }

    private static String header() {
        return "| Configuration | Précision | Rappel | F1 | Secrets ratés | Fausses alertes |\n| --- | --- | --- | --- | --- | --- |\n";
    }

    private static String row(String name, Metrics m) {
        return String.format(Locale.ROOT, "| %s | %.3f | %.3f | %.3f | %d | %d |%n",
                name, m.precision(), m.recall(), m.f1(), m.fn(), m.fp());
    }
}
