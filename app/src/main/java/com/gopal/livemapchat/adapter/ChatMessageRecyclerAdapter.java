package com.gopal.livemapchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.ChatMessage;
import com.gopal.livemapchat.models.Users;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<ChatMessageRecyclerAdapter.ViewHolder> {

    private final List<ChatMessage> messages;
    private final ArrayList<Users> users;
    private final Context context;

    public ChatMessageRecyclerAdapter(List<ChatMessage> messages, ArrayList<Users> users, Context context) {
        this.messages = messages;
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.layout_chat_message_list_item, parent, false );
        return new ViewHolder( view );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (Objects.equals( FirebaseAuth.getInstance().getUid(), messages.get( holder.getAdapterPosition() ).getUser().getUser_id() )) {
            holder.username.setTextColor( context.getColor( R.color.green1 ) );
        } else {
            holder.username.setTextColor( context.getColor( R.color.blue2 ) );
        }

        holder.message.setText( messages.get( position).getMessage() );
        holder.username.setText( messages.get( position ).getUser().getUsername() );

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message, username;

        public ViewHolder(View itemView) {
            super( itemView );
            message = itemView.findViewById( R.id.chat_message_message );
            username = itemView.findViewById( R.id.chat_message_username );
        }
    }
}
