package com.gopal.livemapchat;

import static com.gopal.livemapchat.Constants.ERROR_DIALOG_REQUEST;
import static com.gopal.livemapchat.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.gopal.livemapchat.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.transition.Visibility;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.adapter.ChatroomRecyclerAdapter;
import com.gopal.livemapchat.loginregister.LoginActivity;
import com.gopal.livemapchat.models.Chatroom;
import com.gopal.livemapchat.models.UserLocation;
import com.gopal.livemapchat.models.Users;
import com.gopal.livemapchat.service.LocationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Set<String> chatRoomIds = new HashSet<>();
    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView chatroomRecyclerView;
    private ChatroomRecyclerAdapter chatroomRecyclerAdapter;
    private ListenerRegistration listenerRegistration; //Listen to all new registeration in db
    private FloatingActionButton floatingActionButton;
    private FirebaseFirestore firebaseFirestore;
    private boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationClient;
    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private UserLocation userLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient( this );
        progressBar = findViewById( R.id.progressBar );
        chatroomRecyclerView = findViewById( R.id.chat_room_recycler_view );
        floatingActionButton = findViewById( R.id.fab_create_chatroom );
        firebaseFirestore = FirebaseFirestore.getInstance();
        progressBar = findViewById( R.id.progressBar );

        initSupportActionBar();
        initChatRoomRecyclerView();


        floatingActionButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newChatroomDialog();
            }
        } );
    }

    private void newChatroomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setTitle( "Enter the chat room name" );
        builder.setCancelable( false );

        final EditText input = new EditText( this );
        input.setInputType( InputType.TYPE_CLASS_TEXT );
        builder.setView( input );

        if (input.getText().toString().toLowerCase().contains( "gopal" )) {
            View view = findViewById( R.id.content );
            Snackbar.make( view, "You can not make a chat room with name Gopal", Snackbar.LENGTH_SHORT ).show();
        } else {
            builder.setPositiveButton( "Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (!input.getText().toString().isEmpty()) {
                        createNewChatRoom( input.getText().toString() );
                    } else {
                        Toast.makeText( getApplicationContext(), "Enter the chat name room", Toast.LENGTH_SHORT ).show();
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
    }

    private void createNewChatRoom(String name) {
        showDialog();

        final Chatroom chatroom = new Chatroom();
        chatroom.setTitle( name );

        DocumentReference newChatroomRef = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) )
                .document();

        chatroom.setChatroom_id( newChatroomRef.getId() );
        newChatroomRef.set( chatroom ).addOnCompleteListener( new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();
                if (task.isSuccessful()) {
                    navChatroomActivity( chatroom );
                } else {
                    View view = findViewById( R.id.content );
                    Snackbar.make( view, "Something went wrong", Snackbar.LENGTH_SHORT ).show();
                }
            }
        } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideDialog();
            }
        } );
    }

    private void navChatroomActivity(Chatroom chatroom) {
        Intent intent = new Intent( MainActivity.this, ChatroomActivity.class );
        intent.putExtra( getString( R.string.intent_chatroom ), chatroom );
        startActivity( intent );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkMapService()) {
            if (locationPermissionGranted) {
                getChatRooms();
                getUserDetails();
            } else {
                getLocationPermission();
            }
        }
    }

    public boolean isServiceOK() {
        Log.d( TAG, "isServicesOK: checking google services version" );
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable( this );

        if (available == ConnectionResult.SUCCESS) {
            //Soo everything is fine and user can make the map request
            Log.d( TAG, "isServicesOK: Google Play Services is working" );
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError( available )) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog( this, available, ERROR_DIALOG_REQUEST );
            Log.d( TAG, "isServicesOK: an error occured but we can fix it" );
            dialog.show();
        } else {
            View view = findViewById( android.R.id.content );
            Snackbar.make( view, "You cannot make map request", Snackbar.LENGTH_SHORT ).show();
        }
        return false;
    }

    private boolean checkMapService() {
        if (isServiceOK()) {
            if (isMapsEnabled()) {
                return true;
            } else {
                Toast.makeText( getApplicationContext(), "Oops", Toast.LENGTH_SHORT ).show();
            }
        }
        return false;
    }

    private boolean isMapsEnabled() {
        final LocationManager locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER )) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( "This app needs GPS to work properly, do you need to Enable GPS?" )
                .setCancelable( false )
                .setPositiveButton( "YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent gpsIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
                        startActivityForResult( gpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS );
                    }
                } );
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if (requestCode == PERMISSIONS_REQUEST_ENABLE_GPS) {
            if (locationPermissionGranted) {
                getChatRooms();
                getUserDetails();
            } else {
                getLocationPermission();
            }
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission( this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getChatRooms();
            getUserDetails();
        } else {
            ActivityCompat.requestPermissions( this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION );
        }
    }

    private void getChatRooms() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        firebaseFirestore.setFirestoreSettings( settings );

        CollectionReference roomReference = firebaseFirestore
                .collection( getString( R.string.collection_chatrooms ) );

        listenerRegistration = roomReference.addSnapshotListener( new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.i( TAG, "onEvent: " + error.getMessage() );
                    return;
                }
                if (value != null) {
                    for (QueryDocumentSnapshot snapshot : value) {
                        Chatroom chatroom = snapshot.toObject( Chatroom.class );
                        if (!chatRoomIds.contains( chatroom.getChatroom_id() )) {
                            chatRoomIds.add( chatroom.getChatroom_id() );
                            mChatrooms.add( chatroom );
                        }
                    }
                    Log.i( TAG, "onEvent: Size: " + mChatrooms.size() + " " + mChatrooms );
                    // TODO: 10/1/2021 Look at this 
                    chatroomRecyclerAdapter.notifyDataSetChanged();
                }
            }
        } );
    }

    private void initChatRoomRecyclerView() {
        chatroomRecyclerAdapter = new ChatroomRecyclerAdapter( mChatrooms, MainActivity.this );
        chatroomRecyclerView.setAdapter( chatroomRecyclerAdapter );
        chatroomRecyclerView.setLayoutManager( new LinearLayoutManager( this ) );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void getUserDetails() {
        if (userLocation == null) {
            userLocation = new UserLocation();

            DocumentReference userRef = firebaseFirestore
                    .collection( getString( R.string.collection_users ) )
                    .document( FirebaseAuth.getInstance().getUid() );

            userRef.get().addOnCompleteListener( new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.i( TAG, "onComplete: Task success" );
                        Users users = task.getResult().toObject( Users.class );
                        userLocation.setUsers( users );
                        ((UserClient) getApplicationContext()).setUsers( users );
                        getLastKnownLocation();
                    }
                }
            } );
        } else {
            getLastKnownLocation();
        }
    }

    private void getLastKnownLocation() {
        Log.d( TAG, "getLastKnownLocation: called." );
        if (ActivityCompat.checkSelfPermission( this,
                Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION )
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnCompleteListener( new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    GeoPoint geoPoint = new GeoPoint( location.getLatitude(), location.getLongitude() );
                    userLocation.setGeoPoint( geoPoint );
                    userLocation.setTimestamp( null );
                    Log.i( TAG, "onComplete: last location" + location );
                    saveUserLocation();
                    startLocationService();

                }
            }
        } );
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent( getApplicationContext(), LocationService.class );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // TODO: 10/2/2021 Uncomment
                //MainActivity.this.startForegroundService( intent );
            } else {
                //startService( intent );
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService( ACTIVITY_SERVICE );
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices( Integer.MAX_VALUE );

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            Log.d( TAG, String.format( "Service are:%s", runningServiceInfo.service.getClassName() ) );
            if (runningServiceInfo.service.getClassName().equals( LocationService.class.getName() )) {
                return true;
            }
        }

        Log.i( TAG, "isLocationServiceRunning: is not running" );
        return false;
    }

    private void saveUserLocation() {
        if (userLocation != null) {
            final Users user = ((UserClient) getApplicationContext()).getUsers();

            if (user.getUser_id() != null) {
                DocumentReference locationRef = firebaseFirestore
                        .collection( getString( R.string.collection_user_locations ) )
                        .document( user.getUser_id() );

                locationRef.set( userLocation ).addOnCompleteListener( new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.i( TAG, "saveUserLocation: \ninserted user location into database." +
                                    "\n latitude: " + userLocation.getGeoPoint().getLatitude() +
                                    "\n longitude: " + userLocation.getGeoPoint().getLongitude() );

                        }
                    }
                } );
            } else {
                View view = findViewById( R.id.content );
                Snackbar.make( view, "Unable to update User", Snackbar.LENGTH_LONG ).show();
            }
        }
    }

    private void initSupportActionBar() {
        setTitle( "MapChat Chatroom" );
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent( getApplicationContext(), LoginActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile){
            startActivity( new Intent(getApplicationContext(), ProfileActivity.class));
            return true;
        }else if(item.getItemId() == R.id.action_sign_out){
            signOut();
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    private void showDialog() {
        progressBar.setVisibility( View.VISIBLE );
    }

    private void hideDialog() {
        progressBar.setVisibility( View.GONE );
    }
}