package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.philip.wpcameral.util.BitmapUtil;
import com.philip.wpcameral.util.DateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SystemCamera extends AppCompatActivity {
    private String localPath;
    private ImageView ivMyphoto;
    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_camera);

        ivMyphoto=findViewById(R.id.ivMyPhoto);

        localPath=String.format("%s%s.jpg", BitmapUtil.getCachePath(this), DateUtil.getNowDate());
        photoFile=new File(localPath);
        //if((photoFile.getParent()!=null)&&(!photoFile.getParentFile().exists())){
        //    //photoFile.getParent().mkdirs();
        //}
        System.out.println("相机"+ "路径-localPath：" + localPath);
    }

    public void playPhoto(View view){
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri fileUri=Uri.fromFile(photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
        startActivityForResult(intent,100);
    }

    //@Override
    protected void onActivityResult(int requestCode,int resultCode,Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        show_img(localPath);
    }

    private void show_img(String filePath){
        FileInputStream fis=null;
        try{
            System.out.println("文件路径----------"+filePath);
            System.out.println("文件路径----------"+filePath);
            Log.d("记录Local Path：",filePath);
            Log.d("记录Local Path：",filePath);

            fis=new FileInputStream(filePath);

            Bitmap bitmap= BitmapFactory.decodeStream(fis);
            ivMyphoto.setImageBitmap(bitmap);

        }catch(Exception e){
            System.out.println("___________Read Error__________");
            e.printStackTrace();
        }finally{
            if(fis!=null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
