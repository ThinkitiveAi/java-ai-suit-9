package com.example.session_demo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
        ReflectionTestUtils.setField(passwordUtil, "saltRounds", 12);
    }

    @Test
    void hashPassword_ValidPassword_ReturnsHashedPassword() {
        // Arrange
        String plainPassword = "SecurePassword123!";

        // Act
        String hashedPassword = passwordUtil.hashPassword(plainPassword);

        // Assert
        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void hashPassword_NullPassword_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.hashPassword(null);
        });
    }

    @Test
    void hashPassword_EmptyPassword_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.hashPassword("");
        });
    }

    @Test
    void verifyPassword_CorrectPassword_ReturnsTrue() {
        // Arrange
        String plainPassword = "SecurePassword123!";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);

        // Act
        boolean result = passwordUtil.verifyPassword(plainPassword, hashedPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void verifyPassword_IncorrectPassword_ReturnsFalse() {
        // Arrange
        String plainPassword = "SecurePassword123!";
        String wrongPassword = "WrongPassword123!";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);

        // Act
        boolean result = passwordUtil.verifyPassword(wrongPassword, hashedPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyPassword_NullPassword_ReturnsFalse() {
        // Arrange
        String hashedPassword = passwordUtil.hashPassword("SecurePassword123!");

        // Act
        boolean result = passwordUtil.verifyPassword(null, hashedPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidPassword_ValidPassword_ReturnsTrue() {
        // Arrange
        String validPassword = "SecurePassword123!";

        // Act
        boolean result = passwordUtil.isValidPassword(validPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void isValidPassword_NoUppercase_ReturnsFalse() {
        // Arrange
        String invalidPassword = "securepassword123!";

        // Act
        boolean result = passwordUtil.isValidPassword(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidPassword_NoLowercase_ReturnsFalse() {
        // Arrange
        String invalidPassword = "SECUREPASSWORD123!";

        // Act
        boolean result = passwordUtil.isValidPassword(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidPassword_NoNumber_ReturnsFalse() {
        // Arrange
        String invalidPassword = "SecurePassword!";

        // Act
        boolean result = passwordUtil.isValidPassword(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidPassword_NoSpecialCharacter_ReturnsFalse() {
        // Arrange
        String invalidPassword = "SecurePassword123";

        // Act
        boolean result = passwordUtil.isValidPassword(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidPassword_TooShort_ReturnsFalse() {
        // Arrange
        String invalidPassword = "Sec1!";

        // Act
        boolean result = passwordUtil.isValidPassword(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void generateSecureToken_ReturnsUniqueTokens() {
        // Act
        String token1 = passwordUtil.generateSecureToken();
        String token2 = passwordUtil.generateSecureToken();

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertEquals(64, token1.length()); // 32 bytes * 2 hex chars per byte
        assertEquals(64, token2.length());
    }

    @Test
    void passwordsMatch_SamePasswords_ReturnsTrue() {
        // Arrange
        String password = "SecurePassword123!";
        String confirmPassword = "SecurePassword123!";

        // Act
        boolean result = passwordUtil.passwordsMatch(password, confirmPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void passwordsMatch_DifferentPasswords_ReturnsFalse() {
        // Arrange
        String password = "SecurePassword123!";
        String confirmPassword = "DifferentPassword123!";

        // Act
        boolean result = passwordUtil.passwordsMatch(password, confirmPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void passwordsMatch_NullPassword_ReturnsFalse() {
        // Arrange
        String confirmPassword = "SecurePassword123!";

        // Act
        boolean result = passwordUtil.passwordsMatch(null, confirmPassword);

        // Assert
        assertFalse(result);
    }
} 