package com.tobi.voicebooks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.io.Closeable;
import java.util.ArrayList;

public class Transcriber implements Closeable {
    private final SpeechRecognizer sr;
    private final Context activity;
    private final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    protected Transcriber(Context activity, Listener listener) throws Exception {
        if (!SpeechRecognizer.isRecognitionAvailable(activity))
            throw new UnsupportedOperationException("Speech recognition not supported");

        // setup recogniser
        sr = SpeechRecognizer.createSpeechRecognizer(activity);
        sr.setRecognitionListener(listener);

        // Setup intent
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.tobi.voicebooks");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        this.activity = activity;
    }

    public Transcriber(Context activity, ResultHandler resultHandler) throws Exception {
        this(activity, new Listener(resultHandler));
    }

    public void start() {
        // Speech recognition started
        sr.startListening(intent);
    }

    public void stop() {
        // Speech recognition started
        sr.stopListening();
    }

    @Override
    public void close() {
        sr.destroy();
    }
}

class LiveTranscriber extends Transcriber {

    public LiveTranscriber(Context activity, ResultHandler resultHandler, ResultHandler partialResultHandler) throws Exception {
        super(activity, new PartialListener(resultHandler, partialResultHandler));
    }

    public LiveTranscriber(Context activity, ResultHandler resultHandler) throws Exception {
        this(activity, resultHandler, resultHandler);
    }
}

interface ResultHandler {
    void handler(ArrayList<String> results);
}

class Listener implements RecognitionListener {
    protected final ResultHandler resultHandler;

    Listener(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

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
        resultHandler.handler(result);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}

class PartialListener extends Listener {
    protected final ResultHandler partialResultHandler;

    PartialListener(ResultHandler resultHandler, ResultHandler partialResultHandler) {
        super(resultHandler);
        this.partialResultHandler = partialResultHandler;
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> partialResult = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        partialResultHandler.handler(partialResult);
    }
}