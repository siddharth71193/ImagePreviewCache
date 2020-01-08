package com.distancelin.jjlin.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;

import com.distancelin.jjlin.R;
import com.distancelin.simpleimageloader.SimpleImageLoader;

import java.util.ArrayList;

public class ImageDetails extends AppCompatActivity {
    ArrayList<? extends String> arrayList;
    int idx;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);
        init();
    }

    void init(){
        arrayList = getIntent().getParcelableArrayListExtra("data");
        idx = getIntent().getIntExtra("pos", 0);
        AppCompatImageView appCompatImageView = (AppCompatImageView)findViewById(R.id.imagePreview);
        SimpleImageLoader.getSingleton(getApplicationContext()).loadBitmapAsync(appCompatImageView,arrayList.get(idx));
    }
}
