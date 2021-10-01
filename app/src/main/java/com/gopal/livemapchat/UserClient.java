package com.gopal.livemapchat;

import android.app.Application;

import com.gopal.livemapchat.models.Users;

public class UserClient extends Application {
    private Users users = null;

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }
}
