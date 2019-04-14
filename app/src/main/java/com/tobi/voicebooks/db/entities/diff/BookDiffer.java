package com.tobi.voicebooks.db.entities.diff;

import com.tobi.voicebooks.db.entities.BookEntity;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

public class BookDiffer extends DiffUtil.Callback {
    private final List<BookEntity> newBooks;
    private final List<BookEntity> oldBooks;

    public BookDiffer(List<BookEntity> oldBooks, List<BookEntity> newBooks) {
        this.oldBooks = oldBooks;
        this.newBooks = newBooks;
    }

    @Override
    public int getOldListSize() {
        return oldBooks.size();
    }

    @Override
    public int getNewListSize() {
        return newBooks.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldBooks.get(oldItemPosition).id == newBooks.get(newItemPosition).id;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return areItemsTheSame(oldItemPosition, newItemPosition);
    }
}
