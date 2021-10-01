package com.gopal.livemapchat.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Users implements Parcelable {

    String email;
    String user_id;
    String username;
    String avatar;

    public Users() {

    }

    public Users(String email, String user_id, String username, String avatar) {
        this.email = email;
        this.user_id = user_id;
        this.username = username;
        this.avatar = avatar;
    }

    protected Users(Parcel in) {
        email = in.readString();
        user_id = in.readString();
        username = in.readString();
        avatar = in.readString();
    }

    public static final Creator<Users> CREATOR = new Creator<Users>() {
        @Override
        public Users createFromParcel(Parcel in) {
            return new Users( in );
        }

        @Override
        public Users[] newArray(int size) {
            return new Users[size];
        }
    };


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString( email );
        parcel.writeString( user_id );
        parcel.writeString( username );
        parcel.writeString( avatar );
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", user_id='" + user_id + '\'' +
                ", username='" + username + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
