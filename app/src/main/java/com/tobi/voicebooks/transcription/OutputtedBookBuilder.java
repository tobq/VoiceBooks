package com.tobi.voicebooks.transcription;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

abstract public class OutputtedBookBuilder extends BookBuilder {
    private final OutputStream stream;

    public OutputtedBookBuilder(Locale locale, OutputStream stream) {
        super(locale);
        this.stream = stream;
    }

    @Override
    public void onRead(byte[] read) throws IOException {
        stream.write(read);
    }

    @Override
    public void close() throws Exception {
        super.close();
        stream.flush();
        stream.close();
    }
}
