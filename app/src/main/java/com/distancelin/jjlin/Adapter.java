package com.distancelin.jjlin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.distancelin.simpleimageloader.SimpleImageLoader;

import java.util.List;

public class Adapter extends BaseAdapter {
    private List<String> mUrls;
    private Context mContext;
    public ImagePreviewListener imagePreviewListener;
    public Adapter(Context context, List<String> Urls, ImagePreviewListener listener) {
        this.mUrls = Urls;
        this.mContext=context;
        this.imagePreviewListener = listener;
    }

    @Override
    public int getCount() {
        return mUrls.size();
    }

    @Override
    public Object getItem(int position) {
        return mUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null){
            convertView= LayoutInflater.from(mContext).inflate(R.layout.itemview,parent,false);
            holder=new ViewHolder(convertView,imagePreviewListener,position);
            convertView.setTag(holder);
        }else {
            holder= (ViewHolder) convertView.getTag();
        }
        SimpleImageLoader.getSingleton(mContext).loadBitmapAsync(holder.squareImageView,mUrls.get(position));
        return convertView;
    }
    private static class ViewHolder{
        private SquareImageView squareImageView;
        public View view;
        private ImagePreviewListener listener;
        private int position;
        private ViewHolder(View view, ImagePreviewListener listener, final int position){
            this.view = view;
            squareImageView= (SquareImageView) view.findViewById(R.id.itemView);
            this.listener = listener;
            this.position = position;

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadImagePreview(position);
                }
            });
        }

        private void loadImagePreview(int id){
            listener.loadPreview(id);
        }
    }
}
