package com.gopal.livemapchat.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.UserClient;
import com.gopal.livemapchat.models.UserLocation;
import com.gopal.livemapchat.models.Users;

public class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();
    private final static long UPDATE_INTERVAL = 4 * 1000;  /* 4 secs */
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec is the fastest allowed interval time*/

    /**
     * It will be responsible for retrieving the location
     */
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Nullable
    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( this );

        /**
         * This is the only service that uses GPS
         * We need Notification to keep running in background for android vesion 26 and greater
         *
         * For api level 26 and below we dont need Notification so we used If statement
         */
        if (Build.VERSION.SDK_INT >= 26) {
            String channelId = "my_channel_id";
            NotificationChannel channel = new NotificationChannel( channelId,
                    "My channel",
                    NotificationManager.IMPORTANCE_DEFAULT );

            ((NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE )).createNotificationChannel( channel );

            Notification notification = new NotificationCompat.Builder( this, channelId )
                    .setContentText( "" )
                    .setContentTitle( "" ).build();

            startForeground( 1, notification );
        }
    }

    /**
     * This is called when service actually starts
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d( TAG, "onStartCommand: called." );
        getLocation();
        return START_NOT_STICKY;
    }

    private void getLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval( UPDATE_INTERVAL )
                .setFastestInterval( FASTEST_INTERVAL )
                .setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission( this,
                Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            Log.d( TAG, "getLocation: stopping the location service." );
            stopSelf(); //To stop the services
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates( locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult( locationResult );

                Location location = locationResult.getLastLocation();
                Users user = ((UserClient) (getApplicationContext())).getUsers();
                GeoPoint geoPoint = new GeoPoint( location.getLatitude(), location.getLongitude() );
                UserLocation userLocation = new UserLocation( user, geoPoint, null );
                saveUserLocation( userLocation );
            }
        }, Looper.myLooper() );
    }

    private void saveUserLocation(UserLocation userLocation) {

        try {
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection( getString( R.string.collection_user_locations ) )
                    .document( FirebaseAuth.getInstance().getUid() );

            locationRef.set( userLocation ).addOnCompleteListener( new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d( TAG, "onComplete: \ninserted user location into database." +
                                "\n latitude: " + userLocation.getGeoPoint().getLatitude() +
                                "\n longitude: " + userLocation.getGeoPoint().getLongitude() );
                    }
                }
            } );
        } catch (NullPointerException e) {
            Log.e( TAG, "saveUserLocation: User instance is null, stopping location service." );
            Log.e( TAG, "saveUserLocation: NullPointerException: " + e.getMessage() );
            stopSelf();
        }
    }
}
