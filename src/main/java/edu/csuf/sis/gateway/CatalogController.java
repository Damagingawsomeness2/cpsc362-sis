package edu.csuf.sis.gateway;

import edu.csuf.sis.catalog.CourseView;
import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.SectionView;
import edu.csuf.sis.catalog.TermView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only schedule browsing for every signed-in role. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final ICatalog catalog;

    public CatalogController(ICatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/terms")
    public List<TermView> terms() {
        return catalog.terms();
    }

    @GetMapping("/courses")
    public List<CourseView> courses() {
        return catalog.courses();
    }

    @GetMapping("/terms/{termId}/sections")
    public List<SectionView> sections(@PathVariable long termId, @RequestParam(required = false) String search) {
        return catalog.sections(termId, search);
    }
}
