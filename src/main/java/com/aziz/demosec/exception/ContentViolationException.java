package com.aziz.demosec.exception;

public class ContentViolationException extends RuntimeException {
    private final String cleanedText;

    public ContentViolationException(String message, String cleanedText) {
        super(message);
        this.cleanedText = cleanedText;
    }

    public String getCleanedText() {
        return cleanedText;
    }
}
