package com.gopal.livemapchat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.ClusterMarker;

public class MyClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker> {

    private final IconGenerator iconGenerator; //It is responsible for rendering og images
    private final ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;

    public MyClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super( context, map, clusterManager );

        // initialize cluster item icon generator
        iconGenerator = new IconGenerator( context.getApplicationContext() );
        imageView = new ImageView( context.getApplicationContext() );
        markerHeight = (int) context.getResources().getDimension( R.dimen.custom_marker_image );
        markerWidth = (int) context.getResources().getDimension( R.dimen.custom_marker_image );
        imageView.setLayoutParams( new ViewGroup.LayoutParams( markerWidth, markerHeight ) );
        int padding = (int) context.getResources().getDimension( R.dimen.custom_marker_padding );
        imageView.setPadding( padding, padding, padding, padding );
        iconGenerator.setContentView( imageView );
    }

    /*We are not using group, we are using cluster that stand alone*/
    @Override
    protected void onBeforeClusterItemRendered(@NonNull ClusterMarker item, @NonNull MarkerOptions markerOptions) {

        imageView.setImageResource( item.getIconPicture() );
        Bitmap bitmap = iconGenerator.makeIcon();

        markerOptions.icon( BitmapDescriptorFactory.fromBitmap( bitmap ) )
                .title( item.getTitle() ).snippet(  item.getSnippet() );
    }

    /*we are going to cluster anything so we are returning false
    *
    * CLuster means when we zoom out map, user starts to show 2+ in icons somwtimw, Hopw you understand
    * or else you go to the Docs they are very clear
    * */
    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<ClusterMarker> cluster) {
        return false;
    }

    /***
     * Update the GPS coordinates of a ClusterItem
     */
    public void setUpdateMarker(ClusterMarker clusterMarker){
        Marker marker = getMarker( clusterMarker );
        if (marker!= null){
            marker.setPosition( clusterMarker.getPosition() );
        }
    }
}
