package com.tobi.voicebooks;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_AUDIO_CODE = 0;
    private final int BACKGROUND_FADE_IN_DURATION = 150;
    private final int BACKGROUND_FADE_OUT_DURATION = 75;
    private final int VOICE_REQUEST_CODE = 10;

    private final MediaRecorder recorder = new MediaRecorder();
    private boolean TRANSCRIBING = false;

    /**
     * Views initial initialised within {@link #onCreate}
     */
    private LinearLayout transcriptContainer;
    private TextView transcript;
    private RecyclerView bookList;
    private Toast TRANSCRIBER_ERROR;
    private Toast REQUIRES_AUDIO_MESSAGE;
    private TransitionDrawable backgroundFade;
    private Transcriber transcriber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        REQUIRES_AUDIO_MESSAGE = Toast.makeText(this, "Voice Books requires access to your microphone in order to record", Toast.LENGTH_SHORT);

        bookList = findViewById(R.id.book_list);
        transcript = findViewById(R.id.transcript);
        transcriptContainer = findViewById(R.id.transcript_container);

        backgroundFade = (TransitionDrawable) getWindow().getDecorView().getBackground();

        FloatingActionButton recordButton = findViewById(R.id.record);
        recordButton.setOnClickListener(v -> {
            if (TRANSCRIBING) stopTranscribing();
            else startTranscribing();
        });

        // Set spacing from at bottom of bookList to avoid FloatingActionButton hiding bottom details
        Resources resources = getResources();
        int buttonSize = resources.getDimensionPixelSize(R.dimen.button_size);
        int buttonSpacing = resources.getDimensionPixelSize(R.dimen.button_spacing);
        bookList.setPadding(0, 0, 0, buttonSize + buttonSpacing * 2);

        try {
            transcriber = new LiveTranscriber(this, results -> {
                stopTranscribing();
                this.setTranscription(results);
            }, this::setTranscription);
        } catch (Exception e) {
            // UnsupportedOperationException
            TRANSCRIBER_ERROR = Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT);
            TRANSCRIBER_ERROR.show();
        }

        initialiseBookList();
    }

    @Override
    public void onBackPressed() {
        if (TRANSCRIBING) stopTranscribing();
        else super.onBackPressed();
    }

    /**
     * Starts transcription
     * Fades background to {@link R.color.recording}
     *
     * @see Transcriber
     */
    @SuppressWarnings("JavadocReference")
    public void startTranscribing() {
        if (TRANSCRIBING) return;
        if (transcriber == null) {
            TRANSCRIBER_ERROR.show();
            return;
        }

        //        if (TRANSCRIBING == TranscriptionState.Running) return;
        transcript.setText(null);

        System.out.println("FADE IN");
        backgroundFade.startTransition(BACKGROUND_FADE_IN_DURATION);

        transcriptContainer.setVisibility(View.VISIBLE);
        bookList.setVisibility(View.GONE);
        TRANSCRIBING = false;
        // Speech recognition started
        transcriber.start();
    }

    /**
     * Starts transcription
     * Fades background back to {@link R.color.colorPrimary}
     *
     * @see Transcriber
     */
    @SuppressWarnings("JavadocReference")
    private void stopTranscribing() {
        System.out.println("FADE OUT");
        if (TRANSCRIBING)
            backgroundFade.reverseTransition(BACKGROUND_FADE_OUT_DURATION);
        TRANSCRIBING = false;
        // TODO: TRANSITION FROM 1 TO 0, INSTEAD OF REVERSE (LEADING TO BUG)

        // Show bookList again
        transcriptContainer.setVisibility(View.GONE);
        bookList.setVisibility(View.VISIBLE);

        transcriber.stop();
    }

    public void recordAudio(String fileName) throws IOException {
        assertRecordPermission();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, fileName);
        System.out.println(getFilesDir());
        File output = new File(getFilesDir(), fileName);
        recorder.setOutputFile(output);

        recorder.prepare();
        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("RECORDING ");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setButton("Stop recording", (dialog, whichButton) -> {
            mProgressDialog.dismiss();
            recorder.stop();
            recorder.release();
        });

        mProgressDialog.setOnCancelListener(p1 -> {
            recorder.stop();
            recorder.release();
        });
        recorder.start();
        mProgressDialog.show();
    }

    private boolean assertRecordPermission() {
        boolean hasRecordPermission = hasRecordPermission();
        if (!hasRecordPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_CODE);
        }
        return hasRecordPermission;
    }

    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void setTranscription(String transcription) {
        transcript.setText(transcription);
    }

    public void getSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initialiseRecorder();
                } else REQUIRES_AUDIO_MESSAGE.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case VOICE_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(this, result.get(0), Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void initialiseRecorder() {
        if (assertRecordPermission()) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        } else {
            REQUIRES_AUDIO_MESSAGE.show();
        }
    }

    private void initialiseBookList() {
        final Book[] books = new Book[20];

        for (int i = 0; i < books.length; i++) {
            String title = SimpleRandomSentences.randomSentence();
            Date creation = new Date();
            Duration length = Duration.ofSeconds((int) (Math.random() * (3600 * 10)));

            System.out.println(title);
            System.out.println(creation);
            System.out.println(length);
            books[i] = new Book(title, creation, length);
        }

        BookAdapter adapter = new BookAdapter(books, this);
        bookList.setAdapter(adapter);
        bookList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setTranscription(ArrayList<String> result) {
        setTranscription(result.get(0));
    }

}