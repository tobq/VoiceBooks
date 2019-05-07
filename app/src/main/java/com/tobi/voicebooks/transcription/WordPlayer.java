package com.tobi.voicebooks.transcription;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.tobi.voicebooks.db.entities.BookWord;

import java.time.Duration;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

abstract public class WordPlayer extends RecyclerView {
    @ColorInt
    private static final int PLAYING_COLOUR = 0x80000000;
    private static final int QUIET_COLOUR = 0x20000000;
    private final float textSize;

    public WordPlayer(@NonNull Activity context, float textSize) {
        super(context);
        this.textSize = textSize;
        setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        setLayoutManager(new FlexboxLayoutManager(context));
        new Thread(() -> {
            final WordsAdapter adapter = new WordsAdapter(getWords(), context);
            setAdapter(adapter);
        }).start();
    }

    protected abstract BookWord[] getWords();

    public void play() {
        getAdapter().play();
    }

    @Nullable
    @Override
    public WordsAdapter getAdapter() {
        return (WordsAdapter) super.getAdapter();
    }

    public void stop() {
        getAdapter().stop();
    }

    static class WordView extends AppCompatTextView {

        public WordView(Context context, float textSize) {
            super(context);
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            final float paddingSize = textSize / 5;
            int paddingHorz = (int) paddingSize;
            int paddingVert = (int) paddingSize;
            setPadding(paddingHorz, paddingVert, paddingHorz, paddingVert);
        }

        public void setWord(BookWord word) {
            setText(word.word);
        }
    }

    public class WordsAdapter extends Adapter<WordsAdapter.ViewHolder> {
        public static final int PROGRESS_NOTHING_PLAYING = -1;
        private final Activity activity;
        private final BookWord[] words;
        private volatile int playProgress = PROGRESS_NOTHING_PLAYING;
        private volatile boolean isQuiet = true;
        private Thread playThread;

        public WordsAdapter(BookWord[] words, Activity activity) {
            this.activity = activity;
            this.words = words;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // new BookView instantiated using parent as activity
            WordView wordView = new WordView(parent.getContext(), textSize);
            // New Holder returned
            return new ViewHolder(wordView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
            // i'th book is bound to holder
            final WordView wordView = holder.getWordView();
            wordView.setWord(words[i]);
            wordView.setBackgroundColor(i == playProgress ?
                    (isQuiet ? QUIET_COLOUR : PLAYING_COLOUR) : 0);
        }

        @Override
        public int getItemCount() {
            return words.length;
        }

        public void play() {
            playProgress = PROGRESS_NOTHING_PLAYING;
            playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Duration lastEnd = Duration.ZERO;
                    try {
                        for (BookWord word : words) {
                            // sleep thread for time difference between this words start time and the last
                            final long quietTime = word.startTime.minus(lastEnd).toMillis();
                            final long playTime = word.endTime.minus(word.startTime).toMillis();
                            if (quietTime != 0) {
                                isQuiet = true;
                                updateWords();
                                Thread.sleep(quietTime);
                            }
                            isQuiet = false;
                            playProgress++;
                            updateWords();
                            Thread.sleep(playTime);

                            lastEnd = word.endTime;
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        playProgress = PROGRESS_NOTHING_PLAYING;
                        updateWords();
                    }
                }

                private void updateWords() {
                    activity.runOnUiThread(() -> notifyDataSetChanged());
                }
            });
            playThread.start();
        }

        public void stop() {
            playThread.interrupt();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final WordView wordView;

            public ViewHolder(@NonNull WordView wordView) {
                super(wordView);
                this.wordView = wordView;
            }

            public WordView getWordView() {
                return wordView;
            }
        }
    }
}
