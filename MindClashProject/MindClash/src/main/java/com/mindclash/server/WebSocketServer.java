/**
 * WebSocket сервер.
 * Работает на порту 8081, использует пул из 20 потоков.
 * Хранит:
 * - подключённых клиентов
 * - активные игровые комнаты
 * - ожидающие приглашения
 */

package com.mindclash.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.mindclash.database.UserDAO;
import com.mindclash.model.Level;
import java.util.List;

public class WebSocketServer {

    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private ExecutorService threadPool;
    private Map<Integer, WebSocketHandler> connectedClients;
    private Map<String, GameRoom> activeRooms;
    private Map<Integer, PendingInvite> pendingInvites;

    public WebSocketServer(int port) {
        this.port = port;
        this.connectedClients = new ConcurrentHashMap<>();
        this.activeRooms = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(20);
    }
    //запуск сервера
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println(" WebSocket сервер запущен на порту " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                WebSocketHandler handler = new WebSocketHandler(clientSocket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.out.println(" Ошибка WebSocket сервера: " + e.getMessage());
        }
    }
    //остановка сервера
    public void stop() {
        running = false;
        threadPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //регистрация клиента после успешной авторизации
    public void registerClient(int userId, WebSocketHandler handler) {
        connectedClients.put(userId, handler);
        System.out.println(" Клиент " + userId + " подключен к WebSocket");
    }
    //удаление клиента при отключении
    public void removeClient(int userId) {
        connectedClients.remove(userId);
        System.out.println(" Клиент " + userId + " отключен от WebSocket");

        for (GameRoom room : activeRooms.values()) {
            if (room.hasPlayer(userId)) {
                int opponentId = room.getOpponentId(userId);
                room.setPlayerDisconnected(userId);

                if (opponentId != -1 && connectedClients.containsKey(opponentId)) {
                    sendToUser(opponentId, "{\"type\":\"opponent_disconnected\",\"message\":\"Соперник отключился\"}");
                }
                break;
            }
        }
    }
    //отправка сообщения конкретному пользователю
    public void sendToUser(int userId, String message) {
        WebSocketHandler handler = connectedClients.get(userId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

//    public void broadcastToRoom(String roomId, String message, int excludeUserId) {
//        GameRoom room = activeRooms.get(roomId);
//        if (room == null) return;
//
//        for (int userId : room.getAllPlayers()) {
//            if (userId != excludeUserId && connectedClients.containsKey(userId)) {
//                sendToUser(userId, message);
//            }
//        }
//    }
    //создание игровой комнаты
    public String createRoom(int player1Id, String player1Name, int difficulty) {
        String roomId = generateRoomId();
        GameRoom room = new GameRoom(roomId, player1Id, player1Name, difficulty);
        activeRooms.put(roomId, room);
        return roomId;
    }
    //приглашение
    public boolean joinRoom(String roomId, int player2Id, String player2Name) {
        GameRoom room = activeRooms.get(roomId);
        if (room == null) return false;
        if (room.getPlayer2Id() != -1) return false;

        room.setPlayer2(player2Id, player2Name);
        return true;
    }

    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public GameRoom getPlayerRoom(int userId) {
        for (GameRoom room : activeRooms.values()) {
            if (room.hasPlayer(userId)) {
                return room;
            }
        }
        return null;
    }
    //отправка приглашения
    public boolean sendInvite(int fromUserId, String fromUsername, int toUserId, int difficulty) {
        if (!connectedClients.containsKey(toUserId)) {
            return false;
        }

        String inviteId = generateInviteId();
        PendingInvite invite = new PendingInvite(inviteId, fromUserId, fromUsername, toUserId, difficulty);
        pendingInvites.put(toUserId, invite);

        sendToUser(toUserId, "{\"type\":\"invite\",\"inviteId\":\"" + inviteId + "\",\"fromUserId\":" + fromUserId + ",\"fromUsername\":\"" + fromUsername + "\",\"difficulty\":" + difficulty + "}");
        return true;
    }
    //принять приглашение
    public boolean acceptInvite(int userId, String inviteId) {
        PendingInvite invite = pendingInvites.get(userId);
        if (invite == null || !invite.getInviteId().equals(inviteId)) {
            System.out.println(" Ошибка: приглашение не найдено");
            return false;
        }

        String player1Name = invite.getFromUsername();
        String player2Name = UserDAO.getUserById(userId).getUsername();

        System.out.println(" Принятие приглашения: " + player1Name + " + " + player2Name);

        String roomId = createRoom(invite.getFromUserId(), player1Name, invite.getDifficulty());
        boolean joined = joinRoom(roomId, userId, player2Name);

        if (!joined) return false;

        pendingInvites.remove(userId);
        GameRoom room = activeRooms.get(roomId);
        if (room == null) return false;


        WebSocketHandler handler1 = connectedClients.get(invite.getFromUserId());
        if (handler1 != null) {
            handler1.sendGameStart(room, invite.getFromUserId());
        }

        WebSocketHandler handler2 = connectedClients.get(userId);
        if (handler2 != null) {
            handler2.sendGameStart(room, userId);
        }

        return true;
    }
    //отклонить приглашение
    public void rejectInvite(int userId) {
        PendingInvite invite = pendingInvites.remove(userId);
        if (invite != null) {
            sendToUser(invite.getFromUserId(), "{\"type\":\"invite_rejected\",\"message\":\"Игрок отклонил приглашение\"}");
            System.out.println(" Приглашение отклонено: userId=" + userId);
        }
    }
    //обработка ответа на вопрос
    public void handleAnswer(int userId, String roomId, String answer) {
        GameRoom room = activeRooms.get(roomId);
        if (room == null) {
            System.out.println(" Ошибка: комната не найдена " + roomId);
            return;
        }

        boolean correct = room.submitAnswer(userId, answer);

        sendToUser(userId, String.format("{\"type\":\"answer_result\",\"correct\":%b}", correct));

        String progressUpdate = String.format(
                "{\"type\":\"progress_update\",\"player1Correct\":%d,\"player2Correct\":%d,\"player1Answered\":%d,\"player2Answered\":%d}",
                room.getPlayer1Correct(), room.getPlayer2Correct(),
                room.getPlayerAnswered(room.getPlayer1Id()),
                room.getPlayer2Id() != -1 ? room.getPlayerAnswered(room.getPlayer2Id()) : 0
        );

        for (int playerId : room.getAllPlayers()) {
            sendToUser(playerId, progressUpdate);
        }

        if (room.bothFinished()) {
            int winnerId = room.getWinner();

            if (winnerId == -1) {
                String tieMsg = "{\"type\":\"game_over\",\"winnerId\":-1,\"winnerUsername\":\"Ничья\"}";
                for (int playerId : room.getAllPlayers()) {
                    sendToUser(playerId, tieMsg);
                }
                System.out.println(" Игра окончена! НИЧЬЯ!");
            } else {
                String winnerName = room.getPlayerName(winnerId);
                String gameOverMsg = String.format("{\"type\":\"game_over\",\"winnerId\":%d,\"winnerUsername\":\"%s\"}", winnerId, winnerName);
                for (int playerId : room.getAllPlayers()) {
                    sendToUser(playerId, gameOverMsg);
                }
                UserDAO.addTrophy(winnerId);
                System.out.println(" Игра окончена! Победитель: " + winnerName);
            }
            activeRooms.remove(roomId);
        }
    }
    //генерация id для игровой комнаты
    private String generateRoomId() {
        return "ROOM_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    //генерация id для приглашения
    private String generateInviteId() {
        return "INV_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    //внутренний класс для приглашений
    private static class PendingInvite {
        private String inviteId;
        private int fromUserId;
        private String fromUsername;
        private int toUserId;
        private int difficulty;

        public PendingInvite(String inviteId, int fromUserId, String fromUsername, int toUserId, int difficulty) {
            this.inviteId = inviteId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.toUserId = toUserId;
            this.difficulty = difficulty;
        }

        public String getInviteId() { return inviteId; }
        public int getFromUserId() { return fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public int getToUserId() { return toUserId; }
        public int getDifficulty() { return difficulty; }
    }
    public void removeRoom(String roomId) {
        activeRooms.remove(roomId);
        System.out.println("Комната " + roomId + " удалена");
    }
}