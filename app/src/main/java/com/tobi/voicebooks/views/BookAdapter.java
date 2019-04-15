package com.tobi.voicebooks.views;

import android.view.ViewGroup;

import com.tobi.voicebooks.BookActivity;
import com.tobi.voicebooks.Repository;
import com.tobi.voicebooks.db.entities.BookEntity;
import com.tobi.voicebooks.db.entities.diff.BookDiffer;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
    private final AppCompatActivity parentActivity;
    private final Repository repository;
    /**
     * Initialised as empty array list to prevent null
     * pointer exception at {@link #updateBooks}
     */
    private List<BookEntity> books = new ArrayList<>();

    public BookAdapter(Repository repository, AppCompatActivity activity) {
        this.repository = repository;
        parentActivity = activity;
        repository.getBooks(this::updateBooks);
    }

    /**
     * Callback to observation
     *
     * @param newBooks to be displayed
     * @see Repository#getBooks
     */
    public void updateBooks(List<BookEntity> newBooks) {
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BookDiffer(books, newBooks));
        books = newBooks;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // new BookView instantiated using parent as context
        BookView bookView = new BookView(parent.getContext(), repository);
        // New Holder returned
        return new ViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        final BookEntity bookEntity = books.get(i);
        // i'th book is bound to holder
        final BookView bookView = holder.getBookView();
        bookView.setBook(bookEntity);
        bookView.setOnClickListener(e -> BookActivity.start(bookEntity, parentActivity));
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final BookView bookView;

        public ViewHolder(@NonNull BookView bookView) {
            super(bookView);
            this.bookView = bookView;
        }

        public BookView getBookView() {
            return bookView;
        }
    }
}
