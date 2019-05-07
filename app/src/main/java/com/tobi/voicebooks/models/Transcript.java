package com.tobi.voicebooks.models;

import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.db.entities.TitleWord;

public class Transcript {
    /**
     * Utility empty (IMMUTABLE) transcript
     */
    public static final Transcript EMPTY = new Transcript(new Word[0], new Word[0]);

    private final Word[] titleWords;
    private final Word[] bookWords;

    public Transcript(Word[] titleWords, Word[] bookWords) {
        this.titleWords = titleWords;
        this.bookWords = bookWords;
    }


    /**
     * Gets the title of this transcript
     *
     * @return Word array {@link Word}[] ({@link TitleWord}s)
     */
    public Word[] getTitle() {
        return titleWords;
    }


    /**
     * Gets the content of this transcript
     *
     * @return Word array {@link Word}[] ({@link BookWord}s)
     */
    public Word[] getContent() {
        return bookWords;
    }
}
