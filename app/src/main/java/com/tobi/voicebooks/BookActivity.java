package com.tobi.voicebooks;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tobi.voicebooks.Utils.Utils;
import com.tobi.voicebooks.db.Repository;
import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.transcription.WordPlayer;

import androidx.appcompat.app.AppCompatActivity;

public class BookActivity extends AppCompatActivity {
    public static final String ID_KEY = "id";

    private VoiceBooksDatabase database;
    private Repository repository;
    private long bookId;
    private FloatingActionButton playButton;
    private WordPlayer content;
    private WordPlayer title;
    private LinearLayout titleContainer;
    private LinearLayout rootContainer;
    private MediaPlayer player;
    private FloatingActionButton stopButton;

    public static void build(BookEntity book, Context packageContext) {
        build(book.id, packageContext);
    }

    public static void build(long bookId, Context packageContext) {
        final Intent intent = new Intent(packageContext, BookActivity.class);
        intent.putExtra(ID_KEY, bookId);
        packageContext.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        // get views
        playButton = findViewById(R.id.book_play);
        stopButton = findViewById(R.id.book_stop);
        titleContainer = findViewById(R.id.title_container);
        rootContainer = findViewById(R.id.root_container);


        // initialise database access
        database = Utils.getDatabase(this);
        repository = new Repository(database, this);

        // setup play button
        playButton.setOnClickListener(e -> play());
        stopButton.setOnClickListener(e -> stop());

        // get book ID from the bundle
        final Bundle extras = getIntent().getExtras();
        bookId = extras.getLong(ID_KEY);

        // initialise recycler views
        title = new WordPlayer(this, 70) {
            @Override
            protected BookWord[] getWords() {
                return repository.getBookTitle(bookId);
            }
        };
        titleContainer.addView(title);
        content = new WordPlayer(this, 40) {
            @Override
            protected BookWord[] getWords() {
                return repository.getBookContent(bookId);
            }
        };
        rootContainer.addView(content);
    }

    public void play() {
        // Start playing audio saved on disk
        player = MediaPlayer.create(
                this,
                Uri.fromFile(Utils.getVoiceBookPath(bookId, this))
        );
        player.setOnCompletionListener(mediaPlayer -> stop());
        player.start();

        // Start playing words along with audio
        title.play();
        content.play();

        // Transition buttons
        playButton.hide();
        stopButton.show();
    }

    public void stop() {
        // Start playing audio saved on disk
        player.release();

        // Start playing words along with audio
        title.stop();
        content.stop();

        // Transition buttons
        stopButton.hide();
        playButton.show();
    }
}
