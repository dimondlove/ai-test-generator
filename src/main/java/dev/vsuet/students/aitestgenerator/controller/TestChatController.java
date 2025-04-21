package dev.vsuet.students.aitestgenerator.controller;

import dev.vsuet.students.aitestgenerator.model.QuestionData;
import dev.vsuet.students.aitestgenerator.service.TestChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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
            List<String> chunks = testChatService.splitText(content, 45000);

            int questionsPerChunk = Math.max(1, 10 / chunks.size());
            List<QuestionData> allQuestions = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                int remaining = 10 - allQuestions.size();
                int count = (i == chunks.size() - 1) ? remaining : Math.min(questionsPerChunk, remaining);
                if (count <= 0) break;

                String jsonResponse = generateTestFromAI(chunks.get(i), count);
                List<QuestionData> questions = testChatService.parseJson(jsonResponse);
                allQuestions.addAll(questions);
            }

            if (allQuestions.size() > 10) {
                allQuestions = allQuestions.subList(0, 10); // если случайно больше
            }
            byte[] pdfBytes = testChatService.createPdfFromJson(allQuestions);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateTestFromAI(String text, int count) {
        String prompt = String.format("""
            Ты - генератор тестов. Сгенерируй %d по тексту ниже.
            Структура каждого вопроса:
            "question": строка
            "answers": массив из 4х вариантов
            "correctAnswer": символ - правильный вариант ответа
            Если в лекции не будет хватать информации  для не правильных вариантов ответа - бери из интернета.
            
            Верни результат в JSON-формате:
            {
              "questions": [
                {
                  "question": "...",
                  "answers": ["А. ...", "Б. ...", "В. ...", "Г. ..."],
                  "correctAnswer": "..."
                }
              ]
            }
            
            Только не оборачивай ответ в Markdown.

            Текст:
            %s
            """, count, text);
        String answer =  chatClient.prompt().user(prompt).call().content();
        System.out.println(answer);
        return answer;
    }
}
