package edu.csuf.sis.audit;

import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.domain.Course;
import edu.csuf.sis.domain.CourseRepository;
import edu.csuf.sis.domain.DegreeProgress;
import edu.csuf.sis.domain.Major;
import edu.csuf.sis.domain.MajorRepository;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.StudentProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Depends on IRecords for grades, matching the component diagram. */
@Service
@Transactional
public class DegreeAuditService implements IDegreeAudit {

    private final IRecords records;
    private final MajorRepository majors;
    private final CourseRepository courses;
    private final Clock clock;

    public DegreeAuditService(IRecords records, MajorRepository majors, CourseRepository courses, Clock clock) {
        this.records = records;
        this.majors = majors;
        this.courses = courses;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MajorView> majors() {
        return majors.findAllByOrderByCodeAsc().stream().map(DegreeAuditService::view).toList();
    }

    @Override
    public MajorView defineMajor(String code, MajorRequest request) {
        String majorCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!majorCode.matches("[A-Z0-9-]{2,12}")) {
            throw new IllegalArgumentException("A major code is 2-12 letters or digits, e.g. BSCS.");
        }
        List<Course> required = new ArrayList<>();
        for (String courseCode : request.requiredCourses()) {
            String normalized = Course.normalizeCode(courseCode);
            required.add(courses.findByCode(normalized)
                    .orElseThrow(() -> new RuleViolationException(normalized + " is not in the catalog.")));
        }
        String name = request.name().trim();
        String degree = request.degree().trim();
        Major major = majors.findByCode(majorCode)
                .map(existing -> {
                    existing.update(name, degree, request.totalUnits(), required);
                    return existing;
                })
                .orElseGet(() -> majors.save(new Major(majorCode, name, degree, request.totalUnits(), required)));
        return view(major);
    }

    @Override
    @Transactional(readOnly = true)
    public GraduationCheck checkGraduation(String cwid) {
        StudentProfile student = records.studentByCwid(cwid);
        if (student.majorCode() == null) {
            throw new RuleViolationException(student.name()
                    + " has not declared a major, so there are no degree requirements to check.");
        }
        Major major = majors.findByCode(student.majorCode()).orElseThrow();
        DegreeProgress progress = major.evaluate(records.passedCourseCodes(student.id()), student.unitsCompleted());
        return new GraduationCheck(student, major.getCode(), major.getName(), major.getDegree(), progress.eligible(),
                progress.completedCourses(), progress.missingCourses(), progress.unitsCompleted(),
                progress.unitsRequired(), progress.unitsRemaining(), LocalDate.now(clock));
    }

    private static MajorView view(Major major) {
        return new MajorView(major.getCode(), major.getName(), major.getDegree(), major.getTotalUnits(),
                major.requiredCourseCodes());
    }
}
