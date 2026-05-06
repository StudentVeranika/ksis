/**
 * Класс обработчик одного WebSocket-соединения.
 * Реализует:
 * - WebSocket handshake
 * - Приём и декодирование WebSocket-фреймов
 * - Отправку закодированных фреймов
 * - Маршрутизацию сообщений по типу
 */

package com.mindclash.server;

import com.mindclash.database.UserDAO;
import com.mindclash.model.User;
import com.mindclash.utils.WebSocketFrame;
import java.io.*;
import java.net.Socket;

public class WebSocketHandler implements Runnable {

    private Socket socket;
    private WebSocketServer server;
    private InputStream in;
    private OutputStream out;
    private Integer userId;
    private boolean handshakeDone;
    private boolean running;

    public WebSocketHandler(Socket socket, WebSocketServer server) {
        this.socket = socket;
        this.server = server;
        this.handshakeDone = false;
        this.running = true;
    }
    //обработка соединения
    @Override
    public void run() {
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();

            while (running) {
                if (!handshakeDone) {
                    if (!performHandshake()) {
                        break;
                    }
                    handshakeDone = true;
                } else {
                    String message = WebSocketFrame.decodeTextFrame(in);
                    if (message == null) {
                        break;
                    }
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка соединения: " + e.getMessage());
        } finally {
            close();
        }
    }
    //выполнение рукопожатия
    private boolean performHandshake() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        String websocketKey = null;

        line = reader.readLine();
        if (line == null || line.isEmpty()) {
            return false;
        }

        while (!(line = reader.readLine()).isEmpty()) {
            if (line.startsWith("Sec-WebSocket-Key:")) {
                websocketKey = line.substring(18).trim();
            }
        }

        if (websocketKey == null) {
            return false;
        }

        String acceptKey = WebSocketFrame.generateAcceptKey(websocketKey);

        PrintWriter writer = new PrintWriter(out);
        writer.print("HTTP/1.1 101 Switching Protocols\r\n");
        writer.print("Upgrade: websocket\r\n");
        writer.print("Connection: Upgrade\r\n");
        writer.print("Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n");
        writer.flush();

        return true;
    }
    //обработка входящих сообщений
    private void handleMessage(String message) {
        System.out.println(" Получено: " + message);

        if (message.contains("\"type\":\"login\"")) {
            handleLogin(message);
        } else if (message.contains("\"type\":\"invite\"")) {
            handleInvite(message);
        } else if (message.contains("\"type\":\"accept_invite\"")) {
            handleAcceptInvite(message);
        } else if (message.contains("\"type\":\"reject_invite\"")) {
            handleRejectInvite(message);

        }
        else if (message.contains("\"type\":\"leave_game\"")) {
            handleLeaveGame(message);
        }
        else if (message.contains("\"type\":\"answer\"")) {
            handleAnswer(message);
        } else if (message.contains("\"type\":\"ping\"")) {
            sendMessage("{\"type\":\"pong\"}");
        } else if (message.contains("\"type\":\"update_profile\"")) {
            handleUpdateProfile(message);
        } else if (message.contains("\"type\":\"create_room\"")) {
            handleCreateRoom(message);
        } else if (message.contains("\"type\":\"join_room\"")) {
            handleJoinRoom(message);
        } else {
            sendMessage("{\"type\":\"error\",\"error\":\"Неизвестный тип сообщения\"}");
        }
    }
    //обработка авторизации
    private void handleLogin(String message) {
        try {
            String username = extractValue(message, "username");
            String password = extractValue(message, "password");

            System.out.println("   WebSocket логин: " + username + ", пароль: " + password);

            User user = UserDAO.loginUser(username, password);

            if (user != null) {
                this.userId = user.getId();
                server.registerClient(userId, this);
                sendMessage("{\"type\":\"login_success\",\"userId\":" + userId + ",\"username\":\"" + username + "\",\"trophies\":" + user.getTrophies() + "}");
                System.out.println("   WebSocket авторизация успешна: " + username);
            } else {
                sendMessage("{\"type\":\"login_error\",\"error\":\"Неверный логин или пароль\"}");
                System.out.println("   WebSocket ошибка авторизации: " + username);
            }
        } catch (Exception e) {
            System.out.println("   Ошибка авторизации: " + e.getMessage());
            sendMessage("{\"type\":\"error\",\"error\":\"Ошибка авторизации\"}");
        }
    }
    //обработка отправки приглашения
    private void handleInvite(String message) {
        if (userId == null) {
            sendMessage("{\"type\":\"error\",\"error\":\"Не авторизован\"}");
            return;
        }

        String toUsername = extractValue(message, "toUsername");
        int difficulty = Integer.parseInt(extractValue(message, "difficulty"));

        User toUser = UserDAO.getUserByUsername(toUsername);
        if (toUser == null) {
            sendMessage("{\"type\":\"invite_error\",\"error\":\"Пользователь не найден\"}");
            return;
        }

        boolean success = server.sendInvite(userId, UserDAO.getUserById(userId).getUsername(), toUser.getId(), difficulty);
        if (success) {
            sendMessage("{\"type\":\"invite_sent\",\"toUsername\":\"" + toUsername + "\"}");
        } else {
            sendMessage("{\"type\":\"invite_error\",\"error\":\"Пользователь не в сети\"}");
        }
    }
    //обработка покидания комнаты
    private void handleLeaveGame(String message) {
        if (userId == null) {
            sendMessage("{\"type\":\"error\",\"error\":\"Не авторизован\"}");
            return;
        }

        String roomId = extractValue(message, "roomId");
        GameRoom room = server.getRoom(roomId);

        if (room != null && room.hasPlayer(userId)) {
            int opponentId = room.getOpponentId(userId);

            if (opponentId != -1) {
                server.sendToUser(opponentId, "{\"type\":\"opponent_left\",\"message\":\"Соперник покинул игру\"}");
            }

            server.removeRoom(roomId);
            System.out.println("Игрок " + userId + " покинул комнату " + roomId);
        }
    }
    //обработка редактирования профиля
    private void handleUpdateProfile(String message) {
        if (userId == null) {
            sendMessage("{\"type\":\"profile_updated\",\"success\":false,\"error\":\"Не авторизован\"}");
            return;
        }

        String password = extractValue(message, "password");
        String avatar = extractValue(message, "avatar");

        System.out.println("   Обновление профиля для userId=" + userId + ", password=" + (password != null ? "changed" : "none") + ", avatar=" + avatar);

        if (password != null && !password.isEmpty()) {
            UserDAO.updatePassword(userId, password);
            sendMessage("{\"type\":\"profile_updated\",\"success\":true,\"password\":\"" + password + "\"}");
        } else if (avatar != null && !avatar.isEmpty()) {
            UserDAO.updateAvatar(userId, avatar);
            sendMessage("{\"type\":\"profile_updated\",\"success\":true,\"avatar\":\"" + avatar + "\"}");
        } else {
            sendMessage("{\"type\":\"profile_updated\",\"success\":false,\"error\":\"Нет данных для обновления\"}");
        }
    }
    //обработка принятия приглашения
    private void handleAcceptInvite(String message) {
        if (userId == null) return;
        String inviteId = extractValue(message, "inviteId");
        server.acceptInvite(userId, inviteId);
    }
    //обработка отклонения приглашения
    private void handleRejectInvite(String message) {
        if (userId == null) return;
        server.rejectInvite(userId);
    }

    //обработка ответа на вопрос
    private void handleAnswer(String message) {
        if (userId == null) {
            sendMessage("{\"type\":\"error\",\"error\":\"Не авторизован\"}");
            return;
        }

        String roomId = extractValue(message, "roomId");
        String answer = extractValue(message, "answer");

        if (roomId == null) {
            sendMessage("{\"type\":\"error\",\"error\":\"Неверный формат ответа: нет roomId\"}");
            return;
        }

        System.out.println("   Обработка ответа от пользователя " + userId + " в комнате " + roomId + ": " + answer);
        server.handleAnswer(userId, roomId, answer);
    }
    //обработка создания комнаты
    private void handleCreateRoom(String message) {
        if (userId == null) return;
        int difficulty = Integer.parseInt(extractValue(message, "difficulty"));
        String roomId = server.createRoom(userId, UserDAO.getUserById(userId).getUsername(), difficulty);
        sendMessage("{\"type\":\"room_created\",\"roomId\":\"" + roomId + "\"}");
    }
    //обработка присоединения в комнату
    private void handleJoinRoom(String message) {
        if (userId == null) return;
        String roomId = extractValue(message, "roomId");
        boolean success = server.joinRoom(roomId, userId, UserDAO.getUserById(userId).getUsername());
        if (success) {
            sendMessage("{\"type\":\"room_joined\",\"roomId\":\"" + roomId + "\"}");
        } else {
            sendMessage("{\"type\":\"error\",\"error\":\"Не удалось присоединиться к комнате\"}");
        }
    }
    //отправка сообщения о начале игры + список уровней
    public void sendGameStart(GameRoom room, int targetUserId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"game_start\",");
        sb.append("\"roomId\":\"").append(escapeJson(room.getRoomId())).append("\",");
        sb.append("\"totalLevels\":").append(room.getLevelsCount()).append(",");
        sb.append("\"player1Id\":").append(room.getPlayer1Id()).append(",");
        sb.append("\"player2Id\":").append(room.getPlayer2Id()).append(",");
        sb.append("\"levels\":").append(room.getLevelsJsonString());

        int opponentId = room.getOpponentId(targetUserId);
        if (opponentId != -1 && room.getPlayerName(opponentId) != null) {
            sb.append(",\"opponentUsername\":\"").append(escapeJson(room.getPlayerName(opponentId))).append("\"");
        }

        sb.append("}");
        sendMessage(sb.toString());
        System.out.println("Отправлен game_start пользователю " + targetUserId);
    }
    //оюход некоторых символом
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            searchKey = "\"" + key + "\":";
            start = json.indexOf(searchKey);
            if (start == -1) return null;
            start += searchKey.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String value = json.substring(start, end).trim();
            return value.replace("\"", "");
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
    //отправка сообщения
    public void sendMessage(String message) {
        try {
            byte[] frame = WebSocketFrame.encodeTextFrame(message);
            if (frame != null && frame.length > 0) {
                out.write(frame);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Ошибка отправки: " + e.getMessage());
        }
    }
    //закрытие соединения
    private void close() {
        running = false;
        if (userId != null) {
            server.removeClient(userId);
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}