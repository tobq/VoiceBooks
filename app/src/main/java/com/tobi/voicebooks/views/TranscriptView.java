package com.tobi.voicebooks.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tobi.voicebooks.R;
import com.tobi.voicebooks.Utils.Utils;
import com.tobi.voicebooks.models.Transcript;
import com.tobi.voicebooks.models.Word;

public class TranscriptView extends LinearLayout {
    private final TextView text;
    private final TextView partialText;

    {
        inflate(getContext(), R.layout.transcript, this);
        text = findViewById(R.id.transcript_text);
        partialText = findViewById(R.id.transcript_partial_text);
    }

    public TranscriptView(Context context) {
        super(context);
    }

    public TranscriptView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TranscriptView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTranscript(Transcript transcript) {
        final Word[] words = transcript.getContent();
        final String content = Utils.formatWords(words);
        if (content.isEmpty()) text.setVisibility(GONE);
        else {
            text.setText(content);
            text.setVisibility(VISIBLE);
        }
        partialText.setVisibility(GONE);
    }

    public void setPartial(String partialResult) {
        partialText.setText(partialResult);
        partialText.setVisibility(VISIBLE);
    }
}
