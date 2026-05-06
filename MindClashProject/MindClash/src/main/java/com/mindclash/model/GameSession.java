package com.mindclash.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class GameSession {


    private String sessionId;


    private int difficulty;


    private List<Level> levels;


    private Map<Integer, PlayerProgress> players;


    private String status;


    private int winnerId;


    private long startTime;


    private long endTime;


    private static final int LEVELS_PER_GAME = 10;


    public static class PlayerProgress {
        private int userId;
        private String username;
        private String avatar;
        private int completedLevels;
        private long lastAnswerTime;
        private boolean connected;

        public PlayerProgress(int userId, String username, String avatar) {
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.completedLevels = 0;
            this.lastAnswerTime = System.currentTimeMillis();
            this.connected = true;
        }

        // Геттеры
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getAvatar() { return avatar; }
        public int getCompletedLevels() { return completedLevels; }
        public long getLastAnswerTime() { return lastAnswerTime; }
        public boolean isConnected() { return connected; }

        // Сеттеры
        public void setCompletedLevels(int completedLevels) {
            this.completedLevels = completedLevels;
        }

        public void setLastAnswerTime(long lastAnswerTime) {
            this.lastAnswerTime = lastAnswerTime;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }


        public void incrementLevel() {
            this.completedLevels++;
            this.lastAnswerTime = System.currentTimeMillis();
        }


        public String toJson() {
            return String.format(
                    "{\"userId\":%d,\"username\":\"%s\",\"avatar\":\"%s\",\"completed\":%d,\"connected\":%b}",
                    userId, username, avatar, completedLevels, connected
            );
        }

        @Override
        public String toString() {
            return String.format("PlayerProgress{id=%d, name='%s', completed=%d}",
                    userId, username, completedLevels);
        }
    }


    public GameSession(String sessionId, int difficulty, List<Level> levels) {
        this.sessionId = sessionId;
        this.difficulty = difficulty;
        this.levels = new ArrayList<>(levels);
        this.players = new ConcurrentHashMap<>();
        this.status = "waiting";
        this.winnerId = -1;
        this.startTime = 0;
        this.endTime = 0;
    }


    public boolean addPlayer(int userId, String username, String avatar) {
        if (players.size() >= 2) {
            return false; // максимум 2 игрока
        }

        if (players.containsKey(userId)) {
            return false; // игрок уже в сессии
        }

        players.put(userId, new PlayerProgress(userId, username, avatar));
        return true;
    }


    public void startGame() {
        if (players.size() == 2) {
            this.status = "active";
            this.startTime = System.currentTimeMillis();
            System.out.println(" Игра началась! Сессия: " + sessionId);
        }
    }


    public AnswerResult submitAnswer(int userId, String answer) {

        if (!status.equals("active")) {
            return new AnswerResult(false, false, false, "Игра не активна");
        }


        if (winnerId != -1) {
            return new AnswerResult(false, false, true, "Игра уже завершена");
        }


        PlayerProgress player = players.get(userId);
        if (player == null) {
            return new AnswerResult(false, false, false, "Игрок не найден");
        }


        if (player.getCompletedLevels() >= levels.size()) {
            return new AnswerResult(false, false, false, "Вы уже прошли все уровни");
        }


        int levelIndex = player.getCompletedLevels();
        Level currentLevel = levels.get(levelIndex);

        boolean isCorrect = currentLevel.checkAnswer(answer);

        if (isCorrect) {

            player.incrementLevel();


            if (player.getCompletedLevels() >= levels.size()) {
                this.winnerId = userId;
                this.status = "finished";
                this.endTime = System.currentTimeMillis();

                return new AnswerResult(true, true, true, "Победа!", winnerId);
            }

            return new AnswerResult(true, true, false, "Правильно!");
        } else {
            // Неправильный ответ
            return new AnswerResult(true, false, false, "Неправильно, попробуй ещё");
        }
    }


    public void setPlayerDisconnected(int userId) {
        PlayerProgress player = players.get(userId);
        if (player != null) {
            player.setConnected(false);

            if (status.equals("active")) {
                status = "finished";

                for (Integer id : players.keySet()) {
                    if (id != userId) {
                        winnerId = id;
                        break;
                    }
                }
            }
        }
    }


    public PlayerProgress getPlayerProgress(int userId) {
        return players.get(userId);
    }


    public PlayerProgress getOpponentProgress(int userId) {
        for (Integer id : players.keySet()) {
            if (!id.equals(userId)) {
                return players.get(id);
            }
        }
        return null;
    }


    public Level getCurrentLevelForPlayer(int userId) {
        PlayerProgress player = players.get(userId);
        if (player == null) return null;

        int levelIndex = player.getCompletedLevels();
        if (levelIndex < levels.size()) {
            return levels.get(levelIndex);
        }
        return null;
    }


    public boolean isReady() {
        if (players.size() != 2) return false;

        for (PlayerProgress player : players.values()) {
            if (!player.isConnected()) return false;
        }
        return true;
    }

    // Геттеры
    public String getSessionId() { return sessionId; }
    public int getDifficulty() { return difficulty; }
    public List<Level> getLevels() { return Collections.unmodifiableList(levels); }
    public Map<Integer, PlayerProgress> getPlayers() { return Collections.unmodifiableMap(players); }
    public String getStatus() { return status; }
    public int getWinnerId() { return winnerId; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getLevelsCount() { return levels.size(); }


    public String getStateJson() {
        StringBuilder playersJson = new StringBuilder();
        playersJson.append("{");
        boolean first = true;
        for (Map.Entry<Integer, PlayerProgress> entry : players.entrySet()) {
            if (!first) playersJson.append(",");
            playersJson.append("\"").append(entry.getKey()).append("\":")
                    .append(entry.getValue().toJson());
            first = false;
        }
        playersJson.append("}");


        StringBuilder levelsJson = new StringBuilder();
        levelsJson.append("[");
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) levelsJson.append(",");
            Level level = levels.get(i);

            levelsJson.append(String.format(
                    "{\"id\":%d,\"type\":\"%s\",\"question\":\"%s\",\"image\":\"%s\"}",
                    level.getId(), level.getType(), level.getQuestion(),
                    level.getImagePath() != null ? level.getImagePath() : ""
            ));
        }
        levelsJson.append("]");

        return String.format(
                "{\"sessionId\":\"%s\",\"status\":\"%s\",\"winnerId\":%d,\"players\":%s,\"levels\":%s,\"levelsCount\":%d}",
                sessionId, status, winnerId, playersJson.toString(), levelsJson.toString(), levels.size()
        );
    }


    public static class AnswerResult {
        private boolean success;
        private boolean correct;
        private boolean gameOver;
        private String message;
        private int winnerId;

        public AnswerResult(boolean success, boolean correct, boolean gameOver, String message) {
            this(success, correct, gameOver, message, -1);
        }

        public AnswerResult(boolean success, boolean correct, boolean gameOver, String message, int winnerId) {
            this.success = success;
            this.correct = correct;
            this.gameOver = gameOver;
            this.message = message;
            this.winnerId = winnerId;
        }

        public boolean isSuccess() { return success; }
        public boolean isCorrect() { return correct; }
        public boolean isGameOver() { return gameOver; }
        public String getMessage() { return message; }
        public int getWinnerId() { return winnerId; }


        public String toJson() {
            if (winnerId != -1) {
                return String.format(
                        "{\"type\":\"answer_result\",\"success\":%b,\"correct\":%b,\"gameOver\":%b,\"message\":\"%s\",\"winnerId\":%d}",
                        success, correct, gameOver, message, winnerId
                );
            } else {
                return String.format(
                        "{\"type\":\"answer_result\",\"success\":%b,\"correct\":%b,\"gameOver\":%b,\"message\":\"%s\"}",
                        success, correct, gameOver, message
                );
            }
        }

        @Override
        public String toString() {
            return String.format("AnswerResult{correct=%b, gameOver=%b, message='%s'}",
                    correct, gameOver, message);
        }
    }

    @Override
    public String toString() {
        return String.format("GameSession{id='%s', status='%s', players=%d, levels=%d}",
                sessionId, status, players.size(), levels.size());
    }
}