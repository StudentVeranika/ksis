/**
 * Модель игрового уровня
 * Поддерживает 4 типа заданий:
 * - puzzle
 * - memory
 * - schulte
 * - stroop
 */

package com.mindclash.model;

public class Level {
    private int id;
    private String type;
    private String question;
    private String correctAnswer;
    private String imagePath;
    private int difficulty;
    private Integer displayTime;
    private Integer gridSize;


    public Level(int id, String type, String question, String correctAnswer, String imagePath, int difficulty) {
        this.id = id;
        this.type = type;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.imagePath = imagePath;
        this.difficulty = difficulty;
        this.displayTime = null;
        this.gridSize = null;
    }


    public Level(int id, String type, String question, String correctAnswer,
                 String imagePath, int difficulty, Integer displayTime, Integer gridSize) {
        this.id = id;
        this.type = type;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.imagePath = imagePath;
        this.difficulty = difficulty;
        this.displayTime = displayTime;
        this.gridSize = gridSize;
    }


    public boolean checkAnswer(String userAnswer) {
        if (userAnswer == null || correctAnswer == null) return false;

        if ("schulte".equals(type)) {
            return correctAnswer.equals(userAnswer);
        }


        return correctAnswer.equalsIgnoreCase(userAnswer.trim());
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public Integer getDisplayTime() { return displayTime; }
    public void setDisplayTime(Integer displayTime) { this.displayTime = displayTime; }

    public Integer getGridSize() { return gridSize; }
    public void setGridSize(Integer gridSize) { this.gridSize = gridSize; }
}