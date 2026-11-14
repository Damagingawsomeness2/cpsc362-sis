package edu.csuf.sis.behavior;

import edu.csuf.sis.records.GradeEntry;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.RosterEntry;
import edu.csuf.sis.records.StudentProfile;
import edu.csuf.sis.registration.IRegistration;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfessorSteps {

    private final World world;
    private final IRecords records;
    private final IRegistration registration;

    public ProfessorSteps(World world, IRecords records, IRegistration registration) {
        this.world = world;
        this.records = records;
        this.registration = registration;
    }

    @When("professor {string} posts grade {string} for {string} in section {string} of {string} in {string}")
    public void postsGrade(String professor, String grade, String student,
                           String sectionNumber, String courseCode, String termName) {
        long professorId = world.professorId(professor);
        long sectionId = world.sectionId(sectionNumber, courseCode, termName);
        long studentId = world.studentId(student);
        world.attempt(() -> records.enterGrades(professorId, sectionId, List.of(new GradeEntry(studentId, grade))));
    }

    @When("professor {string} drops {string} from section {string} of {string} in {string}")
    public void professorDrops(String professor, String student,
                               String sectionNumber, String courseCode, String termName) {
        long professorId = world.professorId(professor);
        long sectionId = world.sectionId(sectionNumber, courseCode, termName);
        long studentId = world.studentId(student);
        world.attempt(() -> registration.dropByProfessor(professorId, sectionId, studentId));
    }

    @When("professor {string} views the roll for section {string} of {string} in {string}")
    public void viewsRoll(String professor, String sectionNumber, String courseCode, String termName) {
        world.setRoll(records.classRoll(world.professorId(professor),
                world.sectionId(sectionNumber, courseCode, termName)));
    }

    @Then("the roll lists {string}")
    public void rollLists(String student) {
        assertThat(world.roll()).extracting(RosterEntry::name).contains(student);
    }

    @Then("{string} has {int} units completed and is a {word}")
    public void standing(String student, int units, String gradeLevel) {
        StudentProfile profile = records.student(world.studentId(student));
        assertThat(profile.unitsCompleted()).isEqualTo(units);
        assertThat(profile.gradeLevel().name()).isEqualTo(gradeLevel);
    }
}
