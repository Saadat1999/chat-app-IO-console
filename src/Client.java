import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable{

    private Socket client;
    private volatile boolean isActive;
    private ScheduledExecutorService clientThread;
    private BufferedReader reader;
    private PrintWriter writer;

    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 9696);
            writer = new PrintWriter(client.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler handler = new InputHandler(client);
            clientThread = Executors.newScheduledThreadPool(1);
            clientThread.schedule(handler, 1, TimeUnit.MILLISECONDS);

            String receivedMessage ;
            while((receivedMessage = reader.readLine())!= null) {
                System.out.println(receivedMessage);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        isActive = false;
        try {
            if(!client.isClosed()) {
                client.close();
            }
            clientThread.shutdown();
            clientThread.awaitTermination(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }


    class InputHandler implements Runnable {
        private Socket client;


        public InputHandler(Socket client) {
            this.client = client;
            isActive = true;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                while (isActive) {
                    String message = reader.readLine();
                    if(message.startsWith("/quit")) {
                        writer.println("/quit");
                        this.shutdown();
                    } else {
                        writer.println(message);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void shutdown() {
            try {
                if(!client.isClosed()) {
                    client.close();
                }
                Client.this.shutdown();
            } catch (Exception e) {
                //
            }

        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
