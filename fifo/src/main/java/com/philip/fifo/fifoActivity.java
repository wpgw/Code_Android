package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;

public class fifoActivity extends AppCompatActivity {
    Map<String,String> cookies=new HashMap<>();
    String Session_Key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifo);

        //get cookies and session_id from Intent
        Bundle bundle = getIntent().getExtras();
        //this.host=bundle.getString("host");
        //this.user=bundle.getString("user");
        this.cookies=(Map<String,String>)bundle.getSerializable("cookies");
        this.Session_Key=this.cookies.get("Session_Key");
        this.Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}


    }
}