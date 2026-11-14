package edu.csuf.sis.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The gateway's role-based access: every role reaches its own portal's API and nothing else. */
class AccessControlApiIT extends ApiTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "mlopez,    /api/student/schedule,   200",
            "mlopez,    /api/professor/sections, 403",
            "mlopez,    /api/admin/accounts,     403",
            "akim,      /api/professor/sections, 200",
            "akim,      /api/student/schedule,   403",
            "akim,      /api/admin/accounts,     403",
            "registrar, /api/admin/accounts,     200",
            "registrar, /api/student/schedule,   403",
            "registrar, /api/professor/sections, 403"
    })
    void eachRoleReachesOnlyItsOwnPortal(String username, String path, int expectedStatus) throws Exception {
        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, signIn(username)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void everyRoleCanBrowseTheCatalog() throws Exception {
        for (String username : new String[] {"mlopez", "akim", "registrar"}) {
            mvc.perform(get("/api/catalog/terms").header(HttpHeaders.AUTHORIZATION, signIn(username)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void responsesForbidScriptsFromOtherSites() throws Exception {
        mvc.perform(get("/api/catalog/terms").header(HttpHeaders.AUTHORIZATION, signIn("mlopez")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }
}
