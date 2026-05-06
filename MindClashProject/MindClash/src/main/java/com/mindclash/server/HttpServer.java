/**
 * HTTP сервер для обработки:
 * - GET запросов
 * - POST запросов
 * Работает на порту 8080, использует пул из 10 потоков для подключений.
 */

package com.mindclash.server;

import com.mindclash.database.UserDAO;
import com.mindclash.model.Level;
import com.mindclash.model.User;
import com.mindclash.utils.FileUtils;
import com.mindclash.utils.HttpResponseBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {

    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private boolean running;
    private ExecutorService threadPool;

    //запуск сервера, прием подключений
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            threadPool = Executors.newFixedThreadPool(10);


            System.out.println("   в браузере: http://localhost:" + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println(" Ошибка HTTP сервера: " + e.getMessage());
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
    //обход некоторых символов (чтобы программа не вылетела при таком символе в сообщении)
    private static String escapeJsonForApi(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    //обработка тренировочного режима
    private static  String handleSoloApi(String path) {
        try {
            String mode = "mixed";
            int count = 8;

            String[] parts = path.split("/");
            if (parts.length >= 4) {
                String modePart = parts[3];
                if (modePart.contains("?")) {
                    mode = modePart.split("\\?")[0];
                } else {
                    mode = modePart;
                }
            }

            if (path.contains("count=")) {
                String countStr = path.substring(path.indexOf("count=") + 6);
                if (countStr.contains("&")) {
                    countStr = countStr.split("&")[0];
                }
                count = Integer.parseInt(countStr);
            }

            List<Level> levels = com.mindclash.database.LevelDAO.getSoloLevels(mode, count);

            StringBuilder json = new StringBuilder();
            json.append("{\"levels\":[");
            for (int i = 0; i < levels.size(); i++) {
                if (i > 0) json.append(",");
                com.mindclash.model.Level l = levels.get(i);
                json.append("{");
                json.append("\"type\":\"").append(l.getType()).append("\",");
                json.append("\"question\":\"").append(escapeJsonForApi(l.getQuestion())).append("\",");
                json.append("\"correct_answer\":\"").append(l.getCorrectAnswer()).append("\"");
                if (l.getDisplayTime() != null) {
                    json.append(",\"display_time\":").append(l.getDisplayTime());
                }
                if (l.getGridSize() != null) {
                    json.append(",\"grid_size\":").append(l.getGridSize());
                }
                json.append("}");
            }
            json.append("]}");

            return new HttpResponseBuilder()
                    .setBody(json.toString(), "application/json; charset=utf-8")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponseBuilder.serverError("Ошибка загрузки уровней");
        }

    }

    //класс для обработки действий клиента
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    InputStream in = clientSocket.getInputStream();
                    OutputStream out = clientSocket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in))
            ) {
                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isEmpty()) {
                    return;
                }

                String response = handleRequest(requestLine, reader);
                out.write(response.getBytes());
                out.flush();

            } catch (IOException e) {
                System.err.println(" Ошибка обработки клиента: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {

                }
            }
        }
        //обработка регистрации
        private String handleRequest(String requestLine, BufferedReader reader) throws IOException {
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return HttpResponseBuilder.badRequest("Неверный формат запроса");
            }

            String method = parts[0];
            String path = parts[1];

            System.out.println("   Запрос: " + method + " " + path);

            if (method.equals("POST")) {
                return handlePost(path, reader);
            }

            if (method.equals("GET")) {
                return handleGet(path);
            }

            return HttpResponseBuilder.badRequest("Метод не поддерживается");
        }
        //обработка post-запроса
        private String handlePost(String path, BufferedReader reader) throws IOException {
            String line;
            int contentLength = 0;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }

            char[] bodyChars = new char[contentLength];
            reader.read(bodyChars);
            String body = new String(bodyChars);

            System.out.println("   Тело POST: " + body);

            Map<String, String> params = parseUrlEncoded(body);

            if (path.equals("/register")) {
                return handleRegister(params);
            } else if (path.equals("/login")) {
                return handleLogin(params);
            } else {
                return HttpResponseBuilder.notFound();
            }
        }
        //обработка get-запроса
        private String handleGet(String path) {
            String filePath;
            if (path.startsWith("/api/solo/")) {
                return handleSoloApi(path);
            }
            if (path.equals("/") || path.equals("/index.html")) {
                filePath = "index.html";
            } else if (path.equals("/websocket-test") || path.equals("/websocket-test.html")) {
                filePath = "websocket-test.html";
            } else if (path.startsWith("/")) {
                filePath = path.substring(1);
            } else {
                filePath = path;
            }

            if (filePath.contains("?")) {
                filePath = filePath.substring(0, filePath.indexOf("?"));
            }

            if (!FileUtils.fileExists(filePath)) {
                return HttpResponseBuilder.notFound();
            }

            String content = FileUtils.readWebFile(filePath);
            if (content == null) {
                return HttpResponseBuilder.notFound();
            }

            String mimeType = FileUtils.getMimeType(filePath);
            return new HttpResponseBuilder()
                    .setBody(content, mimeType)
                    .build();
        }
        //парсинг данных из формы
        private Map<String, String> parseUrlEncoded(String body) {
            Map<String, String> params = new HashMap<>();
            if (body == null || body.isEmpty()) return params;

            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8");
                        params.put(key, value);
                    } catch (Exception e) {
                        System.err.println("Ошибка декодирования: " + e.getMessage());
                    }
                }
            }
            return params;
        }
        //обработка регистрации
        private String handleRegister(Map<String, String> params) {
            String username = params.get("username");
            String password = params.get("password");
            String avatar = params.get("avatar");

            if (username == null || password == null) {
                return HttpResponseBuilder.badRequest("Логин и пароль обязательны");
            }

            boolean success = UserDAO.registerUser(username, password, avatar);

            if (success) {
                String html = "<html><body>" +
                        "<script>" +

                        "window.location='/login.html';" +
                        "</script></body></html>";
                return new HttpResponseBuilder().setHtmlBody(html).build();
            } else {
                String html = "<html><body>" +
                        "<script>" +
                        "alert(' Ошибка регистрации: логин уже занят');" +
                        "window.location='/register.html';" +
                        "</script></body></html>";
                return new HttpResponseBuilder().setHtmlBody(html).build();
            }
        }
        //обработка авторизации
        private String handleLogin(Map<String, String> params) {
            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                return HttpResponseBuilder.badRequest("Логин и пароль обязательны");
            }

            User user = UserDAO.loginUser(username, password);

            if (user != null) {

                String html = "<html><body>" +
                        "<script>" +
                        "localStorage.setItem('userId', '" + user.getId() + "');" +
                        "localStorage.setItem('username', '" + user.getUsername() + "');" +
                        "localStorage.setItem('password', '" + password + "');" +
                        "localStorage.setItem('avatar', '" + user.getAvatar() + "');" +
                        "localStorage.setItem('trophies', '" + user.getTrophies() + "');" +

                        "window.location='/game.html';" +
                        "</script></body></html>";
                return new HttpResponseBuilder().setHtmlBody(html).build();
            } else {
                String html = "<html><body>" +
                        "<script>" +

                        "window.location='/login.html';" +
                        "</script></body></html>";
                return new HttpResponseBuilder().setHtmlBody(html).build();
            }
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.start();
    }
}