package com.tobi.voicebooks.views;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tobi.voicebooks.R;
import com.tobi.voicebooks.Repository;
import com.tobi.voicebooks.Utils;
import com.tobi.voicebooks.db.entities.BookEntity;

public class BookView extends LinearLayout {
    private final TextView title;
    private final DurationView length;
    private Repository.ObserverCanceller observerCanceller;
    private Repository repository;

    {
        inflate(getContext(), R.layout.book_item, this);

        title = findViewById(R.id.book_item_title);
        length = findViewById(R.id.book_item_length);
    }

    public BookView(Context context, Repository repository) {
        super(context);
        this.repository = repository;
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
    }

    public void setBook(BookEntity book) {
        if (observerCanceller != null) observerCanceller.cancel();
        observerCanceller = repository.getBookTitle(book, newTitle -> {
            final String collectedTitle = Utils.formatWords(newTitle);
            title.setText(collectedTitle);
        });

        length.setDuration(book.duration);
    }
}
