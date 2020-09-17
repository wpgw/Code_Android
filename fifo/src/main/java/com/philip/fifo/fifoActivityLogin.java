package com.philip.fifo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.philip.comm.Plex_login;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class fifoActivityLogin extends AppCompatActivity implements View.OnClickListener{
    Map<String, String> cookies = new HashMap<>();
    Plex_login plex;
    String host;  //host may be test DB or production DB
    EditText et_userid, et_password;
    int count; //to limit the user to click on login button
    private int msgBEGIN = 0, msgEND = 1, msgFAILLED = 2;  //Used in message
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //plex point to Test DB or Production DB
        host = "test.plexus-online.com";
        TextView tv_message = (TextView) findViewById(R.id.tv_message);
        tv_message.setText("for "+ host +"\n * Powered by Philip *");
        //tv_message.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        plex = new Plex_login(host);

        //display user ID
        et_userid = findViewById(R.id.et_userid);
        et_password = findViewById(R.id.et_password);
        et_userid.setText(plex.get_userInfo().get(0));  //从文件中读取userid
        if (host.equals("test.plexus-online.com")) {
            et_userid.setText("smmp.pwang");   ////////shortcut only for Test DB
            et_password.setText("77665544");
        }
        intent = new Intent(this, fifoActivity.class);

        count = 0;

        findViewById(R.id.button).setOnClickListener(this);
        //findViewById(R.id.et_userid).setOnFocusChangeListener(this);

        //disable the strict polity that do not allows main thread network access
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Log.d("ActivityLogin", "I am a ActivityLogin.");

        // set path to /data/data
        Plex_login.path = getApplicationContext().getExternalCacheDir().getPath();   //.getFilesDir().getPath();  //point to app private directory
        // Plex_login.path=Environment.getExternalStorageDirectory().getPath()+"/Download"; //for sdcard
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button && count < 2) {
            vibrate();

            // 给 userid 自动加上smmp
            String userid=et_userid.getText().toString().toLowerCase().replace("smmp.","");
            et_userid.setText("smmp."+userid);

            new login().start();
            count++;        // to limit the user click on login button to 2 time
        }
    }

    private class login extends Thread {
        @Override
        public void run() {
            //如果有输入用户名和密码
            if (et_userid.getText().toString().length() > 2 && et_password.getText().toString().length() > 5) {
                mHandler.sendEmptyMessage(msgBEGIN);    // child thread to transfer message to mian thread
                try {
                    cookies = plex.login(et_userid.getText().toString(), et_password.getText().toString(), "smmp");
                    if (cookies != null && cookies.get("Session_Key") != null) {
                        //Toast.makeText(this, cookies.get("Session_Key"), Toast.LENGTH_SHORT).show();
                        //如果成功，发出成功消息
                        mHandler.sendEmptyMessage(msgEND);
                    } else {
                        //Toast.makeText(this, "用户名或密码错误!", Toast.LENGTH_SHORT).show();
                        Message message = Message.obtain();
                        message.what = msgFAILLED;
                        message.obj = "用户名或密码错误!或未联网!";
                        mHandler.sendMessage(message);
                    }
                } catch (Exception e) {
                    //Toast.makeText(this,e.getMessage(), Toast.LENGTH_SHORT).show();
                    Message message = Message.obtain();
                    message.what = msgFAILLED;
                    message.obj = e.getMessage();
                    mHandler.sendMessage(message);
                    //e.printStackTrace();
                }
            }
        }
    }

    //处理子线程发回的消息
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Button btn = findViewById(R.id.button);
            if (msg.what == msgBEGIN) {
                btn.setText("正在登录,请等待..." + count);
            }
            if (msg.what == msgEND) { //如果收到成功消息
                btn.setText("已 登 录 ...");
                // Transfer to next page
                Bundle bundle = new Bundle();
                bundle.putString("host", host);
                bundle.putSerializable("cookies", (Serializable) cookies);
                bundle.putString("user",et_userid.getText().toString());
                intent.putExtras(bundle);
                startActivity(intent);
                count = 0;  //count 是登陆计数
            }
            if (msg.what == msgFAILLED) {
                btn.setText(msg.obj.toString() + " 请重试...");
                count = 0;
            }
        }
    };

    //震动
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }
}
