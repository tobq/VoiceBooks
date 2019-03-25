package com.tobi.voicebooks;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tobi.voicebooks.transcription.Book;
import com.tobi.voicebooks.transcription.Transcriber;
import com.tobi.voicebooks.views.BookAdapter;
import com.tobi.voicebooks.views.DurationView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


// TODO: THREADED WORK -> ASYNC TASKS
// TODO: for unfinished result, distribute words over length of sentence (split by space)

public class HomeActivity extends AppCompatActivity {
    private final static int REQUEST_AUDIO_CODE = 0;
    private final int BACKGROUND_FADE_IN_DURATION = 150;
    private final int BACKGROUND_FADE_OUT_DURATION = 75;
    // TODO: FIX FADING OF ENTIRE VIEW

    private String appName;
    private RecyclerView booksView;
    private Toast REQUIRES_AUDIO_MESSAGE;
    private BookAdapter booksAdapter;
    private ArrayList<Book> books;
    private ImageButton settingsButton;
    private TextView title;

    private Transcriber.Builder transcriber;
    private TextView bookTitle;
    private TextView bookTranscript;
    private TextView transcript;

    private View decorView;
    private ValueAnimator colourAnimation;
    private FloatingActionButton recordStop;
    private DurationView recordDuration;
    private Timer durationUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHomeView();

        appName = getResources().getString(R.string.app_name);
        REQUIRES_AUDIO_MESSAGE = Toast.makeText(this, appName + " requires access to your microphone in order to transcribe", Toast.LENGTH_LONG);
        decorView = getWindow().getDecorView();

        // Set spacing from at bottom of booksView to avoid FloatingActionButton hiding bottom details
        Resources resources = getResources();
        int buttonSize = resources.getDimensionPixelSize(R.dimen.button_size);
        int buttonSpacing = resources.getDimensionPixelSize(R.dimen.button_spacing);
        booksView.setPadding(0, 0, 0, buttonSize + buttonSpacing * 2);

        if (!hasRecordPermission()) requestRecordPermission();

        // TODO: TEST
//        transitionBackground(Color.RED, 4000);
    }

    private void recordClicked() {
        if (!hasRecordPermission()) {
            requestRecordPermission();
            return;
        }
        try {
            startTranscribing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_CODE);
    }

    /**
     * Starts transcription
     * Fades background to {@link R.color#recording}
     *
     * @see Transcriber
     */
    @SuppressWarnings("JavadocReference")
    public void startTranscribing() throws Exception {
        initialiseTranscriber();

        // initialise recorder view
        setContentView(R.layout.activity_recording);
        bookTitle = findViewById(R.id.transcript_title);
        recordDuration = findViewById(R.id.record_duration);
        transcript = findViewById(R.id.transcript);
        recordStop = findViewById(R.id.record_stop);
        recordStop.setOnClickListener(v -> stopTranscribing());

        // start updating the record duration value
        durationUpdater = new Timer();
        durationUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> recordDuration.setDuration(transcriber.getEstimateRecordDuration()));
            }
        }, 0, 200);

        // Asset record permission
        // reset bookTranscript
//        title.setText(null);
//        bookTranscript.setText(null);
        // Transform view
//        booksView.setVisibility(View.GONE);
//        settingsButton.setVisibility(View.GONE);
//        transcriptContainer.setVisibility(View.VISIBLE);

        System.out.println("FADE IN");
        transitionBackground(getResources().getColor(R.color.recording), BACKGROUND_FADE_IN_DURATION);

        // Start live transcription
        transcriber.start();
    }

    @Override
    public void onBackPressed() {
        if (transcriber.isTranscribing()) stopTranscribing();
        else super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) initialiseTranscriber();
                else REQUIRES_AUDIO_MESSAGE.show();

        }
    }

    /**
     * closes application if transcriber couldn't be initialised
     */
    private void initialiseTranscriber() {
        try {
            if (transcriber != null) transcriber.close();
            transcriber = new Transcriber.Builder(
                    Transcriber.generateMicSource(),
                    Utils.getCurrentLocale(this),
                    new Transcriber.Listener() {
                        @Override
                        public void onResult(Book result) {
                            runOnUiThread(() -> setTranscription(result));
                        }

                        @Override
                        public void onClose(Book result) {
                            if (result != null)
                                runOnUiThread(() -> booksAdapter.append(result));
                        }

                        @Override
                        public void onError(Throwable t) {
                            t.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(HomeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onRead(byte[] data) {

                        }
                    });
        } catch (Exception e) {
            finishAndRemoveTask();
        }
    }

    public void setTranscription(Book book) {
        bookTitle.setText(book.getTitle().toString());
        this.transcript.setText(book.toString());
    }

    /**
     * Overriden to stop transcription
     * Fades background back to {@link R.color#colorPrimary}
     *
     * @see Transcriber
     */

    public void stopTranscribing() {
        initHomeView();

        transcriber.stop();

        // stop upating the record duration counter
        durationUpdater.cancel();

//        bookTitle.setText(appName);
//        transcriptContainer.setVisibility(View.GONE);
//        settingsButton.setVisibility(View.VISIBLE);
//        booksView.setVisibility(View.VISIBLE);

        System.out.println("FADE OUT");
        transitionBackground(getResources().getColor(R.color.colorPrimary), BACKGROUND_FADE_IN_DURATION);
    }

    private void initHomeView() {
        // initialise home view
        setContentView(R.layout.activity_home);
        settingsButton = findViewById(R.id.settings_button);
        title = findViewById(R.id.title);
        FloatingActionButton recordButton = findViewById(R.id.record);
        recordButton.setOnClickListener(v -> recordClicked());
        // initialise recycler view
        booksView = findViewById(R.id.book_list);
        booksView.setLayoutManager(new LinearLayoutManager(this));
        books = new ArrayList<>();
        booksAdapter = new BookAdapter(books, this);
        booksView.setAdapter(booksAdapter);
    }

    void transitionBackground(Color colour, int duration) {
        transitionBackground(colour.toArgb(), duration);
    }

    void transitionBackground(@ColorInt int colourCode, int duration) {
        if (colourAnimation != null) colourAnimation.cancel();
        colourAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), getBackgroundColour(), colourCode);
        colourAnimation.setDuration(duration); // milliseconds
        colourAnimation.addUpdateListener(animator -> setBackgroundColour((int) animator.getAnimatedValue()));
        colourAnimation.start();
    }

    @ColorInt
    public int getBackgroundColour() {
        return decorView.getSolidColor();
    }

    public void setBackgroundColour(@ColorInt int colourCode) {
        decorView.setBackgroundColor(colourCode);
    }

    public void setBackgroundColour(Color colour) {
        setBackgroundColour(colour.toArgb());
    }
}