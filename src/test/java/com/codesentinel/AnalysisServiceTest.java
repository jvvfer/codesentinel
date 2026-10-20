package com.codesentinel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AnalysisServiceTest {

    private static final String MODEL = "test-model";
    private static final String SQL_FINDING = """
            {"findings":[{"type":"vulnerability","severity":"critical","line":1,
              "title":"Injection SQL","explanation":"concaténation","fix":"PreparedStatement"}]}""";
    private static final String NO_FINDING = "{\"findings\":[]}";
    private static final String TESTS = "{\"tests\":\"class T {}\"}";

    private GeminiClient llm;
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        llm = mock(GeminiClient.class);
        service = new AnalysisService(llm, new SecretMasker());
    }

    private void answer(String prompt, String json) {
        when(llm.ask(eq(MODEL), eq(prompt), anyString())).thenReturn(new GeminiClient.Reply(json, 100));
    }

    @Test
    void validAnswersGiveScoreAndNoFailure() {
        answer(Prompts.SECURITY, SQL_FINDING);
        answer(Prompts.QUALITY, NO_FINDING);
        answer(Prompts.TESTS, TESTS);

        AnalysisResponse r = service.analyze("String q = \"x\" + id;", MODEL, false);

        assertEquals(1, r.findings().size());
        assertEquals(70, r.score());
        assertEquals("class T {}", r.generatedTests());
        assertTrue(r.failedAgents().isEmpty());
        assertEquals(300, r.tokens()); // three agents, 100 tokens each
    }

    @Test
    void invalidJsonIsReportedInsteadOfLookingClean() {
        answer(Prompts.SECURITY, "not json at all");
        answer(Prompts.QUALITY, NO_FINDING);
        answer(Prompts.TESTS, TESTS);

        AnalysisResponse r = service.analyze("int a = 1;", MODEL, false);

        assertEquals(List.of("security"), r.failedAgents());
        assertTrue(r.findings().isEmpty());
    }

    @Test
    void invalidVerifierAnswerKeepsUnverifiedFindings() {
        answer(Prompts.SECURITY, SQL_FINDING);
        answer(Prompts.QUALITY, NO_FINDING);
        answer(Prompts.TESTS, TESTS);
        answer(Prompts.VERIFIER, "{\"oops\":true}");

        AnalysisResponse r = service.analyze("String q = \"x\" + id;", MODEL, true);

        assertEquals(1, r.findings().size());
        assertFalse(r.verified());
        assertEquals(List.of("verifier"), r.failedAgents());
    }

    @Test
    void partialResultIsNotCached() {
        answer(Prompts.SECURITY, "garbage");
        answer(Prompts.QUALITY, NO_FINDING);
        answer(Prompts.TESTS, TESTS);

        service.analyze("int a = 1;", MODEL, false);
        AnalysisResponse second = service.analyze("int a = 1;", MODEL, false);

        assertFalse(second.cached());
        verify(llm, times(2)).ask(eq(MODEL), eq(Prompts.SECURITY), anyString());
    }

    @Test
    void completeResultIsCached() {
        answer(Prompts.SECURITY, NO_FINDING);
        answer(Prompts.QUALITY, NO_FINDING);
        answer(Prompts.TESTS, TESTS);

        service.analyze("int a = 1;", MODEL, false);
        AnalysisResponse second = service.analyze("int a = 1;", MODEL, false);

        assertTrue(second.cached());
        verify(llm, times(1)).ask(eq(MODEL), eq(Prompts.SECURITY), anyString());
    }
}
