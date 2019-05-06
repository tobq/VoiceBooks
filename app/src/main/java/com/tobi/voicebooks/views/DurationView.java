package com.tobi.voicebooks.views;

import android.content.Context;
import android.util.AttributeSet;

import com.tobi.voicebooks.Utils.Utils;

import java.time.Duration;

import androidx.appcompat.widget.AppCompatTextView;

public class DurationView extends AppCompatTextView {
    private Duration duration;

    public DurationView(Context context) {
        super(context);
    }

    public DurationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DurationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
        setText(Utils.formatDuration(duration));
    }
}

