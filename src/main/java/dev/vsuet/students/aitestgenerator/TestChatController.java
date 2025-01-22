package dev.vsuet.students.aitestgenerator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestChatController {

    private final ChatClient chatClient;

    public TestChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/jokes")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke about Russian people in Russian language") String message) {
        return chatClient.prompt().user(message).call().content();
    }
}
