//参照 https://github.com/bingoogolapple/BGAQRCode-Android/blob/master/zxingdemo/src/main/java/cn/bingoogolapple/qrcode/zxingdemo/TestScanActivity.java
package com.philip.plex_qa;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import cn.bingoogolapple.qrcode.core.BarcodeType;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class ActivityScan2 extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, QRCodeView.Delegate,View.OnClickListener{
    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;
    private ZXingView mZXingView;
    private AppCompatActivity activity;
    private String rawResult = "23$^rt826";  // to check if the result is stable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_scan2);

        mZXingView = (ZXingView) findViewById(R.id.zxingview);
        mZXingView.changeToScanQRCodeStyle(); // 切换成扫描二维码样式
        mZXingView.setType(BarcodeType.ALL, null); //  识别所有类型的码
        //mZXingView.startSpotAndShowRect(); // 显示扫描框，并开始识别

        mZXingView.setDelegate(this);
        findViewById(R.id.start_spot).setOnClickListener(this);
        findViewById(R.id.stop_spot).setOnClickListener(this);
        findViewById(R.id.open_flashlight).setOnClickListener(this);
        findViewById(R.id.close_flashlight).setOnClickListener(this);
    }

    @Override  //有关 button
    public void onClick(View v){
        switch(v.getId()){
            case R.id.start_spot:
                //mZXingView.startSpot();
                mZXingView.startSpotAndShowRect(); // 显示扫描框，并开始识别
                //Toast.makeText(activity, "startSpot", Toast.LENGTH_SHORT).show();
                findViewById(R.id.start_spot).setVisibility(View.INVISIBLE);
                findViewById(R.id.stop_spot).setVisibility(View.VISIBLE);
                break;

            case R.id.stop_spot:
                //mZXingView.stopSpot();
                mZXingView.stopSpotAndHiddenRect();
                //Toast.makeText(activity, "stopSpot", Toast.LENGTH_SHORT).show();
                findViewById(R.id.stop_spot).setVisibility(View.INVISIBLE);
                findViewById(R.id.start_spot).setVisibility(View.VISIBLE);
                break;

            case R.id.open_flashlight:
                mZXingView.openFlashlight();
                Toast.makeText(activity, "openFlashlight", Toast.LENGTH_SHORT).show();
                findViewById(R.id.open_flashlight).setVisibility(View.INVISIBLE);
                findViewById(R.id.close_flashlight).setVisibility(View.VISIBLE);
                break;

            case R.id.close_flashlight:
                mZXingView.closeFlashlight();
                Toast.makeText(activity, "closeFlashlight", Toast.LENGTH_SHORT).show();
                findViewById(R.id.close_flashlight).setVisibility(View.INVISIBLE);
                findViewById(R.id.open_flashlight).setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override  //有关 activity
    protected void onStart() {
        super.onStart();
        requestCodeQRCodePermissions();    // 要权限
        mZXingView.startCamera();// 打开后置摄像头开始预览，但是并未开始识别
        //打开前置摄像头开始预览，但是并未开始识别
        //mQRCodeView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mZXingView.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    @Override
    protected void onStop() {
        //mZXingView.stopSpotAndHiddenRect(); // 停止识别，并且隐藏扫描框
        mZXingView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZXingView.onDestroy();
        super.onDestroy();
    }

    //震动
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        //Log.d("二维码扫描结果", "result:" + result);

        setTitle("扫描结果为：" + result);
        vibrate();
        if (result.equals(rawResult)) {   // if get two same result, then accept the result
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("barcode", result);
            intent.putExtras(bundle);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            rawResult = result;  //update rawResult
            // 0.5秒后，重新开始扫描
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mZXingView.startSpot();
                }
            }, 500);
        }
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
        // 这里是通过修改提示文案来展示环境是否过暗的状态，接入方也可以根据 isDark 的值来实现其他交互效果
        String tipText = mZXingView.getScanBoxView().getTipText();
        String ambientBrightnessTip = "\n环境过暗，请打开闪光灯";
        if (isDark) {
            if (!tipText.contains(ambientBrightnessTip)) {
                mZXingView.getScanBoxView().setTipText(tipText + ambientBrightnessTip);
            }
        } else {
            if (tipText.contains(ambientBrightnessTip)) {
                tipText = tipText.substring(0, tipText.indexOf(ambientBrightnessTip));
                mZXingView.getScanBoxView().setTipText(tipText);
            }
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Toast.makeText(activity, "打开相机错误！", Toast.LENGTH_SHORT).show();
    }

    @Override   //有关权限
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }
}

