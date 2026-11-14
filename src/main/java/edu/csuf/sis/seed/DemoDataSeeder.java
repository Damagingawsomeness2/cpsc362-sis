package edu.csuf.sis.seed;

import edu.csuf.sis.domain.Course;
import edu.csuf.sis.domain.CourseRepository;
import edu.csuf.sis.domain.Enrollment;
import edu.csuf.sis.domain.EnrollmentRepository;
import edu.csuf.sis.domain.LetterGrade;
import edu.csuf.sis.domain.Major;
import edu.csuf.sis.domain.MajorRepository;
import edu.csuf.sis.domain.MeetingTime;
import edu.csuf.sis.domain.Professor;
import edu.csuf.sis.domain.ProfessorRepository;
import edu.csuf.sis.domain.Role;
import edu.csuf.sis.domain.Section;
import edu.csuf.sis.domain.SectionRepository;
import edu.csuf.sis.domain.Student;
import edu.csuf.sis.domain.StudentRepository;
import edu.csuf.sis.domain.Term;
import edu.csuf.sis.domain.TermRepository;
import edu.csuf.sis.domain.UserAccount;
import edu.csuf.sis.domain.UserAccountRepository;
import edu.csuf.sis.records.IRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demo data: the team's object diagram (Maria Lopez passed CPSC 131 with an A- in section 02 of
 * Spring 2026 and holds one of 38 taken seats in Dr. Alicia Kim's CPSC 362 section 01, Fall 2026)
 * inside a small Computer Science catalog, plus a senior one class from graduating and a first-year
 * student. Fall 2026 is in progress and Spring 2027 is open for registration.
 *
 * <p>Every demo login uses {@link #DEMO_PASSWORD}. Turn this off ({@code sis.seed.enabled=false})
 * wherever real data lives.
 */
@Component
@ConditionalOnProperty(name = "sis.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Titans-2026";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] FIRST_NAMES = {"Aiden", "Bella", "Carlos", "Diana", "Elijah", "Fatima", "Gabriel",
            "Hannah", "Isaac", "Jasmine", "Kevin", "Leah", "Mateo", "Nadia", "Owen", "Priya", "Rosa", "Samuel",
            "Tara", "Victor"};
    private static final String[] LAST_NAMES = {"Nguyen", "Garcia", "Tran", "Patel", "Martinez", "Chen",
            "Hernandez", "Le", "Rodriguez", "Singh", "Park", "Flores", "Wong", "Rivera", "Shah", "Gonzalez", "Pham",
            "Ramirez", "Torres", "Lee"};
    private static final String[] GRADE_ROTATION = {"A", "A-", "B+", "B", "B-", "C+", "C", "A-", "B+", "B"};

    private final UserAccountRepository accounts;
    private final StudentRepository students;
    private final ProfessorRepository professors;
    private final CourseRepository courses;
    private final TermRepository terms;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final MajorRepository majors;
    private final PasswordEncoder passwordEncoder;
    private final IRecords records;
    private final Clock clock;
    private final TransactionTemplate transaction;

    private final Map<String, Term> termsByName = new HashMap<>();
    private final Map<String, Course> coursesByCode = new HashMap<>();
    private final Map<String, Section> pastSections = new HashMap<>();
    private String passwordHash;

    public DemoDataSeeder(UserAccountRepository accounts, StudentRepository students, ProfessorRepository professors,
                          CourseRepository courses, TermRepository terms, SectionRepository sections,
                          EnrollmentRepository enrollments, MajorRepository majors, PasswordEncoder passwordEncoder,
                          IRecords records, Clock clock, TransactionTemplate transaction) {
        this.accounts = accounts;
        this.students = students;
        this.professors = professors;
        this.courses = courses;
        this.terms = terms;
        this.sections = sections;
        this.enrollments = enrollments;
        this.majors = majors;
        this.passwordEncoder = passwordEncoder;
        this.records = records;
        this.clock = clock;
        this.transaction = transaction;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.count() > 0) {
            return;
        }
        transaction.executeWithoutResult(status -> seed());
        log.warn("Loaded demo data. Every demo account shares one password (see README.md); "
                + "set sis.seed.enabled=false for real data.");
    }

    private void seed() {
        // One BCrypt hash for every demo login keeps startup fast; real accounts each get their own.
        passwordHash = passwordEncoder.encode(DEMO_PASSWORD);
        addTerms();
        addCatalog();
        Major computerScience = majors.save(new Major("BSCS", "Computer Science", "B.S.", 120, courses(
                "CPSC 120", "CPSC 121", "CPSC 131", "CPSC 240", "CPSC 253", "CPSC 315", "CPSC 323", "CPSC 332",
                "CPSC 335", "CPSC 351", "CPSC 362", "CPSC 471", "CPSC 481", "CPSC 490", "CPSC 491",
                "MATH 150A", "MATH 150B", "MATH 170A", "MATH 170B", "MATH 338")));

        account("registrar", Role.ADMIN, "Registrar's Office");
        Professor kim = professor("Dr. Alicia Kim", "Computer Science", "akim");
        Professor carter = professor("Dr. Ben Carter", "Computer Science", "bcarter");
        Professor raman = professor("Dr. Priya Raman", "Mathematics", "praman");

        // Sections from the object diagram.
        Section cpsc131Spring2026 = section("Spring 2026", "CPSC 131", "02", carter, 40, "MW", "13:00", "14:15", "CS-102");
        Section cpsc362Fall2026 = section("Fall 2026", "CPSC 362", "01", kim, 40, "MW", "10:00", "11:15", "CS-104");

        // The rest of Fall 2026 (in progress).
        Section cpsc240 = section("Fall 2026", "CPSC 240", "01", carter, 35, "TuTh", "08:30", "09:45", "CS-102");
        Section cpsc332 = section("Fall 2026", "CPSC 332", "01", carter, 35, "TuTh", "11:30", "12:45", "CS-110");
        Section cpsc351 = section("Fall 2026", "CPSC 351", "01", kim, 35, "MW", "13:00", "14:15", "CS-104");
        Section cpsc491 = section("Fall 2026", "CPSC 491", "01", kim, 25, "F", "09:00", "11:45", "CS-300");
        Section cpsc464 = section("Fall 2026", "CPSC 464", "01", carter, 30, "MW", "16:00", "17:15", "CS-200");
        Section cpsc131Fall2026 = section("Fall 2026", "CPSC 131", "01", carter, 40, "TuTh", "14:30", "15:45", "CS-102");
        Section math150b = section("Fall 2026", "MATH 150B", "01", raman, 40, "MWF", "08:00", "08:50", "MH-380");
        Section hist180 = section("Fall 2026", "HIST 180", "01", null, 45, "TuTh", "10:00", "11:15", "H-121");

        // Spring 2027 (registration open).
        section("Spring 2027", "CPSC 323", "01", carter, 35, "MW", "08:30", "09:45", "CS-102");
        Section algorithmsFull = section("Spring 2027", "CPSC 335", "01", kim, 30, "TuTh", "10:00", "11:15", "CS-104");
        section("Spring 2027", "CPSC 335", "02", kim, 30, "TuTh", "13:00", "14:15", "CS-104");
        section("Spring 2027", "CPSC 349", "01", null, 40, null, null, null, "Online");
        section("Spring 2027", "CPSC 362", "01", kim, 40, "MW", "10:00", "11:15", "CS-104");
        section("Spring 2027", "CPSC 462", "01", carter, 30, "MW", "14:30", "15:45", "CS-200");
        section("Spring 2027", "CPSC 471", "01", carter, 35, "MW", "11:30", "12:45", "CS-110");
        section("Spring 2027", "CPSC 481", "01", kim, 30, "TuTh", "16:00", "17:15", "CS-300");
        section("Spring 2027", "CPSC 490", "01", kim, 30, "F", "13:00", "13:50", "CS-300");
        section("Spring 2027", "MATH 338", "01", raman, 40, "MW", "10:00", "11:50", "MH-380");
        section("Spring 2027", "ENGL 101", "01", null, 25, "TuTh", "13:00", "14:15", "H-210");

        // Maria Lopez, the object diagram's student: 64 units after Spring 2026, so a junior.
        Student maria = student("885104273", "Maria Lopez", "mlopez", computerScience);
        completed(maria, "Fall 2024", "CPSC 120:A", "MATH 150A:B+", "ENGL 101:A-", "COMM 100:B+", "PSYC 101:A");
        completed(maria, "Spring 2025", "CPSC 121:A-", "MATH 150B:B", "MATH 170A:A-", "PHIL 105:A", "HIST 180:B+");
        completed(maria, "Fall 2025", "CPSC 253:A", "MATH 170B:B+", "PHYS 225:B", "PHYS 225L:A", "POSC 100:A-",
                "ASTR 101:A");
        completed(maria, cpsc131Spring2026, LetterGrade.A_MINUS);
        completed(maria, "Spring 2026", "MATH 338:B+", "CPSC 315:A", "ECON 201:B", "ANTH 102:A-");
        enroll(maria, cpsc362Fall2026, cpsc240, cpsc332, cpsc351);

        // Ethan Brooks: 119 units and every required course except the CPSC 491 capstone he is taking now.
        Student ethan = student("885019382", "Ethan Brooks", "ebrooks", computerScience);
        completed(ethan, "Fall 2022", "CPSC 120:A-", "MATH 150A:B", "ENGL 101:B+", "COMM 100:A", "PSYC 101:A-");
        completed(ethan, "Spring 2023", "CPSC 121:B+", "MATH 150B:B", "MATH 170A:A", "PHIL 105:A-", "HIST 180:B");
        completed(ethan, "Fall 2023", "CPSC 131:A", "MATH 170B:B+", "PHYS 225:B", "PHYS 225L:A", "POSC 100:A-",
                "ASTR 101:A");
        completed(ethan, "Spring 2024", "CPSC 240:B+", "CPSC 253:A", "MATH 338:B", "ECON 201:B+", "ANTH 102:A-");
        completed(ethan, "Fall 2024", "CPSC 323:B", "CPSC 332:A-", "CPSC 335:B+", "CPSC 315:A", "CPSC 349:A");
        completed(ethan, "Spring 2025", "CPSC 351:B+", "CPSC 362:A", "CPSC 375:A-", "CPSC 411:B+", "CPSC 431:A");
        completed(ethan, "Fall 2025", "CPSC 471:B", "CPSC 481:A-", "CPSC 490:A", "CPSC 439:B+", "CPSC 440:B");
        completed(ethan, "Spring 2026", "CPSC 462:A", "CPSC 463:A-", "CPSC 474:B+", "CPSC 483:A");
        enroll(ethan, cpsc491, cpsc464);

        // Sam Ortiz: a first-year student taking CPSC 131 now, so not yet eligible for CPSC 362.
        Student sam = student("885331206", "Sam Ortiz", "sortiz", computerScience);
        completed(sam, "Fall 2025", "CPSC 120:B+", "ENGL 101:A", "MATH 150A:B", "COMM 100:A-");
        completed(sam, "Spring 2026", "CPSC 121:B", "MATH 170A:B+", "PHIL 105:A-", "PSYC 101:B+");
        enroll(sam, cpsc131Fall2026, math150b, hist180);

        // Maria's 39 classmates fill CPSC 131 section 02 (40 of 40); 37 of them sit beside her in
        // CPSC 362 section 01 (38 of 40), and 30 already hold every seat in Spring 2027 CPSC 335 section 01.
        for (int i = 0; i < 39; i++) {
            String first = FIRST_NAMES[i % 20];
            String last = LAST_NAMES[(i / 20 * 7 + i) % 20];
            Student classmate = student(String.format("8854%05d", i + 1), first + " " + last,
                    (first.charAt(0) + last).toLowerCase(Locale.ROOT) + (i + 1), computerScience);
            if (i % 2 == 0) {
                completed(classmate, "Fall 2024", "MATH 150A:" + grade(i, 0), "HIST 180:" + grade(i, 1),
                        "POSC 100:" + grade(i, 2));
            }
            completed(classmate, "Spring 2025", "CPSC 120:" + grade(i, 3), "ENGL 101:" + grade(i, 4),
                    "COMM 100:" + grade(i, 5));
            completed(classmate, "Fall 2025", "CPSC 121:" + grade(i, 6), "MATH 170A:" + grade(i, 7),
                    "PHIL 105:" + grade(i, 8));
            completed(classmate, cpsc131Spring2026, LetterGrade.fromLabel(grade(i, 9)));
            completed(classmate, "Spring 2026", "MATH 170B:" + grade(i, 10));
            if (i < 37) {
                enroll(classmate, cpsc362Fall2026);
            }
            if (i >= 9) {
                enroll(classmate, algorithmsFull);
            }
        }

        students.findAll().forEach(student -> records.updateGradeLevel(student.getId()));
    }

    private void addTerms() {
        term("Fall 2022", "2022-08-22", "2022-12-16");
        term("Spring 2023", "2023-01-23", "2023-05-19");
        term("Fall 2023", "2023-08-21", "2023-12-15");
        term("Spring 2024", "2024-01-22", "2024-05-17");
        term("Fall 2024", "2024-08-26", "2024-12-20");
        term("Spring 2025", "2025-01-27", "2025-05-23");
        term("Fall 2025", "2025-08-25", "2025-12-19");
        term("Spring 2026", "2026-01-26", "2026-05-22");
        term("Fall 2026", "2026-08-24", "2026-12-18");
        term("Spring 2027", "2027-01-25", "2027-05-21");
    }

    /** Prerequisites must already be listed, so general education and math come first. */
    private void addCatalog() {
        course("ENGL 101", "Beginning College Writing", 3, "Planning, drafting and revising clear academic essays.");
        course("COMM 100", "Introduction to Human Communication", 3, "How people communicate one-on-one, in groups and in public.");
        course("PHIL 105", "Critical Thinking", 3, "Recognizing, evaluating and building sound arguments.");
        course("HIST 180", "Survey of American History", 3, "United States history from the colonial era to today.");
        course("POSC 100", "American Government", 3, "How national, state and local government work.");
        course("PSYC 101", "Introductory Psychology", 3, "Behavior, perception, learning and development.");
        course("ASTR 101", "Introduction to Astronomy", 3, "Planets, stars, galaxies and how we study them.");
        course("ECON 201", "Principles of Microeconomics", 3, "How markets set prices and how households and firms decide.");
        course("ANTH 102", "Introduction to Cultural Anthropology", 3, "How cultures organize family, work, belief and meaning.");

        course("MATH 150A", "Calculus I", 4, "Limits, derivatives and their uses.");
        course("MATH 150B", "Calculus II", 4, "Integrals, sequences and series.", "MATH 150A");
        course("MATH 170A", "Mathematical Structures I", 3, "Logic, sets, proofs and counting.");
        course("MATH 170B", "Mathematical Structures II", 3, "Relations, graphs, recursion and discrete probability.", "MATH 170A");
        course("MATH 338", "Statistics Applied to Natural Sciences", 4, "Probability, estimation and hypothesis testing.", "MATH 150B");
        course("PHYS 225", "Fundamental Physics: Mechanics", 3, "Motion, forces, energy and momentum.", "MATH 150A");
        course("PHYS 225L", "Fundamental Physics Laboratory: Mechanics", 1, "Hands-on experiments that go with PHYS 225.");

        course("CPSC 120", "Introduction to Programming", 3, "Solving problems with code: variables, decisions, loops and functions.");
        course("CPSC 121", "Object-Oriented Programming", 3, "Classes, objects, inheritance and memory.", "CPSC 120");
        course("CPSC 131", "Data Structures", 3, "Lists, stacks, queues, trees, hash tables and graphs, and how fast they are.", "CPSC 121");
        course("CPSC 240", "Computer Organization and Assembly Language", 3, "How programs run on hardware, down to assembly.", "CPSC 131");
        course("CPSC 253", "Cybersecurity Foundations and Principles", 3, "Threats, defenses and the basics of cryptography.", "CPSC 120");
        course("CPSC 315", "Professional Ethics in Computing", 3, "Ethical, legal and social questions in computing work.", "ENGL 101");
        course("CPSC 323", "Compilers and Languages", 3, "Scanning, parsing and translating programming languages.", "CPSC 131");
        course("CPSC 332", "File Structures and Database Systems", 3, "Relational design, SQL, indexing and transactions.", "CPSC 131");
        course("CPSC 335", "Algorithm Engineering", 3, "Designing and analyzing efficient algorithms.", "CPSC 131", "MATH 170B");
        course("CPSC 349", "Web Front-End Engineering", 3, "Building browser applications with HTML, CSS and JavaScript.", "CPSC 131");
        course("CPSC 351", "Operating Systems Concepts", 3, "Processes, threads, scheduling, memory and file systems.", "CPSC 131");
        course("CPSC 362", "Foundations of Software Engineering", 3, "Process models, requirements, modeling, testing and teamwork.", "CPSC 131");
        course("CPSC 375", "Introduction to Data Science and Big Data", 3, "Collecting, cleaning, analyzing and visualizing data.", "CPSC 131", "MATH 338");
        course("CPSC 411", "Mobile Device Application Programming", 3, "Designing and building apps for phones and tablets.", "CPSC 131");
        course("CPSC 431", "Database and Applications", 3, "Applications built on databases, and tuning their queries.", "CPSC 332");
        course("CPSC 439", "Theory of Computation", 3, "Automata, formal languages and the limits of computing.", "CPSC 335");
        course("CPSC 440", "Computer System Architecture", 3, "Pipelines, caches and the memory hierarchy.", "CPSC 240");
        course("CPSC 462", "Software Design", 3, "Design principles and patterns for maintainable code.", "CPSC 362");
        course("CPSC 463", "Software Testing", 3, "Test design, automation and test-driven development.", "CPSC 362");
        course("CPSC 464", "Software Architecture", 3, "Architectural styles, quality attributes and trade-offs.", "CPSC 362");
        course("CPSC 471", "Computer Communications", 3, "Network layers, protocols and network programming.", "CPSC 351");
        course("CPSC 474", "Parallel and Distributed Computing", 3, "Concurrency, synchronization and distributed algorithms.", "CPSC 351");
        course("CPSC 481", "Artificial Intelligence", 3, "Search, reasoning, planning and learning.", "CPSC 335", "MATH 338");
        course("CPSC 483", "Introduction to Machine Learning", 3, "Training and evaluating predictive models.", "CPSC 335", "MATH 338");
        course("CPSC 484", "Principles of Computer Graphics", 3, "Transformations, lighting and rendering.", "CPSC 131", "MATH 150B");
        course("CPSC 490", "Undergraduate Seminar in Computer Science", 1, "Preparing a capstone proposal and presenting it.", "CPSC 362");
        course("CPSC 491", "Senior Capstone Project in Computer Science", 3, "A semester-long team project for a real client.", "CPSC 490");
    }

    private void term(String name, String start, String end) {
        termsByName.put(name, terms.save(new Term(name, LocalDate.parse(start), LocalDate.parse(end))));
    }

    private void course(String code, String title, int units, String description, String... prerequisites) {
        Course course = new Course(code, title, description, units);
        for (String prerequisite : prerequisites) {
            course.addPrerequisite(coursesByCode.get(prerequisite));
        }
        coursesByCode.put(code, courses.save(course));
    }

    private List<Course> courses(String... codes) {
        return Arrays.stream(codes).map(coursesByCode::get).toList();
    }

    private UserAccount account(String username, Role role, String displayName) {
        return accounts.save(new UserAccount(username, passwordHash, role, displayName));
    }

    private Professor professor(String name, String department, String username) {
        return professors.save(new Professor(name, department, username + "@fullerton.edu",
                account(username, Role.PROFESSOR, name)));
    }

    private Student student(String cwid, String name, String username, Major major) {
        return students.save(new Student(cwid, name, username + "@csu.fullerton.edu", major,
                account(username, Role.STUDENT, name)));
    }

    private Section section(String termName, String courseCode, String number, Professor professor, int capacity,
                            String days, String start, String end, String room) {
        MeetingTime meets = days == null ? MeetingTime.TBA
                : MeetingTime.of(days, LocalTime.parse(start), LocalTime.parse(end));
        return sections.save(new Section(coursesByCode.get(courseCode), termsByName.get(termName), number,
                professor, capacity, meets, room));
    }

    /** Finished classes, written "CPSC 120:A". Past sections need no room or time, so one per course holds everyone. */
    private void completed(Student student, String termName, String... classesAndGrades) {
        for (String classAndGrade : classesAndGrades) {
            String[] parts = classAndGrade.split(":");
            Section section = pastSections.computeIfAbsent(termName + " " + parts[0], key ->
                    sections.save(new Section(coursesByCode.get(parts[0]), termsByName.get(termName), "01", null,
                            60, MeetingTime.TBA, null)));
            completed(student, section, LetterGrade.fromLabel(parts[1]));
        }
    }

    private void completed(Student student, Section section, LetterGrade grade) {
        section.reserveSeat();
        Enrollment enrollment = new Enrollment(student, section, clock.instant());
        enrollment.assignGrade(grade);
        enrollments.save(enrollment);
    }

    private void enroll(Student student, Section... toTake) {
        for (Section section : toTake) {
            section.reserveSeat();
            enrollments.save(new Enrollment(student, section, clock.instant()));
        }
    }

    private static String grade(int student, int course) {
        return GRADE_ROTATION[(student * 3 + course) % GRADE_ROTATION.length];
    }
}
