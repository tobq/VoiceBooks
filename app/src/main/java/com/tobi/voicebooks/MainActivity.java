package com.tobi.voicebooks;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_AUDIO_CODE = 0;
    private static final int MIC_SAMPLE_RATE = 16000; // Hz
    private static final int MIC_AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int MIC_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIC_BUFFER_SIZE = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, MIC_AUDIO_CHANNELS, MIC_AUDIO_ENCODING);
    private final int BACKGROUND_FADE_IN_DURATION = 150;
    private final int BACKGROUND_FADE_OUT_DURATION = 75;
    AudioRecord recorder = new AudioRecord.Builder()
            .setAudioFormat(new AudioFormat.Builder()
                    .setSampleRate(MIC_SAMPLE_RATE)
                    .setEncoding(MIC_AUDIO_ENCODING)
                    .setChannelMask(MIC_AUDIO_CHANNELS)
                    .build())
            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            // unprocessed audio source because applying signal processing algorithms
            // such as noise reduction or gain control reduces recognition accuracy
            .setBufferSizeInBytes(MIC_BUFFER_SIZE)
            .build();
    private boolean isRecording = false;
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
    private OkHttpClient client;
    // Variables


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
        recordButton.setOnClickListener(v -> toggleTranscription());

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
        } catch (UnsupportedOperationException e) {
            TRANSCRIBER_ERROR = Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT);
            TRANSCRIBER_ERROR.show();
        }

        initialiseBookList();
    }

    private void toggleTranscription() {
//        if (TRANSCRIBING) stopTranscribing();
//        else startTranscribing();
        if (isRecording) stopTranscribing();
        else startTranscribing();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else REQUIRES_AUDIO_MESSAGE.show();
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


    private void streamToCloud() {
        Request request = new Request.Builder().url("ws://voicebooks.herokuapp.com").build();
        APIListener listener = new APIListener();
        WebSocket ws = client.newWebSocket(request, listener);

        //Send locale
        ws.send(getCurrentLocale().toString());

        byte[] data = new byte[MIC_BUFFER_SIZE];
        while (isRecording) {
            recorder.read(data, 0, MIC_BUFFER_SIZE);
            ws.send(ByteString.of(data));
        }
        ws.close(APIListener.NORMAL_CLOSURE_STATUS, "Stopped recording");
    }
}