package com.audy.api;

// Represents an exception that occurs during API client operations.
// (RF-11, RNF-02)
// Prevents system crashes and informs the UI if the action can be retried.
public class APIClientException extends Exception {
    private final boolean retryable;

    public APIClientException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}