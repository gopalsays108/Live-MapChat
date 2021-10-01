package com.gopal.livemapchat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.adapter.ChatMessageRecyclerAdapter;
import com.gopal.livemapchat.models.ChatMessageModel;
import com.gopal.livemapchat.models.Chatroom;
import com.gopal.livemapchat.models.UserLocation;
import com.gopal.livemapchat.models.Users;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChatroomActivity extends AppCompatActivity {

    private static final String TAG = ChatroomActivity.class.getSimpleName();

    private Chatroom chatroom;
    private EditText messageEt;

    private ListenerRegistration chatMessageEventListener, userListEventListener;
    private RecyclerView chatMessageRecyclerView;
    private ChatMessageRecyclerAdapter mChatMessageRecyclerAdapter;
    private FirebaseFirestore firebaseFirestore;
    private ArrayList<ChatMessageModel> messages = new ArrayList<>();
    private Set<String> messageIds = new HashSet<>();
    private ArrayList<User> userList = new ArrayList<>();
    private ArrayList<UserLocation> userLocations = new ArrayList<>();
    private ImageView checkMark;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_chatroom );

        messageEt = findViewById( R.id.input_message );
        chatMessageRecyclerView = findViewById( R.id.chatmessage_recycler_view );
        checkMark = findViewById( R.id.checkmark );
        firebaseFirestore = FirebaseFirestore.getInstance();

        getIncomingIntents();
    }

    private void getIncomingIntents() {
        if (getIntent().hasExtra( getString( R.string.intent_chatroom ) )) {
            chatroom = getIntent().getParcelableExtra( getString( R.string.intent_chatroom ) );
            setChatRoomName();
            joinChatRoom();
        }
    }

    private void setChatRoomName() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle( chatroom.getTitle() );
            getSupportActionBar().setDisplayHomeAsUpEnabled( true );
            getSupportActionBar().setHomeButtonEnabled( true );
        }
    }

    private void joinChatRoom() {

        DocumentReference chatRoomRef = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) )
                .document(chatroom.getChatroom_id())
                .collection( getString( R.string.collection_chatroom_user_list ) )
                .document( FirebaseAuth.getInstance().getUid() );

        Users users = ((UserClient)getApplication()).getUsers();
        chatRoomRef.set( users ); //DOnt care about listening
    }
}