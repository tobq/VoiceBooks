package com.tobi.voicebooks.Utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.tobi.voicebooks.transcription.Transcriber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.tobi.voicebooks.Utils.IOUtils.writeToOutput;

/**
 * Utils for handling PCM audio
 */
public final class AudioUtils {
    private AudioUtils() {
    }

    /**
     * overload using AudioFormat
     *
     * @param input  raw PCM data
     *               limit of file size for wave file: < 2^(2*4) - 36 bytes (~4GB)
     * @param output file to encode to in wav format
     * @param format corresponding audioformat for PCM data
     * @throws IOException in event of an error between input/output files
     * @see <a href="http://soundfile.sapp.org/doc/WaveFormat/">soundfile.sapp.org/doc/WaveFormat</a>
     */
    static public void PCMToWAV(File input, File output, AudioFormat format) throws IOException {
        PCMToWAV(
                input,
                output,
                format.getChannelCount(),
                format.getSampleRate(),
                getBitsPerSample(format)
        );
    }

    /**
     * @param input         raw PCM data
     *                      limit of file size for wave file: < 2^(2*4) - 36 bytes (~4GB)
     * @param output        file to encode to in wav format
     * @param channelCount  number of channels: 1 for mono, 2 for stereo, etc.
     * @param sampleRate    sample rate of PCM audio
     * @param bitsPerSample bits per sample, i.e. 16 for PCM16
     * @throws IOException in event of an error between input/output files
     * @see <a href="http://soundfile.sapp.org/doc/WaveFormat/">soundfile.sapp.org/doc/WaveFormat</a>
     */
    static public void PCMToWAV(File input, File output, int channelCount, int sampleRate, int bitsPerSample) throws IOException {
        final int inputSize = (int) input.length();

        try (OutputStream encoded = new FileOutputStream(output)) {
            // WAVE RIFF header
            writeToOutput(encoded, "RIFF"); // chunk id
            writeToOutput(encoded, 36 + inputSize); // chunk size
            writeToOutput(encoded, "WAVE"); // format

            // SUB CHUNK 1 (FORMAT)
            writeToOutput(encoded, "fmt "); // subchunk 1 id
            writeToOutput(encoded, 16); // subchunk 1 size
            writeToOutput(encoded, (short) 1); // audio format (1 = PCM)
            writeToOutput(encoded, (short) channelCount); // number of channelCount
            writeToOutput(encoded, sampleRate); // sample rate
            writeToOutput(encoded, getByteRate(channelCount, sampleRate, bitsPerSample)); // byte rate
            writeToOutput(encoded, (short) (channelCount * bitsPerSample / 8)); // block align
            writeToOutput(encoded, (short) bitsPerSample); // bits per sample

            // SUB CHUNK 2 (AUDIO DATA)
            writeToOutput(encoded, "data"); // subchunk 2 id
            writeToOutput(encoded, inputSize); // subchunk 2 size
            IOUtils.pipe(new FileInputStream(input), encoded);
        }
    }

    /**
     * @param format to be checked
     * @return the bits per sample for a PCM audio format
     * @throws IllegalArgumentException when format isn't for PCM audio
     */
    private static int getBitsPerSample(AudioFormat format) throws IllegalArgumentException {
        int bitsPerSample;
        final int encoding = format.getEncoding();
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitsPerSample = 8;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitsPerSample = 32;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitsPerSample = 16;
                break;
            default:
                throw new IllegalArgumentException("Invalid Audio Format Encoding: " + encoding);
        }
        return bitsPerSample;
    }

    /**
     * @param channelCount  number of channels: 1 for mono, 2 for stereo, etc.
     * @param sampleRate    sample rate of PCM audio
     * @param bitsPerSample bits per sample, i.e. 16 for PCM16
     * @return the byte rate for PCM data
     */
    private static int getByteRate(int channelCount, int sampleRate, int bitsPerSample) {
        return sampleRate * channelCount * bitsPerSample / 8;
    }

    /**
     * The number of audio bytes transmitted per seconds
     * @param format audioformat for PCM data
     * @return the byte rate for PCM data
     */
    public static int getByteRate(AudioFormat format) {
        return getByteRate(
                format.getChannelCount(),
                format.getSampleRate(),
                getBitsPerSample(format)
        );
    }

    /**
     * generates microphone audio source based on static final fields from Transcriber
     *
     * @return new microphone AudioRecord
     */
    public static AudioRecord getMicRecorder() {
        return new AudioRecord.Builder()
                .setAudioFormat(Transcriber.AUDIO_FORMAT)
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                // unprocessed audio source because applying signal processing algorithms
                // such as noise reduction or gain control reduces recognition accuracy
                .setBufferSizeInBytes(Transcriber.MIC_BUFFER_SIZE)
                .build();
    }
}