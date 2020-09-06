package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class fifoActivity extends AppCompatActivity {
    HashMap<String,String> cookies=new HashMap<>();
    String pre_url,Session_Key;

    //init Viewstv
    TextView tv_info,tv_message,tv_canlist,tv_cannotlist;
    EditText et_barcode,et_location;
    Button btn_confirm;
    ImageButton btn_scan;
    Button btn_move,btn_issue;
    RadioGroup radiogroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifo);

        //disable the strict polity that do not allows main thread network access
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //get cookies and session_id from Intent
        Bundle bundle = getIntent().getExtras();
        //this.host=bundle.getString("host");
        //this.user=bundle.getString("user");
        this.cookies=(HashMap<String,String>)bundle.getSerializable("cookies");
        this.Session_Key=this.cookies.get("Session_Key");
        this.Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}
        pre_url="https://www.plexus-online.com/"+Session_Key;

        init();
    }

    private void init(){
        radiogroup=findViewById(R.id.radioGroup);
        et_barcode=findViewById(R.id.et_barcode);btn_confirm=findViewById(R.id.btn_confirm);btn_scan=findViewById(R.id.btn_scan);
        et_location=findViewById(R.id.et_location);
        tv_info=findViewById(R.id.tvInfo);
        tv_canlist=findViewById(R.id.tv_canList);tv_cannotlist=findViewById(R.id.tv_cannotList);

        et_barcode.setVisibility(View.GONE);btn_confirm.setVisibility(View.GONE);btn_scan.setVisibility(View.GONE);
        et_location.setVisibility(View.GONE);
        tv_info.setVisibility(View.GONE);
        tv_canlist.setVisibility(View.GONE);tv_cannotlist.setVisibility(View.GONE);

        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                et_barcode.setVisibility(View.VISIBLE);btn_confirm.setVisibility(View.VISIBLE);btn_scan.setVisibility(View.VISIBLE);
                et_location.setVisibility(View.VISIBLE);
                tv_info.setVisibility(View.VISIBLE);
                //clear barcode text
                et_barcode.setText("");
                et_location.setText("");
                //containerActive="否";  // 此时不能作任何操作 onhold/scrap
                if (checkedId==R.id.rd_move){
                    //only display OK layout
                    tv_canlist.setVisibility(View.GONE);
                    tv_cannotlist.setVisibility(View.GONE);
                    //tv_info.setPadding(dip2px(5),0,0,0);
                }else if(checkedId==R.id.rd_fifo_issue){
                    //only display on hold layout
                    tv_canlist.setVisibility(View.VISIBLE);
                    tv_cannotlist.setVisibility(View.VISIBLE);
                    //tv_info.setPadding(dip2px(5),dip2px(20),0,0);
                }
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId()==R.id.btn_confirm){
                    //refine barcode
                    String barcode=et_barcode.getText().toString();
                    barcode=Utils.refine_label(barcode);
                    et_barcode.setText(barcode);    //Textbox display the refined barcode

                    try {
                        tv_info.setText("正在查条码......"+barcode);
                        String url=pre_url+"/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=";
                        Map<String,String> info=Utils.show_container_info(cookies,url,barcode);
                        System.out.println("条码如下：");
                        System.out.println(info.get("barcode")+"  "+info.get("txtPartNo")+"   "+info.get("txtQTY"));

                        url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(10617)";
                        Utils.check_fifo(cookies,url,barcode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //check_container_info_fifo(barcode);
                }
            }
        });
    }



}