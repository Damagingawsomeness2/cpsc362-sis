package edu.csuf.sis.behavior;

import edu.csuf.sis.common.PermissionDeniedException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.support.MutableClock;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    private final World world;
    private final MutableClock clock;

    public CommonSteps(World world, MutableClock clock) {
        this.world = world;
        this.clock = clock;
    }

    @Given("today is {date}")
    public void todayIs(LocalDate today) {
        clock.setToday(today);
    }

    @When("{int} hours pass")
    public void hoursPass(int hours) {
        clock.advance(Duration.ofHours(hours));
    }

    @Then("the request succeeds")
    public void theRequestSucceeds() {
        assertThat(world.lastError()).as("refusal").isNull();
    }

    @Then("the request is refused with a message mentioning {string}")
    public void theRequestIsRefused(String text) {
        assertThat(world.lastError()).isInstanceOf(RuleViolationException.class).hasMessageContaining(text);
    }

    @Then("the request is refused because the section belongs to another professor")
    public void refusedBecauseNotTheirSection() {
        assertThat(world.lastError()).isInstanceOf(PermissionDeniedException.class);
    }
}
