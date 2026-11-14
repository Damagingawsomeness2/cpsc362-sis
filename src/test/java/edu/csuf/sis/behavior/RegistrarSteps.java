package edu.csuf.sis.behavior;

import edu.csuf.sis.audit.IDegreeAudit;
import edu.csuf.sis.audit.MajorRequest;
import edu.csuf.sis.audit.MajorView;
import edu.csuf.sis.identity.IAuth;
import edu.csuf.sis.identity.IUserAdmin;
import edu.csuf.sis.identity.LoginFailedException;
import edu.csuf.sis.identity.NewStudent;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.TranscriptTerm;
import edu.csuf.sis.support.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegistrarSteps {

    private final World world;
    private final IUserAdmin userAdmin;
    private final IAuth auth;
    private final IDegreeAudit degreeAudit;
    private final IRecords records;

    public RegistrarSteps(World world, IUserAdmin userAdmin, IAuth auth, IDegreeAudit degreeAudit,
                          IRecords records) {
        this.world = world;
        this.userAdmin = userAdmin;
        this.auth = auth;
        this.degreeAudit = degreeAudit;
        this.records = records;
    }

    @Given("the major {string} {string} requires {int} units including {string}")
    public void major(String code, String name, int units, String courseCodes) {
        degreeAudit.defineMajor(code, new MajorRequest(name, "B.S.", units, World.list(courseCodes)));
    }

    @When("the registrar changes major {string} to require {string}")
    public void changeMajor(String code, String courseCodes) {
        MajorView major = degreeAudit.majors().stream()
                .filter(candidate -> candidate.code().equals(code))
                .findFirst().orElseThrow();
        degreeAudit.defineMajor(code, new MajorRequest(major.name(), major.degree(), major.totalUnits(),
                World.list(courseCodes)));
    }

    @When("the registrar runs a graduation check for CWID {string}")
    public void graduationCheck(String cwid) {
        world.setGraduationCheck(degreeAudit.checkGraduation(cwid));
    }

    @When("the registrar tries a graduation check for CWID {string}")
    public void triesGraduationCheck(String cwid) {
        world.attempt(() -> degreeAudit.checkGraduation(cwid));
    }

    @Then("the student is eligible to graduate")
    public void eligible() {
        assertThat(world.graduationCheck().eligible()).isTrue();
    }

    @Then("the student is not eligible to graduate")
    public void notEligible() {
        assertThat(world.graduationCheck().eligible()).isFalse();
    }

    @Then("the missing courses are {string}")
    public void missingCourses(String courseCodes) {
        assertThat(world.graduationCheck().missingCourses()).containsExactlyElementsOf(World.list(courseCodes));
    }

    @Then("{int} units remain")
    public void unitsRemain(int units) {
        assertThat(world.graduationCheck().unitsRemaining()).isEqualTo(units);
    }

    @When("the registrar adds student {string} with CWID {string}, username {string} and password {string}")
    public void addStudent(String name, String cwid, String username, String password) {
        world.attempt(() -> userAdmin.addStudent(
                new NewStudent(cwid, name, username + "@csu.fullerton.edu", null, username, password)));
    }

    @Then("{string} can sign in with password {string}")
    public void canSignIn(String username, String password) {
        assertThat(auth.login(username, password).user().username()).isEqualTo(username);
    }

    @Then("{string} cannot sign in with password {string}")
    public void cannotSignIn(String username, String password) {
        assertThatThrownBy(() -> auth.login(username, password)).isInstanceOf(LoginFailedException.class);
    }

    @Given("{string} is signed in")
    public void signedIn(String name) {
        world.rememberToken(name, auth.login(world.username(name), TestData.PASSWORD).token());
    }

    @When("the registrar revokes access for {string}")
    public void revoke(String name) {
        userAdmin.revokeAccess(world.accountId(name), world.registrarAccountId());
    }

    @When("the registrar restores access for {string}")
    public void restore(String name) {
        userAdmin.restoreAccess(world.accountId(name));
    }

    @When("the registrar tries to revoke their own access")
    public void revokeOwnAccess() {
        world.attempt(() -> userAdmin.revokeAccess(world.registrarAccountId(), world.registrarAccountId()));
    }

    @Then("{string} can no longer sign in")
    public void canNoLongerSignIn(String name) {
        assertThatThrownBy(() -> auth.login(world.username(name), TestData.PASSWORD))
                .isInstanceOf(LoginFailedException.class);
    }

    @Then("{string} can sign in again")
    public void canSignInAgain(String name) {
        assertThat(auth.login(world.username(name), TestData.PASSWORD).token()).isNotBlank();
    }

    @Then("the earlier session of {string} no longer works")
    public void sessionNoLongerWorks(String name) {
        assertThat(auth.authenticate(world.token(name))).isEmpty();
    }

    @When("the registrar generates a transcript for CWID {string}")
    public void transcript(String cwid) {
        world.setTranscript(records.history(records.studentByCwid(cwid).id()));
    }

    @Then("the transcript lists the terms {string} in order")
    public void transcriptTerms(String termNames) {
        assertThat(world.transcript().terms()).extracting(TranscriptTerm::termName)
                .containsExactlyElementsOf(World.list(termNames));
    }

    @Then("the cumulative GPA is {string}")
    public void cumulativeGpa(String gpa) {
        assertThat(world.transcript().gpa()).isEqualByComparingTo(gpa);
    }

    @Then("the transcript shows {int} units earned")
    public void unitsEarned(int units) {
        assertThat(world.transcript().unitsEarned()).isEqualTo(units);
    }

    @Then("the transcript of {string} still shows {string} with grade {string}")
    public void transcriptStillShows(String name, String courseCode, String grade) {
        assertThat(records.history(world.studentId(name)).terms())
                .flatMap(TranscriptTerm::courses)
                .anySatisfy(line -> {
                    assertThat(line.courseCode()).isEqualTo(courseCode);
                    assertThat(line.grade()).isEqualTo(grade);
                });
    }
}
