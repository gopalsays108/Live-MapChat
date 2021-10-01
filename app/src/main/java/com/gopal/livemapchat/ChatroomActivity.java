package com.gopal.livemapchat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.gopal.livemapchat.adapter.ChatMessageRecyclerAdapter;
import com.gopal.livemapchat.models.ChatMessage;
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
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private Set<String> messageIds = new HashSet<>();
    private ArrayList<Users> userList = new ArrayList<>();
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
        initChatroomRecyclerView();
        getChatroomUsers();

        checkMark.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertNewMessages();
            }
        } );

    }

    private void insertNewMessages() {
        String message = messageEt.getText().toString().trim();
        if (!message.equals( "" )) {
            message = message.replaceAll( System.getProperty( "line.separator" ), "" );

            DocumentReference newMessRef = firebaseFirestore
                    .collection( getString( R.string.collection_chatrooms ) )
                    .document( chatroom.getChatroom_id() )
                    .collection( getString( R.string.collection_chat_messages ) )
                    .document();

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage( message );
            chatMessage.setMessage_id( newMessRef.getId() );

            Users users = ((UserClient) getApplicationContext()).getUsers();
            chatMessage.setUser( users );

            newMessRef.set( chatMessage ).addOnCompleteListener( new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        clearMessageBox();
                    } else {
                        View parentLayout = findViewById( android.R.id.content );
                        Snackbar.make( parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT ).show();
                    }
                }
            } );

        }
    }

    private void clearMessageBox() {
        messageEt.setText( "" );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getChatMessages();
    }

    private void getChatMessages() {
    }

    private void getChatroomUsers() {

        CollectionReference userRef = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) )
                .document( chatroom.getChatroom_id() )
                .collection( getString( R.string.collection_chatroom_user_list ) );

        chatMessageEventListener = userRef.addSnapshotListener( new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.i( TAG, "onEvent: fetching user failed" );
                    return;
                }

                if (value != null) {
                    userList.clear();
                    userList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Users users = doc.toObject( Users.class );
                        userList.add( users );
                        getUsersLocations( users );
                    }
                }
            }
        } );
    }

    private void getUsersLocations(Users users) {

        DocumentReference locationRef = firebaseFirestore
                .collection( getString( R.string.collection_user_locations ) )
                .document( users.getUser_id() );

        locationRef.get().addOnCompleteListener( new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    userLocations.add( task.getResult().toObject( UserLocation.class ) );
                }
            }
        } );
    }

    private void initChatroomRecyclerView() {
        mChatMessageRecyclerAdapter = new ChatMessageRecyclerAdapter( messages, userList, this );
        chatMessageRecyclerView.setAdapter( mChatMessageRecyclerAdapter );
        chatMessageRecyclerView.setLayoutManager( new LinearLayoutManager( this ) );

        // TODO: 10/2/2021 : Yet to understand 
        chatMessageRecyclerView.addOnLayoutChangeListener( new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view,
                                       int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                if (i3 < i7) {
                    chatMessageRecyclerView.postDelayed( new Runnable() {
                        @Override
                        public void run() {
                            if (messages.size() > 0) {
                                chatMessageRecyclerView
                                        .smoothScrollToPosition( chatMessageRecyclerView.getAdapter().getItemCount() );
                            }
                        }
                    }, 100 );
                }
            }
        } );
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
                .document( chatroom.getChatroom_id() )
                .collection( getString( R.string.collection_chatroom_user_list ) )
                .document( FirebaseAuth.getInstance().getUid() );

        Users users = ((UserClient) getApplication()).getUsers();
        chatRoomRef.set( users ); //DOnt care about listening
    }
}