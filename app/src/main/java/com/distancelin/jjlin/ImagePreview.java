package com.distancelin.jjlin;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.distancelin.simpleimageloader.SimpleImageLoader;

import java.util.ArrayList;


public class ImagePreview extends DialogFragment {

    ArrayList<String> arrayList;
    int idx;
    public ImagePreview() {
    }

    public static ImagePreview newInstance(Bundle bundle) {
        ImagePreview fragment = new ImagePreview();
        fragment.setArguments(bundle);
        return fragment;
    }

    void init(){
        arrayList = getArguments().getStringArrayList("list");
        idx = getArguments().getInt("idx");
        AppCompatImageView appCompatImageView = (AppCompatImageView) getActivity().findViewById(R.id.imagePreview);
        SimpleImageLoader.getSingleton(getContext()).loadBitmapAsync(appCompatImageView,arrayList.get(idx));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        init();
        return inflater.inflate(R.layout.fragment_image_preview, container, false);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
