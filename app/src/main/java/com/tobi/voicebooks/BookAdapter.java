package com.tobi.voicebooks;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
    private static final String TAG = "com.tobi.voicebooks.BookAdapter";

    private Book[] books;
    private Context mContext;

    public BookAdapter(Book[] books, Context mContext) {
        this.books = books;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        Book book = books[i];
        holder.title.setText(book.title);
        holder.creation.setText(book.getLength());
    }

    @Override
    public int getItemCount() {
        return books.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView creation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.book_item_title);
            creation = itemView.findViewById(R.id.book_item_creation);
        }
    }
}
