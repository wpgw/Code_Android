package com.philip.wpcameral;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class wpCameraActivity extends AppCompatActivity {
    Button btnPrint;
    EditText etText;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wp_camera);
        activity=this;

        final String item="P149WGG";

        btnPrint=(Button)findViewById(R.id.btnPrint);
        etText=(EditText) findViewById(R.id.etText);
        etText.setText("^XA^LH203,203^F0203,203^BY3,2.4,50^B3N,Y,,Y^FDABC123^FS^XZ");
        btnPrint.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                Toast.makeText(activity,"will output to network",Toast.LENGTH_LONG).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Looper.prepare();
                            final String ip="10.70.30.162";
                            final Integer port=9100;
                            Socket client=new Socket(ip,port);
                            //String sb="^XA^LH203,203^F0203,203^BY3,2.4,50^B3N,Y,,Y^FDABC123^FS^XZ";
                            String sb=etText.getText().toString();
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
