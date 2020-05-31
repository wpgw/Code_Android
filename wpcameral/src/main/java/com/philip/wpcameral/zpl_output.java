package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class zpl_output extends AppCompatActivity {
    Button btnPrint;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zpl_output);
        activity=this;

        final String item="P149WGG";

        btnPrint=(Button)findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                Toast.makeText(activity,"will output to network",Toast.LENGTH_LONG).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Looper.prepare();
                            final String ip="127.0.0.1";
                            final Integer port=9100;
                            Socket client=new Socket(ip,port);
                            String sb="^XA^LH0,0^F0203,203^BY3,2.4,50^B3N,Y,,Y^FDABC123^FS^XZ";
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
