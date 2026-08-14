package com.athena.ai;

import java.util.List;

public class OllamaAiService implements AiService {

    private final OllamaClient ollamaClient;

    public OllamaAiService(String ollamaUrl) {
        this.ollamaClient = new OllamaClient(ollamaUrl);
    }

    public List<ModelInfo> listModels() throws Exception {
        return ollamaClient.listModels();
    }

    public boolean modelExists(String model) throws Exception {
        return listModels().stream()
                .anyMatch(m -> m.name().equals(model));
    }

    @Override
    public String generate(String prompt) throws Exception {
        throw new UnsupportedOperationException(
                "A model must be selected before generating a response.");
    }

    public String generate(String model, String prompt) throws Exception {
        return ollamaClient.generate(model, prompt);
    }

    public String chat(String model, ChatSession session)
            throws Exception {

        String response = ollamaClient.chat(
                model,
                session.getMessages());

        session.addAssistantMessage(response);

        return response;
    }

}