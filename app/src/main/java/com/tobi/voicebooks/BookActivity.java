package com.tobi.voicebooks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.views.TranscriptView;

import androidx.appcompat.app.AppCompatActivity;

public class BookActivity extends AppCompatActivity {
    public static final String ID_KEY = "id";

    private TextView title;
    private VoiceBooksDatabase database;
    private Repository repository;
    private TranscriptView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        title = findViewById(R.id.book_title);
        content = findViewById(R.id.book_content);

        long bookId = 0;

        // initialise database access
        database = Utils.getDatabase(this);
        repository = new Repository(database, this);

        // get book ID from the bundle
        final Bundle extras = getIntent().getExtras();

        // setup title watcher
        repository.getBookTitle(bookId, newTitle -> {
            final String collectedTitle = Utils.formatWords(newTitle);
            title.setText(collectedTitle);
        });

        bookId = savedInstanceState.getLong("id");
        repository.getBookContent(bookId, newBookWords -> {
            final String collectedContent = Utils.formatWords(newBookWords);
            content.setPartial(collectedContent);
        });
    }
}
