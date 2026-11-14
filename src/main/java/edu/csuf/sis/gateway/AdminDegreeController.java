package edu.csuf.sis.gateway;

import edu.csuf.sis.audit.GraduationCheck;
import edu.csuf.sis.audit.IDegreeAudit;
import edu.csuf.sis.audit.MajorRequest;
import edu.csuf.sis.audit.MajorView;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.Transcript;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin portal: degree requirements, graduation checks and transcripts (Degree Audit, Academic Records). */
@RestController
@RequestMapping("/api/admin")
public class AdminDegreeController {

    private final IDegreeAudit degreeAudit;
    private final IRecords records;

    public AdminDegreeController(IDegreeAudit degreeAudit, IRecords records) {
        this.degreeAudit = degreeAudit;
        this.records = records;
    }

    @GetMapping("/majors")
    public List<MajorView> majors() {
        return degreeAudit.majors();
    }

    @PutMapping("/majors/{code}")
    public MajorView defineMajor(@PathVariable String code, @Valid @RequestBody MajorRequest request) {
        return degreeAudit.defineMajor(code, request);
    }

    @GetMapping("/students/{cwid}/transcript")
    public Transcript transcript(@PathVariable String cwid) {
        return records.history(records.studentByCwid(cwid).id());
    }

    @GetMapping("/students/{cwid}/graduation-check")
    public GraduationCheck graduationCheck(@PathVariable String cwid) {
        return degreeAudit.checkGraduation(cwid);
    }
}
