package edu.csuf.sis.gateway;

import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.SectionView;
import edu.csuf.sis.identity.AuthenticatedUser;
import edu.csuf.sis.records.EnrollmentView;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.RosterEntry;
import edu.csuf.sis.records.StudentProfile;
import edu.csuf.sis.records.Transcript;
import edu.csuf.sis.registration.IRegistration;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The professor portal's API: their sections, rolls and grades, plus student lookups. */
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    private final ICatalog catalog;
    private final IRecords records;
    private final IRegistration registration;

    public ProfessorController(ICatalog catalog, IRecords records, IRegistration registration) {
        this.catalog = catalog;
        this.records = records;
        this.registration = registration;
    }

    @GetMapping("/sections")
    public List<SectionView> sections(@AuthenticationPrincipal AuthenticatedUser professor) {
        return catalog.sectionsTaughtBy(professor.professorId());
    }

    @GetMapping("/sections/{sectionId}/roll")
    public List<RosterEntry> roll(@AuthenticationPrincipal AuthenticatedUser professor, @PathVariable long sectionId) {
        return records.classRoll(professor.professorId(), sectionId);
    }

    @PutMapping("/sections/{sectionId}/grades")
    public List<RosterEntry> enterGrades(@AuthenticationPrincipal AuthenticatedUser professor,
                                         @PathVariable long sectionId, @Valid @RequestBody GradeSheet sheet) {
        return records.enterGrades(professor.professorId(), sectionId, sheet.grades());
    }

    @DeleteMapping("/sections/{sectionId}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dropStudent(@AuthenticationPrincipal AuthenticatedUser professor,
                            @PathVariable long sectionId, @PathVariable long studentId) {
        registration.dropByProfessor(professor.professorId(), sectionId, studentId);
    }

    @GetMapping("/students/{cwid}")
    public StudentProfile student(@PathVariable String cwid) {
        return records.studentByCwid(cwid);
    }

    @GetMapping("/students/{cwid}/schedule")
    public List<EnrollmentView> studentSchedule(@PathVariable String cwid) {
        return records.schedule(records.studentByCwid(cwid).id());
    }

    @GetMapping("/students/{cwid}/history")
    public Transcript studentHistory(@PathVariable String cwid) {
        return records.history(records.studentByCwid(cwid).id());
    }
}
