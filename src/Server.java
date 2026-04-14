import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ServerSocket server;
    private boolean isActive;
    private List<ConnectionHandler> connections;
    private ExecutorService serverThread;

    public Server() {
        connections = new ArrayList<>();
        serverThread = Executors.newCachedThreadPool();
        isActive = true;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9696);

            while (isActive) {
                Socket client = server.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(client);
                connections.add(connectionHandler);
                serverThread.execute(connectionHandler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for(ConnectionHandler connection : connections) {
            if(connection!=null) {
                connection.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        if(!server.isClosed()) {
            try {
                isActive = false;
                serverThread.shutdown();
                server.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader reader;
        private PrintWriter writer;
        private String nickname;
        private boolean clientIsActive;

        public ConnectionHandler(Socket client) {
            this.client = client;
            clientIsActive = true;
        }

        @Override
        public void run() {
            try {
                writer = new PrintWriter(client.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                writer.println("Please enter a name:");
                nickname = reader.readLine();
                System.out.println(nickname + " connected to the server.");
                broadcast(nickname + " joined the chat.");

                while(clientIsActive) {
                    String message;
                    if((message = reader.readLine()) != null) {
                        if(message.startsWith("/nick ")) {
                            String[] split = message.split(" ", 2);
                            String newName = split[1];
                            System.out.println(nickname + " changed their name to " + newName);
                            writer.println("Successfully changed name to " + newName);
                            broadcast(nickname + " changed name to " + newName);
                            nickname = newName;
                        } else if(message.startsWith("/quit")) {
                            System.out.println(nickname + " left chat");
                            broadcast(nickname + " left the chat.");
                            this.shutdown();
                        } else {
                            broadcast(nickname + ": " + message);
                        }
                    }
                }
            } catch (IOException e) {
                //
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }

        public void shutdown() {
            try {
                writer.close();
                reader.close();
                clientIsActive = false;
                if(!client.isClosed()) {
                    client.close();
                }
                connections.remove(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}
