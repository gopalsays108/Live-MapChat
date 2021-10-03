package com.gopal.livemapchat;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gopal.livemapchat.adapter.ImageListRecyclerAdapter;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageListFragment extends Fragment implements ImageListRecyclerAdapter.ImageListRecyclerClickListener {

    private static final String TAG = "ImageListFragment";
    private static final int NUM_COLUMNS = 2;

    //widgets
    private RecyclerView mRecyclerView;


    //vars
    private ArrayList<Integer> mImageResources = new ArrayList<>();
    private IProfile mIProfile;

    public ImageListFragment() {
        // Required empty public constructor
    }


    public static ImageListFragment newInstance() {
        return new ImageListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.fragment_image_list, container, false );
        mRecyclerView = view.findViewById( R.id.image_list_recyclerview );

        getImageResouces();
        initRecyclerview();

        return view;
    }

    private void getImageResouces() {
        mImageResources.add( R.drawable.naruto );
        mImageResources.add( R.drawable.avatar_angry );
        mImageResources.add( R.drawable.avatar_zoomed );
        mImageResources.add( R.drawable.monkey );
        mImageResources.add( R.drawable.demon_slayer );
        mImageResources.add( R.drawable.girl );
        mImageResources.add( R.drawable.girl_two );
        mImageResources.add( R.drawable.hashira );
        mImageResources.add( R.drawable.jirayas );
        mImageResources.add( R.drawable.kakshi );
        mImageResources.add( R.drawable.kurama );
        mImageResources.add( R.drawable.monkey );
        mImageResources.add( R.drawable.mr_bean );
        mImageResources.add( R.drawable.naruto_funny );
    }

    private void initRecyclerview() {
        ImageListRecyclerAdapter mAdapter = new ImageListRecyclerAdapter( getActivity(), mImageResources, this );
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager( NUM_COLUMNS, LinearLayoutManager.VERTICAL );
        mRecyclerView.setLayoutManager( staggeredGridLayoutManager );
        mRecyclerView.setAdapter( mAdapter );
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach( context );
        mIProfile = (IProfile) getActivity();
    }

    @Override
    public void onImageSelected(int position) {
        mIProfile.onImageSelected( mImageResources.get( position ) );
    }
}








