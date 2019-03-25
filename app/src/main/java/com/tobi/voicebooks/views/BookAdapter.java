package com.tobi.voicebooks.views;

import android.content.Context;
import android.view.ViewGroup;

import com.tobi.voicebooks.transcription.Book;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
    private static final String TAG = "com.tobi.voicebooks.views.BookAdapter";

    private final ArrayList<Book> books;
    private final Context mContext;

    public BookAdapter(ArrayList<Book> books, Context mContext) {
        this.books = books;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BookView bookView = new BookView(parent.getContext());
        return new ViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        Book book = books.get(i);
        ((BookView) holder.itemView).setBook(book);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void append(Book book) {
        books.add(book);
        notifyItemInserted(books.size() - 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull BookView itemView) {
            super(itemView);
        }
    }
}
