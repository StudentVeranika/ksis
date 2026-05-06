/**
 * Data Access Object для таблицы levels.
 * Выполняет операции с уровнями в базе данных.
 */

package com.mindclash.database;

import com.mindclash.model.Level;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelDAO {
    //получение уровней опр.сложности
    public static List<Level> getLevelsByDifficulty(int difficulty, int totalCount) {
        List<Level> allLevels = new ArrayList<>();


        int requiredPuzzle = 2;
        int requiredMemory = 2;
        int requiredSchulte = 2;
        int requiredStroop = 4;


        String sql = "SELECT id, type, question, correct_answer, image_path, difficulty, display_time, grid_size FROM levels WHERE difficulty = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, difficulty);
            ResultSet rs = pstmt.executeQuery();


            List<Level> puzzles = new ArrayList<>();
            List<Level> memories = new ArrayList<>();
            List<Level> schultes = new ArrayList<>();
            List<Level> stroops = new ArrayList<>();

            while (rs.next()) {
                Level level = new Level(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("question"),
                        rs.getString("correct_answer"),
                        rs.getString("image_path"),
                        rs.getInt("difficulty"),
                        rs.getObject("display_time") != null ? rs.getInt("display_time") : null,
                        rs.getObject("grid_size") != null ? rs.getInt("grid_size") : null
                );

                switch (level.getType()) {
                    case "puzzle":
                        puzzles.add(level);
                        break;
                    case "memory":
                        memories.add(level);
                        break;
                    case "schulte":
                        schultes.add(level);
                        break;
                    case "stroop":
                        stroops.add(level);
                        break;
                }
            }


            Collections.shuffle(puzzles);
            Collections.shuffle(memories);
            Collections.shuffle(schultes);
            Collections.shuffle(stroops);


            List<Level> selected = new ArrayList<>();
            selected.addAll(puzzles.subList(0, Math.min(requiredPuzzle, puzzles.size())));
            selected.addAll(memories.subList(0, Math.min(requiredMemory, memories.size())));
            selected.addAll(schultes.subList(0, Math.min(requiredSchulte, schultes.size())));
            selected.addAll(stroops.subList(0, Math.min(requiredStroop, stroops.size())));


            if (selected.size() < totalCount) {
                int remaining = totalCount - selected.size();
                List<Level> allRemaining = new ArrayList<>();
                allRemaining.addAll(puzzles);
                allRemaining.addAll(memories);
                allRemaining.addAll(schultes);
                allRemaining.addAll(stroops);
                Collections.shuffle(allRemaining);
                selected.addAll(allRemaining.subList(0, Math.min(remaining, allRemaining.size())));
            }


            Collections.shuffle(selected);

            return selected;

        } catch (SQLException e) {
            System.err.println("Ошибка получения уровней: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    //получение уровней для одиночного режима
    public static List<Level> getSoloLevels(String mode, int count) {
        List<Level> allLevels = new ArrayList<>();
        String sql;

        if (mode.equals("mixed")) {

            sql = "SELECT id, type, question, correct_answer, image_path, difficulty, display_time, grid_size FROM levels WHERE difficulty = 1 ORDER BY RAND() LIMIT ?";
        } else {

            sql = "SELECT id, type, question, correct_answer, image_path, difficulty, display_time, grid_size FROM levels WHERE type = ? AND difficulty = 1 ORDER BY RAND() LIMIT ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (mode.equals("mixed")) {
                pstmt.setInt(1, count);
            } else {
                pstmt.setString(1, mode);
                pstmt.setInt(2, count);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Level level = new Level(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("question"),
                        rs.getString("correct_answer"),
                        rs.getString("image_path"),
                        rs.getInt("difficulty"),
                        rs.getObject("display_time") != null ? rs.getInt("display_time") : null,
                        rs.getObject("grid_size") != null ? rs.getInt("grid_size") : null
                );
                allLevels.add(level);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка получения уровней для соло-режима: " + e.getMessage());
        }

        return allLevels;
    }

    public static boolean addLevel(String type, String question, String correctAnswer,
                                   String imagePath, int difficulty, Integer displayTime, Integer gridSize) {
        String sql = "INSERT INTO levels (type, question, correct_answer, image_path, difficulty, display_time, grid_size) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, type);
            pstmt.setString(2, question);
            pstmt.setString(3, correctAnswer);
            pstmt.setString(4, imagePath);
            pstmt.setInt(5, difficulty);

            if (displayTime != null) {
                pstmt.setInt(6, displayTime);
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }

            if (gridSize != null) {
                pstmt.setInt(7, gridSize);
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(" Ошибка добавления уровня: " + e.getMessage());
            return false;
        }
    }

}