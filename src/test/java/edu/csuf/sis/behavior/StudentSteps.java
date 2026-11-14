package edu.csuf.sis.behavior;

import edu.csuf.sis.domain.LetterGrade;
import edu.csuf.sis.records.EnrollmentView;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.registration.IRegistration;
import edu.csuf.sis.support.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentSteps {

    private final World world;
    private final TestData testData;
    private final IRegistration registration;
    private final IRecords records;

    public StudentSteps(World world, TestData testData, IRegistration registration, IRecords records) {
        this.world = world;
        this.testData = testData;
        this.registration = registration;
        this.records = records;
    }

    @Given("student {string} with CWID {string}")
    public void student(String name, String cwid) {
        world.addStudent(name, cwid, null);
    }

    @Given("student {string} with CWID {string} has not taken any classes")
    public void newStudent(String name, String cwid) {
        world.addStudent(name, cwid, null);
    }

    @Given("student {string} with CWID {string} majors in {string}")
    public void studentInMajor(String name, String cwid, String majorCode) {
        world.addStudent(name, cwid, majorCode);
    }

    @Given("student {string} with CWID {string} has passed {string} with grade {string}")
    public void studentWhoPassed(String name, String cwid, String courseCode, String grade) {
        world.addStudent(name, cwid, null);
        testData.completed(cwid, courseCode, 3, LetterGrade.fromLabel(grade), "Fall 2025");
        records.updateGradeLevel(world.studentId(name));
    }

    @Given("student {string} with CWID {string} has completed {int} units")
    public void studentWithUnits(String name, String cwid, int units) {
        world.addStudent(name, cwid, null);
        testData.generalEducationUnits(cwid, units);
        records.updateGradeLevel(world.studentId(name));
    }

    @Given("{string} took {string} in {string} and earned {string}")
    public void tookACourse(String name, String courseCode, String termName, String grade) {
        testData.completed(world.cwid(name), courseCode, 3, LetterGrade.fromLabel(grade), termName);
        records.updateGradeLevel(world.studentId(name));
    }

    @Given("{string} has passed {string} and {int} units in all")
    public void passedCoursesAndUnits(String name, String courseCodes, int totalUnits) {
        List<String> courses = World.list(courseCodes);
        for (String courseCode : courses) {
            testData.completed(world.cwid(name), courseCode, 3, LetterGrade.B, "Spring 2026");
        }
        testData.generalEducationUnits(world.cwid(name), totalUnits - 3 * courses.size());
        records.updateGradeLevel(world.studentId(name));
    }

    @Given("{string} is registered for section {string} of {string} in {string}")
    public void isRegistered(String name, String sectionNumber, String courseCode, String termName) {
        registration.enroll(world.studentId(name), world.sectionId(sectionNumber, courseCode, termName));
    }

    @When("{string} registers for section {string} of {string} in {string}")
    public void registers(String name, String sectionNumber, String courseCode, String termName) {
        long studentId = world.studentId(name);
        long sectionId = world.sectionId(sectionNumber, courseCode, termName);
        world.attempt(() -> registration.enroll(studentId, sectionId));
    }

    @When("{string} drops section {string} of {string} in {string}")
    public void drops(String name, String sectionNumber, String courseCode, String termName) {
        long studentId = world.studentId(name);
        long sectionId = world.sectionId(sectionNumber, courseCode, termName);
        world.attempt(() -> registration.drop(studentId, sectionId));
    }

    @Then("the schedule of {string} lists {string}")
    public void scheduleLists(String name, String courseCode) {
        assertThat(scheduledCourses(name)).contains(courseCode);
    }

    @Then("the schedule of {string} does not list {string}")
    public void scheduleDoesNotList(String name, String courseCode) {
        assertThat(scheduledCourses(name)).doesNotContain(courseCode);
    }

    private List<String> scheduledCourses(String name) {
        return records.schedule(world.studentId(name)).stream().map(EnrollmentView::courseCode).toList();
    }
}
