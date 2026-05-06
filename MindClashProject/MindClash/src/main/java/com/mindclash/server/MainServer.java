
/**
 * Главный класс приложения. Точка входа.
 * Запускает HTTP сервер (порт 8080) и WebSocket сервер (порт 8081).
 * Также инициализирует подключение к базе данных.
 */

package com.mindclash.server;

import com.mindclash.database.DatabaseManager;

public class MainServer {

    public static void main(String[] args) {

        System.out.println("запуск сервера");

        DatabaseManager.initialize();

        Thread httpThread = new Thread(() -> {
            HttpServer httpServer = new HttpServer();
            httpServer.start();
        });
        httpThread.start();

        Thread wsThread = new Thread(() -> {
            WebSocketServer wsServer = new WebSocketServer(8081);
            wsServer.start();
        });
        wsThread.start();


        System.out.println(" http://localhost:8080");



        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nОстановка сервера...");
        System.exit(0);
    }
}