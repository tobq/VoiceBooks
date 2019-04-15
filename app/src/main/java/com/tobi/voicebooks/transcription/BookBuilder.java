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


    BookBuilder() {
        this(Instant.now());
    }

    BookBuilder(Instant creation) {
        this.creation = creation;
    }

    public void append(Word word) {
        if (buildingTitle) titleWords.add(word);
        else bookWords.add(word);
    }

    public void append(ApiResult result) {
        append(result.getWords());
        buildingTitle = false;
    }

    public void append(Word... words) {
        if (buildingTitle) Collections.addAll(titleWords, words);
        else Collections.addAll(bookWords, words);
    }

    /**
     * @param elapsed length of corresponding audio recording
     * @return the build book
     * @throws IllegalArgumentException when book title is empty
     */
    public Book build(Duration elapsed) throws IllegalArgumentException {
        return new Book(buildTranscript(), creation, elapsed);
    }

    public Transcript buildTranscript() {
        return new Transcript(titleWords.toArray(new Word[0]), bookWords.toArray(new Word[0]));
    }
}
