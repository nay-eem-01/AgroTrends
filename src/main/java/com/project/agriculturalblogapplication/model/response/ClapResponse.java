package com.project.agriculturalblogapplication.model.response;

/** A post's total claps and how many of them are the caller's. */
public record ClapResponse(long totalClaps, int myClaps) {
}
