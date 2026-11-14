package edu.csuf.sis.gateway;

import edu.csuf.sis.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIT extends ApiTest {

    private static final String WRONG_LOGIN = "Incorrect username or password.";

    @Test
    void signingInReturnsATokenAndWhoTheUserIs() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "MLopez", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.studentId").value(mariaId));
    }

    @Test
    void aWrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "mlopez", "password": "not-her-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(WRONG_LOGIN));
    }

    @Test
    void anUnknownUsernameGetsTheSameAnswerAsAWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "nobody", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(WRONG_LOGIN));
    }

    @Test
    void revokedUsersAreToldTheirAccessWasRevoked() throws Exception {
        userAdmin.revokeAccess(mariaAccountId, registrarAccountId);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "mlopez", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("revoked")));
    }

    @Test
    void requestsWithoutATokenMustSignIn() throws Exception {
        mvc.perform(get("/api/catalog/terms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please sign in."));
    }

    @Test
    void aMadeUpTokenIsNotAccepted() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer made-up-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theTokenIdentifiesTheCaller() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, signIn("mlopez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mlopez"))
                .andExpect(jsonPath("$.displayName").value("Maria Lopez"));
    }

    @Test
    void signingOutEndsTheSession() throws Exception {
        String token = signIn("mlopez");

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }
}
