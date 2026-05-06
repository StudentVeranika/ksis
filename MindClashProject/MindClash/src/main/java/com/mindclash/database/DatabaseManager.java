/**
 * Управление подключением к MySQL базе данных
 * При первом запуске создаёт таблицы users и levels.
 * Использует параметры: localhost:3306, user=root, password=root
 */

package com.mindclash.database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/mindclash?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private static Connection connection;
    //инициализация подключения и создание таблиц
    public static void initialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();
            System.out.println(" База данных инициализирована");
        } catch (ClassNotFoundException e) {
            System.err.println(" Драйвер MySQL не найден: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println(" Ошибка подключения к MySQL: " + e.getMessage());
        }
    }
    //создание таблиц
    private static void createTables() throws SQLException {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                avatar VARCHAR(100) DEFAULT 'default.png',
                trophies INT DEFAULT 0,
                online BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;

        String createLevelsTable = """
            CREATE TABLE IF NOT EXISTS levels (
                id INT PRIMARY KEY AUTO_INCREMENT,
                type VARCHAR(50) NOT NULL,
                question TEXT NOT NULL,
                correct_answer VARCHAR(255) NOT NULL,
                image_path VARCHAR(255),
                difficulty INT DEFAULT 1,
                display_time INT DEFAULT NULL COMMENT 'Время показа для memory заданий (в секундах)',
                grid_size INT DEFAULT NULL COMMENT 'Размер сетки для schulte заданий (2,3,4)'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createLevelsTable);
            System.out.println(" Таблицы созданы");
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            System.err.println(" Ошибка проверки соединения: " + e.getMessage());
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(" Соединение с БД закрыто");
            }
        } catch (SQLException e) {
            System.err.println(" Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}