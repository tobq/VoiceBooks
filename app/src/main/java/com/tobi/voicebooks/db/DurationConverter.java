package com.tobi.voicebooks.db;

import com.tobi.voicebooks.views.DurationView;

import java.time.Duration;
import java.time.Instant;

import androidx.room.TypeConverter;

public class DurationConverter {
    @TypeConverter
    public static Duration toDuration(Long epoch) {
        return Duration.ofMillis(epoch);
    }

    @TypeConverter
    public static Long fromDuration(Duration time) {
        return time.toMillis();
    }
}
