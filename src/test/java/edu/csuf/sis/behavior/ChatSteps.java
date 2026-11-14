package edu.csuf.sis.behavior;

import edu.csuf.sis.chat.IChat;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class ChatSteps {

    private final IChat chat;
    private final World world;

    public ChatSteps(IChat chat, World world) {
        this.chat = chat;
        this.world = world;
    }

    @When("someone asks the assistant {string}")
    public void ask(String question) {
        world.setReply(chat.ask(question).reply());
    }

    @Then("the reply mentions {string}")
    public void replyMentions(String text) {
        assertThat(world.reply()).contains(text);
    }
}
