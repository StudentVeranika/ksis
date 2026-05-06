/**
 * Игровая комната для одной сессии между двумя игроками.
 * Хранит:
 * - ID и имена обоих игроков
 * - список уровней
 * - прогресс каждого игрока
 * - флаги активности и завершения игры
 */

package com.mindclash.server;

import com.mindclash.database.LevelDAO;
import com.mindclash.model.Level;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private String roomId;
    private int player1Id;
    private String player1Name;
    private int player2Id;
    private String player2Name;
    private int difficulty;
    private List<Level> levels;
    private Map<Integer, Integer> playerCorrect;      // Количество правильных ответов
    private Map<Integer, Integer> playerAnswered;     // Количество отвеченных вопросов
    private Map<Integer, Boolean> playerFinished;     // Закончил ли игрок
    private Map<Integer, Boolean> playerConnected;
    private boolean gameActive;
    private boolean gameFinished;

    public GameRoom(String roomId, int player1Id, String player1Name, int difficulty) {
        this.roomId = roomId;
        this.player1Id = player1Id;
        this.player1Name = player1Name;
        this.player2Id = -1;
        this.player2Name = null;
        this.difficulty = difficulty;
        this.levels = LevelDAO.getLevelsByDifficulty(difficulty, 10);
        this.playerCorrect = new ConcurrentHashMap<>();
        this.playerAnswered = new ConcurrentHashMap<>();
        this.playerFinished = new ConcurrentHashMap<>();
        this.playerConnected = new ConcurrentHashMap<>();
        this.gameActive = false;
        this.gameFinished = false;

        playerCorrect.put(player1Id, 0);
        playerAnswered.put(player1Id, 0);
        playerFinished.put(player1Id, false);
        playerConnected.put(player1Id, true);

        System.out.println("   GameRoom создана: " + levels.size() + " уровней");
        for (Level level : levels) {
            System.out.println("      - " + level.getType() + ": " + level.getQuestion());
        }
    }

    public void setPlayer2(int player2Id, String player2Name) {
        this.player2Id = player2Id;
        this.player2Name = player2Name;
        this.playerCorrect.put(player2Id, 0);
        this.playerAnswered.put(player2Id, 0);
        this.playerFinished.put(player2Id, false);
        this.playerConnected.put(player2Id, true);
        this.gameActive = true;
    }
    //обработка ответа
    public boolean submitAnswer(int userId, String answer) {
        if (!gameActive || gameFinished) return false;

        int answered = playerAnswered.getOrDefault(userId, 0);
        if (answered >= levels.size()) return false;

        Level currentLevel = levels.get(answered);
        boolean correct = currentLevel.checkAnswer(answer);

        playerAnswered.put(userId, answered + 1);

        if (correct) {
            playerCorrect.put(userId, playerCorrect.getOrDefault(userId, 0) + 1);
        }

        System.out.println("   Игрок " + userId + ": ответ=" + answer + ", правильно=" + correct +
                ", прогресс=" + (answered + 1) + "/" + levels.size() +
                ", очков=" + playerCorrect.get(userId));

        if (answered + 1 >= levels.size()) {
            playerFinished.put(userId, true);
            System.out.println("   Игрок " + userId + " закончил игру! Очков: " + playerCorrect.get(userId));
        }

        return correct;
    }
    //проверка закончили ли играть
    public boolean bothFinished() {
        if (player2Id == -1) return false;
        return playerFinished.getOrDefault(player1Id, false) &&
                playerFinished.getOrDefault(player2Id, false);
    }
    //определение победителя
    public int getWinner() {
        if (!bothFinished()) return -1;

        int p1Correct = playerCorrect.getOrDefault(player1Id, 0);
        int p2Correct = playerCorrect.getOrDefault(player2Id, 0);

        if (p1Correct > p2Correct) {
            gameFinished = true;
            return player1Id;
        } else if (p2Correct > p1Correct) {
            gameFinished = true;
            return player2Id;
        } else {
            gameFinished = true;
            return -1; // ничья
        }
    }

    //обход некоторых символов
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    //преобразование для отправки клиенту
    public String levelToJsonString(Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(level.getId()).append(",");
        sb.append("\"type\":\"").append(escapeJson(level.getType())).append("\",");
        sb.append("\"question\":\"").append(escapeJson(level.getQuestion())).append("\",");
        sb.append("\"correct_answer\":\"").append(escapeJson(level.getCorrectAnswer())).append("\",");
        sb.append("\"image_path\":\"").append(escapeJson(level.getImagePath() != null ? level.getImagePath() : "")).append("\",");
        sb.append("\"difficulty\":").append(level.getDifficulty());


        if (level.getDisplayTime() != null) {
            sb.append(",\"display_time\":").append(level.getDisplayTime());
        }


        if (level.getGridSize() != null) {
            sb.append(",\"grid_size\":").append(level.getGridSize());
        }

        sb.append("}");
        return sb.toString();
    }


    public String getLevelsJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(levelToJsonString(levels.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public int getPlayerCorrect(int userId) {
        return playerCorrect.getOrDefault(userId, 0);
    }

    public int getPlayer1Correct() {
        return playerCorrect.getOrDefault(player1Id, 0);
    }

    public int getPlayer2Correct() {
        if (player2Id == -1) return 0;
        return playerCorrect.getOrDefault(player2Id, 0);
    }

    public int getPlayerAnswered(int userId) {
        return playerAnswered.getOrDefault(userId, 0);
    }

    public boolean hasPlayer(int userId) {
        return userId == player1Id || userId == player2Id;
    }

    public int getOpponentId(int userId) {
        if (userId == player1Id) return player2Id;
        if (userId == player2Id) return player1Id;
        return -1;
    }

    public String getPlayerName(int userId) {
        if (userId == player1Id) return player1Name;
        if (userId == player2Id) return player2Name;
        return null;
    }

    public void setPlayerDisconnected(int userId) {
        playerConnected.put(userId, false);
        gameActive = false;
    }

    public boolean isGameActive() { return gameActive; }
    public int getLevelsCount() { return levels.size(); }
    public int getPlayer1Id() { return player1Id; }
    public int getPlayer2Id() { return player2Id; }
    public String getRoomId() { return roomId; }
    public List<Level> getLevels() { return levels; }
    public boolean isGameFinished() { return gameFinished; }

    public int[] getAllPlayers() {
        if (player2Id == -1) {
            return new int[]{player1Id};
        }
        return new int[]{player1Id, player2Id};
    }
}