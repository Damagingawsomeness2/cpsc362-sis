package edu.csuf.sis.gateway;

import edu.csuf.sis.registration.IRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfessorApiIT extends ApiTest {

    @Autowired
    IRegistration registration;

    @BeforeEach
    void mariaTakesCpsc362() {
        registration.enroll(mariaId, cpsc362Section);
    }

    @Test
    void professorsSeeTheSectionsTheyTeach() throws Exception {
        mvc.perform(get("/api/professor/sections").header(HttpHeaders.AUTHORIZATION, signIn("akim")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CPSC 362"))
                .andExpect(jsonPath("$[0].enrolled").value(1));
    }

    @Test
    void theRollListsEnrolledStudents() throws Exception {
        mvc.perform(get("/api/professor/sections/{id}/roll", cpsc362Section)
                        .header(HttpHeaders.AUTHORIZATION, signIn("akim")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maria Lopez"))
                .andExpect(jsonPath("$[0].cwid").value(MARIA_CWID));
    }

    @Test
    void postingGradesCompletesTheClassAndAddsUnits() throws Exception {
        mvc.perform(put("/api/professor/sections/{id}/grades", cpsc362Section)
                        .header(HttpHeaders.AUTHORIZATION, signIn("akim"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"grades": [{"studentId": %d, "grade": "A"}]}""".formatted(mariaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grade").value("A"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        assertThat(records.student(mariaId).unitsCompleted()).isEqualTo(6);
    }

    @Test
    void anotherProfessorsSectionIsOffLimits() throws Exception {
        mvc.perform(get("/api/professor/sections/{id}/roll", cpsc362Section)
                        .header(HttpHeaders.AUTHORIZATION, signIn("bcarter")))
                .andExpect(status().isForbidden());
    }

    @Test
    void professorsCanDropAStudentFromTheirSection() throws Exception {
        mvc.perform(delete("/api/professor/sections/{id}/students/{studentId}", cpsc362Section, mariaId)
                        .header(HttpHeaders.AUTHORIZATION, signIn("akim")))
                .andExpect(status().isNoContent());

        assertThat(records.schedule(mariaId)).isEmpty();
    }

    @Test
    void professorsCanLookUpAStudentsScheduleAndHistory() throws Exception {
        String kim = signIn("akim");

        mvc.perform(get("/api/professor/students/{cwid}", MARIA_CWID).header(HttpHeaders.AUTHORIZATION, kim))
                .andExpect(jsonPath("$.name").value("Maria Lopez"));
        mvc.perform(get("/api/professor/students/{cwid}/schedule", MARIA_CWID).header(HttpHeaders.AUTHORIZATION, kim))
                .andExpect(jsonPath("$[0].courseCode").value("CPSC 362"));
        mvc.perform(get("/api/professor/students/{cwid}/history", MARIA_CWID).header(HttpHeaders.AUTHORIZATION, kim))
                .andExpect(jsonPath("$.terms[0].courses[0].courseCode").value("CPSC 131"));
    }

    @Test
    void anUnknownCwidIsNotFound() throws Exception {
        mvc.perform(get("/api/professor/students/{cwid}", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, signIn("akim")))
                .andExpect(status().isNotFound());
    }
}
