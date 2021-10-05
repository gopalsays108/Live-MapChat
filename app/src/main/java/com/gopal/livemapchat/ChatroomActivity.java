package com.gopal.livemapchat;

import static com.gopal.livemapchat.Constants.MY_SHARED_PREFERENCE_NAME;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.adapter.ChatMessageRecyclerAdapter;
import com.gopal.livemapchat.fragments.UserListFragment;
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

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
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
        CollectionReference messageRef = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) )
                .document( chatroom.getChatroom_id() )
                .collection( getString( R.string.collection_chat_messages ) );

        chatMessageEventListener = messageRef
                .orderBy( "timestamp", Query.Direction.ASCENDING )
                .addSnapshotListener( new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.i( TAG, "onEvent: Error  Occured while retreiving data" );
                            return;
                        }

                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                ChatMessage message = doc.toObject( ChatMessage.class );
                                if (!messageIds.contains( message.getMessage_id() )) {
                                    messageIds.add( message.getMessage_id() );
                                    messages.add( message );
                                    chatMessageRecyclerView.smoothScrollToPosition( messageIds.size() - 1 );
                                }
                            }
                            mChatMessageRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                } );
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
        mChatMessageRecyclerAdapter = new ChatMessageRecyclerAdapter( messages, new ArrayList<Users>(), this );
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.chatroom_menu, menu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatMessageEventListener != null)
            chatMessageEventListener.remove();
        if (userListEventListener != null)
            userListEventListener.remove();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                UserListFragment fragment =
                        (UserListFragment) getSupportFragmentManager().findFragmentByTag( getString( R.string.fragment_user_list ) );
                if (fragment != null) {
                    if (fragment.isVisible()) {
                        getSupportFragmentManager().popBackStack();
                        return true;
                    }
                }
                finish();
                return true;
            }
            case R.id.action_chatroom_user_list: {
                try {
                    if (userList != null && userLocations != null)
                        inflateUserListFragment();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            case R.id.action_chatroom_leave: {
                leaveChatroom();
                return true;
            }
            default: {
                return super.onOptionsItemSelected( item );
            }
        }
    }

    private void leaveChatroom() {

        getApplication().getSharedPreferences( MY_SHARED_PREFERENCE_NAME, MODE_PRIVATE )
                .edit().remove( chatroom.getChatroom_id() ).apply();
        finish();

        DocumentReference joinChatroomRef = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) )
                .document( chatroom.getChatroom_id() )
                .collection( getString( R.string.collection_chatroom_user_list ) )
                .document( FirebaseAuth.getInstance().getUid() );

        joinChatroomRef.delete();
    }

    private void inflateUserListFragment() {
        hideSoftKeyboard();

        /*https://stackoverflow.com/questions/9245408/best-practice-for-instantiating-a-new-android-fragment
         * This is link where we can see why we used newInstance */
        UserListFragment fragment = UserListFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList( getString( R.string.intent_user_list ), userList );
        bundle.putParcelableArrayList( getString( R.string.intent_user_locations ), userLocations );
        fragment.setArguments( bundle );

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations( R.anim.slide_in_up, R.anim.slide_out_up );
        transaction.replace( R.id.user_list_container, fragment, getString( R.string.fragment_user_list ) );
        transaction.addToBackStack( getString( R.string.fragment_user_list ) );
        transaction.commit();
    }
}