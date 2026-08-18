package com.athena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    // Markdown
    // =====================================================

    private final MutableDataSet markdownOptions = new MutableDataSet();

    private final Parser markdownParser = Parser.builder(markdownOptions)
            .build();

    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder(markdownOptions)
            .build();

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

    private Button attachButton;

    private Label status;

    private VBox attachmentContainer;

    // =====================================================
    // Session IDs
    // =====================================================

    private final List<String> sessionIds = new ArrayList<>();

    // =====================================================
    // Current attachments
    // =====================================================

    private final List<AttachmentItem> attachments = new ArrayList<>();

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
        // Chat WebView
        // =================================================

        chatView = new WebView();

        chatEngine = chatView.getEngine();

        chatView.setContextMenuEnabled(
                false);

        loadChatHtml();

        // =================================================
        // Attachment container
        // =================================================

        attachmentContainer = new VBox(5);

        attachmentContainer.setPadding(
                new Insets(5, 10, 0, 10));

        // =================================================
        // Input
        // =================================================

        input = new TextField();

        input.setPromptText(
                "Type a message...");

        attachButton = new Button("📎 Attach");

        sendButton = new Button("Send");

        sendButton.setDefaultButton(
                true);

        HBox inputArea = new HBox(
                10,
                attachButton,
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
                5,
                chatView,
                attachmentContainer,
                inputArea);

        VBox.setVgrow(
                chatView,
                Priority.ALWAYS);

        // =================================================
        // Attach files
        // =================================================

        attachButton.setOnAction(
                event -> chooseFiles(stage));

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

                    clearAttachments();

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

        stage.setTitle(
                "Athena");

        stage.setScene(
                scene);

        stage.show();

        // =================================================
        // Initial server operations
        // =================================================

        checkServerConnection();

        loadModels();

        loadSessions();
    }

    // =====================================================
    // Choose files
    // =====================================================

    private void chooseFiles(
            Stage stage) {

        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                "Attach files to Athena");

        chooser.getExtensionFilters().addAll(

                new FileChooser.ExtensionFilter(
                        "All Files",
                        "*.*"),

                new FileChooser.ExtensionFilter(
                        "Text Files",
                        "*.txt",
                        "*.md",
                        "*.json",
                        "*.xml",
                        "*.csv",
                        "*.log"),

                new FileChooser.ExtensionFilter(
                        "Code Files",
                        "*.java",
                        "*.js",
                        "*.jsx",
                        "*.ts",
                        "*.tsx",
                        "*.py",
                        "*.c",
                        "*.cpp",
                        "*.h",
                        "*.cs",
                        "*.go",
                        "*.rs",
                        "*.html",
                        "*.css"),

                new FileChooser.ExtensionFilter(
                        "Images",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.gif",
                        "*.webp"));

        List<java.io.File> selectedFiles = chooser.showOpenMultipleDialog(stage);

        if (selectedFiles == null ||
                selectedFiles.isEmpty()) {

            return;
        }

        for (java.io.File file : selectedFiles) {

            uploadFile(
                    file.toPath());
        }
    }

    // =====================================================
    // Upload file
    // =====================================================

    private void uploadFile(
            Path file) {

        status.setText(
                "Uploading " +
                        file.getFileName() +
                        "...");

        attachButton.setDisable(
                true);

        try {

            byte[] fileBytes = Files.readAllBytes(file);

            String contentType = Files.probeContentType(file);

            if (contentType == null) {

                contentType = "application/octet-stream";
            }

            String boundary = "----AthenaBoundary" +
                    UUID.randomUUID();

            byte[] body = buildMultipartBody(
                    boundary,
                    file,
                    fileBytes,
                    contentType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(
                            URI.create(
                                    SERVER_URL +
                                            "/api/files/upload"))
                    .header(
                            "Content-Type",
                            "multipart/form-data; boundary=" +
                                    boundary)
                    .POST(
                            HttpRequest.BodyPublishers
                                    .ofByteArray(body))
                    .build();

            httpClient.sendAsync(
                    request,
                    HttpResponse.BodyHandlers
                            .ofString())

                    .thenAccept(response -> {

                        Platform.runLater(() -> {

                            attachButton.setDisable(
                                    false);

                            if (response.statusCode() == 200) {

                                try {

                                    AttachmentResponse result = objectMapper.readValue(
                                            response.body(),
                                            AttachmentResponse.class);

                                    attachments.add(
                                            new AttachmentItem(
                                                    result.id(),
                                                    result.fileName(),
                                                    result.contentType(),
                                                    result.size()));

                                    refreshAttachmentList();

                                    status.setText(
                                            "Attached: " +
                                                    result.fileName());

                                } catch (Exception e) {

                                    addErrorMessage(
                                            "Invalid attachment response.\n\n" +
                                                    e.getMessage());

                                    status.setText(
                                            "Attachment Error");
                                }

                            } else {

                                addErrorMessage(
                                        "File upload failed.\n\n" +
                                                "HTTP " +
                                                response.statusCode() +
                                                "\n\n" +
                                                response.body());

                                status.setText(
                                        "Attachment Upload Failed");
                            }
                        });

                    })

                    .exceptionally(error -> {

                        Platform.runLater(() -> {

                            attachButton.setDisable(
                                    false);

                            addErrorMessage(
                                    "Could not upload file:\n\n" +
                                            file.getFileName() +
                                            "\n\n" +
                                            error.getMessage());

                            status.setText(
                                    "Attachment Upload Failed");
                        });

                        return null;
                    });

        } catch (Exception e) {

            attachButton.setDisable(
                    false);

            addErrorMessage(
                    "Could not read file:\n\n" +
                            file.getFileName() +
                            "\n\n" +
                            e.getMessage());

            status.setText(
                    "Attachment Error");
        }
    }

    // =====================================================
    // Build multipart request body
    // =====================================================

    private byte[] buildMultipartBody(
            String boundary,
            Path file,
            byte[] fileBytes,
            String contentType)
            throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; " +
                "name=\"file\"; filename=\"" +
                file.getFileName() +
                "\"\r\n" +
                "Content-Type: " +
                contentType +
                "\r\n\r\n";

        output.write(
                header.getBytes(
                        StandardCharsets.UTF_8));

        output.write(
                fileBytes);

        String footer = "\r\n--" +
                boundary +
                "--\r\n";

        output.write(
                footer.getBytes(
                        StandardCharsets.UTF_8));

        return output.toByteArray();
    }

    // =====================================================
    // Refresh attachment list
    // =====================================================

    private void refreshAttachmentList() {

        attachmentContainer
                .getChildren()
                .clear();

        for (AttachmentItem attachment : attachments) {

            Label fileLabel = new Label(
                    "📎 " +
                            attachment.fileName() +
                            " (" +
                            formatFileSize(
                                    attachment.size())
                            +
                            ")");

            Button removeButton = new Button("×");

            removeButton.setOnAction(
                    event -> {

                        attachments.remove(
                                attachment);

                        refreshAttachmentList();

                        if (attachments.isEmpty()) {

                            status.setText(
                                    "Connected to Athena Server");
                        }
                    });

            HBox row = new HBox(
                    8,
                    fileLabel,
                    removeButton);

            row.setAlignment(
                    Pos.CENTER_LEFT);

            attachmentContainer
                    .getChildren()
                    .add(row);
        }
    }

    // =====================================================
    // Format file size
    // =====================================================

    private String formatFileSize(
            long size) {

        if (size < 1024) {

            return size + " B";
        }

        if (size < 1024 * 1024) {

            return String.format(
                    "%.1f KB",
                    size / 1024.0);
        }

        if (size < 1024L * 1024L * 1024L) {

            return String.format(
                    "%.1f MB",
                    size / (1024.0 * 1024.0));
        }

        return String.format(
                "%.1f GB",
                size / (1024.0 * 1024.0 * 1024.0));
    }

    // =====================================================
    // Clear attachments
    // =====================================================

    private void clearAttachments() {

        attachments.clear();

        refreshAttachmentList();
    }

    // =====================================================
    // Send message
    // =====================================================

    private void sendMessage() {

        String message = input.getText().trim();

        /*
         * Allow sending an attachment without text.
         */
        if (message.isEmpty() &&
                attachments.isEmpty()) {

            return;
        }

        // -------------------------------------------------
        // Display user message
        // -------------------------------------------------

        if (!message.isEmpty()) {

            addUserMessage(
                    message);

        } else {

            addUserMessage(
                    "Attached files");
        }

        // -------------------------------------------------
        // Display attachment information
        // -------------------------------------------------

        if (!attachments.isEmpty()) {

            StringBuilder attachmentText = new StringBuilder();

            attachmentText.append(
                    "Attachments:\n");

            for (AttachmentItem attachment : attachments) {

                attachmentText
                        .append("📎 ")
                        .append(attachment.fileName())
                        .append("\n");
            }

            addSystemMessage(
                    attachmentText.toString());
        }

        input.clear();

        sendButton.setDisable(
                true);

        attachButton.setDisable(
                true);

        modelSelector.setDisable(
                true);

        status.setText(
                "Athena is thinking...");

        // -------------------------------------------------
        // Collect attachment IDs
        // -------------------------------------------------

        List<String> attachmentIds = attachments.stream()
                .map(AttachmentItem::id)
                .toList();

        try {

            String json = objectMapper.writeValueAsString(
                    new ChatRequest(
                            sessionId,
                            selectedModel,
                            message,
                            attachmentIds));

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

            // -------------------------------------------------
            // Send asynchronously
            // -------------------------------------------------

            httpClient.sendAsync(
                    request,
                    HttpResponse.BodyHandlers
                            .ofString())

                    .thenAccept(response -> {

                        Platform.runLater(() -> {

                            sendButton.setDisable(
                                    false);

                            attachButton.setDisable(
                                    false);

                            modelSelector.setDisable(
                                    false);

                            handleChatResponse(
                                    response);

                            /*
                             * The attachment IDs have now
                             * been sent to the server.
                             */
                            clearAttachments();
                        });

                    })

                    .exceptionally(error -> {

                        Platform.runLater(() -> {

                            sendButton.setDisable(
                                    false);

                            attachButton.setDisable(
                                    false);

                            modelSelector.setDisable(
                                    false);

                            addErrorMessage(
                                    "Could not connect to Athena Server.\n\n" +
                                            error.getMessage());

                            status.setText(
                                    "Disconnected from Athena Server");
                        });

                        return null;
                    });

        } catch (Exception e) {

            sendButton.setDisable(
                    false);

            attachButton.setDisable(
                    false);

            modelSelector.setDisable(
                    false);

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

                addAthenaMessage(
                        chatResponse.message());

                status.setText(
                        "Connected to Athena Server");

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
    // Build WebView HTML
    // =====================================================

    private void loadChatHtml() {

        chatEngine.loadContent("""
                <!DOCTYPE html>

                <html>

                <head>

                    <meta charset="UTF-8">

                    <style>

                        * {
                            box-sizing: border-box;
                        }

                        html {
                            background: #ffffff;
                        }

                        body {

                            margin: 0;

                            padding: 20px;

                            background: #ffffff;

                            color: #1e293b;

                            font-family:
                                "Segoe UI",
                                Arial,
                                sans-serif;

                            font-size: 14px;

                            line-height: 1.55;
                        }

                        #chat {

                            display: flex;

                            flex-direction: column;

                            gap: 18px;

                            width: 100%;
                        }

                        .message {

                            display: flex;

                            width: 100%;
                        }

                        .user {

                            justify-content:
                                flex-end;
                        }

                        .assistant {

                            justify-content:
                                flex-start;
                        }

                        .system {

                            justify-content:
                                center;
                        }

                        .user .bubble {

                            max-width: 70%;

                            padding:
                                10px 14px;

                            background:
                                #dbeafe;

                            color:
                                #1e293b;

                            border-radius:
                                14px;

                            border-bottom-right-radius:
                                4px;

                            white-space:
                                pre-wrap;

                            word-wrap:
                                break-word;
                        }

                        .assistant-container {

                            max-width: 75%;

                            min-width: 0;
                        }

                        .sender {

                            font-size:
                                11px;

                            font-weight:
                                bold;

                            color:
                                #64748b;

                            margin-bottom:
                                5px;
                        }

                        .assistant .bubble {

                            padding:
                                14px 16px;

                            background:
                                #f1f5f9;

                            color:
                                #1e293b;

                            border-radius:
                                14px;

                            border-bottom-left-radius:
                                4px;

                            word-wrap:
                                break-word;

                            overflow-wrap:
                                break-word;
                        }

                        .assistant .bubble p {

                            margin-top:
                                0;

                            margin-bottom:
                                14px;
                        }

                        .assistant .bubble p:last-child {

                            margin-bottom:
                                0;
                        }

                        .assistant .bubble h1,
                        .assistant .bubble h2,
                        .assistant .bubble h3,
                        .assistant .bubble h4 {

                            color:
                                #0f172a;

                            margin-top:
                                18px;

                            margin-bottom:
                                10px;

                            line-height:
                                1.3;
                        }

                        .assistant .bubble h1 {
                            font-size:
                                24px;
                        }

                        .assistant .bubble h2 {
                            font-size:
                                20px;
                        }

                        .assistant .bubble h3 {
                            font-size:
                                17px;
                        }

                        .assistant .bubble h4 {
                            font-size:
                                15px;
                        }

                        .assistant .bubble ul,
                        .assistant .bubble ol {

                            margin-top:
                                6px;

                            margin-bottom:
                                14px;

                            padding-left:
                                25px;
                        }

                        .assistant .bubble li {

                            margin-bottom:
                                5px;
                        }

                        .assistant .bubble code {

                            font-family:
                                Consolas,
                                "Courier New",
                                monospace;

                            font-size:
                                13px;

                            background:
                                #e2e8f0;

                            color:
                                #0f172a;

                            padding:
                                2px 5px;

                            border-radius:
                                5px;
                        }

                        .code-container {

                            margin-top:
                                12px;

                            margin-bottom:
                                14px;

                            border-radius:
                                9px;

                            overflow:
                                hidden;

                            background:
                                #0f172a;

                            border:
                                1px solid #1e293b;
                        }

                        .code-header {

                            display:
                                flex;

                            align-items:
                                center;

                            justify-content:
                                space-between;

                            padding:
                                7px 10px;

                            background:
                                #1e293b;

                            color:
                                #cbd5e1;

                            font-size:
                                11px;
                        }

                        .code-language {

                            font-weight:
                                bold;

                            text-transform:
                                uppercase;
                        }

                        .copy-button {

                            border:
                                1px solid #475569;

                            background:
                                #334155;

                            color:
                                #e2e8f0;

                            border-radius:
                                5px;

                            padding:
                                3px 9px;

                            font-size:
                                11px;

                            cursor:
                                pointer;
                        }

                        .copy-button:hover {

                            background:
                                #475569;
                        }

                        .code-content {

                            margin:
                                0;

                            padding:
                                14px;

                            overflow-x:
                                auto;

                            white-space:
                                pre;

                            color:
                                #e2e8f0;

                            background:
                                #0f172a;

                            font-family:
                                Consolas,
                                "Courier New",
                                monospace;

                            font-size:
                                13px;

                            line-height:
                                1.5;
                        }

                        .assistant .bubble blockquote {

                            margin:
                                10px 0;

                            padding-left:
                                12px;

                            border-left:
                                4px solid #94a3b8;

                            color:
                                #475569;
                        }

                        .assistant .bubble table {

                            border-collapse:
                                collapse;

                            width:
                                100%;

                            margin:
                                12px 0;

                            font-size:
                                13px;
                        }

                        .assistant .bubble th,
                        .assistant .bubble td {

                            border:
                                1px solid #cbd5e1;

                            padding:
                                7px 9px;

                            text-align:
                                left;
                        }

                        .assistant .bubble th {

                            background:
                                #e2e8f0;

                            font-weight:
                                bold;
                        }

                        .assistant .bubble a {

                            color:
                                #2563eb;

                            text-decoration:
                                none;
                        }

                        .assistant .bubble a:hover {

                            text-decoration:
                                underline;
                        }

                        .assistant .bubble hr {

                            border:
                                none;

                            border-top:
                                1px solid #cbd5e1;

                            margin:
                                16px 0;
                        }

                        .system .bubble {

                            color:
                                #64748b;

                            font-size:
                                13px;

                            padding:
                                5px;

                            white-space:
                                pre-wrap;
                        }

                        .error .bubble {

                            background:
                                #fee2e2;

                            color:
                                #991b1b;

                            border:
                                1px solid #fecaca;
                        }

                        ::-webkit-scrollbar {

                            width:
                                10px;

                            height:
                                10px;
                        }

                        ::-webkit-scrollbar-track {

                            background:
                                #f8fafc;
                        }

                        ::-webkit-scrollbar-thumb {

                            background:
                                #cbd5e1;

                            border-radius:
                                5px;
                        }

                        ::-webkit-scrollbar-thumb:hover {

                            background:
                                #94a3b8;
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

                            if (text === null ||
                                text === undefined) {

                                return "";
                            }

                            return String(text)
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

                        function addAssistantMessage(html) {

                            const chat =
                                document.getElementById("chat");

                            const message =
                                document.createElement("div");

                            message.className =
                                "message assistant";

                            message.innerHTML =
                                '<div class="assistant-container">' +

                                    '<div class="sender">' +
                                        'Athena' +
                                    '</div>' +

                                    '<div class="bubble">' +
                                        html +
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

                        function addErrorMessage(text) {

                            const chat =
                                document.getElementById("chat");

                            const message =
                                document.createElement("div");

                            message.className =
                                "message assistant error";

                            message.innerHTML =
                                '<div class="assistant-container">' +

                                    '<div class="sender">' +
                                        'Athena Error' +
                                    '</div>' +

                                    '<div class="bubble">' +
                                        escapeHtml(text) +
                                    '</div>' +

                                '</div>';

                            chat.appendChild(message);

                            scrollBottom();
                        }

                        function clearChat() {

                            document.getElementById("chat")
                                .innerHTML = "";
                        }

                        function copyCode(button) {

                            const code =
                                button
                                    .closest(".code-container")
                                    .querySelector(".code-content")
                                    .innerText;

                            const textarea =
                                document.createElement("textarea");

                            textarea.value = code;

                            document.body.appendChild(
                                textarea);

                            textarea.select();

                            try {

                                document.execCommand(
                                    "copy");

                                button.innerText =
                                    "Copied!";

                                setTimeout(
                                    function() {

                                        button.innerText =
                                            "Copy";

                                    },
                                    1200);

                            } catch (error) {

                                button.innerText =
                                    "Failed";
                            }

                            document.body.removeChild(
                                textarea);
                        }

                        function formatCodeBlocks() {

                            const blocks =
                                document.querySelectorAll(
                                    ".assistant pre");

                            blocks.forEach(
                                function(pre) {

                                    if (pre.parentElement
                                            .classList
                                            .contains(
                                                "code-container")) {

                                        return;
                                    }

                                    const code =
                                        pre.querySelector(
                                            "code");

                                    if (!code) {
                                        return;
                                    }

                                    let language =
                                        "code";

                                    const classes =
                                        code.className
                                            .split(" ");

                                    classes.forEach(
                                        function(className) {

                                            if (className
                                                .startsWith(
                                                    "language-")) {

                                                language =
                                                    className
                                                        .substring(
                                                            9);
                                            }
                                        });

                                    const container =
                                        document.createElement(
                                            "div");

                                    container.className =
                                        "code-container";

                                    const header =
                                        document.createElement(
                                            "div");

                                    header.className =
                                        "code-header";

                                    const languageLabel =
                                        document.createElement(
                                            "span");

                                    languageLabel.className =
                                        "code-language";

                                    languageLabel.innerText =
                                        language;

                                    const copyButton =
                                        document.createElement(
                                            "button");

                                    copyButton.className =
                                        "copy-button";

                                    copyButton.innerText =
                                        "Copy";

                                    copyButton.onclick =
                                        function() {

                                            copyCode(
                                                copyButton);
                                        };

                                    header.appendChild(
                                        languageLabel);

                                    header.appendChild(
                                        copyButton);

                                    const codeContent =
                                        document.createElement(
                                            "pre");

                                    codeContent.className =
                                        "code-content";

                                    codeContent.innerText =
                                        code.innerText;

                                    container.appendChild(
                                        header);

                                    container.appendChild(
                                        codeContent);

                                    pre.replaceWith(
                                        container);
                                });
                        }

                        function scrollBottom() {

                            setTimeout(
                                function() {

                                    window.scrollTo(
                                        0,
                                        document.body
                                            .scrollHeight);

                                },
                                50);
                        }

                    </script>

                </body>

                </html>
                """);
    }

    // =====================================================
    // Add user message
    // =====================================================

    private void addUserMessage(
            String text) {

        if (chatEngine == null) {
            return;
        }

        chatEngine.executeScript(
                "addUserMessage(" +
                        toJsString(text) +
                        ");");
    }

    // =====================================================
    // Add Athena message
    // =====================================================

    private void addAthenaMessage(
            String markdown) {

        if (chatEngine == null) {
            return;
        }

        String html = markdownToHtml(markdown);

        chatEngine.executeScript(
                "addAssistantMessage(" +
                        toJsString(html) +
                        ");");

        chatEngine.executeScript(
                "formatCodeBlocks();");
    }

    // =====================================================
    // Add system message
    // =====================================================

    private void addSystemMessage(
            String text) {

        if (chatEngine == null) {
            return;
        }

        chatEngine.executeScript(
                "addSystemMessage(" +
                        toJsString(text) +
                        ");");
    }

    // =====================================================
    // Add error message
    // =====================================================

    private void addErrorMessage(
            String text) {

        if (chatEngine == null) {
            return;
        }

        chatEngine.executeScript(
                "addErrorMessage(" +
                        toJsString(text) +
                        ");");
    }

    // =====================================================
    // Markdown -> HTML
    // =====================================================

    private String markdownToHtml(
            String markdown) {

        if (markdown == null) {
            return "";
        }

        try {

            var document = markdownParser.parse(
                    markdown);

            return markdownRenderer.render(
                    document);

        } catch (Exception e) {

            System.err.println(
                    "Markdown rendering failed: " +
                            e.getMessage());

            return "<p>" +
                    escapeHtml(markdown) +
                    "</p>";
        }
    }

    // =====================================================
    // Escape HTML
    // =====================================================

    private static String escapeHtml(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    // =====================================================
    // Java -> JavaScript string
    // =====================================================

    private static String toJsString(
            String text) {

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

                                        if (response.statusCode() == 200) {

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
                                            "name")
                                            .asText();

                                    if (!name.isBlank() &&
                                            !name.startsWith(
                                                    "nomic-embed")) {

                                        models.add(name);
                                    }
                                }

                                Platform.runLater(
                                        () -> {

                                            modelSelector.setItems(
                                                    FXCollections
                                                            .observableArrayList(
                                                                    models));

                                            if (models.contains(
                                                    selectedModel)) {

                                                modelSelector.setValue(
                                                        selectedModel);

                                            } else if (!models.isEmpty()) {

                                                selectedModel = models.get(0);

                                                modelSelector.setValue(
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
                                            "sessionId")
                                            .asText();

                                    if (id.isBlank()) {
                                        continue;
                                    }

                                    String title = extractSessionTitle(
                                            session);

                                    newIds.add(id);

                                    titles.add(
                                            shorten(title));
                                }

                                Platform.runLater(
                                        () -> {

                                            sessionIds.clear();

                                            sessionIds.addAll(
                                                    newIds);

                                            conversationList.setItems(
                                                    FXCollections
                                                            .observableArrayList(
                                                                    titles));

                                            int currentIndex = sessionIds.indexOf(
                                                    sessionId);

                                            if (currentIndex >= 0) {

                                                conversationList
                                                        .getSelectionModel()
                                                        .select(
                                                                currentIndex);
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

                String role = message.path(
                        "role")
                        .asText();

                if ("user".equals(role)) {

                    String content = message.path(
                            "content")
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
                                        "model")
                                        .asText();

                                Platform.runLater(
                                        () -> {

                                            sessionId = id;

                                            if (!sessionModel.isBlank()) {

                                                selectedModel = sessionModel;

                                                modelSelector.setValue(
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

        clearAttachments();

        JsonNode messages = root.get("messages");

        if (messages == null ||
                !messages.isArray()) {

            addSystemMessage(
                    "This conversation is empty.");

            return;
        }

        for (JsonNode message : messages) {

            String role = message.path(
                    "role")
                    .asText();

            String content = message.path(
                    "content")
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
                " ")
                .trim();

        if (text.length() <= 35) {

            return text;
        }

        return text.substring(
                0,
                35) +
                "...";
    }

    // =====================================================
    // DTOs
    // =====================================================

    public record ChatRequest(
            String sessionId,
            String model,
            String message,
            List<String> attachmentIds) {
    }

    public record ChatResponse(
            String sessionId,
            String model,
            String message,
            String timestamp) {
    }

    public record AttachmentResponse(
            String id,
            String fileName,
            String contentType,
            long size) {
    }

    private record AttachmentItem(
            String id,
            String fileName,
            String contentType,
            long size) {
    }

    // =====================================================
    // Main
    // =====================================================

    public static void main(
            String[] args) {

        launch(args);
    }
}