package bank;

import java.sql.*;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class SecureBank {
    private static final String DB_URL = "jdbc:sqlite:bank.db";
    private static final Logger logger = Logger.getLogger(SecureBank.class.getName());

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z]{3,15}[.][a-z]{3,15}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("[A-Za-z]+#\\d+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\S+@\\S+\\.\\S+");

    // Initializes the SQLite database and creates the users table if it doesn't exist
    public void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT, " +
                    "salt TEXT, " +
                    "balance REAL DEFAULT 0)";
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            logger.info("DB error: " + e.getMessage());
        }
    }

    // Validates the format of the username (e.g., first.last)
    public boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    // Validates the password format (e.g., must include letters, #, and numbers)
    public boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // Validates email format using a basic regex
    public boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // Registers a new user with hashed password and salt
    public void registerUser(String username, String password) throws RegistrationException {
        String salt = generateSalt();
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (Exception e) {
            throw new RegistrationException("Password hashing failed.");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users(username, password, salt) VALUES (?, ?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, salt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Registration error: " + e.getMessage());
            throw new RegistrationException("Database error during registration.");
        }
    }

    // Authenticates a user by comparing hashed input with stored hash
    public boolean authenticateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT password, salt FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        String salt = rs.getString("salt");
                        String inputHash = hashPassword(password, salt);
                        return storedHash.equals(inputHash);
                    }
                }
            }
        } catch (SQLException | InvalidKeySpecException e) {
            logger.warning("Authentication failed for user: " + username + " — " + e.getMessage());
        }
        return false;
    }

    // Returns the user's current account balance from the database
    public double getBalance(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT balance FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to get balance for " + username + ": " + e.getMessage());
        }
        return 0.0;
    }

    // Adds a specified amount to the user's balance
    public boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setString(2, username);
                return pstmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            logger.warning("Deposit failed: " + e.getMessage());
        }
        return false;
    }

    // Withdraws a specified amount from the user's balance if enough funds exist
    public boolean withdraw(String username, double amount) {
        if (amount <= 0 || amount > getBalance(username)) return false;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "UPDATE users SET balance = balance - ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setString(2, username);
                return pstmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            logger.warning("Withdrawal failed: " + e.getMessage());
        }
        return false;
    }

    // Generates a secure random salt and encodes it in Base64
    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Hashes a password using PBKDF2 with the given salt
    private String hashPassword(String password, String salt) throws InvalidKeySpecException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new InvalidKeySpecException("Hashing failed");
        }
    }
}
