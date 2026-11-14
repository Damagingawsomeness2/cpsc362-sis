package edu.csuf.sis.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentApiIT extends ApiTest {

    @Test
    void browsingSectionsShowsOpenSeats() throws Exception {
        mvc.perform(get("/api/catalog/terms/{termId}/sections", spring2027).param("search", "cpsc362")
                        .header(HttpHeaders.AUTHORIZATION, signIn("mlopez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].meets").value("MW 10:00 AM–11:15 AM"))
                .andExpect(jsonPath("$[0].professorName").value("Alicia Kim"))
                .andExpect(jsonPath("$[0].openSeats").value(40));
    }

    @Test
    void registeringReturnsTheNewClassAndTakesASeat() throws Exception {
        String maria = signIn("mlopez");

        mvc.perform(post("/api/student/enrollments").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"sectionId": %d}""".formatted(cpsc362Section)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("CPSC 362"))
                .andExpect(jsonPath("$.status").value("ENROLLED"));

        mvc.perform(get("/api/catalog/terms/{termId}/sections", spring2027).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$[0].openSeats").value(39));
    }

    @Test
    void aRefusedRegistrationExplainsWhy() throws Exception {
        mvc.perform(post("/api/student/enrollments").header(HttpHeaders.AUTHORIZATION, signIn("sortiz"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"sectionId": %d}""".formatted(cpsc362Section)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("CPSC 131")));
    }

    @Test
    void aRegistrationWithoutASectionIsRejected() throws Exception {
        mvc.perform(post("/api/student/enrollments").header(HttpHeaders.AUTHORIZATION, signIn("mlopez"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("sectionId")));
    }

    @Test
    void droppingRemovesTheClassFromTheSchedule() throws Exception {
        String maria = signIn("mlopez");
        mvc.perform(post("/api/student/enrollments").header(HttpHeaders.AUTHORIZATION, maria)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sectionId": %d}""".formatted(cpsc362Section)));

        mvc.perform(delete("/api/student/enrollments/{sectionId}", cpsc362Section)
                        .header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/student/schedule").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void historyShowsCompletedClassesWithGpa() throws Exception {
        mvc.perform(get("/api/student/history").header(HttpHeaders.AUTHORIZATION, signIn("mlopez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms[0].termName").value("Fall 2025"))
                .andExpect(jsonPath("$.terms[0].courses[0].courseCode").value("CPSC 131"))
                .andExpect(jsonPath("$.terms[0].courses[0].grade").value("A-"))
                .andExpect(jsonPath("$.gpa").value(3.7));
    }

    @Test
    void profileShowsTheStudentsOwnRecordOnly() throws Exception {
        mvc.perform(get("/api/student/profile").header(HttpHeaders.AUTHORIZATION, signIn("mlopez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cwid").value(MARIA_CWID))
                .andExpect(jsonPath("$.unitsCompleted").value(3))
                .andExpect(jsonPath("$.gradeLevel").value("FRESHMAN"));
    }
}
