import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/atm_db";
    private static final String USER = "root";
    private static final String PASS = ""; // Replace with actual MySQL root password

    public void createDatabaseAndTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            // Create database if it doesn't exist
            stmt.execute("CREATE DATABASE IF NOT EXISTS atm_db");

            // Switch to the newly created database
            stmt.execute("USE atm_db");

            // Set auto_increment increment to 1
            stmt.execute("SET @auto_increment = 1;");

            // Create users table
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "balance DECIMAL(10, 2) DEFAULT 0.00)";


            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();

            System.out.println("Users table created successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public boolean signUp(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String hashedPassword = hashPassword(password);

            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();

            // Get the generated ID
            ResultSet rs = pstmt.getGeneratedKeys();
            long id = 0;
            if (rs.next()) {
                id = rs.getLong(1);
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Error signing up user: " + e.getMessage());
            return false;
        }
    }



    public void deposit(String username, double amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, amount);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("User not found or insufficient funds");
            }
        }
    }

    public double getBalance(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT balance FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            }
            throw new SQLException("User not found");
        }
    }

    public boolean withdraw(String username, double amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT balance FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");

                if (currentBalance >= amount) {
                    sql = "UPDATE users SET balance = balance - ? WHERE username = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setDouble(1, amount);
                    pstmt.setString(2, username);
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected == 0) {
                        throw new SQLException("Insufficient funds");
                    }

                    return true;
                } else {
                    throw new SQLException("Insufficient funds");
                }
            }
            throw new SQLException("User not found");
        }
    }

    public boolean authenticateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");
                String[] parts = storedHashedPassword.split(":");
                String salt = parts[0];
                String passwordWithoutSalt = parts[1];

                System.out.println("Input password: " + password);
                System.out.println("Password without salt: " + passwordWithoutSalt);

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));

                String calculatedHash = salt + ":" + Base64.getEncoder().encodeToString(digest);
                System.out.println("Calculated hashed password: " + calculatedHash);

                return calculatedHash.equals(storedHashedPassword);
            }
            System.out.println("No matching user found for username: " + username);
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    private String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Convert salt to Base64 string
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            // Combine salt with hashed password
            String combined = saltBase64 + ":" + password;

            // Use SHA-256 to hash the combined string
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Combine salt and hash
            return saltBase64 + ":" + Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    public boolean testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Database connection successful!");
            return true;
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            return false;
        }
    }

}
