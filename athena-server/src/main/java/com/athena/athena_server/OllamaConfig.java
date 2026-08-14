package com.athena.athena_server;

import com.athena.ai.OllamaAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Bean
    public OllamaAiService ollamaAiService(
            @Value("${ollama.url}") String ollamaUrl) {

        return new OllamaAiService(ollamaUrl);
    }
}