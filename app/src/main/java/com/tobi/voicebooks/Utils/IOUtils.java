package com.tobi.voicebooks.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOUtils {
    /**
     * Size of buffer used for transfer, by default
     */
    private static final int DEFAULT_TRANSFER_BUFFER_SIZE = 10 * 1024;

    private IOUtils() {
    }

    /**
     * Writes a string in big endian form to an output stream
     *
     * @param output stream
     * @param data   string
     * @throws IOException
     */
    public static void writeToOutput(OutputStream output, String data) throws IOException {
        for (int i = 0; i < data.length(); i++)
            output.write(data.charAt(i));
    }

    /**
     * Writes an int in little endian form to an output stream
     *
     * @param output stream
     * @param data   string
     * @throws IOException
     */
    public static void writeToOutput(OutputStream output, int data) throws IOException {
        output.write(data);
        output.write(data >> 8);
        output.write(data >> 16);
        output.write(data >> 24);
    }

    /**
     * Writes a short in little endian form to an output stream
     *
     * @param output stream
     * @param data   string
     * @throws IOException
     */
    public static void writeToOutput(OutputStream output, short data) throws IOException {
        output.write(data);
        output.write(data >> 8);
    }

    /**
     * Pipes (copies) data from source stream to output stream.
     * overloaded to use the {@link #DEFAULT_TRANSFER_BUFFER_SIZE}
     * Pipes (copies) data from source stream to output stream
     *
     * @param source input stream
     * @param output stream
     * @return the amount of data piped
     * @throws IOException error from input/output stream
     */
    public static long pipe(InputStream source, OutputStream output)
            throws IOException {
        return pipe(source, output, DEFAULT_TRANSFER_BUFFER_SIZE);
    }

    /**
     * Pipes (copies) data from source stream to output stream
     *
     * @param source     input stream
     * @param output     stream
     * @param bufferSize bufferSize to use for transfer of data
     * @return the amount of data piped
     * @throws IOException error from input/output stream
     */

    public static long pipe(InputStream source, OutputStream output, int bufferSize) throws IOException {
        long read = 0L;
        byte[] buffer = new byte[bufferSize];
        for (int n; (n = source.read(buffer)) != -1; read += n)
            output.write(buffer, 0, n);

        return read;
    }
}
