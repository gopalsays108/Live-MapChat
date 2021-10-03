package com.gopal.livemapchat.adapter;

import static com.gopal.livemapchat.Constants.MY_SHARED_PREFERENCE_NAME;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.ChatroomActivity;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.Chatroom;
import com.gopal.livemapchat.models.Users;

import java.util.ArrayList;
import java.util.List;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder> {

    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private final Context context;

    public ChatroomRecyclerAdapter(ArrayList<Chatroom> chatrooms, Context context) {
        this.context = context;
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

        SharedPreferences sharedPreferences = context.getSharedPreferences( MY_SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE );
        boolean isAdded = sharedPreferences.getBoolean( mChatrooms.get( holder.getAdapterPosition() ).getChatroom_id(), false );

        if (mChatrooms.get( holder.getAdapterPosition() ).getPassword() == null) {
            holder.getLockView().setVisibility( View.INVISIBLE );
        } else {
            if (isAdded) {
                holder.getOpenLockView().setVisibility( View.VISIBLE );
            } else {
                holder.getLockView().setVisibility( View.VISIBLE );
            }
        }

        holder.getTextView().setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mChatrooms.get( holder.getAdapterPosition() ).getPassword() == null) {
                    chatRoomIntent( holder.getAdapterPosition() );
                } else {
                    if (isAdded)
                        chatRoomIntent( holder.getAdapterPosition() );
                    else
                        askForPassword( mChatrooms.get( holder.getAdapterPosition() ).getPassword(), holder.getAdapterPosition() );
                }
            }
        } );
    }

    private void chatRoomIntent(int adapterPosition) {
        Intent intent = new Intent( context, ChatroomActivity.class );
        intent.putExtra( context.getString( R.string.intent_chatroom ), mChatrooms.get( adapterPosition ) );
        context.startActivity( intent );
    }

    private void askForPassword(String pass, int adapterPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder( context );
        builder.setTitle( "Enter the chat room password" );
        builder.setCancelable( true );

        final EditText input = new EditText( context );
        input.setInputType( InputType.TYPE_CLASS_TEXT );
        builder.setView( input );

        builder.setPositiveButton( "Join", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!input.getText().toString().equals( pass )) {
                    Toast.makeText( context, "Wrong password", Toast.LENGTH_SHORT ).show();
                    dialogInterface.dismiss();
                } else {
                    SharedPreferences sharedPreferences = context.getSharedPreferences( MY_SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE );
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean( mChatrooms.get( adapterPosition ).getChatroom_id(), true );
                    editor.apply();

                    chatRoomIntent( adapterPosition );
                }
            }
        } ).setNegativeButton( "cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        } );
        builder.show();
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
        private final ImageView lock;
        private final ImageView openLock;

        public ViewHolder(@NonNull View itemView) {
            super( itemView );
            chatRoomNameTv = itemView.findViewById( R.id.chatroom_title );
            lock = itemView.findViewById( R.id.lockIv );
            openLock = itemView.findViewById( R.id.open_lockIv );
        }

        public TextView getTextView() {
            return chatRoomNameTv;
        }

        public ImageView getLockView() {
            return lock;
        }

        public ImageView getOpenLockView() {
            return openLock;
        }
    }
}















