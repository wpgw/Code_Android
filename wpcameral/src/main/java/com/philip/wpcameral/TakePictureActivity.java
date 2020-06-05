package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
// Take picture with camera
public class TakePictureActivity extends AppCompatActivity implements OnClickListener {
    private CameraView camera_view;
    private int mTaketype=0; // 拍照类型。0为单拍，1为连拍

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        camera_view=findViewById(R.id.camera_view);

        findViewById(R.id.btn_shutter).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.btn_shutter){
            camera_view.doTakePicture();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TakePictureActivity.this,"已完成拍照:"+camera_view.mPhotoPath,Toast.LENGTH_LONG).show();
                }
            },1500);

        }
    }
}
