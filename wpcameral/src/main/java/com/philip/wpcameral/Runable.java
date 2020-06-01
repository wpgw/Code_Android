package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Runable extends AppCompatActivity implements View.OnClickListener {
    Button btn_runnable;
    TextView tv_result;
    boolean status=false;
    int count=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runable);
        btn_runnable=findViewById(R.id.btn_runnable);
        tv_result=findViewById(R.id.tv_result);

        btn_runnable.setOnClickListener(this);
    }
    @Override
    public void onClick(View V){
        if(V.getId()==R.id.btn_runnable){
            if(!status){
                btn_runnable.setText("stop");
                handler.post(on_count);
            }else{
                btn_runnable.setText("start");
                handler.removeCallbacks(on_count);
            }
            status=!status;
        }
    }

    private Handler handler=new Handler();
    private Runnable on_count=new Runnable(){
        @Override
        public void run(){  //这个没有任何出错处理
            count++;
            tv_result.setText("currunt count is "+count);
            System.out.println("currunt count is "+count);
            handler.postDelayed(this,1000);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

        }
    };

}
