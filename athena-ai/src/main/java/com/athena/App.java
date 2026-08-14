package com.athena;

import com.athena.ai.ChatSession;
import com.athena.ai.ModelInfo;
import com.athena.ai.OllamaAiService;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class App {

    public static void main(String[] args) {

        Properties properties = new Properties();

        try (InputStream input = App.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "application.properties not found");
            }

            properties.load(input);

        } catch (Exception e) {
            System.err.println("Failed to load configuration.");
            e.printStackTrace();
            return;
        }

        String ollamaUrl = properties.getProperty("ollama.url");

        OllamaAiService ai = new OllamaAiService(ollamaUrl);

        String model = "qwen2.5-coder:7b";

        String sessionId = "session-1";

        ChatSession session = new ChatSession(sessionId, model);

        try {

            session.addUserMessage(
                    "My name is Athena. Remember that my favorite programming language is Java.");

            String response1 = ai.chat(model, session);

            System.out.println("Athena: " + response1);

            session.addUserMessage(
                    "What is my favorite programming language?");

            String response2 = ai.chat(model, session);

            System.out.println("Athena: " + response2);

        } catch (Exception e) {
            System.err.println("AI communication failed.");
            e.printStackTrace();
        }

        try {
            List<ModelInfo> models = ai.listModels();

            System.out.println("Models available in Ollama:");

            for (ModelInfo modell : models) {

                System.out.println(
                        " - " + modell.name()
                                + " | size=" + modell.size()
                                + " | modified=" + modell.modifiedAt());
            }

        } catch (Exception e) {
            System.err.println("Failed to communicate with Ollama.");
            e.printStackTrace();
        }
    }
}