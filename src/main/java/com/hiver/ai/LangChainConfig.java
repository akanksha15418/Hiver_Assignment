package com.hiver.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-2.0-flash")
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        // Use Gemini's embedding API instead of local ONNX model.
        // The local AllMiniLmL6V2EmbeddingModel requires >512MB RAM (OOM on Render free tier).
        return GoogleAiGeminiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName("text-embedding-004")
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
