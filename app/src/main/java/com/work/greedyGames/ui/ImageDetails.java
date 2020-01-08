package com.work.greedyGames.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.Button;

import com.work.greedyGames.R;
import com.work.simpleimageloader.SimpleImageLoader;

import java.util.ArrayList;

public class ImageDetails extends AppCompatActivity {
    ArrayList<? extends String> arrayList;
    Button stop;
    int idx;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);
        init();
    }

    void init(){
        stop = (Button) findViewById(R.id.stopDetails);
        arrayList = getIntent().getParcelableArrayListExtra("data");
        idx = getIntent().getIntExtra("pos", 0);
        AppCompatImageView appCompatImageView = (AppCompatImageView)findViewById(R.id.imagePreview);
        SimpleImageLoader.getSingleton(getApplicationContext()).loadBitmapAsync(appCompatImageView,arrayList.get(idx));

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleImageLoader.getSingleton(getApplicationContext()).stopLoading();
            }
        });
    }
}
