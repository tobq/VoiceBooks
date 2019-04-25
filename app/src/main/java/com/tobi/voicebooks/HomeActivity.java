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
import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.models.Book;
import com.tobi.voicebooks.models.Transcript;
import com.tobi.voicebooks.transcription.Transcriber;
import com.tobi.voicebooks.transcription.TranscriberBuilder;
import com.tobi.voicebooks.views.BookAdapter;
import com.tobi.voicebooks.views.DurationView;
import com.tobi.voicebooks.views.TranscriptView;

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

    private Toast REQUIRES_AUDIO_MESSAGE;

    private TranscriberBuilder transcriber;
    private TextView bookTitle;
    private TranscriptView transcript;

    private View decorView;
    private ValueAnimator colourAnimation;
    private DurationView recordDuration;
    private Timer durationUpdater;
    private VoiceBooksDatabase database;
    private Repository repository;
    private FloatingActionButton recordStop;
    private FloatingActionButton recordPause;
    private FloatingActionButton recordResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String APP_NAME = Utils.getAppName(this);
        REQUIRES_AUDIO_MESSAGE = Toast.makeText(this, APP_NAME + " requires access to your microphone in order to transcribe", Toast.LENGTH_LONG);

        decorView = getWindow().getDecorView();
        if (requiresRecordPermission()) requestRecordPermission();

        // Initialise database
        database = Utils.getDatabase(this);
        repository = new Repository(database, this);

        initHomeView();
    }

    private void recordClicked() {
        if (requiresRecordPermission()) {
            requestRecordPermission();
            return;
        }
        try {
            initTranscriptView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean requiresRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_CODE);
    }

    /**
     * Initialised transcript view
     *
     * @see #startRecording
     */

    @SuppressWarnings("JavadocReference")
    public void initTranscriptView() throws Exception {
        initialiseTranscriber();

        // initialise recorder view
        setContentView(R.layout.activity_recording);
        bookTitle = findViewById(R.id.transcript_title);
        recordDuration = findViewById(R.id.record_duration);
        transcript = findViewById(R.id.transcript);
        recordStop = findViewById(R.id.record_stop);
        recordPause = findViewById(R.id.record_pause);
        recordResume = findViewById(R.id.record_resume);

        // Setup on click listeners
        recordStop.setOnClickListener(v -> stopTranscription());
        recordPause.setOnClickListener(v -> pauseTranscription());
        recordResume.setOnClickListener(v -> {
            try {
                startRecording();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "An error occurred while trying to resume transcription", Toast.LENGTH_SHORT).show();
            }
        });

        startRecording();
    }
//TODO: IMPLEMENT LOADING ON WEBSOCKET CONNECT, WEN SERVER IS STARTING UP

    /**
     * Starts transcription
     * Fades background to {@link R.color#recordingColour}
     *
     * @see Transcriber
     */
    private void startRecording() throws Exception {
        // Initialise timer used to update the duration counter
        durationUpdater = new Timer();
        // start updating the record duration value
        durationUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> recordDuration.setDuration(transcriber.getElapsed()));
            }
        }, 0, 500);

        // fade background
        transitionBackground(getResources().getColor(R.color.recordingColour), BACKGROUND_FADE_IN_DURATION);

        // Start live transcription
        transcriber.start();

        // setup button visibilities
        recordResume.hide();
        recordPause.show();
    }

    @Override
    public void onBackPressed() {
        if (transcriber.isTranscribing()) {
            try {
                stopTranscription();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else super.onBackPressed();
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
            transcriber = new TranscriberBuilder(
                    Utils.getCurrentLocale(this),
                    new Transcriber.Listener() {
                        @Override
                        public void onPartial(String partialResult) {
                            runOnUiThread(() -> transcript.setPartial(partialResult));
                        }

                        @Override
                        public void onUpdate(Transcript update) {
                            runOnUiThread(() -> setTranscription(update));
                        }

                        @Override
                        public void onClose(Book result) {
                            // Ran on thread to prevent blocking of UI
                            new Thread(() -> result.post(database)).start();
                        }

                        @Override
                        public void onError(Throwable t) {
                            t.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(HomeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                                pauseTranscription();
                            });
                        }
                    });
        } catch (Exception e) {
            finishAndRemoveTask();
        }
    }

    public void setTranscription(Transcript transcription) {
        bookTitle.setText(Utils.formatWords(transcription.getTitle()));
        transcript.setTranscript(transcription);
    }

    /**
     * Pause transcription
     * Fades background back to {@link R.color#pauseColour}
     *
     * @see Transcriber
     */

    public void pauseTranscription() {
        // exit if not transcribing
        if (!transcriber.isTranscribing()) return;

        // stop updating the record duration value
        stopDurationUpdater();

        // fade background
        transitionBackground(getResources().getColor(R.color.pauseColour), BACKGROUND_FADE_IN_DURATION);

        // Start live transcription
        transcriber.pause();

        // setup button visibilities
        recordPause.hide();
        recordResume.show();
    }

    private void stopDurationUpdater() {
        durationUpdater.cancel();
        durationUpdater.purge();
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

    /**
     * Stop transcription
     * Fades background back to {@link R.color#primaryColour}
     *
     * @see Transcriber
     */

    public void stopTranscription() {
        initHomeView();
        try {
            transcriber.close();
        } catch (NullPointerException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // stop updating the record duration counter
        stopDurationUpdater();

        transitionBackground(getResources().getColor(R.color.primaryColour), BACKGROUND_FADE_IN_DURATION);
    }

    private void initHomeView() {
        // initialise home view
        setContentView(R.layout.activity_home);
        ImageButton settingsButton = findViewById(R.id.settings_button);
        TextView title = findViewById(R.id.title);
        FloatingActionButton recordStart = findViewById(R.id.record_resume);
        recordStart.setOnClickListener(v -> recordClicked());

        // initialise recycler view
        RecyclerView booksList = findViewById(R.id.book_list);
        booksList.setLayoutManager(new LinearLayoutManager(this));
        BookAdapter booksAdapter = new BookAdapter(repository, this);
        booksList.setAdapter(booksAdapter);

        // Set spacing from at bottom of booksList to avoid FloatingActionButton hiding bottom details
        Resources resources = getResources();
        int buttonSize = resources.getDimensionPixelSize(R.dimen.button_size);
        int buttonSpacing = resources.getDimensionPixelSize(R.dimen.button_spacing);
        booksList.setPadding(0, 0, 0, buttonSize + buttonSpacing * 2);
    }

    void transitionBackground(Color colour, int duration) {
        transitionBackground(colour.toArgb(), duration);
    }
}