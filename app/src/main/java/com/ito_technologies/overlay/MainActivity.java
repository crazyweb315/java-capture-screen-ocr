package com.ito_technologies.overlay;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private ImageView imageView = null;
    public static MainActivity activity;

    /********************************Start Screen capture*************************/
    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_CODE = 2000;
    public static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private int mResultCode;
    private Intent mResultData;
    private OrientationChangeCallback mOrientationChangeCallback;
    Bitmap bitmap = null;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";
    private MediaProjectionStopCallback mediaProjectionStopCallback;
    /***************End Screen capture******************************************/

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        activity = this;
        createDirectory();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            initializeView();
        }

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }

    /**
     * Set and initialize the view elements.
     */
    private void initializeView() {
        findViewById(R.id.notify_me).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                if( mResultCode == 0 || mResultData == null ){
                    startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
                }else{
                    prepareService();
                }
                //finish();
//takeScreenshot();
            }
        });
    }
    private void prepareService(){
        // register orientation change callback
        mOrientationChangeCallback = new OrientationChangeCallback(this);
        if (mOrientationChangeCallback.canDetectOrientation()) {
            mOrientationChangeCallback.enable();
        }
        Intent intent = new Intent(MainActivity.this, FloatingViewService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }else{
            startService( intent );
        }

    }
    public void askScreenCapture(){
        if (sMediaProjection != null) {
            createVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            createVirtualDisplay();
        } else {
            Log.i(TAG, "Requesting confirmation");
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initializeView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "他のアプリケーションに重ねて表示するパーミッションが有効ではありません。 アプリケーションを閉じます。",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        }else if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                Toast.makeText( this, R.string.user_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            prepareService();
            mResultCode = resultCode;
            mResultData = data;
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    /****************************************** UI Widget Callbacks *******************************/
    private void setUpMediaProjection() {
        sMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
    }
    private void stopProjection() {
        mHandler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }

    /****************************************** Factoring Virtual Display creation ****************/

    private void createDirectory(){
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            STORE_DIRECTORY = externalFilesDir.getAbsolutePath() + Setting.SCREEN_PATH;
            File storeDirectory = new File(STORE_DIRECTORY);
            if (!storeDirectory.exists()) {
                boolean success = storeDirectory.mkdirs();
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.");
                    return;
                }
            }
        } else {
            Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
            return;
        }
    }
    private void createVirtualDisplay(){
        // display metrics
        FloatingViewService.floatingService.getRootView().setVisibility( View.GONE );

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something here
                prepareScreenCapture();
            }
        }, 100 );

    }

    private void prepareScreenCapture(){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensity = metrics.densityDpi;

        if( mWidth == 0 || mHeight == 0 ){
            mDisplay.getMetrics(metrics);
            Point size = new Point();
            mDisplay.getRealSize(size);
            mWidth = size.x;
            mHeight = size.y;
        }
Log.i( "Size", mWidth + "-" + mHeight + "-" + mRotation );
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2 );
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

        // register media projection stop callback
        mediaProjectionStopCallback = new MediaProjectionStopCallback();
        sMediaProjection.registerCallback( mediaProjectionStopCallback, mHandler);
    }
    private void initialize(){
        if (mVirtualDisplay != null) mVirtualDisplay.release();
        mVirtualDisplay = null;
        if (mImageReader != null){
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader = null;
        }
//        if (mOrientationChangeCallback != null){
//            mOrientationChangeCallback.disable();
//            mOrientationChangeCallback = null;
//        }
        if( mediaProjectionStopCallback != null ){
            sMediaProjection.unregisterCallback( mediaProjectionStopCallback );
        }
        mediaProjectionStopCallback = null;
        sMediaProjection = null;
        IMAGES_PRODUCED = 0;
    }
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            bitmap = null;
            Bitmap tempBitmap = null;
            if( !FloatingViewService.floatingService.getPossibleCapture() || IMAGES_PRODUCED > 0 ){
                stopProjection();
                return;
            }
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    //The real size with the notch
                    int notchWidthDiff = 0;
                    int notchHeightDiff = 0;
                    if (NotchUtils.hasNotchAtHuawei(getApplicationContext())) {
                        if( mWidth > mHeight ){ // if Landscape.
                            notchHeightDiff = NotchUtils.getStatusBarHeight( getApplicationContext() );
                        }else{
                            notchWidthDiff = NotchUtils.getNotHeightAtXiaomi( getApplicationContext() );
                        }
                    }

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888 );
                    bitmap.copyPixelsFromBuffer(buffer);

//                    bitmap =  Bitmap.createBitmap( tempBitmap, notchWidthDiff, notchHeightDiff,
//                            mWidth, tempBitmap.getHeight() - notchHeightDiff);

                    // write bitmap to a file
                    fos = new FileOutputStream(STORE_DIRECTORY + "/my_screen.png");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                FloatingViewService.floatingService.setScreenShot();
                            } catch (Throwable e) {
                                // Several error may come out with file handling or DOM
                                e.printStackTrace();
                            }
                        }
                    });
                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
//                if( tempBitmap != null ){
//                    tempBitmap.recycle();
//                }
                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
            stopProjection();
        }
    }

    private class OrientationChangeCallback extends OrientationEventListener {
        OrientationChangeCallback(Context context) {
            super(context);
        }
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOrientationChanged(int orientation) {
            mDisplay = ((WindowManager) MainActivity.activity.getSystemService(Context.WINDOW_SERVICE) ).getDefaultDisplay();
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                final DisplayMetrics metrics = new DisplayMetrics();
                mDisplay.getMetrics(metrics);
                Point size = new Point();
                mDisplay.getRealSize(size);
                mWidth = size.x;
                mHeight = size.y;
                Log.i( "Orientation", mWidth + "---" + mHeight );
                mRotation = rotation;
                if( FloatingViewService.floatingService == null || !FloatingViewService.floatingService.getPossibleCapture() ){
                    return;
                }
                try {
                    // clean up
                    initialize();
                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initialize();
                }
            });
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOrientationChangeCallback != null){
            mOrientationChangeCallback.disable();
            mOrientationChangeCallback = null;
        }
    }
    /****************************************** Activity Lifecycle methods ************************/
}