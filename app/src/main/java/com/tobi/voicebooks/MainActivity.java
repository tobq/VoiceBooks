package com.tobi.voicebooks;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
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
    private TextView transcript;
    private RecyclerView bookList;
    private boolean SPEECH_RECOGNITION_AVAILABLE;
    private static final int REQUEST_AUDIO_CODE = 0;
    private final int VOICE_REQUEST_CODE = 10;
    private Toast REQUIRES_AUDIO_MESSAGE;
    private static boolean LISTENING = false;

    private final MediaRecorder recorder = new MediaRecorder();
    private SpeechRecognizer sr;

    private Animation fadeIn = new AlphaAnimation(0, 1);
    private Animation fadeOut = new AlphaAnimation(1, 0);
    private FloatingActionButton recordButton;
    private LinearLayout verticalLayout;

    {
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(1000);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setDuration(1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        REQUIRES_AUDIO_MESSAGE = Toast.makeText(this, "Voice Books requires access to your microphone in order to record", Toast.LENGTH_SHORT);
        setContentView(R.layout.activity_main);
        SPEECH_RECOGNITION_AVAILABLE = SpeechRecognizer.isRecognitionAvailable(this);
        recordButton = findViewById(R.id.record);
        verticalLayout = findViewById(R.id.vertical_layout);
        bookList = findViewById(R.id.book_list);
        transcript = (TextView) LayoutInflater.from(this).inflate(R.layout.transcript_fragment, verticalLayout, false);

        recordButton.setOnClickListener(v -> {
            if (LISTENING) {
                sr.stopListening();
                LISTENING = false;
            } else startTranscription();

        });

        initialiseSpeechRecogniser();
        initialiseRecorder();
        initialiseBookList();
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

    public void startTranscription() {
        LISTENING = true;
        int index = verticalLayout.indexOfChild(bookList);
        if (index != -1) {
            verticalLayout.addView(transcript, index);
            verticalLayout.removeView(bookList);
        }
        startListening();
    }

    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // the following appears to be a requirement, but can be a "dummy" value
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.tobi.voicebooks");
        // define any other intent extras you want
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");

        // start playback of audio clip here

        // this will start the speech recognizer service in the background
        // without starting a separate activity
        sr.startListening(intent);
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

    private void initialiseSpeechRecogniser() {
        if (SPEECH_RECOGNITION_AVAILABLE) {
            if (sr != null) sr.destroy();
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    setTranscription(result);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partialResult = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    setTranscription(partialResult);
                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
    }

    private void setTranscription(ArrayList<String> result) {
        setTranscription(result.get(0));
    }

}
