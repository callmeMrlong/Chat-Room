package org.example.chatroom.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.*;
import java.net.Socket;
import java.net.URL;



public class FXChatClient extends Application {
    private static final String SUPABASE_URL =
            "https://gxmlzwapthuqggcvwwyu.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_DbkMAlj9wcLLPH092DLC-A_3mmCt4g3";

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final Set<String> typingUsers =
            new HashSet<>();

    private Stage primaryStage;
    private String currentUser;

    private boolean guestMode;

    private Socket socket;
    private volatile boolean connected = false;

    private PrintWriter output;
    private BufferedReader input;

    private Label userCountLabel;
    private ListView<String> userList;

    private VBox messageBox;
    private ScrollPane chatScroll;

    private TextField inputField;

    private Label typingLabel;

    private boolean darkMode = false;

    @Override
    public void start(Stage stage) {

        this.primaryStage = stage;

        showLoginScene();
    }
    private void showChatScene(
            String username,
            boolean guestMode
    ) {

        this.currentUser = username;
        this.guestMode = guestMode;

        Button themeBtn = new Button("Dark Mode");

        // Chat container
        messageBox = new VBox(10);
        messageBox.setId("messageBox");
        messageBox.setPadding(new Insets(10));
        messageBox.setFillWidth(true);

        chatScroll = new ScrollPane(messageBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        chatScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        chatScroll.setPrefHeight(400);

        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // User list
        userCountLabel = new Label("Users in room: 0");

        userList = new ListView<>();
        userList.setPrefHeight(120);

        // Typing label
        typingLabel = new Label("");
        typingLabel.setStyle("""
        -fx-text-fill: #888888;
        -fx-font-size: 12px;
        -fx-font-style: italic;
    """);

        // Room input
        Label roomCodeLabel = new Label("Room Code");

        TextField roomField = new TextField();
        roomField.setPromptText("Room Code");

        // Username display
        Label userLabel = new Label(
                "Logged in as: "
                        + username
                        + (guestMode ? " (Guest)" : "")
        );

        // Connect / Leave
        Button connectBtn = new Button("Connect");

        Button leaveBtn = new Button("Leave");
        leaveBtn.setDisable(true);

        HBox connectionButtons =
                new HBox(10, connectBtn, leaveBtn);

        // Message input
        inputField = new TextField();

        inputField.setPromptText(
                "Type message..."
        );

        inputField.setDisable(true);

        // Send / Clear
        Button sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        Button clearBtn =
                new Button("Clear Chat");

        HBox messageButtons =
                new HBox(10, sendBtn, clearBtn);

        // Typing indicator logic
        inputField.textProperty().addListener(
                (obs, oldVal, newVal) -> {

                    if (output == null) return;

                    if (!newVal.isBlank()) {

                        output.println(
                                "[TYPING] " + currentUser
                        );

                    } else {

                        output.println(
                                "[STOP_TYPING] " + currentUser
                        );
                    }
                }
        );

        // Actions
        connectBtn.setOnAction(e ->
                connect(
                        roomField.getText(),
                        currentUser,
                        sendBtn,
                        leaveBtn,
                        connectBtn
                )
        );

        leaveBtn.setOnAction(e ->
                leaveRoom(
                        sendBtn,
                        leaveBtn,
                        connectBtn
                )
        );

        sendBtn.setOnAction(e -> sendMessage());

        inputField.setOnAction(e -> sendMessage());

        clearBtn.setOnAction(e ->
                messageBox.getChildren().clear()
        );

        themeBtn.setOnAction(e -> {

            darkMode = !darkMode;

            themeBtn.setText(
                    darkMode
                            ? "Light Mode"
                            : "Dark Mode"
            );

            applyTheme(themeBtn.getScene());
        });

        // Left side
        VBox chatBox = new VBox(
                10,
                themeBtn,
                roomCodeLabel,
                roomField,
                userLabel,
                connectionButtons,
                chatScroll,
                typingLabel,
                inputField,
                messageButtons
        );

        // Right side
        VBox userBox = new VBox(
                10,
                userCountLabel,
                userList
        );

        userBox.setId("userBox");

        chatBox.setStyle("-fx-padding: 10;");

        userBox.setPrefWidth(200);

        VBox.setVgrow(userList, Priority.ALWAYS);

        // Root
        HBox root = new HBox(
                15,
                chatBox,
                userBox
        );

        root.setPadding(new Insets(10));

        HBox.setHgrow(
                chatBox,
                Priority.ALWAYS
        );

        Scene scene = new Scene(
                root,
                900,
                600
        );

        primaryStage.setScene(scene);

        applyTheme(scene);
    }
    private void showLoginScene() {

        Label title = new Label("Chat Login");

        TextField displayNameField =
                new TextField();

        displayNameField.setPromptText(
                "Display Name"
        );

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField =
                new PasswordField();

        passwordField.setPromptText("Password");

        Button loginBtn = new Button("Login");

        Button registerBtn =
                new Button("Register");

        Button guestBtn =
                new Button("Continue as Guest");

        Label statusLabel = new Label();

        VBox root = new VBox(
                15,
                title,
                displayNameField,
                emailField,
                passwordField,
                loginBtn,
                registerBtn,
                guestBtn,
                statusLabel
        );

        root.setPadding(new Insets(30));

        Scene scene = new Scene(root, 350, 400);

        applyTheme(scene);

        primaryStage.setScene(scene);

        // LOGIN
        loginBtn.setOnAction(e -> {

            boolean success = login(
                    emailField.getText(),
                    passwordField.getText()
            );

            if (success) {

                String displayName =
                        getDisplayNameFromEmail(
                                emailField.getText()
                        );

                showChatScene(
                        displayName,
                        false
                );

            } else {

                statusLabel.setText(
                        "Login failed"
                );
            }
        });

        // REGISTER
        registerBtn.setOnAction(e -> {

            boolean success = register(
                    emailField.getText(),
                    passwordField.getText(),
                    displayNameField.getText()
            );

            statusLabel.setText(
                    success
                            ? "Account created!"
                            : "Register failed"
            );
        });

        // GUEST
        guestBtn.setOnAction(e -> {

            showChatScene(
                    "Guest_" +
                            (int)(Math.random() * 1000),
                    true
            );
        });
        primaryStage.show();
    }

    private void addMessage(String message, boolean systemMessage) {

        String time = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("h:mm a"));

        String sender = "";
        String content = message;

        if (message.contains(": ")) {

            int split = message.indexOf(": ");

            sender = message.substring(0, split);

            content = message.substring(split + 2);
        }

        Label messageLabel = new Label(content);

        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(500);

        Label nameLabel = new Label(sender);

        nameLabel.setStyle(darkMode
                ? """
        -fx-text-fill: #aaaaaa;
        -fx-font-size: 11px;
        -fx-font-weight: bold;
        """
                : """
        -fx-text-fill: #666666;
        -fx-font-size: 11px;
        -fx-font-weight: bold;
        """
        );

        Label timeLabel = new Label(time);

        timeLabel.setStyle(darkMode
                ? "-fx-text-fill: #888888; -fx-font-size: 10px;"
                : "-fx-text-fill: #777777; -fx-font-size: 10px;"
        );

        VBox messageContainer = new VBox(2);

        HBox wrapper = new HBox();

        wrapper.setPadding(new Insets(4, 10, 4, 10));

        // SYSTEM MESSAGE
        if (systemMessage) {

            messageLabel.setStyle("""
            -fx-text-fill: #ff4d4d;
            -fx-font-size: 13px;
            -fx-font-style: italic;
            """);

            timeLabel.setVisible(false);

            messageContainer.getChildren().add(messageLabel);

            wrapper.setAlignment(javafx.geometry.Pos.CENTER);
        }

        // YOUR MESSAGE
        else if (message.startsWith("You:")) {

            messageLabel.setStyle("""
            -fx-background-color: #0a84ff;
            -fx-text-fill: white;
            -fx-background-radius: 18;
            -fx-padding: 10 14 10 14;
            -fx-font-size: 14px;
            """);

            messageContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            messageContainer.getChildren().addAll(
                    nameLabel,
                    messageLabel,
                    timeLabel
            );

            wrapper.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        }

        // OTHER USERS
        else {

            messageLabel.setStyle(darkMode
                    ? """
                -fx-background-color: #3c3f41;
                -fx-text-fill: white;
                -fx-background-radius: 18;
                -fx-padding: 10 14 10 14;
                -fx-font-size: 14px;
                """
                    : """
                -fx-background-color: #e5e5ea;
                -fx-text-fill: black;
                -fx-background-radius: 18;
                -fx-padding: 10 14 10 14;
                -fx-font-size: 14px;
                """
            );

            messageContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            messageContainer.getChildren().addAll(
                    nameLabel,
                    messageLabel,
                    timeLabel
            );

            wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        wrapper.getChildren().add(messageContainer);

        wrapper.setOpacity(0);

        wrapper.setTranslateY(15);

        messageBox.getChildren().add(wrapper);

        //fade animation
        FadeTransition fade = new FadeTransition(
                Duration.millis(220),
                wrapper
        );

        fade.setFromValue(0);

        fade.setToValue(1);

        //slide animation
        TranslateTransition slide =
                new TranslateTransition(
                        Duration.millis(220),
                        wrapper
                );

        slide.setFromY(15);

        slide.setToY(0);

        //play the animations for the messages
        fade.play();
        slide.play();
    }

    private void handleUserUpdate(String msg) {

        String[] parts = msg.split(" ", 3);

        int count = Integer.parseInt(parts[1]);

        userCountLabel.setText("Users in room: " + count);

        userList.getItems().clear();

        if (parts.length > 2) {

            String[] users = parts[2].split(",");

            for (String u : users) {

                if (!u.isBlank()) {
                    userList.getItems().add(u);
                }
            }
        }
    }

    private void applyTheme(Scene scene) {

        scene.getStylesheets().clear();

        String path = darkMode
                ? "/styles/dark.css"
                : "/styles/light.css";

        URL url = getClass().getResource(path);

        if (url == null) {
            System.out.println("CSS not found: " + path);
            return;
        }

        scene.getStylesheets().add(url.toExternalForm());
    }
    private boolean register(
            String email,
            String password,
            String displayName
    ) {

        try {

            // CREATE AUTH ACCOUNT
            String authJson = """
        {
            "email":"%s",
            "password":"%s"
        }
        """.formatted(email, password);

            HttpRequest authRequest =
                    HttpRequest.newBuilder()

                            .uri(URI.create(
                                    SUPABASE_URL +
                                            "/auth/v1/signup"
                            ))

                            .header(
                                    "apikey",
                                    SUPABASE_KEY
                            )

                            .header(
                                    "Content-Type",
                                    "application/json"
                            )

                            .POST(HttpRequest.BodyPublishers
                                    .ofString(authJson))

                            .build();

            HttpResponse<String> authResponse =
                    httpClient.send(
                            authRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(authResponse.body());

            if (authResponse.statusCode() != 200 &&
                    authResponse.statusCode() != 201) {

                return false;
            }

            // EXTRACT USER ID
            String body = authResponse.body();

            int idStart =
                    body.indexOf("\"id\":\"") + 6;

            int idEnd =
                    body.indexOf("\"", idStart);

            String userId =
                    body.substring(idStart, idEnd);

            // INSERT PROFILE
            String profileJson = """
        {
            "uid":"%s",
            "display_name":"%s",
            "email":"%s"
        }
        """.formatted(
                    userId,
                    displayName,
                    email
            );

            HttpRequest profileRequest =
                    HttpRequest.newBuilder()

                            .uri(URI.create(
                                    SUPABASE_URL +
                                            "/rest/v1/profiles"
                            ))

                            .header(
                                    "apikey",
                                    SUPABASE_KEY
                            )

                            .header(
                                    "Authorization",
                                    "Bearer " + SUPABASE_KEY
                            )

                            .header(
                                    "Content-Type",
                                    "application/json"
                            )

                            .header(
                                    "Prefer",
                                    "return=minimal"
                            )

                            .POST(HttpRequest.BodyPublishers
                                    .ofString(profileJson))

                            .build();

            HttpResponse<String> profileResponse =
                    httpClient.send(
                            profileRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(profileResponse.body());

            return profileResponse.statusCode() == 201;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
    private String getDisplayNameFromEmail(
            String email
    ) {

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()

                            .uri(URI.create(
                                    SUPABASE_URL +
                                            "/rest/v1/profiles" +
                                            "?select=display_name" +
                                            "&email=eq." + email
                            ))

                            .header(
                                    "apikey",
                                    SUPABASE_KEY
                            )

                            .header(
                                    "Authorization",
                                    "Bearer " + SUPABASE_KEY
                            )

                            .GET()

                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            String body = response.body();

            System.out.println(body);

            int start =
                    body.indexOf("\"display_name\":\"") + 16;

            int end =
                    body.indexOf("\"", start);

            return body.substring(start, end);

        } catch (Exception e) {

            e.printStackTrace();

            return email;
        }
    }

    private boolean login(
            String email,
            String password
    ) {

        try {

            String json = """
        {
            \"email\":\"%s\",
            \"password\":\"%s\"
        }
        """.formatted(email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SUPABASE_URL +
                                    "/auth/v1/token?grant_type=password"
                    ))
                    .header("apikey", SUPABASE_KEY)
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(HttpRequest.BodyPublishers
                            .ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(response.body());

            return response.statusCode() == 200;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    private void updateTypingIndicator() {

        int size = typingUsers.size();

        if (size == 0) {

            typingLabel.setText("");

            return;
        }

        var users = new ArrayList<>(typingUsers);

        // 1 user
        if (size == 1) {

            typingLabel.setText(
                    users.get(0) + " is typing..."
            );
        }

        // 2 users
        else if (size == 2) {

            typingLabel.setText(
                    users.get(0)
                            + " and "
                            + users.get(1)
                            + " are typing..."
            );
        }

        // 3-5 users
        else if (size <= 5) {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < users.size(); i++) {

                if (i == users.size() - 1) {

                    sb.append("and ")
                            .append(users.get(i));

                } else {

                    sb.append(users.get(i))
                            .append(", ");
                }
            }

            sb.append(" are typing...");

            typingLabel.setText(sb.toString());
        }

        // 6+ users
        else {

            typingLabel.setText(
                    size + " people are typing..."
            );
        }
    }

    private void connect(
            String room,
            String username,
            Button sendBtn,
            Button leaveBtn,
            Button connectBtn
    ) {

        try {

            socket = new Socket("localhost", 1234);
            connected = true;

            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            output = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            // Join
            output.println("JOIN " + room + " " + username);

            addMessage(
                    "[SYSTEM] Connected to room " + room,
                    true
            );

            // Listener thread
            Thread listenerThread = new Thread(() -> {

                try {

                    String msg;

                    while (connected && (msg = input.readLine()) != null) {

                        String finalMsg = msg;

                        Platform.runLater(() -> {
                            // JOIN SUCCESS
                            if (finalMsg.equals("JOIN_SUCCESS")) {

                                inputField.setDisable(false);

                                sendBtn.setDisable(false);

                                leaveBtn.setDisable(false);

                                connectBtn.setDisable(true);

                                addMessage(
                                        "[SYSTEM] Connected successfully.",
                                        true
                                );

                                return;
                            }

// JOIN ERROR
                            if (finalMsg.startsWith("ERROR")) {

                                addMessage(
                                        "[SYSTEM] " + finalMsg,
                                        true
                                );

                                return;
                            }

                            // USERS UPDATE
                            if (finalMsg.startsWith("USERS ")) {

                                handleUserUpdate(finalMsg);
                                return;
                            }

                            // USER STARTED TYPING
                            if (finalMsg.startsWith("[TYPING] ")) {

                                String typingUser =
                                        finalMsg.replace("[TYPING] ", "");

                                typingUsers.add(typingUser);

                                updateTypingIndicator();

                                return;
                            }

// USER STOPPED TYPING
                            if (finalMsg.startsWith("[STOP_TYPING] ")) {

                                String typingUser =
                                        finalMsg.replace("[STOP_TYPING] ", "");

                                typingUsers.remove(typingUser);

                                updateTypingIndicator();

                                return;
                            }

                            // NORMAL MESSAGE
                            addMessage(
                                    finalMsg,
                                    finalMsg.startsWith("[SYSTEM]")
                            );
                        });
                    }

                } catch (IOException e) {

                    Platform.runLater(() ->
                            addMessage(
                                    "[SYSTEM] Disconnected.",
                                    true
                            )
                    );
                }

            });
            listenerThread.setDaemon(true);

            listenerThread.start();

        } catch (IOException e) {

            addMessage(
                    "[SYSTEM] Connection failed.",
                    true
            );
        }
    }

    private void leaveRoom(
            Button sendBtn,
            Button leaveBtn,
            Button connectBtn
    ) {

        try {

            connected = false;

            if (socket != null &&
                    !socket.isClosed()) {

                socket.close();
            }

            if (input != null) {
                input.close();
            }

            if (output != null) {
                output.close();
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        inputField.setDisable(true);

        sendBtn.setDisable(true);

        leaveBtn.setDisable(true);

        connectBtn.setDisable(false);

        userList.getItems().clear();

        userCountLabel.setText("Users in room: 0");

        addMessage(
                "[SYSTEM] You left the room.",
                true
        );
    }

    private void sendMessage() {

        String msg = inputField.getText();

        if (!msg.isEmpty()) {

            output.println(msg);

            output.println(
                    "[STOP_TYPING] " + currentUser
            );

            addMessage("You: " + msg, false);

            inputField.clear();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}