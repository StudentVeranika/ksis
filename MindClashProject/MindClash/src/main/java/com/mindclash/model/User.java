/**
 * Модель пользователя.
 * Хранит данные из таблицы users:
 * - id
 * - username
 * - passwordHash
 * - avatar
 * - trophies
 */

package com.mindclash.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String avatar;
    private int trophies;
    private boolean online;
    private String createdAt;

    public User() {}

    public User(String username, String passwordHash, String avatar) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.avatar = avatar != null ? avatar : "default.png";
        this.trophies = 0;
        this.online = false;
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getTrophies() { return trophies; }
    public void setTrophies(int trophies) { this.trophies = trophies; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', trophies=" + trophies + "}";
    }


    public String toJson() {
        return String.format(
                "{\"id\":%d,\"username\":\"%s\",\"avatar\":\"%s\",\"trophies\":%d,\"online\":%b}",
                id, username, avatar, trophies, online
        );
    }
}