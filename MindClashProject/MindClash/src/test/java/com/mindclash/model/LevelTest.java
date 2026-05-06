package com.mindclash.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LevelTest {

    @Test
    void testPuzzleCorrectAnswer() {
        Level level = new Level(1, "puzzle", "Зимой и летом одним цветом",
                "ёлка", null, 1, null, null);

        // правильные ответы в разных регистрах
        assertTrue(level.checkAnswer("ёлка"));
        assertTrue(level.checkAnswer("Ёлка"));
        assertTrue(level.checkAnswer("  ёлка  "));
    }

    @Test
    void testPuzzleWrongAnswer() {
        Level level = new Level(1, "puzzle", "Зимой и летом одним цветом",
                "ёлка", null, 1, null, null);

        // Проверка неправильных ответов
        assertFalse(level.checkAnswer("берёза"));
        assertFalse(level.checkAnswer("сосна"));
        assertFalse(level.checkAnswer(""));
        assertFalse(level.checkAnswer(null));
    }

    @Test
    void testMemoryCorrectAnswer() {
        Level level = new Level(2, "memory", "Запомните число",
                "7294", null, 1, 2, 4);


        assertTrue(level.checkAnswer("7294"));
    }

    @Test
    void testMemoryWrongAnswer() {
        Level level = new Level(2, "memory", "Запомните число",
                "7294", null, 1, 2, 4);


        assertFalse(level.checkAnswer("7295"));
        assertFalse(level.checkAnswer("729"));
        assertFalse(level.checkAnswer(""));
    }



    @Test
    void testSchulteCorrectAnswer() {
        Level level = new Level(3, "schulte", "Нажмите на клетки...",
                "1,2,3,4", null, 1, 15, 2);


        assertTrue(level.checkAnswer("1,2,3,4"));
    }

    @Test
    void testSchulteWrongAnswer() {
        Level level = new Level(3, "schulte", "Нажмите на клетки...",
                "1,2,3,4", null, 1, 15, 2);


        assertFalse(level.checkAnswer("1,3,2,4"));
        assertFalse(level.checkAnswer("1,2,3"));
        assertFalse(level.checkAnswer(""));
    }
}