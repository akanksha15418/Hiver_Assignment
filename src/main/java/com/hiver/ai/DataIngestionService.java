package com.hiver.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class DataIngestionService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);

    @Autowired
    private CsvUtility csvUtility;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void run(String... args) throws Exception {
        String originalData = "dummy_dataset.csv";
        String subsetData = "subset_dataset.csv";

        File originalFile = new File(originalData);
        if (!originalFile.exists()) {
            logger.warn("Original dataset {} not found. Skipping data ingestion.", originalData);
            return;
        }

        File subsetFile = new File(subsetData);
        if (!subsetFile.exists()) {
            logger.info("Filtering dataset to create subset...");
            csvUtility.filterDataset(originalData, subsetData, "AppleSupport", 500);
        }

        logger.info("Loading subset dataset into Vector Store...");
        List<Tweet> tweets = csvUtility.readSubset(subsetData);
        
        for (Tweet tweet : tweets) {
            String text = tweet.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            
            Metadata metadata = Metadata.metadata("tweet_id", tweet.tweetId())
                    .put("author_id", tweet.authorId());
            
            TextSegment segment = TextSegment.from(text, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        logger.info("Data ingestion complete. {} tweets loaded into the vector store.", tweets.size());
    }
}
