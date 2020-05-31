// should deal with the exception other than myExcept, such as network time out.
// add scan function：1. to study icon   2, to study callback result
// add et_info as log text

package com.philip.plex_qa;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.util.Log;

public class ActivitydoTask extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener,View.OnClickListener{
    //sessions data
    Map<String,String> cookies=new HashMap<>();  //should get cookies if it is null
    Plex_qa plex_qa;
    String Session_Key,host,user;  //host may be test DB or production DB
    //data for scrap
    String containerQTY,workcenter_key="";               // store QTY and workcenter_key of container for scrap method
    String containerActive="否";
    String containerStatus,containerNote;
    //dropdown data collections
    List<String> defect_list=new LinkedList<>();
    Map<String,String> scrap_map=new LinkedHashMap<>();
    TreeMap<String,String> workcenter_name_map=new TreeMap<>();  // map<workcenter_name:workcenter_key>
    //address the included layout
    View include_onhold;  //the included layout
    View include_ok;
    View include_scrap;
    //init Views
    TextView tv_info,tv_message;
    EditText et_barcode;
    Button btn_confirm;
    ImageButton btn_scan;
    Button btn_scrap,btn_onhold,btn_ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_task);

        //get cookies and session_id from Intent
        Bundle bundle = getIntent().getExtras();
        this.host=bundle.getString("host");
        this.user=bundle.getString("user");
        this.cookies=(Map<String,String>)bundle.getSerializable("cookies");
        this.Session_Key=this.cookies.get("Session_Key");
        this.Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}
        // init a plex_qa class instance
        plex_qa =new Plex_qa(this.host);
        plex_qa.cookies=this.cookies;   // transfer cookies to plex_qa instance
        // init all Views here
        tv_message = findViewById(R.id.tv_message);
        tv_info=findViewById(R.id.container_info);
        et_barcode=findViewById(R.id.et_barcode);
        btn_confirm=findViewById(R.id.btn_confirm);
        btn_scan=findViewById(R.id.btn_scan);
        btn_scrap=findViewById(R.id.btn_scrap);
        btn_onhold=findViewById(R.id.btn_onhold);
        btn_ok=findViewById(R.id.btn_ok);
        include_onhold = findViewById(R.id.include_onhold);
        include_ok=findViewById(R.id.include_ok);
        include_scrap=findViewById(R.id.include_scrap);
        //show hostname at bottom: production DB or test DB
        tv_message.setText(host+"\n * Powered by Philip *");

        tv_info.setMovementMethod(ScrollingMovementMethod.getInstance());
        Hide_Some_Views();
        show_defect_list();     //show dropdown lists
        show_scrap_reason();
        show_workcenter_list();

        // set views listeners
        {
            RadioGroup radiogroup=findViewById(R.id.radioGroup);
            radiogroup.setOnCheckedChangeListener(this);
            btn_scrap.setOnClickListener(this);btn_ok.setOnClickListener(this);btn_onhold.setOnClickListener(this);
            btn_confirm.setOnClickListener(this);btn_scan.setOnClickListener(this);
            et_barcode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //如果 barcode 有输入, 变白色,button变灰等
                    init_input_status();
                }
                @Override
                public void afterTextChanged(Editable s) {
                    //回车, 点击 btn_confirm按钮
                    String str=s.toString();
                    if(str.contains("\r")||str.contains("\n")){
                        btn_confirm.performClick();
                    }
                }
            });

        }
        et_barcode.setSelectAllOnFocus(true);  //et_barcode获得焦点时全选
    }// end of onCreate

    void Hide_Some_Views(){
        //Hide the barcode textview
        et_barcode.setVisibility(View.INVISIBLE);
        btn_confirm.setVisibility(View.INVISIBLE);
        btn_scan.setVisibility(View.INVISIBLE);
        // Hide onhold, OK and scrap layout
        this.include_onhold.setVisibility(View.GONE);
        this.include_ok.setVisibility(View.GONE);
        this.include_scrap.setVisibility(View.GONE);
    }
    void set_btn_color(int color){
        btn_scrap.setBackgroundColor(color);
        btn_onhold.setBackgroundColor(color);
        btn_ok.setBackgroundColor(color);
    }
    void set_btn_clickable(){
        btn_ok.setClickable(true);btn_scrap.setClickable(true);btn_onhold.setClickable(true);
    }
    void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }
    void init_input_status(){
        et_barcode.setBackgroundColor(Color.WHITE);
        //tv_info_addText("");
        containerActive="否";  // 此时不能作任何操作 onhold/scrap
        set_btn_color(Color.GRAY);
        btn_ok.setClickable(false);btn_scrap.setClickable(false);btn_onhold.setClickable(false);
    }
    public int dip2px(float dpValue){
        float scale=this.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale);
    }

    @Override
    public void onCheckedChanged(RadioGroup group,int checkedId){
        //clear barcode text
        et_barcode.setVisibility(View.VISIBLE);
        et_barcode.setText("");
        containerActive="否";  // 此时不能作任何操作 onhold/scrap
        et_barcode.setBackgroundColor(Color.WHITE);
        btn_confirm.setVisibility(View.VISIBLE);
        btn_scan.setVisibility(View.VISIBLE);

        if (checkedId==R.id.rd_ok){
            //only display OK layout
            this.include_ok.setVisibility(View.VISIBLE);
            btn_ok.setBackgroundColor(Color.GRAY);
            this.include_onhold.setVisibility(View.GONE);
            this.include_scrap.setVisibility(View.GONE);
            tv_info.setPadding(dip2px(5),0,0,0);
        }else if(checkedId==R.id.rd_onhold){
            //only display on hold layout
            this.include_onhold.setVisibility(View.VISIBLE);
            btn_onhold.setBackgroundColor(Color.GRAY);
            this.include_ok.setVisibility(View.GONE);
            this.include_scrap.setVisibility(View.GONE);
            tv_info.setPadding(dip2px(5),dip2px(20),0,0);
        }else if(checkedId==R.id.rd_scrap) {
            //only display scrap layout
            this.include_scrap.setVisibility(View.VISIBLE);
            btn_scrap.setBackgroundColor(Color.GRAY);
            this.include_ok.setVisibility(View.GONE);
            this.include_onhold.setVisibility(View.GONE);
            tv_info.setPadding(dip2px(5),dip2px(70),0,0);  //change place of tv_info
        }
    }

    @Override
    public void onClick(View v){
        tv_info_addText("^^^^ "+getNowTime()+" ^^^^\n");
        vibrate();
        et_barcode.setSelectAllOnFocus(true);   //// need to check the result
        et_barcode.clearFocus();
        et_barcode.requestFocus();
        et_barcode.selectAll();

        // show container info
        if(v.getId()==R.id.btn_confirm){
            init_input_status();  // can not run scrap/onhold before sub thread get container info
            try {
                Log.e(this.getClass().toString(),"Now, check container info...");
                // hide softkeyboard
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                tv_info_addText("开始检查条码...");
                    //refine barcode
                    String barcode=et_barcode.getText().toString();
                    barcode=plex_qa.refine_label(barcode);
                    et_barcode.setText(barcode);    //Textbox display the refined barcode

                    // if valid barcode inputted, then query
                    if(barcode.length()>5){
                        tv_info_addText("正在查询条码"+barcode+",请等待...");
                        Toast.makeText(this,"正在查询条码"+barcode+",请等待...",Toast.LENGTH_SHORT).show();
                        new thread_check_container_info(barcode).start();  // 多线程
                    }else{
                        et_barcode.setText("smmp");
                        tv_info_addText("条码" +barcode+"无效!");
                        Toast.makeText(this,"条码" +barcode+"无效!",Toast.LENGTH_SHORT).show();
                        //et_barcode.requestFocus();  //这个不能有,会lock barcode textbox
                    }
            } catch (Exception e) {
                tv_info_addText(e.getMessage());
                Toast.makeText(ActivitydoTask.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                //e.printStackTrace();
             }
        }else if(v.getId()==R.id.btn_scan){
            tv_info_addText("启动扫描...");
            Intent intent=new Intent();
            intent.setClass(this, ActivityScan2.class);
            startActivityForResult(intent,0);
        }else { //if scrap, on-hold or 放行
            String barcode = et_barcode.getText().toString();  //get inputted barcode
            //define valid barcode status
            List<String> valid_barcode = new ArrayList<>();
            valid_barcode.add("ok"); valid_barcode.add("hold");valid_barcode.add("defective");
            valid_barcode.add("rework"); valid_barcode.add("suspect"); valid_barcode.add("warehouse receive status");
            if(user.equals("smmp.pwang")){   //pwang 有特殊权限
                valid_barcode.add("scrap");  //scrap 用于修改报废
            }

            //if carcode status is valid, then scrap/onHold/放行......
            if (barcode.length() > 5 && containerActive.equals("是") && valid_barcode.contains(containerStatus.toLowerCase())) {
                try {
                    if (v.getId() == R.id.btn_scrap) {  //scrap
                        Spinner sp_scrap = findViewById(R.id.sp_scrap_reason);
                        Spinner sp_workcenter = findViewById(R.id.sp_workcenter);

                        String scrap_key = scrap_map.get(sp_scrap.getSelectedItem().toString());
                        String workcenter_key = workcenter_name_map.get(sp_workcenter.getSelectedItem().toString());

                        try { //报废
                            if (plex_qa.scrap_container(Session_Key, barcode, scrap_key, workcenter_key, this.containerQTY)) {
                                tv_info_addText("报废 " + barcode + " 成功!");
                            } else {
                                tv_info_addText("报废 " + barcode + " 不成功!");
                                et_barcode.setBackgroundColor(Color.YELLOW);
                            }
                        } catch (Exception e) {
                            tv_info_addText("Exception: 报废 " + barcode + " 不成功!");
                            et_barcode.setBackgroundColor(Color.YELLOW);
                            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (v.getId() == R.id.btn_ok) {
                        if (plex_qa.change_status(Session_Key, et_barcode.getText().toString(), "OK", "", containerNote)) {
                            tv_info_addText("放行 " + barcode + " 成功!");
                        } else {
                            tv_info_addText("放行 " + barcode + " 不成功!");
                            et_barcode.setBackgroundColor(Color.YELLOW);
                        }
                    }

                    if (v.getId() == R.id.btn_onhold) {
                        Spinner sp_onHold = findViewById(R.id.sp_onhold_reason);
                        String onHold_reason = sp_onHold.getSelectedItem().toString();
                        onHold_reason = onHold_reason.substring(0, 5);    //取 reason code

                        if (plex_qa.change_status(Session_Key, et_barcode.getText().toString(), "Hold", onHold_reason, containerNote)) {
                            tv_info_addText("待处理 " + barcode + " 成功!");
                        } else {
                            tv_info_addText("待处理 " + barcode + " 不成功!");
                            et_barcode.setBackgroundColor(Color.YELLOW);
                        }
                    }
                    //onClick之后, et_barcode获得焦点
                    //et_barcode.setSelectAllOnFocus(true);
                    //et_barcode.requestFocus();
                } catch (Exception e) {
                    tv_info_addText("操作 " + barcode + " 不成功!\n");
                    et_barcode.setBackgroundColor(Color.YELLOW);
                    Toast.makeText(ActivitydoTask.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                }
            } else {
                init_input_status();
                et_barcode.setBackgroundColor(Color.RED);
                set_btn_color(Color.GRAY);
                tv_info_addText("条码" + barcode + "不能操作!");
                Toast.makeText(ActivitydoTask.this, "条码" + barcode + "不能操作!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override   //显示 选项菜单
    public boolean onCreateOptionsMenu(Menu menu){
        if(user.equals("smmp.pwang")) {  //pwang有特殊功能
            getMenuInflater().inflate(R.menu.bottom_nav_menu, menu);
            return true;
        }
        return false;
    }

    @Override    //执行 选项菜单
    public boolean onOptionsItemSelected(MenuItem item){
        int id=item.getItemId();
        //show container history
        if(id==R.id.navigation_container_history && et_barcode.getText().length()>9){
            //refine barcode
            String barcode=et_barcode.getText().toString();
            barcode=plex_qa.refine_label(barcode);

            // Transfer to next page
            Intent intent = new Intent(this, ActivityContainerHistory.class);
            Bundle bundle = new Bundle();
            bundle.putString("host", host);
            bundle.putString("barcode",barcode);
            bundle.putString("function","Inventory_History");  //显示 箱号历史
            bundle.putSerializable("cookies", (Serializable) cookies);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;    //true if the callback consumed the long click, false otherwise, will run onClick
        }
        if(id==R.id.navigation_container_loaded ){  //长按最下边的文字
            // Transfer to next page
            Intent intent = new Intent(this, ActivityContainerHistory.class);
            Bundle bundle = new Bundle();
            bundle.putString("host", host);
            bundle.putString("barcode","");
            bundle.putString("function","Inventory_Loaded");  //决定 下一个页面 到底干什么
            bundle.putSerializable("cookies", (Serializable) cookies);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;    //true if the callback consumed the long click, false otherwise, will run onClick
        }else if(id==R.id.navigation_finish){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        et_barcode.setText("");
        if (data!=null){
            String result=data.getStringExtra("barcode");
            if (!TextUtils.isEmpty(result)) {
                et_barcode.setText(data.getStringExtra("barcode"));
            }
        }
    }

    private class thread_check_container_info extends Thread {
        String barcode;
        //构造函数
        public thread_check_container_info(String barcode){
            this.barcode=barcode;
        }
        @Override
        public void run() {
            try {
                //multi thread to get container info
                Map<String,String> map_info=plex_qa.show_container_info(Session_Key,barcode);
                Message message1 = Message.obtain();
                message1.what = 1;
                message1.obj = map_info;
                mHandler.sendMessage(message1);
                //multi thread to get workcenter_key
                workcenter_key=plex_qa.get_container_workcenter(Session_Key,barcode);  //get workcenter_key for scrap method
                mHandler.sendEmptyMessage(2);
            }catch (MyException e) {
                Message message1 = Message.obtain();
                message1.what = 3;
                message1.obj = e.getMessage();   //发送exception信息给 主线程
                mHandler.sendMessage(message1);
            }catch (Exception e) {
                e.printStackTrace();
                Message message1 = Message.obtain();
                message1.what = 3;
                message1.obj = e.getMessage()+"\n条码可能不存在！Exception!";   //发送exception信息给 主线程
                mHandler.sendMessage(message1);
            }
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            String Barcode="";
            if (msg.what == 1) {  // 1 means "multi thread" got container info
                Map<String,String> map_info=(LinkedHashMap<String, String>)msg.obj;

                // if get valid container information, show it
                if (map_info !=null) {
                    // change color according to container status
                    if (map_info.get("txtActive").equals("false")||map_info.get("txtQTY").equals("0")){
                        init_input_status();   // hide buttons
                        containerActive="否";
                        et_barcode.setBackgroundColor(Color.RED);
                        set_btn_color(Color.GRAY);
                        //et_barcode.requestFocus();  //but this will lock et_barcode!!!!
                    }else{
                        containerActive="是";
                        et_barcode.setBackgroundColor(Color.WHITE);
                        set_btn_color(Color.GREEN);
                        set_btn_clickable();
                    }
                    //barcode info to be showed
                    containerQTY=map_info.get("txtQTY");   //get containerQTY for scrap method
                    containerStatus=map_info.get("curStatus");
                    containerNote=map_info.get("txtNote");
                    Barcode=map_info.get("barcode");
                    String info = "条码: " + Barcode + "   料号: " + map_info.get("txtPartNo")+ "\n";
                    info += "库位: " + map_info.get("txtLocation") + "　数量: "+ containerQTY + "\n";
                    info += "激活: " + containerActive + "   状态: " + containerStatus + "\n";
                    info += "备注: " + containerNote+"\n";
                    tv_info_addText(info);
                    //button should be show here
                    //btn_scrap.setClickable(true);
                    //btn_onhold.setVisibility(View.VISIBLE);
                    //btn_ok.setVisibility(view.VISIBLE);
                }else{
                    tv_info_addText("查询不成功! 条码可能不存在\n");
                    containerActive="否";   // 此时不能作任何操作 onhold/scrap
                    et_barcode.setBackgroundColor(Color.RED);
                    set_btn_color(Color.GRAY);
                }
            }
            if (msg.what == 2) {  // 2 means "multi thread" got workcenter key
                //let splinder dropdown show container workcenter
                List<String> workcenter_key_list = new ArrayList<>();
                workcenter_key_list.addAll(workcenter_name_map.values());  // this list only store data of workcenter key, which will provide the index for SP to show
                Spinner sp = findViewById(R.id.sp_workcenter);
                sp.setSelection(workcenter_key_list.indexOf(workcenter_key));
            }
            if(msg.what==3){    // 3 means exception at child thread
                tv_info_addText(msg.obj+"\n");
                containerActive="否";   // 此时不能作任何操作 onhold/scrap
                et_barcode.setBackgroundColor(Color.RED);
                set_btn_color(Color.GRAY);
            }
        }
    };

    private void show_dropdown_list(String tip,int viewID,List<String> list){
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,R.layout.item_select,list);
        adapter.setDropDownViewResource(R.layout.item_dropdown); //item_dropdown refers to dropdown list
        Spinner sp_drowpdown=findViewById(viewID);
        sp_drowpdown.setPrompt(tip);
        sp_drowpdown.setAdapter(adapter);
    }
    private void show_defect_list(){
        //should check if the return is null
        try {
            defect_list=plex_qa.get_defect_list(this.Session_Key);
            show_dropdown_list("On Hold Reason...",R.id.sp_onhold_reason,defect_list);
        } catch (Exception e) {
            Toast.makeText(ActivitydoTask.this,e.getMessage(),Toast.LENGTH_SHORT).show();
            //e.printStackTrace();
        }

    }
    private void show_workcenter_list(){
        try {
            //should check if the return is null
            workcenter_name_map=plex_qa.get_workcenter_list(this.Session_Key);
            //workcenter_name_map.put("","");
            List<String> workcenter_list=new ArrayList<>();
            workcenter_list.addAll(workcenter_name_map.keySet());

            show_dropdown_list("工作中心 Workcenter",R.id.sp_workcenter,workcenter_list);
        } catch (Exception e) {
            Toast.makeText(ActivitydoTask.this,e.getMessage(),Toast.LENGTH_SHORT).show();
            //e.printStackTrace();
        }
    }
    private void show_scrap_reason(){
        //should check if the return is null
        try {
            scrap_map=plex_qa.get_scrap_reason_file(this.Session_Key);
            List<String> scrap_list=new ArrayList<>();
            scrap_list.addAll(scrap_map.keySet());

            show_dropdown_list("报废原因 Scrap Reason",R.id.sp_scrap_reason,scrap_list);
        } catch (Exception e) {
                Toast.makeText(ActivitydoTask.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                //e.printStackTrace();
            }
    }
    private void tv_info_addText(String str){
        if (!TextUtils.isEmpty(str)){
            String str_raw=tv_info.getText().toString();
            if (str_raw.length()>2600){
                str_raw=str_raw.substring(0,2400);
            }
            tv_info.setText(str+"\n"+str_raw);
            tv_info.scrollTo(0,0);
            // 以下是顺序显示
            // tv_info.append("\n"+str);
            // int offset=tv_info.getLineCount()*tv_info.getLineHeight();
            // if(offset>tv_info.getHeight()){
            //     tv_info.scrollTo(0,offset-tv_info.getHeight()+tv_info.getLineHeight()*2);
            //}
        }
    }

    public static String getNowDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }

    public static String getNowTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

}

