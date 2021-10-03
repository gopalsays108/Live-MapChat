package com.gopal.livemapchat.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterMarker implements ClusterItem {

    private LatLng position;
    private String title;
    private String snippet;

    /*All the images are going to be stored in Drawables folder so they can be integer resources so,
     * these gonna act as pointers to the images in memory*/
    private int iconPicture;
    private Users users;

    public ClusterMarker(LatLng position, String title,
                         String snippet, int iconPicture,
                         Users users) {
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
        this.users = users;
    }

    @NonNull
    @Override
    public String toString() {
        return "ClusterMarker{" +
                "position=" + position +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", iconPicture=" + iconPicture +
                ", users=" + users +
                '}';
    }

    public ClusterMarker() {

    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public int getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(int iconPicture) {
        this.iconPicture = iconPicture;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

}
