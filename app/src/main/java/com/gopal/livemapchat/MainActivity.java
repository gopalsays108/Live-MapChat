package com.gopal.livemapchat;

import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView chatroomRecyclerView;
    private ListenerRegistration listenerRegistration; //Listen to all new registeration in db
    private FloatingActionButton floatingActionButton;
    private FirebaseFirestore firebaseFirestore;
    private boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient( this );
        progressBar = findViewById( R.id.progressBar );
        chatroomRecyclerView = findViewById( R.id.chat_room_recycler_view );
        floatingActionButton = findViewById( R.id.fab_create_chatroom );
        firebaseFirestore = FirebaseFirestore.getInstance();

        initSupportActionBar();


    }

    private void initSupportActionBar() {
        setTitle( "MapChat Chatroom" );
    }
}