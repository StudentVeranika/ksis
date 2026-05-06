package com.mindclash.database;

import com.mindclash.model.User;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass";
    private static final String TEST_AVATAR = "cat.png";

    @BeforeEach
    void setUp() {

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM users WHERE username = '" + TEST_USERNAME + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Test
    void testRegisterNewUser() {

        boolean result = UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);

        assertTrue(result);

        User user = UserDAO.getUserByUsername(TEST_USERNAME);
        assertNotNull(user);
        assertEquals(TEST_USERNAME, user.getUsername());
        assertEquals(TEST_AVATAR, user.getAvatar());
        assertEquals(0, user.getTrophies());
    }

    @Test
    void testRegisterDuplicateUser() {

        UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);

        boolean result = UserDAO.registerUser(TEST_USERNAME, "pass", "dog.png");

        assertFalse(result);
    }


    @Test
    void testLoginWithCorrectData() {

        UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);

        User user = UserDAO.loginUser(TEST_USERNAME, TEST_PASSWORD);

        assertNotNull(user);
        assertEquals(TEST_USERNAME, user.getUsername());
    }

    @Test
    void testLoginWithWrongPassword() {
        UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);

        User user = UserDAO.loginUser(TEST_USERNAME, "wrongpassword");

        assertNull(user);
    }

    @Test
    void testLoginWithNonExistentUser() {
        User user = UserDAO.loginUser("nonexistent", "password");

        assertNull(user);
    }

    @Test
    void testAddTrophy() {
        UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);
        User user = UserDAO.getUserByUsername(TEST_USERNAME);
        assertNotNull(user);
        int initialTrophies = user.getTrophies();

        UserDAO.addTrophy(user.getId());

        User updatedUser = UserDAO.getUserByUsername(TEST_USERNAME);
        assertNotNull(updatedUser);
        assertEquals(initialTrophies + 1, updatedUser.getTrophies());
    }


    @Test
    void testGetUserById() {
        UserDAO.registerUser(TEST_USERNAME, TEST_PASSWORD, TEST_AVATAR);
        User user = UserDAO.getUserByUsername(TEST_USERNAME);
        assertNotNull(user);

        User foundUser = UserDAO.getUserById(user.getId());
        assertNotNull(foundUser);
        assertEquals(TEST_USERNAME, foundUser.getUsername());
    }

    @Test
    void testGetUserByNonExistentId() {
        User user = UserDAO.getUserById(99999);
        assertNull(user);
    }
}