//https://www.jianshu.com/p/eca7335602c1
//https://www.cnblogs.com/io1024/p/11590382.html

package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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
        Uri fileUri=this.getUriForFile(this,photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
        startActivityForResult(intent,100);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        System.out.println("嘿嘿  ："+requestCode);
        if(resultCode==RESULT_OK) {
            show_img(localPath);
        }
    }

    private void show_img(String filePath){
        FileInputStream fis=null;
        try{
            Log.d("记录Local Path",filePath);

            fis=new FileInputStream(filePath);
            Bitmap bitmap= BitmapFactory.decodeStream(fis);
            ivMyphoto.setImageBitmap(bitmap);

        }catch(Exception e){
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

    private Uri getUriForFile(Context context, File file) {
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= 24) {  //7.0以上
            //参数：authority 需要和清单文件Mannifest中配置的保持完全一致：${applicationId}.fileprovider
            fileUri = FileProvider.getUriForFile(context,"com.philip.wpcameral.fileProvider" , file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        return fileUri;
    }
}
