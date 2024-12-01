import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {


//        DatabaseManager manager = new DatabaseManager();
//        manager.createDatabaseAndTables();

        DatabaseManager dbm = new DatabaseManager();

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("\n1. Sign Up");
            System.out.println("2. Login");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline left-over

            switch(choice) {
                case 1:
                    signUpProcess(dbm, scanner);
                    break;
                case 2:
                    loginProcess(dbm, scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void signUpProcess(DatabaseManager dbm, Scanner scanner) {
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

    private static void loginProcess(DatabaseManager dbm, Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (dbm.authenticate(username, password)) {
            System.out.println("Login successful!");
        } else {
            System.out.println("Invalid credentials. Please try again.");
        }
    }
}
