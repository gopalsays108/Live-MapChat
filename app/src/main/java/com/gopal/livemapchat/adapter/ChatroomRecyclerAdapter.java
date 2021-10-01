package com.gopal.livemapchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.Chatroom;

import java.util.ArrayList;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder> {

    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();

    public ChatroomRecyclerAdapter(ArrayList<Chatroom> chatrooms) {
        this.mChatrooms = chatrooms;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.layout_chatroom_list_item,
                parent, false );
        return new ViewHolder( view );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getTextView().setText( mChatrooms.get( position ).getTitle() );

        holder.getTextView().setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText( view.getContext(), holder.getTextView().getText().toString(), Toast.LENGTH_SHORT ).show();
            }
        } );
    }

    //Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mChatrooms.size();
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView chatRoomNameTv;

        public ViewHolder(@NonNull View itemView) {
            super( itemView );
            chatRoomNameTv = itemView.findViewById( R.id.chatroom_title );
        }

        public TextView getTextView() {
            return chatRoomNameTv;
        }
    }
}















