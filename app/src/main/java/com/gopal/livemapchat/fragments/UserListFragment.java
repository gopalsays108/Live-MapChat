package com.gopal.livemapchat.fragments;

import static com.gopal.livemapchat.Constants.CHANGE_ICON_DURATION;
import static com.gopal.livemapchat.Constants.CHANGE_VIEW_DURATION;
import static com.gopal.livemapchat.Constants.MAPVIEW_BUNDLE_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.adapter.UserRecyclerAdapter;
import com.gopal.livemapchat.models.ClusterMarker;
import com.gopal.livemapchat.models.UserLocation;
import com.gopal.livemapchat.models.Users;
import com.gopal.livemapchat.utils.MyClusterManagerRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserListFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = UserListFragment.class.getSimpleName();
    private boolean isMapFullScreen = false;

    private RecyclerView userListRecyclerView;
    private MapView mapView;

    private ArrayList<Users> usersArrayList = new ArrayList<>();
    private ArrayList<UserLocation> userLocationArrayList = new ArrayList<>();
    private UserRecyclerAdapter mUserRecyclerAdapter;
    private GoogleMap mGoogleMap;
    private UserLocation mUserPosition;
    private LatLngBounds mapBoundary;  //This is help to set boundary where camera view is lookign at
    private ImageView fullScreenImage;
    private ImageView resetMapImageView;
    private GeoApiContext geoApiContext = null;

    private ClusterManager<ClusterMarker> clusterManager;
    private MyClusterManagerRenderer myClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    private ConstraintLayout constraintLayout;


    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    public UserListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UserListFragment.
     */
    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        if (userLocationArrayList.size() == 0) {
            if (getArguments() != null) {
                final ArrayList<Users> users = getArguments().getParcelableArrayList( getString( R.string.intent_user_list ) );
                usersArrayList.addAll( users );

                final ArrayList<UserLocation> userLocations = getArguments().getParcelableArrayList( getString( R.string.intent_user_locations ) );
                userLocationArrayList.addAll( userLocations );
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_user_list, container, false );

        userListRecyclerView = view.findViewById( R.id.user_list_recycler_view );
        mapView = view.findViewById( R.id.user_list_map );
        resetMapImageView = view.findViewById( R.id.btn_reset_map );
        fullScreenImage = view.findViewById( R.id.btn_full_screen_map );
        constraintLayout = view.findViewById( R.id.constraint );

        fullScreenImage.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fullScreenImage.animate().alpha( 0.0f ).setDuration( CHANGE_ICON_DURATION );
                resetMapImageView.animate().alpha( 0.0f ).setDuration( CHANGE_ICON_DURATION );


                new Handler().postDelayed( new Runnable() {
                    @Override
                    public void run() {
                        if (isMapFullScreen)
                            contractMapAnimation();
                        else
                            expandMapAnimation();
                    }
                }, CHANGE_VIEW_DURATION );

            }
        } );

        initUserListRecyclerView();
        initGoogleMap( savedInstanceState );
        setUserPosition();

        return view;
    }

    private void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle( MAPVIEW_BUNDLE_KEY );
        }

        mapView.onCreate( mapViewBundle );
        mapView.getMapAsync( this );

        if (geoApiContext == null) {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey( getString( R.string.google_maps_key ) )
                    .build();
        }
    }

    private void initUserListRecyclerView() {
        mUserRecyclerAdapter = new UserRecyclerAdapter( usersArrayList );
        userListRecyclerView.setAdapter( mUserRecyclerAdapter );
        userListRecyclerView.setLayoutManager( new LinearLayoutManager( getContext() ) );
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        if (getActivity() != null)
            if (ActivityCompat.checkSelfPermission( getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission( getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION )
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }

        googleMap.setMyLocationEnabled( true );
        googleMap.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        googleMap.setTrafficEnabled( true );
        googleMap.setBuildingsEnabled( true );

        mGoogleMap = googleMap;
        // setCameraView();

        addMapMarker();

        changeMyCurrentPositionButtonLocation();

        //Handles all click listener
        handleClusterItemClickListener();
        handleClusterItemInfoClickListener();
        handleClusterItemInfoLongClickListener();

    }

    private void setUserPosition() {
        for (UserLocation userLocation : userLocationArrayList) {
            if (userLocation.getUsers().getUser_id().equals( FirebaseAuth.getInstance().getUid() )) {
                mUserPosition = userLocation;
            }
        }
    }

    private void setCameraView() {

        /** Overall map view window 0.2 * 0.2 = 0.04 */
        double bottomBoundary = mUserPosition.getGeoPoint().getLatitude() - .01;
        double leftBoundary = mUserPosition.getGeoPoint().getLongitude() - .01;

        double topBoundary = mUserPosition.getGeoPoint().getLatitude() + .01;
        double rightBoundary = mUserPosition.getGeoPoint().getLongitude() + .01;

        mapBoundary = new LatLngBounds(
                new LatLng( bottomBoundary, leftBoundary ), // SW bounds
                new LatLng( topBoundary, rightBoundary ) // NE bounds
        );

        /**https://developers.google.com/maps/documentation/android-sdk/views
         * Above website to read more about camera */
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target( new LatLng( mUserPosition.getGeoPoint().getLatitude(),
                        mUserPosition.getGeoPoint().getLongitude() ) )
                .zoom( 16 )                   // Sets the zoom
                .bearing( 35 )                // Sets the orientation of the camera to east
                .tilt( 35 )
                .build();

        mGoogleMap.moveCamera( CameraUpdateFactory.newLatLngBounds( mapBoundary, 0 ) );
        mGoogleMap.animateCamera( CameraUpdateFactory.newCameraPosition( cameraPosition ) );
    }

    private void addMapMarker() {

        if (mGoogleMap != null) {
            resetMap();

            if (clusterManager == null) {
                clusterManager = new ClusterManager<>( getActivity().getApplicationContext(),
                        mGoogleMap );
            }

            if (myClusterManagerRenderer == null) {
                myClusterManagerRenderer = new MyClusterManagerRenderer(
                        getActivity().getApplicationContext(),
                        mGoogleMap,
                        clusterManager
                );
                clusterManager.setRenderer( myClusterManagerRenderer );
            }

            for (UserLocation userLocation : userLocationArrayList) {
                try {
                    Log.d( TAG, "addMapMarkers: location: " + userLocation.getGeoPoint().toString() );
                    String snippet;
                    if (userLocation.getUsers().getUser_id().equals( FirebaseAuth.getInstance().getUid() ))
                        snippet = getString( R.string.this_is_you );
                    else
                        snippet = "Determine route to " + userLocation.getUsers().getUsername() + "?";

                    int avatar = R.drawable.naruto;

                    try {
                        avatar = Integer.parseInt( userLocation.getUsers().getAvatar() );
                    } catch (Exception e) {
                        Log.d( TAG, "addMapMarkers: no avatar for " + userLocation.getUsers().getUsername() + ", setting default." + " " + snippet );
                    }

                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng( userLocation.getGeoPoint().getLatitude(),
                                    userLocation.getGeoPoint().getLongitude() ),
                            userLocation.getUsers().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUsers()
                    );

                    Log.i( TAG, "addMapMarker: " + newClusterMarker.toString() );

                    clusterManager.addItem( newClusterMarker );
                    mClusterMarkers.add( newClusterMarker );
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e( TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
                }
            }
            clusterManager.cluster();
            setCameraView();
        }

    }

    private void resetMap() {
        if (mGoogleMap != null) {
            mGoogleMap.clear();

            if (clusterManager != null)
                clusterManager.cluster();

            if (mClusterMarkers != null) {
                mClusterMarkers.clear();
                mClusterMarkers = new ArrayList<>();
            }

        }
    }

    private void startUserLocationRunnable() {
        Log.i( TAG, "startUserLocationRunnable: Inside runnable" );
        mHandler.postDelayed( mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocation();
                mHandler.postDelayed( mRunnable, LOCATION_UPDATE_INTERVAL );
            }
        }, LOCATION_UPDATE_INTERVAL );
    }

    private void stopUsersLocationUpdates() {
        mHandler.removeCallbacks( mRunnable );
    }

    private void retrieveUserLocation() {
        Log.i( TAG, "retrieveUserLocation: Inside Retrieval" );

        try {
            for (final ClusterMarker clusterMarker : mClusterMarkers) {
                DocumentReference userLocationReference = FirebaseFirestore.getInstance()
                        .collection( getString( R.string.collection_user_locations ) )
                        .document( clusterMarker.getUsers().getUser_id() );

                userLocationReference.get().addOnCompleteListener( new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final UserLocation userLocation = task.getResult().toObject( UserLocation.class );

                            //Update the location
                            if (userLocation != null)
                                for (int i = 0; i < mClusterMarkers.size(); i++) {
                                    try {
                                        if (mClusterMarkers.get( i ).getUsers().getUser_id().equals(
                                                userLocation.getUsers().getUser_id()
                                        )) {
                                            LatLng updatedVal = new LatLng(
                                                    userLocation.getGeoPoint().getLatitude(),
                                                    userLocation.getGeoPoint().getLongitude()
                                            );

                                            mClusterMarkers.get( i ).setPosition( updatedVal );
                                            myClusterManagerRenderer.setUpdateMarker( mClusterMarkers.get( i ) );
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                        } else {
                            Log.i( TAG, "onComplete: Unable to update users location line no 328" );
                        }
                    }
                } );

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        startUserLocationRunnable(); // update user locations every @LOCATION_UPDATE_INTERVAL
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        stopUsersLocationUpdates(); // stop updating user locations
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        stopUsersLocationUpdates();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void handleClusterItemInfoClickListener() {
        clusterManager.setOnClusterItemInfoWindowClickListener(
                new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterMarker>() {
                    @Override
                    public void onClusterItemInfoWindowClick(ClusterMarker item) {
                        if (item.getSnippet() != null)
                            if (!item.getSnippet().equals( getString( R.string.this_is_you ) )) {
                                final AlertDialog.Builder dialog = new AlertDialog.Builder( getActivity() );
                                dialog.setMessage( item.getSnippet() )
                                        .setCancelable( true )
                                        .setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                calculateDirection( item );
                                                dialogInterface.dismiss();
                                            }
                                        } )
                                        .setNegativeButton( "No", (dialogInterface, i) -> {
                                            dialogInterface.dismiss();
                                        } );

                                dialog.show();

                            }
                    }
                } );
    }

    private void handleClusterItemInfoLongClickListener() {
        clusterManager.setOnClusterItemInfoWindowLongClickListener( new ClusterManager.OnClusterItemInfoWindowLongClickListener<ClusterMarker>() {
            @Override
            public void onClusterItemInfoWindowLongClick(ClusterMarker item) {
                Geocoder geocode = new Geocoder( getContext() );
                try {
                    List<Address> fromLocation = geocode.getFromLocation( item.getPosition().latitude, item.getPosition().longitude, 1 );

                    String add = "";
                    if (fromLocation != null && fromLocation.size() > 0) {
                        if (fromLocation.get( 0 ).getAddressLine( 0 ) != null) {
                            add += fromLocation.get( 0 ).getAddressLine( 0 ) + "\n";
                        }

                        if (fromLocation.get( 0 ).getLocality() != null) {
                            add += fromLocation.get( 0 ).getLocality() + " ";
                        }

                        if (fromLocation.get( 0 ).getPostalCode() != null) {
                            add += fromLocation.get( 0 ).getPostalCode() + " ";
                        }

                        if (fromLocation.get( 0 ).getAdminArea() != null) {
                            add += fromLocation.get( 0 ).getAdminArea();
                        }

                        if (getContext() != null) {
                            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService( Context.CLIPBOARD_SERVICE );
                            ClipData data = ClipData.newPlainText( "Address", add );
                            clipboardManager.setPrimaryClip( data );

                            if (item.getUsers().getUser_id().equals( FirebaseAuth.getInstance().getUid() )) {
                                Toast.makeText( getContext(), "Copied your address to clipboard", Toast.LENGTH_LONG ).show();
                            } else {
                                Toast.makeText( getContext(), "Copied " + item.getUsers().getUsername() + " address to clipboard", Toast.LENGTH_LONG ).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d( TAG, "onClusterItemInfoWindowLongClick: " + e.getMessage() );
                }
            }
        } );
    }

    private void handleClusterItemClickListener() {

        clusterManager.setOnClusterItemClickListener(
                new ClusterManager.OnClusterItemClickListener<ClusterMarker>() {
                    @Override
                    public boolean onClusterItemClick(ClusterMarker item) {

                        if (mGoogleMap.getCameraPosition().zoom < 18) {
                            new Handler().postDelayed( new Runnable() {
                                @Override
                                public void run() {
                                    mGoogleMap.animateCamera( CameraUpdateFactory.newLatLngZoom(
                                            new LatLng( item.getPosition().latitude,
                                                    item.getPosition().longitude ),
                                            18 ) );
                                }
                            }, 850 );

                        }
                        return false;
                    }
                } );
    }

    private void calculateDirection(ClusterMarker item) {
        LatLng destination = new LatLng(
                item.getPosition().latitude,
                item.getPosition().longitude
        );

        DirectionsApiRequest directionsApiRequest = new DirectionsApiRequest( geoApiContext );
        directionsApiRequest.alternatives( true );
        directionsApiRequest.origin( new com.google.maps.model.LatLng(
                mUserPosition.getGeoPoint().getLatitude(),
                mUserPosition.getGeoPoint().getLongitude()
        ) );

        directionsApiRequest.destination( String.valueOf( destination ) )
                .setCallback( new PendingResult.Callback<DirectionsResult>() {
                    @Override
                    public void onResult(DirectionsResult result) {

//                        Log.d( TAG, "onResult: " + result.toString() );
//                        Log.d( TAG, "calculateDirections: routes: " + result.routes[0].toString() );
//                        Log.d( TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration );
//                        Log.d( TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance );
//                        Log.d( TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString() );
                        addPolyLinesToMap( result );
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.d( TAG, "onFailure: inside " + e.getMessage() );
                    }
                } );
    }

    private void addPolyLinesToMap(DirectionsResult result) {

        //Anything inside of this is posted on the main thread
        new Handler( Looper.getMainLooper() ).post( new Runnable() {
            @Override
            public void run() {
                for (DirectionsRoute route : result.routes) {
                    List<com.google.maps.model.LatLng> decodedPaths = PolylineEncoding.decode(
                            route.overviewPolyline.getEncodedPath() );

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    for (com.google.maps.model.LatLng latLng : decodedPaths) {
                        newDecodedPath.add( new LatLng(
                                latLng.lat,
                                latLng.lng
                        ) );
                    }

                    Polyline polyline;
                    polyline = mGoogleMap.addPolyline( new PolylineOptions().addAll( newDecodedPath ) );
                    polyline.setColor( ContextCompat.getColor( getActivity(), R.color.blue1 ) );
                    polyline.setClickable( true );
                }

            }
        } );
    }

    private void expandMapAnimation() {
        isMapFullScreen = true;

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( constraintLayout );
        constraintSet.constrainPercentHeight( R.id.user_list_map, 1 );

        Transition autoTransition = new ChangeBounds();
        autoTransition.setDuration( CHANGE_VIEW_DURATION );
        autoTransition.setInterpolator( new AnticipateInterpolator( 0.0f ) );

        TransitionManager.beginDelayedTransition( mapView, autoTransition );
        constraintSet.applyTo( constraintLayout );

        new Handler().postDelayed( new Runnable() {
            @Override
            public void run() {
                fullScreenImage.animate().alpha( 1f ).setDuration( CHANGE_ICON_DURATION );
                resetMapImageView.animate().alpha( 1f ).setDuration( CHANGE_ICON_DURATION );
            }
        }, CHANGE_VIEW_DURATION );
    }

    private void contractMapAnimation() {

        isMapFullScreen = false;
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( constraintLayout );
        constraintSet.constrainPercentHeight( R.id.user_list_map, 0.5F );

        Transition autoTransition = new ChangeBounds();
        autoTransition.setDuration( CHANGE_VIEW_DURATION );
        autoTransition.setInterpolator( new AnticipateInterpolator( -2.0f ) );

        TransitionManager.beginDelayedTransition( mapView, autoTransition );
        constraintSet.applyTo( constraintLayout );

        new Handler().postDelayed( new Runnable() {
            @Override
            public void run() {
                fullScreenImage.animate().alpha( 1f ).setDuration( CHANGE_ICON_DURATION );
                resetMapImageView.animate().alpha( 1f ).setDuration( CHANGE_ICON_DURATION );
            }
        }, CHANGE_VIEW_DURATION );
    }

    private void changeMyCurrentPositionButtonLocation() {

        View locationButton = ((View) mapView.findViewById( Integer.parseInt( "1" ) )
                .getParent()).findViewById( Integer.parseInt( "2" ) );
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        rlp.addRule( RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE );
        rlp.addRule( RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE );
        rlp.addRule( RelativeLayout.ALIGN_PARENT_RIGHT, 0 );
        rlp.addRule( RelativeLayout.ALIGN_PARENT_TOP, 0 );
        rlp.setMargins( 0, 0, 15, 170 );
    }
}