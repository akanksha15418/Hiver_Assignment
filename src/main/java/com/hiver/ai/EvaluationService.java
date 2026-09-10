package com.hiver.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

    @Autowired
    private SupportAgentService supportAgentService;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    public Map<String, Object> evaluate(String filePath) {
        int totalRecords = 0;
        int correctIntents = 0;
        double totalScore = 0;

        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                String userMessage = csvRecord.get("user_message");
                String expectedIntent = csvRecord.get("expected_intent");

                SupportResponse response = supportAgentService.handleSupportRequest(userMessage);

                if (expectedIntent.equalsIgnoreCase(response.getIntent())) {
                    correctIntents++;
                }

                String evalPrompt = "Rate this AI reply on a scale of 1-5 compared to how a human support agent would respond, based on helpfulness and brand tone.\n\n"
                        + "User Message: " + userMessage + "\n"
                        + "AI Reply: " + response.getDraftReply() + "\n\n"
                        + "Output exactly a single integer between 1 and 5.";

                String scoreString = chatLanguageModel.generate(evalPrompt).trim();
                try {
                    int score = Integer.parseInt(scoreString.replaceAll("[^0-9]", ""));
                    totalScore += score;
                } catch (Exception e) {
                    logger.error("Failed to parse evaluation score: {}", scoreString);
                }

                totalRecords++;
            }

        } catch (Exception e) {
            logger.error("Evaluation failed", e);
            throw new RuntimeException("Evaluation failed", e);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("total_records", totalRecords);
        if (totalRecords > 0) {
            report.put("intent_match_accuracy", (double) correctIntents / totalRecords);
            report.put("average_reply_score", totalScore / totalRecords);
        } else {
            report.put("intent_match_accuracy", 0.0);
            report.put("average_reply_score", 0.0);
        }

        return report;
    }
}
