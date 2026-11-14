package edu.csuf.sis.gateway;

import com.jayway.jsonpath.JsonPath;
import edu.csuf.sis.seed.DemoDataSeeder;
import edu.csuf.sis.support.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The demo data reproduces the team's object diagram (a snapshot during Fall 2026). */
@SpringBootTest(properties = "sis.seed.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class DemoDataIT {

    @Autowired
    MockMvc mvc;

    @Test
    void mariaLopezMatchesTheObjectDiagram() throws Exception {
        String maria = signIn("mlopez");

        mvc.perform(get("/api/student/profile").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$.cwid").value("885104273"))
                .andExpect(jsonPath("$.name").value("Maria Lopez"))
                .andExpect(jsonPath("$.unitsCompleted").value(64))
                .andExpect(jsonPath("$.gradeLevel").value("JUNIOR"))
                .andExpect(jsonPath("$.majorName").value("Computer Science"));

        List<Map<String, Object>> schedule = JsonPath.read(body(get("/api/student/schedule"), maria), "$[?(@.courseCode == 'CPSC 362')]");
        assertThat(schedule).singleElement().satisfies(cpsc362 -> {
            assertThat(cpsc362).containsEntry("termName", "Fall 2026");
            assertThat(cpsc362).containsEntry("sectionNumber", "01");
            assertThat(cpsc362).containsEntry("professorName", "Dr. Alicia Kim");
        });

        List<String> spring2026Grades = JsonPath.read(body(get("/api/student/history"), maria),
                "$.terms[?(@.termName == 'Spring 2026')].courses[?(@.courseCode == 'CPSC 131')].grade");
        assertThat(spring2026Grades).containsExactly("A-");
    }

    @Test
    void cpsc362Section01HasThirtyEightOfFortySeatsTaken() throws Exception {
        List<Map<String, Object>> sections = JsonPath.read(body(get("/api/professor/sections"), signIn("akim")),
                "$[?(@.courseCode == 'CPSC 362' && @.termName == 'Fall 2026' && @.sectionNumber == '01')]");

        assertThat(sections).singleElement().satisfies(section -> {
            assertThat(section).containsEntry("capacity", 40);
            assertThat(section).containsEntry("enrolled", 38);
        });
    }

    @Test
    void cpsc131Section02InSpring2026WasFull() throws Exception {
        String registrar = signIn("registrar");
        List<Integer> spring2026 = JsonPath.read(body(get("/api/catalog/terms"), registrar),
                "$[?(@.name == 'Spring 2026')].id");

        List<Map<String, Object>> sections = JsonPath.read(
                body(get("/api/catalog/terms/{id}/sections", spring2026.get(0)).param("search", "CPSC 131"), registrar),
                "$[?(@.sectionNumber == '02')]");
        assertThat(sections).singleElement().satisfies(section -> {
            assertThat(section).containsEntry("capacity", 40);
            assertThat(section).containsEntry("enrolled", 40);
        });
    }

    @Test
    void ethanBrooksNeedsOnlyTheCapstoneToGraduate() throws Exception {
        mvc.perform(get("/api/admin/students/{cwid}/graduation-check", "885019382")
                        .header(HttpHeaders.AUTHORIZATION, signIn("registrar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.missingCourses[0]").value("CPSC 491"))
                .andExpect(jsonPath("$.unitsCompleted").value(119));
    }

    private String signIn(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "%s"}""".formatted(username, DemoDataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                        String authorization) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
