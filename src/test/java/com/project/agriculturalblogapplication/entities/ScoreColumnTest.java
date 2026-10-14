package com.project.agriculturalblogapplication.entities;

import jakarta.persistence.Column;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ScoreColumnTest {

    // Regression: editing a post (or accepting an answer) saved the whole row, including the score it had
    // loaded, which could undo a vote cast in between. Only the atomic UPDATE in the repository may change it.
    @ParameterizedTest
    @ValueSource(classes = {Question.class, Answer.class})
    void entitySavesNeverWriteTheScore(Class<?> entity) throws NoSuchFieldException {
        assertFalse(entity.getDeclaredField("score").getAnnotation(Column.class).updatable());
    }
}
