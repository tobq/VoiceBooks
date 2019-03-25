package com.tobi.voicebooks;

import android.annotation.SuppressLint;
import android.content.Context;

import com.tobi.voicebooks.db.AppDatabase;

import java.time.Duration;
import java.util.Locale;
import java.util.StringJoiner;

import androidx.room.Room;

public class Utils {
    public static Duration randomDuration() {
        return randomDuration(10);
    }

    public static Duration randomDuration(int hours) {
        return Duration.ofSeconds((int) (Math.random() * (3600 * hours)));
    }

    public static <T> String join(String delimiter, T[] array) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (T word : array) joiner.add(word.toString());
        return joiner.toString();
    }

    public static Locale getCurrentLocale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    @SuppressLint("DefaultLocale")
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();

        long hours = seconds / 3600;
        if (hours != 0) return String.format(
                "%02d:%02d",
                (seconds % 3600) / 60,
                seconds % 60
        );
        else return String.format(
                "%d:%02d:%02d",
                hours,
                (seconds % 3600) / 60,
                seconds % 60
        );

    }

    public static AppDatabase getDatebase(Context context) {
        return Room
                .databaseBuilder(context, AppDatabase.class, "voicebooks")
                .fallbackToDestructiveMigration()
                .build();
    }
}
