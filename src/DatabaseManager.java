
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.AlgorithmConstraints;
import java.sql.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/atmTask19_db";
    private static final String USER = "root";
    private static final String PASS = ""; // Replace with actual MySQL root password




    public void createDatabaseAndTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            // Create database if it doesn't exist
            stmt.execute("CREATE DATABASE IF NOT EXISTS atmTask19_db");

            // Switch to the newly created database
            stmt.execute("USE atmTask19_db");

            // Set auto_increment increment to 1
            stmt.execute("SET @auto_increment = 1;");

            // Create users table
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "encrypted_balance VARCHAR(255) )";


            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();

            System.out.println("Users table created successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public void deposit(String username, String amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Algorithms rsa = new Algorithms();

            // Generate keys if they don't exist
            if (rsa.publicKey == null || rsa.privateKey == null) {
                rsa.generateKeys(2048);
            }

            // Encrypt only the amount, not the entire balance
            String encryptedAmount = rsa.encrypt(amount);
            System.out.println("Decrypted amount: " + encryptedAmount);
            // Decrypt the amount (for verification)
            String decryptedAmount = rsa.decrypt(encryptedAmount);
            System.out.println("Decrypted amount: " + decryptedAmount);

            double amountAsDouble = Double.parseDouble(decryptedAmount);

            String sql = "UPDATE users SET encrypted_balance = ? WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);



            // Encrypt new balance using AES
            String encryptedNewBalance = Algorithms.encrypt(String.valueOf(amountAsDouble), ATMServer.key, ATMServer.iv);
            System.out.println(encryptedNewBalance);
            String encryptedNew= Algorithms.decrypt(String.valueOf(encryptedNewBalance), ATMServer.key, ATMServer.iv);
            System.out.println(encryptedNew);
            // Set the encrypted amount as a blob
            pstmt.setString(1, encryptedNewBalance);

            // Set the username as a string
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("User not found");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    public String getBalance(String username) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {

            String sql = "SELECT encrypted_balance FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String encryptedBalance = rs.getString("encrypted_balance");


                try {
                    return Algorithms.decrypt(String.valueOf(encryptedBalance), ATMServer.key, ATMServer.iv);  // Return the decrypted balance as a string
                } catch (Exception e) {
                    throw new SQLException("Failed to decrypt balance", e);
                }
            }

            throw new SQLException("User not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }









    public boolean withdraw(String username, String amount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Algorithms rsa = new Algorithms();

            // Generate keys if they don't exist
            if (rsa.publicKey == null || rsa.privateKey == null) {
                rsa.generateKeys(2048);
            }

            // Encrypt only the amount, not the entire balance
            String encryptedAmount = rsa.encrypt(amount);
            System.out.println("Decrypted amount: " + encryptedAmount);
            // Decrypt the amount (for verification)
            String decryptedAmount = rsa.decrypt(encryptedAmount);
            System.out.println("Decrypted amount: " + decryptedAmount);

            double amountAsDouble = Double.parseDouble(decryptedAmount);

            String sql = "UPDATE users SET encrypted_balance = ? WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);


            // Encrypt new balance using AES
            String encryptedNewBalance = Algorithms.encrypt(String.valueOf(amountAsDouble), ATMServer.key, ATMServer.iv);
            System.out.println(encryptedNewBalance);
            String encryptedNew = Algorithms.decrypt(String.valueOf(encryptedNewBalance), ATMServer.key, ATMServer.iv);
            System.out.println(encryptedNew);
            // Set the encrypted amount as a blob
            pstmt.setString(1, encryptedNewBalance);

            // Set the username as a string
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();

            return  true;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

