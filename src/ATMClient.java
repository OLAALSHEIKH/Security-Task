import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;

public class ATMClient {
    private static DatabaseManager dbm;

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 3000;
        Scanner scanner = new Scanner(System.in);

        try {
            dbm = new DatabaseManager(); // Initialize DatabaseManager here

            Socket socket = new Socket(serverAddress, port);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Welcome message from the server
                System.out.println("Server: " + in.readLine());

                boolean keepRunning = true;
                while (keepRunning) {
                    System.out.println("\n1. Sign Up");
                    System.out.println("2. Login");
                    System.out.print("Enter your choice: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline left-over

                    switch (choice) {
                        case 1:
                            signUpProcess(out, in, scanner);
                            break;
                        case 2:
                            loginProcess(in, out, scanner);
                            break;
                        case 3:
                            keepRunning = false;
                            System.out.println("Exiting...");
                            break;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void signUpProcess(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (dbm.signUp(username, password)) {
            System.out.println("Sign up successful!");
        } else {
            System.out.println("Failed to sign up. Please try again.");
        }
    }

    private static void loginProcess(BufferedReader in, PrintWriter out, Scanner scanner) throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (dbm.authenticateUser(username, password)) {
            System.out.println("Login successful!");

            // After login, show banking options
            bankingProcess(dbm, scanner,username);
        } else {
            System.out.println("Invalid credentials. Please try again.");
        }
    }


    private static void bankingProcess(DatabaseManager dbm, Scanner scanner, String userName) {
        while (true) {
            System.out.println("\n1. Deposit");
            System.out.println("2. Get Balance");
            System.out.println("3. Withdraw");
            System.out.println("4. Exit");

            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline left-over

            switch (choice) {
                case 1:
                    try {
                        System.out.print("Enter amount to deposit: ");
                        double amount = scanner.nextDouble();
                        dbm.deposit(userName, amount);
                        System.out.println("Deposit successful!");
                    } catch (SQLException e) {
                        System.err.println("Error during deposit: " + e.getMessage());
                    }
                    break;
                case 2:
                    try {
                        double balance = dbm.getBalance(userName);
                        System.out.println("Your current balance is: $" + balance);
                    } catch (SQLException e) {
                        System.err.println("Error during balance inquiry: " + e.getMessage());
                    }
                    break;
                case 3:
                    try {
                        System.out.print("Enter amount to withdraw: ");
                        double amountToWithdraw = scanner.nextDouble();
                        boolean success = dbm.withdraw(userName, amountToWithdraw);
                        if (success) {
                            System.out.println("Withdrawal successful!");
                        } else {
                            System.out.println("Withdrawal failed!");
                        }
                    } catch (SQLException e) {
                        System.err.println("Error during withdrawal: " + e.getMessage());
                    }
                    break;
                case 4:
                    return; // Exit the banking process
                default:
                    System.out.println("Invalid choice. Please choose again.");
            }

            System.out.print("Press Enter to continue...");
            scanner.nextLine(); // Consume newline left-over
        }
    }
}
