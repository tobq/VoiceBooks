package com.tobi.voicebooks.transcription;

import android.app.Activity;

import com.tobi.voicebooks.Utils.AudioUtils;
import com.tobi.voicebooks.Utils.Utils;
import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.models.Book;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

abstract public class OutputtedBookBuilder extends BookBuilder {
    private final VoiceBooksDatabase db;
    private final Activity context;
    //    private final File tempFolder;
    //    private final File recordingsFolder;
    private final File tempFile;
    private final OutputStream tempFileStream;
    private final File filesDir;
    private File outputFile;

    public OutputtedBookBuilder(Activity context, VoiceBooksDatabase db) throws FileNotFoundException {
        super(context);
        this.context = context;
        this.db = db;
        filesDir = context.getFilesDir();

        final long tempID = creation.toEpochMilli();
        tempFile = new File(filesDir, Long.toString(tempID));
        tempFileStream = new FileOutputStream(tempFile);
    }

    @Override
    public void onRead(byte[] read, int byteCount) throws IOException {
        tempFileStream.write(read, 0, byteCount);
    }

    @Override
    public void close() throws Exception {
        tempFileStream.flush();
        tempFileStream.close();
        super.close();
    }

    @Override
    public void onClose(Book result) {
        // Ran on thread to prevent blocking of UI
        new Thread(() -> {
            final long bookId = result.post(db);
            // tODO: move to close method, so it can prevent sending of book
            //  even when this onClose method is overriden

            outputFile = Utils.getBookPath(bookId, context);
            try {
                AudioUtils.PCMToWAV(tempFile, outputFile, Transcriber.AUDIO_FORMAT);
            } catch (IOException e) {
                onError(e);
            }

        }).start();
    }

}
