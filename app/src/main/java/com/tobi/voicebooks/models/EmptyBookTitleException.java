package com.tobi.voicebooks.models;

public class EmptyBookTitleException extends RuntimeException {
    public EmptyBookTitleException() {
        super("Empty title used for book");
    }

}
