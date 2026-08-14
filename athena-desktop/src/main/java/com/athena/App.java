package com.athena;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class App extends Application {

    private static final String SERVER_URL = "http://localhost:8080";

    private static final String MODEL = "qwen2.5-coder:7b";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String sessionId = UUID.randomUUID().toString();

    private ListView<String> conversationList;

    private final List<String> sessionIds = new ArrayList<>();

    @Override
    public void start(Stage stage) {

        // =========================
        // Header
        // =========================

        Label title = new Label("Athena");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;");

        Label status = new Label("Checking Athena Server...");

        HBox header = new HBox(20, title, status);

        header.setAlignment(
                Pos.CENTER_LEFT);

        header.setPadding(
                new Insets(15));

        // =========================
        // Sidebar
        // =========================

        Label conversationsTitle = new Label("Conversations");

        conversationsTitle.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;");

        Button newChatButton = new Button("+ New Chat");

        newChatButton.setMaxWidth(
                Double.MAX_VALUE);

        conversationList = new ListView<>();

        VBox sidebar = new VBox(
                15,
                conversationsTitle,
                newChatButton,
                conversationList);

        sidebar.setPadding(
                new Insets(15));

        sidebar.setPrefWidth(260);

        VBox.setVgrow(
                conversationList,
                javafx.scene.layout.Priority.ALWAYS);

        // =========================
        // Chat area
        // =========================

        TextArea messages = new TextArea();

        messages.setEditable(false);
        messages.setWrapText(true);

        messages.setText(
                "Athena\n\n" +
                        "Hello! I am ready to help you.\n");

        TextField input = new TextField();

        input.setPromptText(
                "Type a message...");

        Button sendButton = new Button("Send");

        sendButton.setDefaultButton(true);

        HBox inputArea = new HBox(
                10,
                input,
                sendButton);

        HBox.setHgrow(
                input,
                javafx.scene.layout.Priority.ALWAYS);

        inputArea.setPadding(
                new Insets(10));

        VBox chatArea = new VBox(
                10,
                messages,
                inputArea);

        VBox.setVgrow(
                messages,
                javafx.scene.layout.Priority.ALWAYS);

        // =========================
        // Send message
        // =========================

        Runnable sendMessage = () -> {

            String message = input.getText().trim();

            if (message.isEmpty()) {
                return;
            }

            messages.appendText(
                    "\nYou\n" +
                            message +
                            "\n");

            input.clear();

            sendButton.setDisable(true);

            status.setText(
                    "Athena is thinking...");

            try {

                String json = objectMapper.writeValueAsString(
                        new ChatRequest(
                                sessionId,
                                MODEL,
                                message));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        SERVER_URL +
                                                "/api/chat"))
                        .header(
                                "Content-Type",
                                "application/json")
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json))
                        .build();

                httpClient.sendAsync(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString())

                        .thenAccept(response -> {

                            Platform.runLater(() -> {

                                sendButton.setDisable(false);

                                if (response.statusCode() == 200) {

                                    try {

                                        ChatResponse chatResponse = objectMapper.readValue(
                                                response.body(),
                                                ChatResponse.class);

                                        messages.appendText(
                                                "\nAthena\n" +
                                                        chatResponse.message() +
                                                        "\n");

                                        status.setText(
                                                "Connected to Athena Server");

                                        loadSessions();

                                    } catch (Exception e) {

                                        messages.appendText(
                                                "\nAthena Error\n" +
                                                        "Invalid server response.\n");

                                        status.setText(
                                                "Invalid server response");
                                    }

                                } else {

                                    messages.appendText(
                                            "\nAthena Error\n" +
                                                    response.body() +
                                                    "\n");

                                    status.setText(
                                            "Server Error: " +
                                                    response.statusCode());
                                }
                            });

                        })

                        .exceptionally(error -> {

                            Platform.runLater(() -> {

                                sendButton.setDisable(false);

                                messages.appendText(
                                        "\nAthena Error\n" +
                                                "Could not connect to Athena Server.\n" +
                                                error.getMessage() +
                                                "\n");

                                status.setText(
                                        "Disconnected from Athena Server");
                            });

                            return null;
                        });

            } catch (Exception e) {

                sendButton.setDisable(false);

                messages.appendText(
                        "\nAthena Error\n" +
                                e.getMessage() +
                                "\n");
            }
        };

        sendButton.setOnAction(
                event -> sendMessage.run());

        input.setOnAction(
                event -> sendMessage.run());

        // =========================
        // New Chat
        // =========================

        newChatButton.setOnAction(event -> {

            sessionId = UUID.randomUUID().toString();

            messages.setText(
                    "Athena\n\n" +
                            "New conversation started.\n");

            input.clear();

            status.setText(
                    "Connected to Athena Server");

            conversationList
                    .getSelectionModel()
                    .clearSelection();
        });

        // =========================
        // Select conversation
        // =========================

        conversationList
                .getSelectionModel()
                .selectedIndexProperty()
                .addListener(
                        (observable,
                                oldValue,
                                newValue) -> {

                            int index = newValue.intValue();

                            if (index >= 0 &&
                                    index < sessionIds.size()) {

                                String selectedSessionId = sessionIds.get(index);

                                loadSession(
                                        selectedSessionId,
                                        messages,
                                        status);
                            }
                        });

        // =========================
        // Main layout
        // =========================

        BorderPane root = new BorderPane();

        root.setTop(header);
        root.setLeft(sidebar);
        root.setCenter(chatArea);

        Scene scene = new Scene(
                root,
                1100,
                700);

        stage.setTitle("Athena");
        stage.setScene(scene);
        stage.show();

        // =========================
        // Server connection
        // =========================

        checkServerConnection(status);

        // =========================
        // Load conversations
        // =========================

        loadSessions();
    }

    // =====================================================
    // Check server connection
    // =====================================================

    private void checkServerConnection(
            Label status) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                SERVER_URL +
                                        "/api/models"))
                .GET()
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers
                        .ofString())

                .thenAccept(response -> {

                    Platform.runLater(() -> {

                        if (response.statusCode() == 200) {

                            status.setText(
                                    "Connected to Athena Server");

                        } else {

                            status.setText(
                                    "Athena Server Error");
                        }
                    });

                })

                .exceptionally(error -> {

                    Platform.runLater(
                            () -> status.setText(
                                    "Disconnected from Athena Server"));

                    return null;
                });
    }

    // =====================================================
    // Load sessions
    // =====================================================

    private void loadSessions() {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                SERVER_URL +
                                        "/api/sessions"))
                .GET()
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString())

                .thenAccept(response -> {

                    if (response.statusCode() != 200) {
                        System.err.println(
                                "Failed to load sessions. HTTP " +
                                        response.statusCode());
                        return;
                    }

                    try {

                        JsonNode root = objectMapper.readTree(
                                response.body());

                        if (!root.isArray()) {
                            return;
                        }

                        List<String> newSessionIds = new ArrayList<>();

                        List<String> titles = new ArrayList<>();

                        for (JsonNode session : root) {

                            String id = session.path("sessionId")
                                    .asText(null);

                            if (id == null ||
                                    id.isBlank()) {
                                continue;
                            }

                            String title = extractSessionTitle(
                                    session);

                            newSessionIds.add(id);

                            titles.add(
                                    shorten(title));
                        }

                        Platform.runLater(() -> {

                            sessionIds.clear();

                            sessionIds.addAll(
                                    newSessionIds);

                            conversationList.setItems(
                                    FXCollections
                                            .observableArrayList(
                                                    titles));
                        });

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to parse sessions: " +
                                        e.getMessage());

                        e.printStackTrace();
                    }

                })

                .exceptionally(error -> {

                    System.err.println(
                            "Failed to load sessions: " +
                                    error.getMessage());

                    return null;
                });
    }

    private String extractSessionTitle(
            JsonNode session) {

        JsonNode messages = session.get("messages");

        if (messages != null &&
                messages.isArray()) {

            for (JsonNode message : messages) {

                String role = message.path("role")
                        .asText();

                if ("user".equals(role)) {

                    String content = message.path("content")
                            .asText();

                    if (!content.isBlank()) {

                        return content;
                    }
                }
            }
        }

        return "New conversation";
    }

    // =====================================================
    // Load one session
    // =====================================================

    private void loadSession(
            String id,
            TextArea messages,
            Label status) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                SERVER_URL +
                                        "/api/sessions/" +
                                        id))
                .GET()
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers
                        .ofString())

                .thenAccept(response -> {

                    if (response.statusCode() != 200) {
                        return;
                    }

                    try {

                        JsonNode root = objectMapper.readTree(
                                response.body());

                        Platform.runLater(() -> {

                            sessionId = id;

                            messages.setText(
                                    buildConversationText(
                                            root));

                            status.setText(
                                    "Connected to Athena Server");
                        });

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to parse session: " +
                                        e.getMessage());
                    }

                })

                .exceptionally(error -> {

                    System.err.println(
                            "Failed to load session: " +
                                    error.getMessage());

                    return null;
                });
    }

    // =====================================================
    // Build conversation display
    // =====================================================

    private String buildConversationText(
            JsonNode root) {

        StringBuilder result = new StringBuilder();

        result.append("Athena\n\n");

        JsonNode messages = root.get("messages");

        if (messages == null ||
                !messages.isArray()) {

            return result.toString();
        }

        for (JsonNode message : messages) {

            String role = message.path("role")
                    .asText();

            String content = message.path("content")
                    .asText();

            if ("user".equals(role)) {

                result.append("You\n");

            } else {

                result.append("Athena\n");
            }

            result.append(content)
                    .append("\n\n");
        }

        return result.toString();
    }

    // =====================================================
    // Utility
    // =====================================================

    private static String shorten(
            String text) {

        text = text.replace(
                "\n",
                " ");

        if (text.length() <= 35) {
            return text;
        }

        return text.substring(0, 35)
                + "...";
    }

    // =====================================================
    // DTOs
    // =====================================================

    public record ChatRequest(
            String sessionId,
            String model,
            String message) {
    }

    public record ChatResponse(
            String sessionId,
            String model,
            String message,
            String timestamp) {
    }

    public record SessionSummary(
            String sessionId,
            String model,
            String title,
            String createdAt,
            String updatedAt) {
    }

    // =====================================================
    // Main
    // =====================================================

    public static void main(String[] args) {
        launch(args);
    }
}