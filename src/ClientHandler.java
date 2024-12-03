
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final DatabaseManager dbManager; // Database manager instance

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Send welcome message
            out.println("Welcome to the ATM server!");

            boolean keepRunning = true;
            while (keepRunning) {
                out.println("\n1. Sign Up");
                out.println("2. Login");
                out.println("3. Exit");
                out.print("Enter your choice: ");

                String request = in.readLine();
                if (request == null) break;

                // Handle client requests
                String serverResponse = handleRequest(in, out, request);

                if ("exit".equalsIgnoreCase(request)) {
                    break; // Exit the loop when "exit" command is received
                }

                // Send the result back to the client
                out.println(serverResponse);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        }
    }

    private String handleRequest(BufferedReader in, PrintWriter out, String request) throws IOException {
        switch (request.toLowerCase()) {
            case "1": // Sign Up
                return handleSignup(in, out);
            case "2": // Login
                return handleLogin(in, out);
            case "3": // Exit
                return "Goodbye!";
            default:
                return "Unknown command! Please choose a valid option.";
        }
    }

    private String handleSignup(BufferedReader in, PrintWriter out) throws IOException {
        try {
            out.println("Enter username:");
            String username = in.readLine();

            out.println("Enter password:");
            String password = in.readLine();

            boolean success = dbManager.signUp(username, password);

            return success ? "Signup successful!" : "Signup failed. Username may already exist.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during signup: " + e.getMessage();
        }
    }

    private String handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        try {
            out.println("Enter username:");
            String username = in.readLine();

            out.println("Enter password:");
            String password = in.readLine();

            boolean isAuthenticated = dbManager.authenticateUser(username, password);

            if (isAuthenticated) {
                out.println("Login successful!");
                // Start banking process after login
                return bankingProcess(username, in, out);
            } else {
                return "Login failed. Invalid credentials.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during login: " + e.getMessage();
        }
    }

    // Banking process for after login
    private String bankingProcess(String username, BufferedReader in, PrintWriter out) throws IOException, SQLException {
        while (true) {
            out.println("\n1. Deposit");
            out.println("2. Get Balance");
            out.println("3. Withdraw");
            out.println("4. Exit");

            out.print("Choose an option: ");
            String choice = in.readLine();

            switch (choice) {
                case "1":
                    out.print("Enter amount to deposit: ");
                    double depositAmount = Double.parseDouble(in.readLine());
                    String amountAsString = String.valueOf(depositAmount);
                    try {
                        dbManager.deposit(username, amountAsString);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    out.println("Deposit successful!");
                    break;
                case "2":
                    String balance = dbManager.getBalance(username);
                    out.println("Your current balance is: $" + balance);
                    break;
                case "3":
                    out.print("Enter amount to withdraw: ");
                    double withdrawAmount = Double.parseDouble(in.readLine());
                    String amountAsString1 = String.valueOf(withdrawAmount);
                    boolean success = dbManager.withdraw(username, amountAsString1);
                    if (success) {
                        out.println("Withdrawal successful!");
                    } else {
                        out.println("Insufficient funds or error.");
                    }
                    break;
                case "4":
                    out.println("Exiting...");
                    return "Exiting banking operations."; // Exit the banking process
                default:
                    out.println("Invalid choice. Please choose again.");
            }
        }
    }
}