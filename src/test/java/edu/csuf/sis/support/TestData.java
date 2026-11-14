package edu.csuf.sis.support;

import edu.csuf.sis.domain.Course;
import edu.csuf.sis.domain.CourseRepository;
import edu.csuf.sis.domain.Enrollment;
import edu.csuf.sis.domain.EnrollmentRepository;
import edu.csuf.sis.domain.LetterGrade;
import edu.csuf.sis.domain.MeetingTime;
import edu.csuf.sis.domain.Role;
import edu.csuf.sis.domain.Section;
import edu.csuf.sis.domain.SectionRepository;
import edu.csuf.sis.domain.Student;
import edu.csuf.sis.domain.StudentRepository;
import edu.csuf.sis.domain.Term;
import edu.csuf.sis.domain.TermRepository;
import edu.csuf.sis.domain.UserAccount;
import edu.csuf.sis.domain.UserAccountRepository;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Test fixtures that need no production feature: wiping the database between tests,
 * the registrar's login, and finished classes from earlier terms.
 */
@TestComponent
public class TestData {

    public static final String PASSWORD = "Titans-2027";

    private final JdbcTemplate jdbc;
    private final UserAccountRepository accounts;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final TermRepository terms;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final PasswordEncoder passwordEncoder;
    private final MutableClock clock;

    public TestData(JdbcTemplate jdbc, UserAccountRepository accounts, StudentRepository students,
                    CourseRepository courses, TermRepository terms, SectionRepository sections,
                    EnrollmentRepository enrollments, PasswordEncoder passwordEncoder, MutableClock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.students = students;
        this.courses = courses;
        this.terms = terms;
        this.sections = sections;
        this.enrollments = enrollments;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Empties every table and sets the clock back to the default day. */
    public void reset() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = 'PUBLIC' and table_type = 'BASE TABLE'", String.class);
        jdbc.execute("set referential_integrity false");
        tables.forEach(table -> jdbc.execute("truncate table \"" + table + "\" restart identity"));
        jdbc.execute("set referential_integrity true");
        clock.setToday(TestClockConfig.DEFAULT_TODAY);
    }

    /** Creates the registrar's (admin) login and returns its account id. */
    public long registrar() {
        return accounts.save(new UserAccount("registrar", passwordEncoder.encode(PASSWORD), Role.ADMIN,
                "Registrar's Office")).getId();
    }

    /** Records a class the student finished in an earlier term, creating the course and term if needed. */
    @Transactional
    public void completed(String cwid, String courseCode, int units, LetterGrade grade, String termName) {
        Student student = students.findByCwid(cwid).orElseThrow();
        Course course = courses.findByCode(courseCode)
                .orElseGet(() -> courses.save(new Course(courseCode, "Course " + courseCode, "", units)));
        Term term = terms.findByName(termName).orElseGet(() -> terms.save(pastTerm(termName)));
        Section section = sections.findByCourseCodeAndTermNameAndSectionNumber(course.getCode(), termName, "01")
                .orElseGet(() -> sections.save(new Section(course, term, "01", null, 60, MeetingTime.TBA, null)));
        section.reserveSeat();
        Enrollment enrollment = new Enrollment(student, section, clock.instant());
        enrollment.assignGrade(grade);
        enrollments.save(enrollment);
    }

    /** Fills a student's history with passed general-education classes worth {@code units} units. */
    @Transactional
    public void generalEducationUnits(String cwid, int units) {
        for (int i = 1; i <= units / 3; i++) {
            completed(cwid, "GE " + (100 + i), 3, LetterGrade.B, "Fall 2024");
        }
        for (int i = 1; i <= units % 3; i++) {
            completed(cwid, "LAB " + (100 + i), 1, LetterGrade.B, "Fall 2024");
        }
    }

    private static Term pastTerm(String name) {
        String[] parts = name.split(" ");
        int year = Integer.parseInt(parts[1]);
        return switch (parts[0]) {
            case "Spring" -> new Term(name, LocalDate.of(year, 1, 20), LocalDate.of(year, 5, 15));
            case "Summer" -> new Term(name, LocalDate.of(year, 6, 1), LocalDate.of(year, 8, 1));
            default -> new Term(name, LocalDate.of(year, 8, 24), LocalDate.of(year, 12, 18));
        };
    }
}
