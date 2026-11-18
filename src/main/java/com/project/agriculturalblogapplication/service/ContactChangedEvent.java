package com.project.agriculturalblogapplication.service;

/** A user's e-mail or mobile number was added or changed and needs confirming; published after it is saved. */
public record ContactChangedEvent(Long userId, boolean emailChanged, boolean phoneChanged) {
}
