import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ChatApplication extends Application {

    private final ChatClient chatClient = new ChatClient();

    @Override
    public void start(Stage stage) {
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");

        Button sendButton = new Button("Send");

        HBox bottom = new HBox(10, inputField, sendButton);
        VBox root = new VBox(10, chatArea, bottom);

        sendButton.setOnAction(e -> {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                chatClient.sendMessage(msg);
                inputField.clear();
            }
        });

        inputField.setOnAction(e -> sendButton.fire());

        try {
            chatClient.connect("localhost", 5000);
            chatClient.listen(message -> {
                javafx.application.Platform.runLater(() ->
                        chatArea.appendText(message + "\n")
                );
            });
        } catch (Exception e) {
            chatArea.appendText("Failed to connect: " + e.getMessage() + "\n");
        }

        stage.setScene(new Scene(root, 500, 400));
        stage.setTitle("JavaFX Chat");
        stage.show();

        stage.setOnCloseRequest(e -> chatClient.close());
    }

    public static void main(String[] args) {
        launch(args);
    }
}