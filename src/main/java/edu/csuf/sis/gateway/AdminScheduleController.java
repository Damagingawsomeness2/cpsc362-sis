package edu.csuf.sis.gateway;

import edu.csuf.sis.catalog.CourseView;
import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.NewCourse;
import edu.csuf.sis.catalog.NewSection;
import edu.csuf.sis.catalog.NewTerm;
import edu.csuf.sis.catalog.SectionView;
import edu.csuf.sis.catalog.TermView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin portal: course descriptions and each semester's class schedule (Course Catalog &amp; Scheduling). */
@RestController
@RequestMapping("/api/admin")
public class AdminScheduleController {

    private final ICatalog catalog;

    public AdminScheduleController(ICatalog catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseView addCourse(@Valid @RequestBody NewCourse course) {
        return catalog.addCourse(course);
    }

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public TermView addTerm(@Valid @RequestBody NewTerm term) {
        return catalog.addTerm(term);
    }

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionView addSection(@Valid @RequestBody NewSection section) {
        return catalog.addSection(section);
    }
}
