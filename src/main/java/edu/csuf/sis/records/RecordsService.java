package edu.csuf.sis.records;

import edu.csuf.sis.common.NotFoundException;
import edu.csuf.sis.common.PermissionDeniedException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.domain.Course;
import edu.csuf.sis.domain.Enrollment;
import edu.csuf.sis.domain.EnrollmentRepository;
import edu.csuf.sis.domain.EnrollmentStatus;
import edu.csuf.sis.domain.GradePointAverage;
import edu.csuf.sis.domain.GradedUnits;
import edu.csuf.sis.domain.LetterGrade;
import edu.csuf.sis.domain.Major;
import edu.csuf.sis.domain.MeetingTime;
import edu.csuf.sis.domain.Professor;
import edu.csuf.sis.domain.Section;
import edu.csuf.sis.domain.SectionRepository;
import edu.csuf.sis.domain.Student;
import edu.csuf.sis.domain.StudentRepository;
import edu.csuf.sis.domain.Term;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecordsService implements IRecords {

    private static final List<EnrollmentStatus> ON_ROLL = List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);
    private static final Comparator<Term> TERM_ORDER = Comparator.comparing(Term::getStartDate).thenComparing(Term::getName);
    private static final Comparator<Enrollment> BY_TERM_THEN_COURSE = Comparator
            .comparing((Enrollment enrollment) -> enrollment.getSection().getTerm(), TERM_ORDER)
            .thenComparing(enrollment -> enrollment.getSection().getCourse().getCode());

    private final StudentRepository students;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final Clock clock;

    public RecordsService(StudentRepository students, SectionRepository sections,
                          EnrollmentRepository enrollments, Clock clock) {
        this.students = students;
        this.sections = sections;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfile student(long studentId) {
        return profile(findStudent(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfile studentByCwid(String cwid) {
        String wanted = cwid == null ? "" : cwid.trim();
        return profile(students.findByCwid(wanted)
                .orElseThrow(() -> new NotFoundException("There is no student with CWID " + wanted + ".")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentView> schedule(long studentId) {
        return enrollments.findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED).stream()
                .sorted(BY_TERM_THEN_COURSE)
                .map(RecordsService::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Transcript history(long studentId) {
        Student student = findStudent(studentId);
        List<Enrollment> completed = enrollments.findByStudentIdAndStatus(studentId, EnrollmentStatus.COMPLETED);

        Map<Term, List<Enrollment>> byTerm = completed.stream().collect(Collectors.groupingBy(
                enrollment -> enrollment.getSection().getTerm(), () -> new TreeMap<>(TERM_ORDER), Collectors.toList()));
        List<TranscriptTerm> terms = new ArrayList<>();
        byTerm.forEach((term, classes) -> terms.add(new TranscriptTerm(term.getName(),
                classes.stream().sorted(BY_TERM_THEN_COURSE).map(RecordsService::line).toList(),
                GradePointAverage.of(graded(classes)))));

        int unitsAttempted = completed.stream().mapToInt(RecordsService::units).sum();
        int unitsEarned = completed.stream().filter(Enrollment::isPassed).mapToInt(RecordsService::units).sum();
        return new Transcript(profile(student), terms, unitsAttempted, unitsEarned,
                GradePointAverage.of(graded(completed)));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> passedCourseCodes(long studentId) {
        return enrollments.findByStudentIdAndStatus(studentId, EnrollmentStatus.COMPLETED).stream()
                .filter(Enrollment::isPassed)
                .map(enrollment -> enrollment.getSection().getCourse().getCode())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RosterEntry> classRoll(long professorId, long sectionId) {
        return roll(sectionTaughtBy(professorId, sectionId).getId());
    }

    @Override
    public List<RosterEntry> enterGrades(long professorId, long sectionId, List<GradeEntry> grades) {
        sectionTaughtBy(professorId, sectionId);
        Set<Long> graded = new LinkedHashSet<>();
        for (GradeEntry entry : grades) {
            LetterGrade grade = parseGrade(entry.grade());
            Enrollment enrollment = enrollments.findByStudentIdAndSectionId(entry.studentId(), sectionId)
                    .orElseThrow(() -> new RuleViolationException("That student is not on this class roll."));
            enrollment.assignGrade(grade);
            graded.add(entry.studentId());
        }
        graded.forEach(this::updateGradeLevel);
        return roll(sectionId);
    }

    @Override
    public StudentProfile updateGradeLevel(long studentId) {
        Student student = findStudent(studentId);
        int unitsPassed = enrollments.findByStudentIdAndStatus(studentId, EnrollmentStatus.COMPLETED).stream()
                .filter(Enrollment::isPassed)
                .mapToInt(RecordsService::units)
                .sum();
        student.updateStanding(unitsPassed);
        return profile(student);
    }

    @Override
    public EnrollmentView recordEnrollment(long studentId, long sectionId) {
        Enrollment enrollment = enrollments.findByStudentIdAndSectionId(studentId, sectionId)
                .map(RecordsService::rejoin)
                .orElseGet(() -> enrollments.save(new Enrollment(students.getReferenceById(studentId),
                        sections.getReferenceById(sectionId), clock.instant())));
        return view(enrollment);
    }

    @Override
    public void recordDrop(long studentId, long sectionId) {
        enrollments.findByStudentIdAndSectionId(studentId, sectionId)
                .orElseThrow(() -> new RuleViolationException("That student is not registered in this section."))
                .drop();
    }

    private static Enrollment rejoin(Enrollment earlier) {
        if (earlier.getStatus() != EnrollmentStatus.DROPPED) {
            throw new RuleViolationException("There is already a record for this student in this section.");
        }
        earlier.reenroll();
        return earlier;
    }

    private List<RosterEntry> roll(long sectionId) {
        return enrollments.findBySectionIdAndStatusInOrderByStudentNameAsc(sectionId, ON_ROLL).stream()
                .map(enrollment -> {
                    Student student = enrollment.getStudent();
                    return new RosterEntry(student.getId(), student.getCwid(), student.getName(),
                            student.getGradeLevel(), enrollment.getStatus(), label(enrollment.getGrade()));
                })
                .toList();
    }

    private Section sectionTaughtBy(long professorId, long sectionId) {
        Section section = sections.findWithDetailsById(sectionId)
                .orElseThrow(() -> new NotFoundException("There is no section with id " + sectionId + "."));
        if (!section.isTaughtBy(professorId)) {
            throw new PermissionDeniedException("You can only manage sections you teach.");
        }
        return section;
    }

    private Student findStudent(long studentId) {
        return students.findWithMajorById(studentId)
                .orElseThrow(() -> new NotFoundException("There is no student with id " + studentId + "."));
    }

    private static LetterGrade parseGrade(String label) {
        try {
            return LetterGrade.fromLabel(label);
        } catch (IllegalArgumentException notAGrade) {
            throw new RuleViolationException(notAGrade.getMessage());
        }
    }

    private static List<GradedUnits> graded(List<Enrollment> completed) {
        return completed.stream().map(enrollment -> new GradedUnits(units(enrollment), enrollment.getGrade())).toList();
    }

    private static int units(Enrollment enrollment) {
        return enrollment.getSection().getCourse().getUnits();
    }

    private static String label(LetterGrade grade) {
        return grade == null ? null : grade.label();
    }

    private static TranscriptLine line(Enrollment enrollment) {
        Course course = enrollment.getSection().getCourse();
        return new TranscriptLine(course.getCode(), course.getTitle(), course.getUnits(), label(enrollment.getGrade()));
    }

    private static StudentProfile profile(Student student) {
        Major major = student.getMajor();
        return new StudentProfile(student.getId(), student.getCwid(), student.getName(), student.getEmail(),
                major == null ? null : major.getCode(), major == null ? null : major.getName(),
                student.getUnitsCompleted(), student.getGradeLevel(), student.getAccount().isEnabled());
    }

    private static EnrollmentView view(Enrollment enrollment) {
        Section section = enrollment.getSection();
        Course course = section.getCourse();
        Term term = section.getTerm();
        Professor professor = section.getProfessor();
        MeetingTime meets = section.meetingTime();
        return new EnrollmentView(section.getId(), term.getId(), term.getName(), course.getCode(), course.getTitle(),
                course.getUnits(), section.getSectionNumber(), meets.isTba() ? null : meets.dayCodes(),
                meets.start(), meets.end(), meets.describe(), section.getRoom(),
                professor == null ? "Staff" : professor.getName(), enrollment.getStatus(),
                label(enrollment.getGrade()));
    }
}
