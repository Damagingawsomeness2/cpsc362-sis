package edu.csuf.sis.catalog;

import edu.csuf.sis.common.NotFoundException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.domain.Course;
import edu.csuf.sis.domain.CourseRepository;
import edu.csuf.sis.domain.MeetingTime;
import edu.csuf.sis.domain.Professor;
import edu.csuf.sis.domain.ProfessorRepository;
import edu.csuf.sis.domain.Section;
import edu.csuf.sis.domain.SectionRepository;
import edu.csuf.sis.domain.Term;
import edu.csuf.sis.domain.TermRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class CatalogService implements ICatalog {

    private final CourseRepository courses;
    private final TermRepository terms;
    private final SectionRepository sections;
    private final ProfessorRepository professors;
    private final Clock clock;

    public CatalogService(CourseRepository courses, TermRepository terms, SectionRepository sections,
                          ProfessorRepository professors, Clock clock) {
        this.courses = courses;
        this.terms = terms;
        this.sections = sections;
        this.professors = professors;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> courses() {
        return courses.findAllByOrderByCodeAsc().stream().map(CatalogService::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView course(String code) {
        return view(findCourse(code));
    }

    @Override
    public CourseView addCourse(NewCourse request) {
        String code = Course.normalizeCode(request.code());
        if (courses.existsByCode(code)) {
            throw new RuleViolationException(code + " is already in the catalog.");
        }
        Course course = new Course(code, request.title().trim(), blankToNull(request.description()), request.units());
        if (request.prerequisites() != null) {
            for (String prerequisite : request.prerequisites()) {
                String prerequisiteCode = Course.normalizeCode(prerequisite);
                course.addPrerequisite(courses.findByCode(prerequisiteCode).orElseThrow(() ->
                        new RuleViolationException("Add " + prerequisiteCode + " to the catalog before listing it as a prerequisite.")));
            }
        }
        return view(courses.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> missingPrerequisites(String courseCode, Set<String> passedCourseCodes) {
        return findCourse(courseCode).missingPrerequisites(passedCourseCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermView> terms() {
        LocalDate today = LocalDate.now(clock);
        return terms.findAllByOrderByStartDateAsc().stream().map(term -> view(term, today)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TermView term(long termId) {
        return view(findTerm(termId), LocalDate.now(clock));
    }

    @Override
    public TermView addTerm(NewTerm request) {
        String name = request.name().trim();
        if (terms.existsByNameIgnoreCase(name)) {
            throw new RuleViolationException("The term " + name + " already exists.");
        }
        return view(terms.save(new Term(name, request.startDate(), request.endDate())), LocalDate.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionView> sections(long termId, String search) {
        findTerm(termId);
        String wanted = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return sections.findByTermIdOrderByCourseCodeAscSectionNumberAsc(termId).stream()
                .filter(section -> wanted.isEmpty() || matches(section.getCourse(), wanted))
                .map(CatalogService::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionView> sectionsTaughtBy(long professorId) {
        return sections.findByProfessorIdOrderByTermStartDateDescCourseCodeAsc(professorId).stream()
                .map(CatalogService::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SectionView section(long sectionId) {
        return view(sections.findWithDetailsById(sectionId)
                .orElseThrow(() -> new NotFoundException("There is no section with id " + sectionId + ".")));
    }

    @Override
    public SectionView addSection(NewSection request) {
        Term term = findTerm(request.termId());
        Course course = findCourse(request.courseCode());
        String number = request.sectionNumber().trim();
        if (sections.existsByCourseIdAndTermIdAndSectionNumber(course.getId(), term.getId(), number)) {
            throw new RuleViolationException(
                    course.getCode() + " section " + number + " already exists in " + term.getName() + ".");
        }
        Professor professor = request.professorId() == null ? null : professors.findById(request.professorId())
                .orElseThrow(() -> new NotFoundException("There is no professor with id " + request.professorId() + "."));
        MeetingTime meets = MeetingTime.of(request.days(), request.startTime(), request.endTime());
        return view(sections.save(new Section(course, term, number, professor, request.capacity(), meets,
                blankToNull(request.room()))));
    }

    @Override
    public void reserveSeat(long sectionId) {
        lockedSection(sectionId).reserveSeat();
    }

    @Override
    public void releaseSeat(long sectionId) {
        lockedSection(sectionId).releaseSeat();
    }

    private Section lockedSection(long sectionId) {
        return sections.findForUpdate(sectionId)
                .orElseThrow(() -> new NotFoundException("There is no section with id " + sectionId + "."));
    }

    private Course findCourse(String code) {
        String normalized = Course.normalizeCode(code);
        return courses.findByCode(normalized)
                .orElseThrow(() -> new NotFoundException(normalized + " is not in the catalog."));
    }

    private Term findTerm(long termId) {
        return terms.findById(termId)
                .orElseThrow(() -> new NotFoundException("There is no term with id " + termId + "."));
    }

    /** "cpsc362", "CPSC 362" and "software" all find CPSC 362 Foundations of Software Engineering. */
    private static boolean matches(Course course, String wanted) {
        String code = course.getCode().toLowerCase(Locale.ROOT).replace(" ", "");
        return code.contains(wanted.replace(" ", ""))
                || course.getTitle().toLowerCase(Locale.ROOT).contains(wanted);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static CourseView view(Course course) {
        return new CourseView(course.getCode(), course.getTitle(), course.getDescription(), course.getUnits(),
                course.prerequisiteCodes());
    }

    private static TermView view(Term term, LocalDate today) {
        return new TermView(term.getId(), term.getName(), term.getStartDate(), term.getEndDate(),
                term.addDropDeadline(), term.isAddDropOpen(today));
    }

    private static SectionView view(Section section) {
        Course course = section.getCourse();
        Term term = section.getTerm();
        Professor professor = section.getProfessor();
        MeetingTime meets = section.meetingTime();
        return new SectionView(section.getId(), term.getId(), term.getName(), course.getCode(), course.getTitle(),
                course.getUnits(), section.getSectionNumber(),
                professor == null ? null : professor.getId(), professor == null ? "Staff" : professor.getName(),
                meets.isTba() ? null : meets.dayCodes(), meets.start(), meets.end(), meets.describe(),
                section.getRoom(), section.getCapacity(), section.getEnrolled(), section.openSeats());
    }
}
