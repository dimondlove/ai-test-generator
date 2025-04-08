package dev.vsuet.students.aitestgenerator.controller;

import dev.vsuet.students.aitestgenerator.service.TestChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TestChatController {

    private final ChatClient chatClient;
    private final TestChatService testChatService;

    public TestChatController(ChatClient.Builder chatClientBuilder, TestChatService testChatService) {
        this.chatClient = chatClientBuilder.build();
        this.testChatService = testChatService;
    }

    @GetMapping("/jokes")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke about Russian people in Russian language") String message) {
        return chatClient.prompt().user(message).call().content();
    }

    @CrossOrigin
    @PostMapping("/generate-test")
    public ResponseEntity<ByteArrayResource> generateTest(@RequestParam("file") MultipartFile file) {
        try {
            String content = testChatService.extractText(file);
            List<String> chunks = testChatService.splitText(content, 15000);

            StringBuilder allQuestions = new StringBuilder();
            StringBuilder allAnswers = new StringBuilder();

            for (String chunk : chunks) {
                String response = generateTestFromAI(chunk);
                String[] parts = response.split("\n\n### Ответы:\n", 2);

                if (parts.length == 2) {
                    allQuestions.append(parts[0]).append("\n\n");
                    allAnswers.append(parts[1]).append("\n\n");
                }
            }

            byte[] pdfBytes = testChatService.createPdf(allQuestions.toString(), allAnswers.toString());
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateTestFromAI(String text) {
        String prompt = "Сгенерируй следующий тест с вариантами ответа на 15 вопросов по следующему тексту лекции. Часть с ответами должна разделяться двумя пустыми строками, заголовком ###Ответы, двоеточием и пустой строкой:\n\n"
                + text;
        String answer =  chatClient.prompt().user(prompt).call().content();
        System.out.println(answer);
        return answer;
    }
}
