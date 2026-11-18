package com.project.agriculturalblogapplication.model.response;

/**
 * What a member's work did: public answers, accepted ones, views on the questions they answered, AI answers that cite
 * their vetted answers, and AI citations of their stories.
 */
public record UserImpactResponse(long answers, long acceptedAnswers, long readersReached, long citedInAiAnswers,
                                 long storyCitations) {
}
