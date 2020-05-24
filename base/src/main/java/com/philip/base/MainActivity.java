package com.philip.base;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    Button btnPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String item="P149WGG";

        btnPrint=(Button)findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Looper.prepare();
                            final String ip="127.0.0.1";
                            final Integer port=9100;
                            Socket client=new Socket(ip,port);
                            String sb="content";
                            byte[] mybytearray=sb.getBytes();
                            OutputStream outputStream=client.getOutputStream();
                            outputStream.write(mybytearray,0,mybytearray.length);
                            outputStream.flush();
                            outputStream.close();
                            client.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        });
    }
}
