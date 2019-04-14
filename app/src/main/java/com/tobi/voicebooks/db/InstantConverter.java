package com.tobi.voicebooks.db;

import java.time.Instant;

import androidx.room.TypeConverter;

public class InstantConverter {
    @TypeConverter
    public static Instant toInstant(Long epoch) {
        return Instant.ofEpochMilli(epoch);
    }

    @TypeConverter
    public static Long fromInstant(Instant time) {
        return time.toEpochMilli();
    }
}
