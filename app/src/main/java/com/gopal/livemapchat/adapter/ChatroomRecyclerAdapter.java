package com.gopal.livemapchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.Chatroom;

import java.util.ArrayList;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder> {

    private ArrayList<Chatroom> chatrooms = new ArrayList<>();
    private final ChatroomRecyclerClickListener listener;


    public ChatroomRecyclerAdapter(ArrayList<Chatroom> chatrooms, ChatroomRecyclerClickListener chatroomRecyclerClickListener) {
        this.chatrooms = chatrooms;
        listener = chatroomRecyclerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.layout_chatroom_list_item, parent, false );
        return new ViewHolder( view, listener );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((ViewHolder) holder).chatroomTitle.setText( chatrooms.get( position ).getTitle() );
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView chatroomTitle;
        ChatroomRecyclerClickListener clickListener;

        public ViewHolder(@NonNull View itemView, ChatroomRecyclerClickListener listener) {
            super( itemView );
            chatroomTitle = itemView.findViewById( R.id.chatroom_title );
            this.clickListener = listener;
            itemView.setOnClickListener( this );

        }

        @Override
        public void onClick(View view) {
            clickListener.onChatroomSelected( getAdapterPosition() );
        }
    }

    public interface ChatroomRecyclerClickListener {
        public void onChatroomSelected(int position);
    }

}
