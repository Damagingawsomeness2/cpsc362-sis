package edu.csuf.sis.gateway;

import edu.csuf.sis.identity.AuthenticatedUser;
import edu.csuf.sis.records.EnrollmentView;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.StudentProfile;
import edu.csuf.sis.records.Transcript;
import edu.csuf.sis.registration.IRegistration;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The student portal's API. A student only ever sees and changes their own record. */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final IRecords records;
    private final IRegistration registration;

    public StudentController(IRecords records, IRegistration registration) {
        this.records = records;
        this.registration = registration;
    }

    @GetMapping("/profile")
    public StudentProfile profile(@AuthenticationPrincipal AuthenticatedUser student) {
        return records.student(student.studentId());
    }

    @GetMapping("/schedule")
    public List<EnrollmentView> schedule(@AuthenticationPrincipal AuthenticatedUser student) {
        return records.schedule(student.studentId());
    }

    @GetMapping("/history")
    public Transcript history(@AuthenticationPrincipal AuthenticatedUser student) {
        return records.history(student.studentId());
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentView enroll(@AuthenticationPrincipal AuthenticatedUser student,
                                 @Valid @RequestBody EnrollmentRequest request) {
        return registration.enroll(student.studentId(), request.sectionId());
    }

    @DeleteMapping("/enrollments/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop(@AuthenticationPrincipal AuthenticatedUser student, @PathVariable long sectionId) {
        registration.drop(student.studentId(), sectionId);
    }
}
