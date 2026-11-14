package edu.csuf.sis.behavior;

import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.NewCourse;
import edu.csuf.sis.catalog.NewSection;
import edu.csuf.sis.catalog.NewTerm;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogSteps {

    private final ICatalog catalog;
    private final World world;

    public CatalogSteps(ICatalog catalog, World world) {
        this.catalog = catalog;
        this.world = world;
    }

    @Given("the term {string} runs from {date} to {date}")
    public void term(String name, LocalDate start, LocalDate end) {
        catalog.addTerm(new NewTerm(name, start, end));
    }

    @Given("the course {string} {string} is in the catalog")
    public void course(String code, String title) {
        catalog.addCourse(new NewCourse(code, title, "", 3, List.of()));
    }

    @Given("the courses {string} are in the catalog")
    public void courses(String codes) {
        for (String code : World.list(codes)) {
            catalog.addCourse(new NewCourse(code, "Course " + code, "", 3, List.of()));
        }
    }

    @Given("the course {string} {string} requires {string}")
    public void courseWithPrerequisites(String code, String title, String prerequisites) {
        catalog.addCourse(new NewCourse(code, title, "", 3, World.list(prerequisites)));
    }

    @Given("professor {string} teaches section {string} of {string} in {string} with {int} seat(s) on {string} from {time} to {time}")
    public void professorTeaches(String professor, String sectionNumber, String courseCode, String termName,
                                 int seats, String days, LocalTime start, LocalTime end) {
        catalog.addSection(new NewSection(world.termId(termName), courseCode, sectionNumber,
                world.professorId(professor), seats, days, start, end, "CS-104"));
    }

    @Then("section {string} of {string} in {string} has {int} open seats")
    public void openSeats(String sectionNumber, String courseCode, String termName, int expected) {
        assertThat(catalog.section(world.sectionId(sectionNumber, courseCode, termName)).openSeats())
                .isEqualTo(expected);
    }
}
