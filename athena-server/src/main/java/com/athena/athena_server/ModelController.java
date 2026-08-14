package com.athena.athena_server;

import com.athena.ai.ModelInfo;
import com.athena.ai.OllamaAiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final OllamaAiService aiService;

    public ModelController(OllamaAiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping
    public List<ModelInfo> getModels() throws Exception {
        return aiService.listModels();
    }
}