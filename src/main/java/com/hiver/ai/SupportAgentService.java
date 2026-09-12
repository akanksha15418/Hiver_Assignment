package com.hiver.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportAgentService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    public SupportResponse handleSupportRequest(String message, String companyName) {
        SupportResponse response = new SupportResponse();

        // 1. Intent Classification
        String intentPrompt = "Classify the following customer support message into one of these intents: Refund, Query, Complaint, TechSupport, Other. Reply with ONLY the intent name.\n\nMessage: " + message;
        String intent = chatLanguageModel.generate(intentPrompt).trim();
        response.setIntent(intent);

        // 2. Draft a Reply (RAG)
        Embedding userEmbedding = embeddingModel.embed(message).content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(userEmbedding)
                .maxResults(3)
                .minScore(0.6)
                .build();
                
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        String contextContext = searchResult.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n"));

        String agentContext = (companyName != null && !companyName.isBlank())
                ? "You are a helpful customer support agent for " + companyName + "."
                : "You are a helpful customer support agent.";

        String replyPrompt = agentContext + " Draft a concise and polite reply to the customer's message. Use the following similar past resolutions for context if applicable:\n\n"
                + "Context: " + contextContext + "\n\n"
                + "Customer Message: " + message + "\n\n"
                + "Reply:";
        String draftReply = chatLanguageModel.generate(replyPrompt).trim();
        response.setDraftReply(draftReply);

        // 3. Escalation Decision
        String escalationPrompt = "Decide whether the following customer message should be 'auto-handled' or 'escalated' to a human.\n"
                + "Escalate if the user is very angry, asks for a refund, or has a complex issue.\n"
                + "Output exactly in this format on two lines:\n"
                + "ESCALATE: true or false\n"
                + "REASON: brief reason\n\n"
                + "Customer Message: " + message;
        String escalationResult = chatLanguageModel.generate(escalationPrompt).trim();

        boolean escalate = false;
        String reason = "";
        String[] lines = escalationResult.split("\n");
        for (String line : lines) {
            if (line.toUpperCase().startsWith("ESCALATE:")) {
                escalate = Boolean.parseBoolean(line.substring(9).trim());
            } else if (line.toUpperCase().startsWith("REASON:")) {
                reason = line.substring(7).trim();
            }
        }

        response.setEscalate(escalate);
        response.setEscalationReason(reason);

        return response;
    }
}
