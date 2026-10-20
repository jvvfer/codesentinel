package com.codesentinel.secrets;

import smile.classification.RandomForest;
import smile.data.Tuple;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/** Random Forest that says whether a string literal assigned to a variable is a secret. */
public class SecretClassifier {

    public static final String MODEL_RESOURCE = "/secrets/secret-rf.model";
    public static final double DEFAULT_THRESHOLD = 0.5;

    private final RandomForest forest;
    private final double threshold;

    public SecretClassifier(RandomForest forest, double threshold) {
        this.forest = forest;
        this.threshold = threshold;
    }

    /** Loads the model trained by {@link TrainSecretModel}; null when it is missing, so masking falls back to regex. */
    public static SecretClassifier loadDefault() {
        try (InputStream in = SecretClassifier.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (in == null) return null;
            try (ObjectInputStream ois = new ObjectInputStream(in)) {
                return new SecretClassifier((RandomForest) ois.readObject(), DEFAULT_THRESHOLD);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load " + MODEL_RESOURCE, e);
        }
    }

    /** Probability, between 0 and 1, that the value is a secret. */
    public double probability(String variable, String value) {
        double[] posteriori = new double[2];
        forest.predict(Tuple.of(SecretFeatures.of(variable, value), forest.schema()), posteriori);
        return posteriori[1];
    }

    public boolean isSecret(String variable, String value) {
        return probability(variable, value) >= threshold;
    }
}
