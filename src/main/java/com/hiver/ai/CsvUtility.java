package com.hiver.ai;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvUtility {

    public void filterDataset(String inputFilePath, String outputFilePath, String targetAuthor, int limit) throws IOException {
        try (Reader reader = new FileReader(inputFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
             FileWriter writer = new FileWriter(outputFilePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("tweet_id", "author_id", "inbound", "created_at", "text", "response_tweet_id", "in_response_to_tweet_id"))) {

            int count = 0;
            for (CSVRecord csvRecord : csvParser) {
                String authorId = csvRecord.get("author_id");
                if (targetAuthor.equalsIgnoreCase(authorId)) {
                    csvPrinter.printRecord(
                            csvRecord.get("tweet_id"),
                            authorId,
                            csvRecord.get("inbound"),
                            csvRecord.get("created_at"),
                            csvRecord.get("text"),
                            csvRecord.get("response_tweet_id"),
                            csvRecord.get("in_response_to_tweet_id")
                    );
                    count++;
                    if (count >= limit) {
                        break;
                    }
                }
            }
            csvPrinter.flush();
        }
    }

    public List<Tweet> readSubset(String filePath) throws IOException {
        List<Tweet> tweets = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                Tweet tweet = new Tweet(
                        csvRecord.get("tweet_id"),
                        csvRecord.get("author_id"),
                        Boolean.parseBoolean(csvRecord.get("inbound")),
                        csvRecord.get("created_at"),
                        csvRecord.get("text"),
                        csvRecord.get("response_tweet_id"),
                        csvRecord.get("in_response_to_tweet_id")
                );
                tweets.add(tweet);
            }
        }
        return tweets;
    }
}
