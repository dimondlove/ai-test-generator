package dev.vsuet.students.aitestgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import dev.vsuet.students.aitestgenerator.model.QuestionData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestChatService {
    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public List<String> splitText(String text, int maxLength) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxLength);
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }

    public List<QuestionData> parseJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;

        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге JSON: " + e.getMessage());
            System.err.println("Исходная строка:\n" + json);
            throw e;
        }

        List<QuestionData> result = new ArrayList<>();
        JsonNode questionsNode = root.get("questions");

        if (questionsNode != null && questionsNode.isArray()) {
            for (JsonNode qNode : questionsNode) {
                QuestionData q = new QuestionData();
                q.setQuestion(qNode.has("question") ? qNode.get("question").asText() : "");

                List<String> answers = new ArrayList<>();
                JsonNode answersNode = qNode.get("answers");
                if (answersNode != null && answersNode.isArray()) {
                    for (JsonNode ans : answersNode) {
                        answers.add(ans.asText());
                    }
                }
                q.setAnswers(answers);

                q.setCorrectAnswer(qNode.has("correctAnswer") ? qNode.get("correctAnswer").asText() : "");
                result.add(q);
            }
        }

        return result;
    }

    public byte[] createPdfFromJson(List<QuestionData> questions) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            String fontPath = "C:/Windows/Fonts/times.ttf";
            BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(baseFont, 12);

            document.add(new Paragraph("Вопросы", font));
            for (int i = 0; i < questions.size(); i++) {
                QuestionData q = questions.get(i);
                document.add(new Paragraph((i + 1) + ". " + q.getQuestion(), font));
                for (String ans : q.getAnswers()) {
                    document.add(new Paragraph(" - " + ans, font));
                }
                document.add(new Paragraph(" "));
            }

            document.newPage();
            document.add(new Paragraph("Ответы", font));
            for (int i = 0; i < questions.size(); i++) {
                QuestionData q = questions.get(i);
                document.add(new Paragraph((i + 1) + ". " + q.getCorrectAnswer(), font));
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }
}
