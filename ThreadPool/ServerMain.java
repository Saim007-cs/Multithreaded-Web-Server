import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ServerMain {
    private final ExecutorService threadPool;
    private static final AtomicInteger serveCount = new AtomicInteger(0);

    public ServerMain(int poolSize) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

  // ServerMain server = new ServerMain(10);

    public void handleClient(Socket clientSocket) {
        try (
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                byte[] fileBytes = Files.readAllBytes(file.toPath());


                int count = serveCount.incrementAndGet();
                System.out.println("✅ Served index.html to request number: " + count);

                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + fileBytes.length + "\r\n" +
                        "\r\n";

                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.write(fileBytes);
            }
           else {
                String notFoundMessage = "404 - File Not Found";
                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + notFoundMessage.length() + "\r\n" +
                        "\r\n";

                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.write(notFoundMessage.getBytes(StandardCharsets.UTF_8));
            }


            outputStream.flush();
            outputStream.close();
            clientSocket.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }




    public static void main(String[] args) {
        int port = 8010;
        int poolSize = 50;
        ServerMain server = new ServerMain(poolSize);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(70000);
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();


                server.threadPool.execute(() -> server.handleClient(clientSocket));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {

            server.threadPool.shutdown();
        }
    }

    public Consumer<Socket> getConsumer() {
        return null;
    }



    public void run() {
    }
}
