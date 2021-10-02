package com.gopal.livemapchat.fragments;

import static com.gopal.livemapchat.Constants.MAPVIEW_BUNDLE_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.maps.GeoApiContext;
import com.google.maps.android.clustering.ClusterManager;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.adapter.UserRecyclerAdapter;
import com.gopal.livemapchat.models.ClusterMarker;
import com.gopal.livemapchat.models.UserLocation;
import com.gopal.livemapchat.models.Users;
import com.gopal.livemapchat.utils.MyClusterManagerRenderer;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserListFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = UserListFragment.class.getSimpleName();
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;

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
    private GeoApiContext geoApiContext;

    private ClusterManager<ClusterMarker> clusterManager;
    private MyClusterManagerRenderer myClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();


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
    // TODO: Rename and change types and number of parameters
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

        if (ActivityCompat.checkSelfPermission( getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( getContext(), Manifest.permission.ACCESS_COARSE_LOCATION )
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled( false );
        googleMap.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        googleMap.setTrafficEnabled( true );
        googleMap.setBuildingsEnabled( true );

        mGoogleMap = googleMap;
        // setCameraView();

        addMapMarker();
        // mGoogleMap.setOnPolygonClickListener( this );
    }

    private void setUserPosition() {
        for (UserLocation userLocation : userLocationArrayList) {
            if (userLocation.getUsers().getUser_id().equals( FirebaseAuth.getInstance().getUid() )) {
                mUserPosition = userLocation;
            }
        }
    }

    private void setCameraView() {

        /**Overall map view window 0.2 * 0.2 = 0.04*/
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
            mGoogleMap.setOnInfoWindowClickListener( this );

            for (UserLocation userLocation : userLocationArrayList) {
                try {
                    Log.d( TAG, "addMapMarkers: location: " + userLocation.getGeoPoint().toString() );
                    String snippet = "";
                    if (userLocation.getUsers().getUser_id().equals( FirebaseAuth.getInstance().getUid() ))
                        snippet = "This is You";
                    else
                        snippet = "Determine route to " + userLocation.getUsers().getUsername() + "?";

                    int avatar = R.drawable.avatar_angry;

                    try {
                        avatar = Integer.parseInt( userLocation.getUsers().getAvatar() );
                    } catch (Exception e) {
                        Log.d( TAG, "addMapMarkers: no avatar for " + userLocation.getUsers().getUsername() + ", setting default." + " " + snippet );
                    }

                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng( userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude() ),
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

    private void stopLocationUpdates() {
    }

    private void startUserLocationsRunnable() {
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        startUserLocationsRunnable(); // update user locations every 'LOCATION_UPDATE_INTERVAL'
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
        stopLocationUpdates(); // stop updating user locations
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        Log.i( TAG, "onInfoWindowClick: " + marker.toString() );
        Toast.makeText( getActivity(), marker.getSnippet(), Toast.LENGTH_SHORT ).show();
    }
}