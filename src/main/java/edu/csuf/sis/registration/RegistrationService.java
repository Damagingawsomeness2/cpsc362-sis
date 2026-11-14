package edu.csuf.sis.registration;

import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.SectionView;
import edu.csuf.sis.catalog.TermView;
import edu.csuf.sis.common.PermissionDeniedException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.records.EnrollmentView;
import edu.csuf.sis.records.IRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Depends only on ICatalog and IRecords, matching the component diagram. */
@Service
@Transactional
public class RegistrationService implements IRegistration {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private final ICatalog catalog;
    private final IRecords records;

    public RegistrationService(ICatalog catalog, IRecords records) {
        this.catalog = catalog;
        this.records = records;
    }

    @Override
    public EnrollmentView enroll(long studentId, long sectionId) {
        SectionView section = catalog.section(sectionId);
        TermView term = catalog.term(section.termId());
        if (!term.addDropOpen()) {
            throw new RuleViolationException("The add/drop period for " + term.name() + " ended on "
                    + DATE.format(term.addDropDeadline()) + ".");
        }

        List<EnrollmentView> sameTerm = records.schedule(studentId).stream()
                .filter(enrolled -> enrolled.termId() == term.id())
                .toList();
        for (EnrollmentView enrolled : sameTerm) {
            if (enrolled.courseCode().equals(section.courseCode())) {
                throw new RuleViolationException("You are already registered for " + enrolled.courseCode()
                        + " section " + enrolled.sectionNumber() + " in " + term.name() + ".");
            }
        }

        Set<String> passed = records.passedCourseCodes(studentId);
        if (passed.contains(section.courseCode())) {
            throw new RuleViolationException("You already passed " + section.courseCode() + ".");
        }
        List<String> missing = catalog.missingPrerequisites(section.courseCode(), passed);
        if (!missing.isEmpty()) {
            throw new RuleViolationException("You need to pass " + String.join(" and ", missing)
                    + " before taking " + section.courseCode() + ".");
        }

        for (EnrollmentView enrolled : sameTerm) {
            if (enrolled.meetingTime().overlaps(section.meetingTime())) {
                throw new RuleViolationException(section.courseCode() + " (" + section.meets() + ") conflicts with "
                        + enrolled.courseCode() + " (" + enrolled.meets() + ") on your schedule.");
            }
        }

        catalog.reserveSeat(sectionId);
        return records.recordEnrollment(studentId, sectionId);
    }

    @Override
    public void drop(long studentId, long sectionId) {
        SectionView section = catalog.section(sectionId);
        TermView term = catalog.term(section.termId());
        if (!term.addDropOpen()) {
            throw new RuleViolationException("Classes can only be dropped before the second week. The last day to drop "
                    + term.name() + " classes was " + DATE.format(term.addDropDeadline()) + ".");
        }
        records.recordDrop(studentId, sectionId);
        catalog.releaseSeat(sectionId);
    }

    @Override
    public void dropByProfessor(long professorId, long sectionId, long studentId) {
        SectionView section = catalog.section(sectionId);
        if (section.professorId() == null || section.professorId() != professorId) {
            throw new PermissionDeniedException("You can only drop students from sections you teach.");
        }
        records.recordDrop(studentId, sectionId);
        catalog.releaseSeat(sectionId);
    }
}
