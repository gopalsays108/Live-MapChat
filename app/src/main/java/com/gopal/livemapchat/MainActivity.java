package com.gopal.livemapchat;

import static com.gopal.livemapchat.Constants.ERROR_DIALOG_REQUEST;
import static com.gopal.livemapchat.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.gopal.livemapchat.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gopal.livemapchat.adapter.ChatroomRecyclerAdapter;
import com.gopal.livemapchat.models.Chatroom;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView chatroomRecyclerView;
    private ChatroomRecyclerAdapter chatroomRecyclerAdapter;
    private ListenerRegistration listenerRegistration; //Listen to all new registeration in db
    private FloatingActionButton floatingActionButton;
    private FirebaseFirestore firebaseFirestore;
    private boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationClient;
    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();


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
        initChatRoomRecyclerView();
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
        Toast.makeText( getApplicationContext(), "Chat rooms", Toast.LENGTH_SHORT ).show();
    }

    private void getUserDetails() {
        Toast.makeText( getApplicationContext(), "user", Toast.LENGTH_SHORT ).show();
    }

    private void initChatRoomRecyclerView() {
//        chatroomRecyclerAdapter = new ChatroomRecyclerAdapter( mChatrooms, (ChatroomRecyclerAdapter.ChatroomRecyclerClickListener) this );
//        chatroomRecyclerView.setAdapter( chatroomRecyclerAdapter );
//        chatroomRecyclerView.setLayoutManager( new LinearLayoutManager( this ) );
    }

    private void initSupportActionBar() {
        setTitle( "MapChat Chatroom" );
    }
}