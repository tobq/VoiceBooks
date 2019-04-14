package com.tobi.voicebooks.models;

import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.db.entities.TitleWord;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class Book {
    private final Instant creation;
    private final Transcript transcript;
    // bTODO: RESTRICT ACCESS TO TRANSCRIPT
    private final Duration duration;

    public Book(Transcript transcript, Instant creation, Duration duration) throws IllegalArgumentException {
        this.transcript = transcript;
        if (getTitle().length == 0)
            throw new NullPointerException("Empty title used for book");
        this.creation = creation;
        this.duration = duration;
    }

    /**
     * Gets the title of this book from its transcript
     * @return Word array {@link Word}[] ({@link TitleWord}s)
     */
    public Word[] getTitle() {
        return transcript.getTitle();
    }

    /**
     * @return Duration of book recording
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * @return Creation time of book
     */
    public Instant getCreation() {
        return creation;
    }

    /**
     * Handles the posting of this book, along with its
     * words to the Room database.
     *
     * Maps {@link Word}s to {@link BookWord}s before inserting
     *
     * @param database voice books database
     * @see com.tobi.voicebooks.db.VoiceBooksDatabase
     */
    public void post(VoiceBooksDatabase database) {
        // Insert and get ID of new book
        final BookEntity bookEntity = new BookEntity(creation, duration);
        final long bookId = database.books().insert(bookEntity);

        // map words to entities
        final TitleWord[] titleWordEntities = Arrays.stream(getTitle())
                .map(titleWord -> new TitleWord(bookId, titleWord))
                .toArray(TitleWord[]::new);
        final BookWord[] bookWordEntities = Arrays.stream(getContent())
                .map(titleWord -> new BookWord(bookId, titleWord))
                .toArray(BookWord[]::new);

        database.titleWords().insert(titleWordEntities);
        database.bookWords().insert(bookWordEntities);
    }

    /**
     * Gets content of book from its transcript
     *
     * @return Word array {@link Word}[] ({@link BookWord}s)
     */
    private Word[] getContent() {
        return transcript.getContent();
    }
}
