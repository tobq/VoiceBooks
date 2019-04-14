package com.tobi.voicebooks;

import com.tobi.voicebooks.db.BookDao;
import com.tobi.voicebooks.db.BookWordDao;
import com.tobi.voicebooks.db.TitleWordDao;
import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.db.entities.TitleWord;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;


public class Repository {
    private final BookDao booksDao;
    private final TitleWordDao titleWordsDao;
    private final BookWordDao bookWordsDao;
    private final LiveData<List<BookEntity>> books;
    private final AppCompatActivity activity;

    public Repository(VoiceBooksDatabase database, AppCompatActivity activity) {
        this.activity = activity;
        booksDao = database.books();
        titleWordsDao = database.titleWords();
        bookWordsDao = database.bookWords();

        books = booksDao.load();
    }

    public ObserverCanceller getBookTitle(BookEntity book, Observer<? super List<TitleWord>> titleObserver) {
        return getBookTitle(book.id, titleObserver);
    }

    public ObserverCanceller getBookTitle(long bookId, Observer<? super List<TitleWord>> titleObserver) {
        final LiveData<List<TitleWord>> liveTitleWords = titleWordsDao.load(bookId);
        return observe(liveTitleWords, titleObserver);
    }

    /**
     * Observes an instance of LiveData
     *
     * @param liveData to be observed
     * @param observer the callback called on a change
     * @param <T>      type of Live Data
     * @return A function used to cancel the observer, before {@link #activity} closes
     */
    private <T> ObserverCanceller observe(LiveData<T> liveData, Observer<? super T> observer) {
        liveData.observe(activity, observer);
        return () -> liveData.removeObserver(observer);
    }

    public ObserverCanceller getBookContent(BookEntity book, Observer<? super List<BookWord>> contentObserver) {
        return getBookContent(book.id, contentObserver);
    }

    public ObserverCanceller getBookContent(long bookId, Observer<? super List<BookWord>> contentObserver) {
        final LiveData<List<BookWord>> liveTitleWords = bookWordsDao.load(bookId);
        return observe(liveTitleWords, contentObserver);
    }

    public ObserverCanceller getBooks(Observer<? super List<BookEntity>> booksObserver) {
        return observe(books, booksObserver);
    }

    public interface ObserverCanceller {
        void cancel();
    }
}