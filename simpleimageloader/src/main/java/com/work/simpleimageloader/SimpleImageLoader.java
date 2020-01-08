package com.work.simpleimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleImageLoader {
    private static final String TAG = "SimpleImageLoader";
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private ImageResizer mImageResizer;
    private Context mContext;
    private static volatile SimpleImageLoader mInstance;
    private boolean mIsDiskLruCacheCreated = false;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = 2 * CPU_COUNT + 1;
    private static final int THREAD_LIFETIME = 10;
    //50MB
    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "SimpleImageLoader#" + mCount.getAndIncrement());
        }
    };
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, THREAD_LIFETIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), mThreadFactory);

    private SimpleImageLoader(Context context) {
        mContext = context;
        initCache();
        initImageResizer();
    }

    public static SimpleImageLoader getSingleton(Context context) {
        if (mInstance == null) {
            synchronized (SimpleImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new SimpleImageLoader(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private void initImageResizer() {
        mImageResizer = ImageResizer.getInstance();
    }

    private void initCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int memoryCacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(memoryCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(mContext, "bitmaps");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (getUsableDiskCacheSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long getUsableDiskCacheSpace(File diskCacheDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return diskCacheDir.getUsableSpace();
        }
        StatFs stats = new StatFs(diskCacheDir.getPath());
        return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
    }

    private File getDiskCacheDir(Context context, String bitmaps) {
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.pathSeparator + bitmaps);
    }

    /**
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public void loadBitmapSyn(String url, ImageView target, int reqWidth, int reqHeight) {
        Bitmap bitmap = getBitmapFromMemoryCache(url);
        if (bitmap != null) {
            Log.d(TAG, "load bitmap from memoryCache successfully" + url);
            showBitmapInTargetOnUiThread(target, bitmap);
            return;
        }
        try {
            bitmap = getBitmapFromDiskCache(url, reqWidth, reqHeight);
            if (bitmap != null) {
                Log.d(TAG, "load bitmap from diskCache successfully" + url);
                showBitmapInTargetOnUiThread(target, bitmap);
                return;
            }
            bitmap = getBitmapFromHttp(url, reqWidth, reqHeight);
            Log.d(TAG, "load bitmap from network " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.w(TAG, "DiskLruCache is not created,");
            downloadBitmapFromUrl(url);
        }
        showBitmapInTargetOnUiThread(target, bitmap);
    }



    public void loadBitmapAsync(ImageView target, String url) {
        targetImageView = target;
        loadBitmapAsync(target, url, target.getWidth(), target.getHeight());
    }

    public void loadBitmapAsync(final ImageView target, final String url, final int reqWidth, final int reqHeight) {
        final Bitmap bitmap = getBitmapFromMemoryCache(url);
        if (bitmap != null) {
            target.setImageBitmap(bitmap);
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                loadBitmapSyn(url, target, reqWidth, reqHeight);
            }
        };
        THREAD_POOL_EXECUTOR.execute(task);
    }


    private void showBitmapInTargetOnUiThread(final ImageView target, final Bitmap bitmap) {
        if(load == true) {
            targetRunnable = new Runnable() {
                @Override
                public void run() {
                    target.setImageBitmap(bitmap);
                }
            };
            target.post(targetRunnable);
        }else {
            load = true;
        }
    }

    ImageView targetImageView;
    Runnable targetRunnable;
    boolean load = true;
    public void stopLoading(){
        load = false;
    }


    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private Bitmap getBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("loading bitmap on UI thread!");
        }
        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
        if (snapShot != null) {
            FileInputStream fis = (FileInputStream) snapShot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fd = fis.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fd, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    private Bitmap getBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not network on UI thread!");
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream os = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadFromUrlToStream(url, os)) {
                editor.commit();
            } else {
                editor.abort();
            }
        }
        mDiskLruCache.flush();
        return getBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private boolean downloadFromUrlToStream(String urlString, OutputStream os) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(urlConnection.getInputStream());
            bos = new BufferedOutputStream(os);
            int temp;
            while ((temp = bis.read()) != -1) {
                bos.write(temp);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "download bitmap failed." + e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                bos.close();
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream bis = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(urlConnection.getInputStream());
            bitmap = BitmapFactory.decodeStream(bis);
        } catch (IOException e) {
            Log.e(TAG, "download bitmap failed." + e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }


    private String hashKeyFromUrl(String url) {
        String cacheKey;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(url.getBytes());
            cacheKey = bytesToHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
            e.printStackTrace();
        }
        return cacheKey;
    }


    private String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


}
