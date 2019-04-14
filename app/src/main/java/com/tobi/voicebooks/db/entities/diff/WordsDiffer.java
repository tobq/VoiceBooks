package com.tobi.voicebooks.db.entities.diff;

import com.tobi.voicebooks.db.entities.BookWord;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

public class WordsDiffer<T extends BookWord> extends DiffUtil.Callback {
    private final List<T> newTitle;
    private final List<T> oldTitle;

    public WordsDiffer(List<T> oldTitle, List<T> newTitle) {
        this.oldTitle = oldTitle;
        this.newTitle = newTitle;
    }

    @Override
    public int getOldListSize() {
        return oldTitle.size();
    }

    @Override
    public int getNewListSize() {
        return newTitle.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTitle.get(oldItemPosition).id == newTitle.get(newItemPosition).id;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTitle.get(oldItemPosition).equals(newTitle.get(newItemPosition));
    }
}
