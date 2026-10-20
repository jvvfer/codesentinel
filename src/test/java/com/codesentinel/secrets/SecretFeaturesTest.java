package com.codesentinel.secrets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecretFeaturesTest {

    private static double feature(String name, String variable, String value) {
        return SecretFeatures.of(variable, value)[SecretFeatures.NAMES.indexOf(name)];
    }

    @Test
    void entropyIsZeroForOneRepeatedCharAndOneBitForTwo() {
        assertEquals(0.0, SecretFeatures.entropy("aaaa"), 1e-9);
        assertEquals(1.0, SecretFeatures.entropy("abab"), 1e-9);
        assertTrue(SecretFeatures.entropy("q8Zt3VxK9mP2wL5n") > SecretFeatures.entropy("password"));
    }

    @Test
    void vectorHasOneValuePerFeatureName() {
        assertEquals(SecretFeatures.NAMES.size(), SecretFeatures.of("x", "abc").length);
    }

    @Test
    void variableNameHintIsCaseInsensitive() {
        assertEquals(1.0, feature("nameHint", "DB_PASS", "x"));
        assertEquals(1.0, feature("nameHint", "motDePasse", "x"), "mdp is not in motDePasse, but pass is");
        assertEquals(0.0, feature("nameHint", "message", "x"));
    }

    @Test
    void knownPrefixesAndShapesAreFlagged() {
        assertEquals(1.0, feature("knownPrefix", "k", "AKIAIOSFODNN7EXAMPLE"));
        assertEquals(1.0, feature("looksUuid", "id", "550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(1.0, feature("looksHex", "h", "d41d8cd98f00b204e9800998ecf8427e"));
        assertEquals(1.0, feature("hasSpace", "m", "Mot de passe"));
    }

    @Test
    void csvReaderHandlesQuotedCommasAndQuotes() {
        assertEquals(List.of("a", "b,c", "say \"hi\"", "0"),
                SecretDataset.parseCsvLine("a,\"b,c\",\"say \"\"hi\"\"\",0"));
    }

    @Test
    void generatedSecretsAreReproducible() {
        assertEquals(SecretDataset.generateSecrets(50, 7), SecretDataset.generateSecrets(50, 7));
    }
}
