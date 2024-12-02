import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class ATMServer {

    private static final int PORT = 3000;

    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();

        // تحقق من الاتصال بقاعدة البيانات
        System.out.println("Checking database connection...");
        boolean dbConnected = dbManager.testDatabaseConnection();

        if (!dbConnected) {
            System.err.println("Database connection failed. Shutting down the server.");
            return; // إنهاء الخادم إذا لم يتمكن من الاتصال بقاعدة البيانات
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            // حلقة لاستقبال الاتصالات من العملاء
            while (true) {
                Socket clientSocket = serverSocket.accept(); // قبول اتصال جديد
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // إنشاء خيط لمعالجة العميل
                ClientHandler handler = new ClientHandler(clientSocket, dbManager);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
