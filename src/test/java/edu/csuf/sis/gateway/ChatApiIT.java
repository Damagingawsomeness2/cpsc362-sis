package edu.csuf.sis.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatApiIT extends ApiTest {

    @Test
    void theAssistantAnswersSignedInUsers() throws Exception {
        mvc.perform(post("/api/chat").header(HttpHeaders.AUTHORIZATION, signIn("mlopez"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"message": "What are the prerequisites for CPSC 362?"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("CPSC 131")));
    }

    @Test
    void overlyLongQuestionsAreRejected() throws Exception {
        mvc.perform(post("/api/chat").header(HttpHeaders.AUTHORIZATION, signIn("mlopez"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"message": "%s"}""".formatted("a".repeat(501))))
                .andExpect(status().isBadRequest());
    }
}
