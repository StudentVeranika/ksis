/**
 * Data Access Object для таблицы users.
 * Выполняет операции с пользователями в базе данных.
 */

package com.mindclash.database;

import com.mindclash.model.User;
import com.mindclash.utils.PasswordUtils;
import java.sql.*;

import static com.mindclash.utils.PasswordUtils.hashPassword;


public class UserDAO {

    //регистрация пользователя в бд
    public static boolean registerUser(String username, String password, String avatar) {
        String hash = hashPassword(password);
        String sql = "INSERT INTO users (username, password_hash, avatar) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.setString(3, avatar != null ? avatar : "default.png");

            int rows = pstmt.executeUpdate();
            System.out.println(" Зарегистрирован пользователь: " + username);
            return rows > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate") || e.getMessage().contains("UNIQUE")) {
                System.err.println("Логин уже занят: " + username);
            } else {
                System.err.println(" Ошибка регистрации: " + e.getMessage());
            }
            return false;
        }
    }

    //авторизация
    public static User loginUser(String username, String password) {
        String hash = hashPassword(password);
        String sql = "SELECT id, username, password_hash, avatar, trophies, online FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (storedHash.equals(hash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(storedHash);
                    user.setAvatar(rs.getString("avatar"));
                    user.setTrophies(rs.getInt("trophies"));
                    user.setOnline(true);


                    updateUserOnlineStatus(user.getId(), true);

                    System.out.println(" Вход пользователя: " + username);
                    return user;
                }
            }

        } catch (SQLException e) {
            System.err.println(" Ошибка входа: " + e.getMessage());
        }

        return null;
    }

    //получение пользователя по id
    public static User getUserById(int userId) {
        String sql = "SELECT id, username, avatar, trophies, online FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setAvatar(rs.getString("avatar"));
                user.setTrophies(rs.getInt("trophies"));
                user.setOnline(rs.getBoolean("online"));
                return user;
            }

        } catch (SQLException e) {
            System.err.println(" Ошибка получения пользователя: " + e.getMessage());
        }

        return null;
    }

    //получение пользователя по имени
    public static User getUserByUsername(String username) {
        String sql = "SELECT id, username, avatar, trophies, online FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setAvatar(rs.getString("avatar"));
                user.setTrophies(rs.getInt("trophies"));
                user.setOnline(rs.getBoolean("online"));
                return user;
            }

        } catch (SQLException e) {
            System.err.println(" Ошибка получения пользователя: " + e.getMessage());
        }

        return null;
    }

    //изменение статуса
    public static void updateUserOnlineStatus(int userId, boolean online) {
        String sql = "UPDATE users SET online = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, online);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println(" Ошибка обновления статуса: " + e.getMessage());
        }
    }

    //добавить трофей при победе
    public static void addTrophy(int userId) {
        String sql = "UPDATE users SET trophies = trophies + 1 WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println(" Кубок добавлен пользователю ID: " + userId);

        } catch (SQLException e) {
            System.err.println(" Ошибка добавления кубка: " + e.getMessage());
        }
    }
    //обновление пароля
    public static void updatePassword(int userId, String newPassword) {
        String hash = hashPassword(newPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            System.out.println("   Пароль обновлён для userId=" + userId);
        } catch (SQLException e) {
            System.err.println(" Ошибка обновления пароля: " + e.getMessage());
        }
    }
    //обновление значка аватара
    public static void updateAvatar(int userId, String avatar) {
        String sql = "UPDATE users SET avatar = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, avatar);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            System.out.println("   Аватар обновлён для userId=" + userId + ": " + avatar);
        } catch (SQLException e) {
            System.err.println(" Ошибка обновления аватара: " + e.getMessage());
        }
    }

}