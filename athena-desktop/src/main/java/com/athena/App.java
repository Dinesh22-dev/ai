package com.athena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
// import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
// import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class App extends Application {

        // =====================================================
        // Configuration
        // =====================================================

        private static final String SERVER_URL = "http://localhost:8080";

        private String selectedModel = "qwen2.5-coder:7b";

        // =====================================================
        // HTTP / JSON
        // =====================================================

        private final HttpClient httpClient = HttpClient.newHttpClient();

        private final ObjectMapper objectMapper = new ObjectMapper();

        // =====================================================
        // Current session
        // =====================================================

        private String sessionId = UUID.randomUUID().toString();

        // =====================================================
        // UI
        // =====================================================

        private ComboBox<String> modelSelector;

        private ListView<String> conversationList;

        private WebView chatView;

        private WebEngine chatEngine;

        private TextField input;

        private Button sendButton;

        private Label status;

        // =====================================================
        // Session IDs corresponding to sidebar items
        // =====================================================

        private final List<String> sessionIds = new ArrayList<>();

        // =====================================================
        // Start application
        // =====================================================

        @Override
        public void start(Stage stage) {

                // =================================================
                // Header
                // =================================================

                Label title = new Label("Athena");

                title.setStyle(
                                "-fx-font-size: 24px;" +
                                                "-fx-font-weight: bold;");

                status = new Label(
                                "Checking Athena Server...");

                Label modelLabel = new Label("Model:");

                modelSelector = new ComboBox<>();

                modelSelector.setPrefWidth(220);

                modelSelector.setValue(
                                selectedModel);

                modelSelector.setOnAction(event -> {

                        String model = modelSelector.getValue();

                        if (model != null &&
                                        !model.isBlank()) {

                                selectedModel = model;
                        }
                });

                HBox header = new HBox(
                                20,
                                title,
                                status,
                                modelLabel,
                                modelSelector);

                header.setAlignment(
                                Pos.CENTER_LEFT);

                header.setPadding(
                                new Insets(15));

                // =================================================
                // Sidebar
                // =================================================

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
                                Priority.ALWAYS);

                // =================================================
                // Chat messages container
                // =================================================

                // =================================================
                // Chat WebView
                // =================================================

                chatView = new WebView();

                chatEngine = chatView.getEngine();

                chatView.setContextMenuEnabled(false);

                chatEngine.loadContent("""
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta charset="UTF-8">

                                    <style>

                                        * {
                                            box-sizing: border-box;
                                        }

                                        body {
                                            margin: 0;
                                            padding: 20px;
                                            background: #ffffff;
                                            font-family: Arial, sans-serif;
                                        }

                                        #chat {
                                            display: flex;
                                            flex-direction: column;
                                            gap: 14px;
                                        }

                                        .message {
                                            display: flex;
                                            width: 100%;
                                        }

                                        .user {
                                            justify-content: flex-end;
                                        }

                                        .assistant {
                                            justify-content: flex-start;
                                        }

                                        .bubble {
                                            max-width: 70%;
                                            padding: 12px 16px;
                                            border-radius: 14px;
                                            font-size: 14px;
                                            line-height: 1.5;
                                            white-space: pre-wrap;
                                        }

                                        .user .bubble {
                                            background: #dbeafe;
                                            color: #1e293b;
                                            border-bottom-right-radius: 4px;
                                        }

                                        .assistant .bubble {
                                            background: #f1f5f9;
                                            color: #1e293b;
                                            border-bottom-left-radius: 4px;
                                        }

                                        .sender {
                                            font-size: 11px;
                                            font-weight: bold;
                                            color: #64748b;
                                            margin-bottom: 4px;
                                        }

                                        .assistant-container {
                                            max-width: 70%;
                                        }

                                        .system {
                                            justify-content: center;
                                        }

                                        .system .bubble {
                                            background: transparent;
                                            color: #64748b;
                                            font-size: 13px;
                                        }

                                    </style>
                                </head>

                                <body>

                                    <div id="chat">

                                        <div class="message assistant">
                                            <div class="assistant-container">

                                                <div class="sender">
                                                    Athena
                                                </div>

                                                <div class="bubble">
                                                    Hello! I am ready to help you.
                                                </div>

                                            </div>
                                        </div>

                                    </div>

                                    <script>

                                        function escapeHtml(text) {

                                            return text
                                                .replace(/&/g, "&amp;")
                                                .replace(/</g, "&lt;")
                                                .replace(/>/g, "&gt;")
                                                .replace(/"/g, "&quot;")
                                                .replace(/'/g, "&#039;");
                                        }

                                        function addUserMessage(text) {

                                            const chat =
                                                document.getElementById("chat");

                                            const message =
                                                document.createElement("div");

                                            message.className =
                                                "message user";

                                            message.innerHTML =
                                                '<div class="bubble">' +
                                                escapeHtml(text) +
                                                '</div>';

                                            chat.appendChild(message);

                                            scrollBottom();
                                        }

                                        function addAssistantMessage(text) {

                                            const chat =
                                                document.getElementById("chat");

                                            const message =
                                                document.createElement("div");

                                            message.className =
                                                "message assistant";

                                            message.innerHTML =
                                                '<div class="assistant-container">' +
                                                '<div class="sender">Athena</div>' +
                                                '<div class="bubble">' +
                                                escapeHtml(text) +
                                                '</div>' +
                                                '</div>';

                                            chat.appendChild(message);

                                            scrollBottom();
                                        }

                                        function addSystemMessage(text) {

                                            const chat =
                                                document.getElementById("chat");

                                            const message =
                                                document.createElement("div");

                                            message.className =
                                                "message system";

                                            message.innerHTML =
                                                '<div class="bubble">' +
                                                escapeHtml(text) +
                                                '</div>';

                                            chat.appendChild(message);

                                            scrollBottom();
                                        }

                                        function clearChat() {

                                            document.getElementById("chat")
                                                .innerHTML = "";

                                        }

                                        function scrollBottom() {

                                            window.scrollTo(
                                                0,
                                                document.body.scrollHeight
                                            );

                                        }

                                    </script>

                                </body>
                                </html>
                                """);

                // =================================================
                // Input
                // =================================================

                input = new TextField();

                input.setPromptText(
                                "Type a message...");

                sendButton = new Button("Send");

                sendButton.setDefaultButton(
                                true);

                HBox inputArea = new HBox(
                                10,
                                input,
                                sendButton);

                HBox.setHgrow(
                                input,
                                Priority.ALWAYS);

                inputArea.setPadding(
                                new Insets(10));

                // =================================================
                // Chat area
                // =================================================

                VBox chatArea = new VBox(
                                10,
                                chatView,
                                inputArea);

                VBox.setVgrow(
                                chatView,
                                Priority.ALWAYS);

                // =================================================
                // Send message
                // =================================================

                sendButton.setOnAction(
                                event -> sendMessage());

                input.setOnAction(
                                event -> sendMessage());

                // =================================================
                // New Chat
                // =================================================

                newChatButton.setOnAction(
                                event -> {

                                        sessionId = UUID.randomUUID()
                                                        .toString();

                                        chatEngine.executeScript(
                                                        "clearChat();");

                                        addSystemMessage(
                                                        "New conversation started.");

                                        input.clear();

                                        status.setText(
                                                        "Connected to Athena Server");

                                        conversationList
                                                        .getSelectionModel()
                                                        .clearSelection();
                                });

                // =================================================
                // Select existing conversation
                // =================================================

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
                                                                                selectedSessionId);
                                                        }
                                                });

                // =================================================
                // Main layout
                // =================================================

                BorderPane root = new BorderPane();

                root.setTop(header);

                root.setLeft(sidebar);

                root.setCenter(chatArea);

                // =================================================
                // Scene
                // =================================================

                Scene scene = new Scene(
                                root,
                                1100,
                                700);

                stage.setTitle("Athena");

                stage.setScene(scene);

                stage.show();

                // =================================================
                // Initial server operations
                // =================================================

                checkServerConnection();

                loadModels();

                loadSessions();
        }

        // =====================================================
        // Send message
        // =====================================================

        private void sendMessage() {

                String message = input.getText().trim();

                if (message.isEmpty()) {
                        return;
                }

                // -----------------------------------------------
                // Add user message immediately
                // -----------------------------------------------

                addUserMessage(message);

                input.clear();

                sendButton.setDisable(true);

                modelSelector.setDisable(true);

                status.setText(
                                "Athena is thinking...");

                // -----------------------------------------------
                // Build request
                // -----------------------------------------------

                try {

                        String json = objectMapper.writeValueAsString(
                                        new ChatRequest(
                                                        sessionId,
                                                        selectedModel,
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

                        // -------------------------------------------
                        // Send asynchronously
                        // -------------------------------------------

                        httpClient.sendAsync(
                                        request,
                                        HttpResponse.BodyHandlers
                                                        .ofString())
                                        .thenAccept(
                                                        response -> {

                                                                Platform.runLater(
                                                                                () -> {

                                                                                        sendButton
                                                                                                        .setDisable(
                                                                                                                        false);

                                                                                        modelSelector
                                                                                                        .setDisable(
                                                                                                                        false);

                                                                                        handleChatResponse(
                                                                                                        response);
                                                                                });
                                                        })
                                        .exceptionally(
                                                        error -> {

                                                                Platform.runLater(
                                                                                () -> {

                                                                                        sendButton
                                                                                                        .setDisable(
                                                                                                                        false);

                                                                                        modelSelector
                                                                                                        .setDisable(
                                                                                                                        false);

                                                                                        addErrorMessage(
                                                                                                        "Could not connect to Athena Server.\n\n"
                                                                                                                        +
                                                                                                                        error.getMessage());

                                                                                        status.setText(
                                                                                                        "Disconnected from Athena Server");
                                                                                });

                                                                return null;
                                                        });

                } catch (Exception e) {

                        sendButton.setDisable(false);

                        modelSelector.setDisable(false);

                        addErrorMessage(
                                        e.getMessage());

                        status.setText(
                                        "Request Error");
                }
        }

        // =====================================================
        // Handle chat response
        // =====================================================

        private void handleChatResponse(
                        HttpResponse<String> response) {

                if (response.statusCode() == 200) {

                        try {

                                ChatResponse chatResponse = objectMapper.readValue(
                                                response.body(),
                                                ChatResponse.class);

                                // ---------------------------------------
                                // Add Athena response
                                // ---------------------------------------

                                addAthenaMessage(
                                                chatResponse.message());

                                status.setText(
                                                "Connected to Athena Server");

                                // ---------------------------------------
                                // Refresh sidebar
                                // ---------------------------------------

                                loadSessions();

                        } catch (Exception e) {

                                addErrorMessage(
                                                "Invalid server response.\n\n" +
                                                                e.getMessage());

                                status.setText(
                                                "Invalid server response");
                        }

                } else {

                        addErrorMessage(
                                        "HTTP " +
                                                        response.statusCode() +
                                                        "\n\n" +
                                                        response.body());

                        status.setText(
                                        "Server Error: " +
                                                        response.statusCode());
                }
        }

        // =====================================================
        // User message bubble
        // =====================================================

        private void addUserMessage(String text) {

                if (chatEngine == null) {
                        return;
                }

                chatEngine.executeScript(
                                "addUserMessage(" +
                                                toJsString(text) +
                                                ");");
        }

        // =====================================================
        // Athena message bubble
        // =====================================================

        private void addAthenaMessage(String text) {

                if (chatEngine == null) {
                        return;
                }

                chatEngine.executeScript(
                                "addAssistantMessage(" +
                                                toJsString(text) +
                                                ");");
        }

        // =====================================================
        // System message
        // =====================================================

        private void addSystemMessage(String text) {

                if (chatEngine == null) {
                        return;
                }

                chatEngine.executeScript(
                                "addSystemMessage(" +
                                                toJsString(text) +
                                                ");");
        }

        // =====================================================
        // Error message
        // =====================================================

        private void addErrorMessage(String text) {

                if (chatEngine == null) {
                        return;
                }

                chatEngine.executeScript(
                                "addAssistantMessage(" +
                                                toJsString("Athena Error\n\n" + text) +
                                                ");");
        }

        private static String toJsString(String text) {

                if (text == null) {
                        return "''";
                }

                return "'" +
                                text
                                                .replace("\\", "\\\\")
                                                .replace("'", "\\'")
                                                .replace("\r", "\\r")
                                                .replace("\n", "\\n")
                                                .replace("</", "<\\/")
                                + "'";
        }

        // =====================================================
        // Create message label
        // =====================================================

        // private Label createMessageLabel(
        // String text) {

        // Label label = new Label(
        // text == null
        // ? ""
        // : text);

        // label.setWrapText(true);

        // label.setMaxWidth(
        // 650);

        // label.setMinHeight(
        // Region.USE_PREF_SIZE);

        // return label;
        // }

        // =====================================================
        // Sender label
        // =====================================================

        // private Label createSenderLabel(
        // String name) {

        // Label label = new Label(name);

        // label.setStyle(
        // "-fx-font-weight: bold;" +
        // "-fx-font-size: 12px;" +
        // "-fx-text-fill: #475569;");

        // return label;
        // }

        // =====================================================
        // Scroll chat to bottom
        // =====================================================

        // private void scrollToBottom() {

        // Platform.runLater(() -> {

        // chatScrollPane.setVvalue(
        // 1.0);
        // });
        // }

        // =====================================================
        // Check server connection
        // =====================================================

        private void checkServerConnection() {

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
                                .thenAccept(
                                                response -> {

                                                        Platform.runLater(
                                                                        () -> {

                                                                                if (response
                                                                                                .statusCode() == 200) {

                                                                                        status.setText(
                                                                                                        "Connected to Athena Server");

                                                                                } else {

                                                                                        status.setText(
                                                                                                        "Athena Server Error");
                                                                                }
                                                                        });
                                                })
                                .exceptionally(
                                                error -> {

                                                        Platform.runLater(
                                                                        () -> status.setText(
                                                                                        "Disconnected from Athena Server"));

                                                        return null;
                                                });
        }

        // =====================================================
        // Load models
        // =====================================================

        private void loadModels() {

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
                                .thenAccept(
                                                response -> {

                                                        if (response.statusCode() != 200) {

                                                                System.err.println(
                                                                                "Failed to load models. HTTP " +
                                                                                                response.statusCode());

                                                                return;
                                                        }

                                                        try {

                                                                JsonNode root = objectMapper.readTree(
                                                                                response.body());

                                                                if (!root.isArray()) {
                                                                        return;
                                                                }

                                                                List<String> models = new ArrayList<>();

                                                                for (JsonNode model : root) {

                                                                        String name = model.path(
                                                                                        "name").asText();

                                                                        // Don't show embedding models
                                                                        if (!name.isBlank() &&
                                                                                        !name.startsWith(
                                                                                                        "nomic-embed")) {

                                                                                models.add(name);
                                                                        }
                                                                }

                                                                Platform.runLater(
                                                                                () -> {

                                                                                        modelSelector
                                                                                                        .setItems(
                                                                                                                        FXCollections
                                                                                                                                        .observableArrayList(
                                                                                                                                                        models));

                                                                                        if (models.contains(
                                                                                                        selectedModel)) {

                                                                                                modelSelector
                                                                                                                .setValue(
                                                                                                                                selectedModel);

                                                                                        } else if (!models.isEmpty()) {

                                                                                                selectedModel = models
                                                                                                                .get(0);

                                                                                                modelSelector
                                                                                                                .setValue(
                                                                                                                                selectedModel);
                                                                                        }
                                                                                });

                                                        } catch (Exception e) {

                                                                System.err.println(
                                                                                "Failed to parse models: " +
                                                                                                e.getMessage());
                                                        }

                                                })
                                .exceptionally(
                                                error -> {

                                                        System.err.println(
                                                                        "Failed to load models: " +
                                                                                        error.getMessage());

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
                                HttpResponse.BodyHandlers
                                                .ofString())
                                .thenAccept(
                                                response -> {

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

                                                                List<String> newIds = new ArrayList<>();

                                                                List<String> titles = new ArrayList<>();

                                                                for (JsonNode session : root) {

                                                                        String id = session.path(
                                                                                        "sessionId").asText();

                                                                        if (id.isBlank()) {
                                                                                continue;
                                                                        }

                                                                        String title = extractSessionTitle(
                                                                                        session);

                                                                        newIds.add(id);

                                                                        titles.add(
                                                                                        shorten(title));
                                                                }

                                                                Platform.runLater(() -> {

                                                                        sessionIds.clear();
                                                                        sessionIds.addAll(newIds);

                                                                        conversationList.setItems(
                                                                                        FXCollections.observableArrayList(
                                                                                                        titles));

                                                                        int currentIndex = sessionIds
                                                                                        .indexOf(sessionId);

                                                                        if (currentIndex >= 0) {

                                                                                conversationList
                                                                                                .getSelectionModel()
                                                                                                .select(currentIndex);
                                                                        }
                                                                });

                                                        } catch (Exception e) {

                                                                System.err.println(
                                                                                "Failed to parse sessions: " +
                                                                                                e.getMessage());

                                                                e.printStackTrace();
                                                        }

                                                })
                                .exceptionally(
                                                error -> {

                                                        System.err.println(
                                                                        "Failed to load sessions: " +
                                                                                        error.getMessage());

                                                        return null;
                                                });
        }

        // =====================================================
        // Extract session title
        // =====================================================

        private String extractSessionTitle(
                        JsonNode session) {

                JsonNode messages = session.get("messages");

                if (messages != null &&
                                messages.isArray()) {

                        for (JsonNode message : messages) {

                                String role = message.path("role")
                                                .asText();

                                if ("user".equals(role)) {

                                        String content = message.path(
                                                        "content").asText();

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
                        String id) {

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
                                .thenAccept(
                                                response -> {

                                                        if (response.statusCode() != 200) {

                                                                System.err.println(
                                                                                "Failed to load session. HTTP " +
                                                                                                response.statusCode());

                                                                return;
                                                        }

                                                        try {

                                                                JsonNode root = objectMapper.readTree(
                                                                                response.body());

                                                                String sessionModel = root.path(
                                                                                "model").asText();

                                                                Platform.runLater(
                                                                                () -> {

                                                                                        sessionId = id;

                                                                                        // Switch model selector
                                                                                        if (!sessionModel
                                                                                                        .isBlank()) {

                                                                                                selectedModel = sessionModel;

                                                                                                modelSelector
                                                                                                                .setValue(
                                                                                                                                sessionModel);
                                                                                        }

                                                                                        displaySession(
                                                                                                        root);

                                                                                        status.setText(
                                                                                                        "Connected to Athena Server");
                                                                                });

                                                        } catch (Exception e) {

                                                                System.err.println(
                                                                                "Failed to parse session: " +
                                                                                                e.getMessage());

                                                                e.printStackTrace();
                                                        }

                                                })
                                .exceptionally(
                                                error -> {

                                                        System.err.println(
                                                                        "Failed to load session: " +
                                                                                        error.getMessage());

                                                        return null;
                                                });
        }

        // =====================================================
        // Display existing session
        // =====================================================

        private void displaySession(
                        JsonNode root) {

                chatEngine.executeScript(
                                "clearChat();");

                JsonNode messages = root.get("messages");

                if (messages == null ||
                                !messages.isArray()) {

                        addSystemMessage(
                                        "This conversation is empty.");

                        return;
                }

                for (JsonNode message : messages) {

                        String role = message.path("role")
                                        .asText();

                        String content = message.path("content")
                                        .asText();

                        if (content.isBlank()) {
                                continue;
                        }

                        if ("user".equals(role)) {

                                addUserMessage(
                                                content);

                        } else if ("assistant".equals(role)) {

                                addAthenaMessage(
                                                content);
                        }
                }

                // scrollToBottom();
        }

        // =====================================================
        // Utility
        // =====================================================

        private static String shorten(
                        String text) {

                if (text == null ||
                                text.isBlank()) {

                        return "New conversation";
                }

                text = text.replace(
                                "\n",
                                " ").trim();

                if (text.length() <= 35) {
                        return text;
                }

                return text.substring(
                                0,
                                35) + "...";
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

        // =====================================================
        // Main
        // =====================================================

        public static void main(String[] args) {

                launch(args);
        }
}