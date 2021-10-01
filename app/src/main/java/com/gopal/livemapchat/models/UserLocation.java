package com.gopal.livemapchat.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserLocation implements Parcelable {

    private Users users;
    /**
     * Immutable class representing a GeoPoint in Cloud Firestore
     * <p>
     * //Its the firebase class that is used to store lat and longitude
     */
    private GeoPoint geoPoint;

    /**
     * Annotation used to mark a timestamp field to be populated with a server timestamp. If a POJO
     * being written contains null for a @ServerTimestamp-annotated field, it will be replaced with
     * a server-generated timestamp
     */
    private @ServerTimestamp
    Date timestamp;

    public UserLocation(Users users, GeoPoint geoPoint, Date timestamp) {
        this.users = users;
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
    }

    public UserLocation() {
    }

    protected UserLocation(Parcel in) {
    }

    public static final Creator<UserLocation> CREATOR = new Creator<UserLocation>() {
        @Override
        public UserLocation createFromParcel(Parcel in) {
            return new UserLocation( in );
        }

        @Override
        public UserLocation[] newArray(int size) {
            return new UserLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable( users, i );
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "user=" + users +
                ", geo_point=" + geoPoint +
                ", timestamp=" + timestamp +
                '}';
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
