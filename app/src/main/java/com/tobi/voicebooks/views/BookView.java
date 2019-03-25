package com.tobi.voicebooks.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tobi.voicebooks.R;
import com.tobi.voicebooks.transcription.Book;

import androidx.annotation.Nullable;

public class BookView extends LinearLayout {
    private final TextView title;
    private final DurationView length;

    {
        inflate(getContext(), R.layout.book_item, this);
        title = findViewById(R.id.book_item_title);
        length = findViewById(R.id.book_item_length);
    }

    public BookView(Context context) {
        super(context);
    }

    public BookView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BookView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBook(Book book) {
        title.setText(book.getTitle().toString());
        length.setDuration(book.getLength());
    }
}
