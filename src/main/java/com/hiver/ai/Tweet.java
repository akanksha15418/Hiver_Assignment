package com.hiver.ai;

public record Tweet(String tweetId, String authorId, boolean inbound, String createdAt, String text, String responseTweetId, String inResponseToTweetId) {
}
