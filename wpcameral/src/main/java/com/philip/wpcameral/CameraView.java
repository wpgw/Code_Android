package com.philip.wpcameral;

import com.philip.wpcameral.util.BitmapUtil;
import com.philip.wpcameral.util.DateUtil;
import com.philip.wpcameral.util.CameraUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView {
    private Camera mCamera;
    private boolean isPreviwing=false;
    private Point mCameraSize;
    public String mPhotoPath;   //可返回照片保存的路径
    private Context mContext;

    public CameraView(Context context, AttributeSet attrs){
        super(context,attrs);
        mContext=context;
        SurfaceHolder holder=getHolder();
        holder.addCallback(mSurfaceCallback);  //mSurfaceCallback全在操作camera
        holder.setFormat(PixelFormat.TRANSPARENT);
    }

    public void doTakePicture(){
        if(isPreviwing&&mCamera!=null){ //mShutterCallback操作声音  mPictureCallback操作结果照片
            mCamera.takePicture(mShutterCallback,null,mPictureCallback);
        }
    }

    private Camera.ShutterCallback mShutterCallback=new Camera.ShutterCallback(){
        @Override
        public void onShutter() {
            System.out.println("At ShutterCalback.");  //咔嚓一声
        }
    };

    private Camera.PictureCallback mPictureCallback=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) { //处理结果 data
            Bitmap raw=null;
            if(data!=null){
                raw= BitmapFactory.decodeByteArray(data,0,data.length);
                mCamera.stopPreview();
                isPreviwing=false;
            }
            Bitmap bitmap=BitmapUtil.getRotateBitmap(raw,90);
            mPhotoPath=String.format("%s%s.jpg",BitmapUtil.getCachePath(mContext), DateUtil.getNowDateTime());
            BitmapUtil.saveBitmap(mPhotoPath,bitmap,"jpg",80);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            isPreviwing=true;
        }
    };

    private SurfaceHolder.Callback mSurfaceCallback=new SurfaceHolder.Callback() {//这个回调 操作Camera
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCamera=Camera.open(0);  //0 refers to behind camera, 1 refers to front camera
            try{
                mCamera.setPreviewDisplay(holder);
                mCameraSize=CameraUtil.getCameraSize(mCamera.getParameters(),CameraUtil.getSize(mContext));
                Camera.Parameters parameters=mCamera.getParameters();
                parameters.setPreviewSize(mCameraSize.x,mCameraSize.y);
                parameters.setPictureSize(mCameraSize.x,mCameraSize.y);
                parameters.setPictureFormat(ImageFormat.JPEG);
                //只有后摄像头可自动对焦
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(parameters);

            }catch(Exception e){
                e.printStackTrace();
                mCamera.release();
                mCamera=null;
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            isPreviwing=true;
            mCamera.autoFocus(null);
            //mCamera.setPreviewCallback(mPreviwCallback);  //连拍用的
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    };
}
