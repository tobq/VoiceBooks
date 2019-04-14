package com.tobi.voicebooks;

import android.annotation.SuppressLint;
import android.content.Context;

import com.tobi.voicebooks.db.VoiceBooksDatabase;
import com.tobi.voicebooks.db.entities.BookWord;
import com.tobi.voicebooks.models.Word;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import androidx.room.Room;


public final class Utils {

    /**
     * Generates a random duration
     *
     * @param hours max hours of duration
     * @return a random duration between 0 and {hours} hours
     */
    public static Duration randomDuration(int hours) {
        return Duration.ofSeconds((int) (Math.random() * (3600 * hours)));
    }

    /**
     * Used to join an array of objects together
     * Mostly for debugging
     * <p>
     * Uses {@link Object#toString()} to format object
     *
     * @param delimiter delimiter to separate objects
     * @param array     array of objects to concatenate
     * @param <T>       Type of objects in array
     * @return the result of concatenation
     */
    public static <T> String join(String delimiter, T[] array) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (T word : array) joiner.add(word.toString());
        return joiner.toString();
    }

    /**
     * utility function used to get the devices {@link Locale}, based on
     * the saved instance states context
     *
     * @param context
     * @return Locale
     */
    public static Locale getCurrentLocale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    /**
     * Formats a {@link Duration} to string
     * H:MM:SS format
     *
     * @param duration to format
     * @return H:MM:SS format of duration
     */
    @SuppressLint("DefaultLocale")
    public static String formatDuration(Duration duration) {
        final long totalSeconds = duration.getSeconds();

        final int HOURS_SECONDS = 3600;
        final int MINUTES_SECONDS = 60;
        final long H = totalSeconds / HOURS_SECONDS;
        final long MM = (totalSeconds % HOURS_SECONDS) / MINUTES_SECONDS;
        final long SS = totalSeconds % MINUTES_SECONDS;

        return H == 0 ?
                String.format("%d:%02d:%02d", H, MM, SS) :
                String.format("%02d:%02d", MM, SS);
    }

    /**
     * Gets DB connection
     * uses {@link R.string#app_name} as the default dbName
     *
     * @param context saved instance state context
     * @return database connection
     */
    public static VoiceBooksDatabase getDatabase(Context context) {
        return Room
                .databaseBuilder(context, VoiceBooksDatabase.class, getAppName(context))
                .fallbackToDestructiveMigration()
                // destroy older version of DB if exists
                .build();
    }

    static String getAppName(Context context) {
        return context.getResources().getString(R.string.app_name);
    }

    /**
     * Used to form a block of text from a list of {@link Word}s
     *
     * @param words to format
     * @return Concatenation of the words
     */
    public static String formatWords(Word[] words) {
        return Arrays.stream(words)
                .map(word -> word.word)
                .collect(Collectors.joining(" "));
    }

    /**
     * Used to form a block of text from a list of {@link BookWord}s
     *
     * @param words to format
     * @param <T>   Type of {@link BookWord}
     * @return Concatenation of the words
     */
    public static <T extends BookWord> String formatWords(List<T> words) {
        return words.stream()
                .map(titleWord -> titleWord.word)
                .collect(Collectors.joining(" "));
    }

}