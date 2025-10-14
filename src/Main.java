import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final Logger logger = Logger.getLogger("Logger");

    public static void main(String[] args) {

        try {
            new Main().start();
        } catch (IOException e) {
            logger.warning("Error while starting server: " + e.getMessage());
        } finally {
            shutdownExecutor();
        }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port " + PORT);

            while (!Thread.currentThread().isInterrupted()) {
                logger.info("Waiting for connection...");
                Socket socket = serverSocket.accept();
                executorService.submit(() -> clientHandler(socket));
            }
        } finally {
            shutdownExecutor();
        }
    }

    private void clientHandler(Socket socket) {
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        logger.info("Client connected: " + clientInfo);

        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // Для корректного отображения в консоли
            writer.write("Welcome to Echo Server! Type your message and press Enter:\n");
            writer.flush();

            String inputLine;

            // Читаем строки пока клиент не закроет соединение
            while ((inputLine = reader.readLine()) != null) {
                if (!inputLine.trim().isEmpty()) {
                    logger.info("Received from " + clientInfo + ": " + inputLine);

                    // Эхо-ответ с добавлением префикса
                    String response = "ECHO: " + inputLine + "\n";
                    writer.write(response);
                    writer.flush();

                    logger.info("Sent to " + clientInfo + ": " + response.trim());

                    // Автоматическое отключение при специальной команде
                    if ("exit".equalsIgnoreCase(inputLine.trim()) ||
                            "quit".equalsIgnoreCase(inputLine.trim())) {
                        logger.info("Client " + clientInfo + " requested disconnect");
                        break;
                    }
                }
            }

        } catch (IOException e) {
            if (!socket.isClosed()) {
                logger.warning("Error with client " + clientInfo + ": " + e.getMessage());
            }
        } finally {
            logger.info("Client disconnected: " + clientInfo);
        }
    }

    private static void shutdownExecutor() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warning("Executor service did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
