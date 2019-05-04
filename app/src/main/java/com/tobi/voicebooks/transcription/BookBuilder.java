package com.tobi.voicebooks.transcription;

import com.tobi.voicebooks.models.Book;
import com.tobi.voicebooks.models.Transcript;
import com.tobi.voicebooks.models.Word;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

class BookBuilder {
    private final Instant creation;
    private final ArrayList<Word> titleWords = new ArrayList<>();
    private final ArrayList<Word> bookWords = new ArrayList<>();
    private boolean buildingTitle = true;
    private Duration elapsed = Duration.ZERO;


    BookBuilder() {
        this(Instant.now());
    }

    BookBuilder(Instant creation) {
        this.creation = creation;
    }

    public void append(ApiResult result) {
        Word[] words = result.getWords();
        if (buildingTitle) Collections.addAll(titleWords, words);
        else Collections.addAll(bookWords, words);
        elapsed = elapsed.plus(result.getDuration());
        buildingTitle = false;
    }

    /**
     * @return the build book
     * @throws IllegalArgumentException when book title is empty
     */
    public Book build() throws IllegalArgumentException {
        return new Book(buildTranscript(), creation, elapsed);
    }

    public Transcript buildTranscript() {
        return new Transcript(titleWords.toArray(new Word[0]), bookWords.toArray(new Word[0]));
    }
}
