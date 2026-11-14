package edu.csuf.sis.gateway;

import edu.csuf.sis.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiIT extends ApiTest {

    @Test
    void theRegistrarAddsACourseWithPrerequisites() throws Exception {
        mvc.perform(post("/api/admin/courses").header(HttpHeaders.AUTHORIZATION, signIn("registrar"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"code": "cpsc 332", "title": "File Structures and Database Systems",
                                 "description": "Files, indexing and relational databases.",
                                 "units": 3, "prerequisites": ["CPSC 131"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CPSC 332"))
                .andExpect(jsonPath("$.prerequisites[0]").value("CPSC 131"));
    }

    @Test
    void aMalformedCourseCodeIsABadRequest() throws Exception {
        mvc.perform(post("/api/admin/courses").header(HttpHeaders.AUTHORIZATION, signIn("registrar"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"code": "databases", "title": "Databases", "units": 3}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("course code")));
    }

    @Test
    void theRegistrarSchedulesASection() throws Exception {
        mvc.perform(post("/api/admin/sections").header(HttpHeaders.AUTHORIZATION, signIn("registrar"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"termId": %d, "courseCode": "CPSC 362", "sectionNumber": "02",
                                 "professorId": %d, "capacity": 35, "days": "TuTh",
                                 "startTime": "13:00", "endTime": "14:15", "room": "CS-110"}"""
                                .formatted(spring2027, kimId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meets").value("TuTh 1:00 PM–2:15 PM"))
                .andExpect(jsonPath("$.openSeats").value(35));
    }

    @Test
    void aNewStudentCanSignInWithTheAssignedPassword() throws Exception {
        mvc.perform(post("/api/admin/students").header(HttpHeaders.AUTHORIZATION, signIn("registrar"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cwid": "885600005", "name": "Nina Shah", "email": "nshah@csu.fullerton.edu",
                                 "username": "nshah", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        signIn("nshah");
    }

    @Test
    void invalidStudentDetailsAreReportedFieldByField() throws Exception {
        mvc.perform(post("/api/admin/students").header(HttpHeaders.AUTHORIZATION, signIn("registrar"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cwid": "123", "name": "Nina Shah", "username": "nshah", "password": "short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("cwid")))
                .andExpect(jsonPath("$.detail", containsString("password")));
    }

    @Test
    void revokingAccessBlocksSignInButKeepsTheTranscript() throws Exception {
        String registrar = signIn("registrar");

        mvc.perform(post("/api/admin/accounts/{id}/revoke", mariaAccountId).header(HttpHeaders.AUTHORIZATION, registrar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "mlopez", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/students/{cwid}/transcript", MARIA_CWID).header(HttpHeaders.AUTHORIZATION, registrar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms[0].courses[0].courseCode").value("CPSC 131"))
                .andExpect(jsonPath("$.student.accessEnabled").value(false));
    }

    @Test
    void theGraduationCheckListsMissingCourses() throws Exception {
        String registrar = signIn("registrar");
        mvc.perform(put("/api/admin/majors/{code}", "BSCS").header(HttpHeaders.AUTHORIZATION, registrar)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Computer Science", "degree": "B.S.", "totalUnits": 120,
                                 "requiredCourses": ["CPSC 131", "CPSC 362"]}"""))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/students").header(HttpHeaders.AUTHORIZATION, registrar)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"cwid": "885500004", "name": "Ethan Brooks", "majorCode": "BSCS",
                         "username": "ebrooks", "password": "%s"}""".formatted(TestData.PASSWORD)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/students/{cwid}/graduation-check", "885500004").header(HttpHeaders.AUTHORIZATION, registrar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.missingCourses[0]").value("CPSC 131"))
                .andExpect(jsonPath("$.missingCourses[1]").value("CPSC 362"));
    }
}
