package edu.csuf.sis.gateway;

import com.jayway.jsonpath.JsonPath;
import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.NewCourse;
import edu.csuf.sis.catalog.NewSection;
import edu.csuf.sis.catalog.NewTerm;
import edu.csuf.sis.domain.LetterGrade;
import edu.csuf.sis.identity.IUserAdmin;
import edu.csuf.sis.identity.NewProfessor;
import edu.csuf.sis.identity.NewStudent;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.support.TestClockConfig;
import edu.csuf.sis.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests of the API gateway. Each test starts from a small campus:
 * Spring 2027 open for registration, CPSC 131 -> CPSC 362, professors Kim and Carter,
 * Maria Lopez (passed CPSC 131) and Sam Ortiz (no classes yet), and the registrar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestClockConfig.class, TestData.class})
abstract class ApiTest {

    static final String MARIA_CWID = "885104273";
    static final String SAM_CWID = "885200001";

    @Autowired
    MockMvc mvc;
    @Autowired
    TestData testData;
    @Autowired
    ICatalog catalog;
    @Autowired
    IUserAdmin userAdmin;
    @Autowired
    IRecords records;

    long registrarAccountId;
    long spring2027;
    long cpsc362Section;
    long kimId;
    long mariaId;
    long mariaAccountId;
    long samId;

    @BeforeEach
    void buildCampus() {
        testData.reset();
        registrarAccountId = testData.registrar();
        spring2027 = catalog.addTerm(new NewTerm("Spring 2027", LocalDate.of(2027, 1, 25), LocalDate.of(2027, 5, 21))).id();
        catalog.addCourse(new NewCourse("CPSC 131", "Data Structures", "", 3, List.of()));
        catalog.addCourse(new NewCourse("CPSC 362", "Foundations of Software Engineering", "", 3, List.of("CPSC 131")));
        userAdmin.addProfessor(new NewProfessor("Alicia Kim", "Computer Science", "akim@fullerton.edu", "akim", TestData.PASSWORD));
        userAdmin.addProfessor(new NewProfessor("Ben Carter", "Computer Science", "bcarter@fullerton.edu", "bcarter", TestData.PASSWORD));
        kimId = userAdmin.professors().stream().filter(p -> p.name().equals("Alicia Kim")).findFirst().orElseThrow().id();
        cpsc362Section = catalog.addSection(new NewSection(spring2027, "CPSC 362", "01", kimId, 40, "MW",
                LocalTime.of(10, 0), LocalTime.of(11, 15), "CS-104")).id();

        mariaAccountId = userAdmin.addStudent(new NewStudent(MARIA_CWID, "Maria Lopez", "mlopez@csu.fullerton.edu",
                null, "mlopez", TestData.PASSWORD)).accountId();
        userAdmin.addStudent(new NewStudent(SAM_CWID, "Sam Ortiz", "sortiz@csu.fullerton.edu",
                null, "sortiz", TestData.PASSWORD));
        testData.completed(MARIA_CWID, "CPSC 131", 3, LetterGrade.A_MINUS, "Fall 2025");
        mariaId = records.updateGradeLevel(records.studentByCwid(MARIA_CWID).id()).id();
        samId = records.studentByCwid(SAM_CWID).id();
    }

    /** Signs in and returns the Authorization header value. */
    String signIn(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}""".formatted(username, TestData.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }
}
