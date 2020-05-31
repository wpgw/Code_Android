package com.philip.wpcameral;

import android.content.Context;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView {
    private Camera mCamera;
    private boolean isPreviwing=false;
    private Point mCameraSize;
    public String mPhotoPath;

    public CameraView(Context context, AttributeSet attrs){
        super(context,attrs);
        //mContext=context;
        SurfaceHolder holder=getHolder();
        holder.addCallback(mSurfaceCallback);
        holder.setFormat(PixelFormat.TRANSPARENT);
    }

    public void doTakePicture(){
        mCamera.takePicture(mShutterCallback,null,mPictureCallback);
    }



    private Camera.ShutterCallback mShutterCallback=new Camera.ShutterCallback(){
        @Override
        public void onShutter() {
        }
    };

    private Camera.PictureCallback mPictureCallback=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };

    private SurfaceHolder.Callback mSurfaceCallback=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };
}
