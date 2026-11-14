package edu.csuf.sis.gateway;

import edu.csuf.sis.chat.ChatReply;
import edu.csuf.sis.chat.IChat;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final IChat chat;

    public ChatController(IChat chat) {
        this.chat = chat;
    }

    @PostMapping
    public ChatReply ask(@Valid @RequestBody ChatRequest request) {
        return chat.ask(request.message());
    }
}
