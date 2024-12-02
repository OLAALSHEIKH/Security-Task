import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public  class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final DatabaseManager dbManager; // Database manager instance

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        System.err.println(0);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Welcome message
            out.println("Welcome to the ATM server!");

            String request;
            while ((request = in.readLine()) != null) {
                System.err.println(0);
                System.out.println("Received from client: " + request);

                // Handle client requests
                String serverResponse = handleRequest(in, out, request);

                if ("exit".equalsIgnoreCase(request)) {
                    break; // Exit the loop when exit command is received
                }

                // Print the result on the server side
                System.out.println("Server response: " + serverResponse);

                // Send the result back to the client
                out.println(serverResponse);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        }
    }

    private String handleRequest(BufferedReader in, PrintWriter out, String request) throws IOException {
        switch (request.toLowerCase()) {
            case "signup":
                return handleSignup(in, out);
            case "login":
                return handleLogin(in, out);
            default:
                return "Unknown command! Use signup, login, or exit.";
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

            return isAuthenticated ? "Login successful!" : "Login failed. Invalid credentials.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during login: " + e.getMessage();
        }
    }
}
