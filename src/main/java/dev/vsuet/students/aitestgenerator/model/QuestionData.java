package dev.vsuet.students.aitestgenerator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionData {
    private String question;
    private List<String> answers;
    private String correctAnswer;
}
