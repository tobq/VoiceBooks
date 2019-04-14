package com.tobi.voicebooks;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.views.TranscriptView;

import androidx.appcompat.app.AppCompatActivity;

public class BookActivity extends AppCompatActivity {
    private TextView title;
    private VoiceBooksDatabase database = Utils.getDatabase(this);
    private Repository repository = new Repository(database, this);
    private TranscriptView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        title = findViewById(R.id.book_title);
        content = findViewById(R.id.book_content);

        long bookId = 0;
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
