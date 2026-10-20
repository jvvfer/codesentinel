package com.codesentinel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretMaskerTest {

    private final SecretMasker masker = new SecretMasker();

    @Test
    void hardcodedPasswordIsMaskedButVariableNameKept() {
        String out = masker.mask("String password = \"Sup3rS3cret!\";");
        assertTrue(out.contains("password = \"[MASKED_SECRET]\""));
        assertFalse(out.contains("Sup3rS3cret"));
    }

    @Test
    void emailBearerAndDbUrlAreMasked() {
        String out = masker.mask("""
                String mail = "alice@example.com";
                String h = "Bearer abcdefghijklmnopqrstuvwxyz123";
                String url = "jdbc:mysql://db.local:3306/app";
                """);
        assertTrue(out.contains("[MASKED_EMAIL]"));
        assertTrue(out.contains("Bearer [MASKED_SECRET]"));
        assertTrue(out.contains("[MASKED_DB_URL]"));
        assertFalse(out.contains("alice@example.com"));
    }

    @Test
    void ordinaryCodeIsUnchanged() {
        String code = "public int add(int a, int b) { return Math.addExact(a, b); }";
        assertEquals(code, masker.mask(code));
    }
}
