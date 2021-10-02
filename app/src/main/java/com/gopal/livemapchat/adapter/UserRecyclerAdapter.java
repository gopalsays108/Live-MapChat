package com.gopal.livemapchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gopal.livemapchat.R;
import com.gopal.livemapchat.models.Users;

import java.util.ArrayList;

public class UserRecyclerAdapter extends RecyclerView.Adapter<UserRecyclerAdapter.ViewHolder> {

    private ArrayList<Users> usersArrayList = new ArrayList<>();

    public UserRecyclerAdapter(ArrayList<Users> usersArrayList) {
        this.usersArrayList = usersArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.layout_user_list_item,
                parent, false );
        return new ViewHolder( view );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.userName.setText( usersArrayList.get( position ).getUsername() );
        holder.email.setText( usersArrayList.get( position ).getEmail() );
    }

    @Override
    public int getItemCount() {
        return usersArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView userName, email;

        public ViewHolder(@NonNull View itemView) {
            super( itemView );
            userName = itemView.findViewById( R.id.username );
            email = itemView.findViewById( R.id.email );
        }
    }
}
