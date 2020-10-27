package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.mlplugin.asr.MLAsrCaptureActivity;
import com.huawei.hms.mlplugin.asr.MLAsrCaptureConstants;
import com.huawei.hms.mlsdk.asr.MLAsrConstants;
import com.huawei.hms.mlsdk.asr.MLAsrListener;
import com.huawei.hms.mlsdk.asr.MLAsrRecognizer;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.philip.comm.Utils;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

public class fifoActivity extends AppCompatActivity {
    HashMap<String,String> cookies=new HashMap<>();
    String pre_url,Session_Key,host,user;
    final int MSG=1, showBarcode_info =2, refresh_FIFOlist_on_UI =3,alartColor=4,normalColor=5,MOVED=6,enableRadioGroup=7;
    int scanRequestCode=1016,mSpeechRecognizeCode=1010;
    //init Views
    TextView tv_info,tv_BarcodeInfo,tv_canlist,tv_cannotlist,tv_movedlist;  //tv_movedlist记录移动成功的条码
    EditText et_barcode,et_location;
    Button btn_confirm,btn_speechRec;
    ImageButton btn_scan;
    RadioButton rd_move,rd_issue,rd_report;
    RadioGroup radiogroup;
    int movedCount; //用以记录业务操作成功的记数
    int issueLock;  //用以锁定发货，以防一个未完，就连击另一个 0：开放   1：加锁

    String barcode;   //用于存当前处理的条码号，传给thread
    ArrayList<Part_FIFO_Data> alllist=new ArrayList<Part_FIFO_Data>();
    ArrayList<Part_FIFO_Data> canlist=new ArrayList<Part_FIFO_Data>();
    ArrayList<Part_FIFO_Data> cannotlist=new ArrayList<Part_FIFO_Data>();  //fifo允许的物料及不允许的物料

    private TextToSpeech textToSpeech = null;//创建自带语音对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fifo);

        //HMS 云端鉴权信息: 用于实时语音识别
        MLApplication.getInstance().setApiKey("CgB6e3x98+cyGVKCai8OVbmAc91GfT4gOjKFA8VeKzSms+JC54jSknujW1146rx7dqd8hVOf1HNOeKpI6zWfy+wK");

        //还需加上权限代码
        
        //disable the strict polity that do not allows main thread network access
//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }

        //get cookies and session_id from Intent
        Bundle bundle = getIntent().getExtras();
        this.host=bundle.getString("host");
        this.user=bundle.getString("user");
        this.cookies=(HashMap<String,String>)bundle.getSerializable("cookies");
        this.Session_Key=this.cookies.get("Session_Key");
        this.Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}
        pre_url="https://"+host+"/"+Session_Key;

        init();
        initTTS();
    }

    private void init(){
        radiogroup=findViewById(R.id.radioGroup);rd_move=findViewById(R.id.rd_move);rd_issue=findViewById(R.id.rd_issue);rd_report=findViewById(R.id.rd_report);
        et_barcode=findViewById(R.id.et_barcode);btn_confirm=findViewById(R.id.btn_confirm);btn_speechRec=findViewById(R.id.btn_speechRec);btn_scan=findViewById(R.id.btn_scan);
        et_location=findViewById(R.id.et_location);tv_BarcodeInfo=findViewById(R.id.tvBarcodeInfo);
        tv_info=findViewById(R.id.tvInfo);
        tv_movedlist=findViewById(R.id.tv_movedList);tv_canlist=findViewById(R.id.tv_canList);tv_cannotlist=findViewById(R.id.tv_cannotList);
        tv_movedlist.setMovementMethod(ScrollingMovementMethod.getInstance());tv_canlist.setMovementMethod(ScrollingMovementMethod.getInstance());tv_cannotlist.setMovementMethod(ScrollingMovementMethod.getInstance());
        movedCount=0;issueLock=0;enableRadioGroup(radiogroup);

        et_barcode.requestFocusFromTouch();et_barcode.requestFocus();
        et_barcode.setSelectAllOnFocus(true);   //et_barcode获得焦点时全选
        et_location.setSelectAllOnFocus(true);  //et_barcode获得焦点时全选
        //开始时不显示有关控件
        et_barcode.setVisibility(View.GONE);btn_confirm.setVisibility(View.GONE);btn_speechRec.setVisibility(View.GONE);btn_scan.setVisibility(View.GONE);
        et_location.setVisibility(View.GONE);tv_BarcodeInfo.setVisibility(View.GONE);
        tv_info.setVisibility(View.INVISIBLE);
        tv_movedlist.setVisibility(View.GONE);tv_canlist.setVisibility(View.GONE);tv_cannotlist.setVisibility(View.GONE);

        et_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if((actionId== EditorInfo.IME_ACTION_DONE)){
                    System.out.println("准备执行 软健盘 click");
                    btn_confirm.performClick();
                    return true;  // 消费 CR
                }else if((event!=null)&&event.getAction()==KeyEvent.ACTION_DOWN&&(event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){
                    System.out.println("准备执行 扫描枪 click2");
                    btn_confirm.performClick();
                    return true;  // 消费 CR
                }
                return false;
            }
        });

        et_barcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //如果 barcode 有输入, 变白色,button变灰等
                et_barcode.setBackgroundColor(Color.WHITE);
                tv_info.setBackgroundColor(Color.WHITE);
            }
            @Override
            public void afterTextChanged(Editable s) {
                String str=s.toString();
                if(str.contains("\n")||str.contains("\r")){
                    btn_confirm.performClick();   //从相机返回的含\n, 会执行
                }
            }
        });
        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                et_barcode.setVisibility(View.VISIBLE);btn_confirm.setVisibility(View.VISIBLE);btn_speechRec.setVisibility(View.VISIBLE);btn_scan.setVisibility(View.VISIBLE);
                et_location.setVisibility(View.VISIBLE);
                //tv_info.setVisibility(View.VISIBLE);
                //clear barcode text
                et_barcode.setText("");et_location.setText("");tv_info.setText("");
                et_barcode.requestFocus();et_barcode.requestFocusFromTouch();
                sendMessage(normalColor,""); //信息栏显成白色
                movedCount=0;issueLock=0;enableRadioGroup(radiogroup);
                tv_movedlist.setText("");tv_info.setText("");  //清空记数及info显示
                tv_info.setVisibility(View.INVISIBLE);    //在内容有变时，会自动显出来
                tv_movedlist.setVisibility(View.GONE);
                clear_list_data_and_UI_display();  //每次变化，都初始化fifo数据与显示
                //containerActive="否";  // 此时不能作任何操作 onhold/scrap
                SpannableString s=new SpannableString("请扫码...发料");
                if (checkedId==R.id.rd_move){
                    //移库
                    tv_canlist.setVisibility(View.GONE);
                    tv_cannotlist.setVisibility(View.GONE);
                    tv_BarcodeInfo.setText("");tv_BarcodeInfo.setVisibility(View.GONE);  //不显示条码信息
                    et_location.setText("");et_location.setVisibility(View.VISIBLE);     //显示 location
                    //et_location.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                    //et_location.setEnabled(true);
                    //tv_info.setPadding(dip2px(5),0,0,0);
                    s = new SpannableString("扫库位或箱号......");     //这里输入自己想要的提示文字
                }else if(checkedId==R.id.rd_issue||checkedId==R.id.rd_report){  //如 FIFO发货 或查 FIFO报表
                    //发货
                    tv_canlist.setVisibility(View.VISIBLE);
                    tv_cannotlist.setVisibility(View.VISIBLE);
                    tv_BarcodeInfo.setText("");tv_BarcodeInfo.setVisibility(View.VISIBLE);  //显示条码信息
                    et_location.setText("");et_location.setVisibility(View.GONE);        //不显 location
                    //et_location.setEnabled(false);
                    //tv_info.setPadding(dip2px(5),dip2px(20),0,0);
                    if(checkedId==R.id.rd_report){
                        s = new SpannableString("输料号 如5%742 ");  //这里输入自己想要的提示文字
                        et_barcode.setHint(s);
                        et_barcode.setText("5%");
                        et_barcode.setSelection(1);
                        btn_scan.setVisibility(View.GONE);
                    }
                }
                et_barcode.setHint(s);
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcode = et_barcode.getText().toString().trim().toUpperCase();  //扫描结果变大写
                barcode=barcode.replace("\n","");
                et_barcode.setText(barcode);  //多余，保险
                if(rd_issue.isChecked()&&issueLock==0) {  //如果在发货界面
                    //refine barcode
                    //barcode = Utils.refine_label(barcode);  //会自动转upper，无效返回 ""
                    et_barcode.setText(barcode);    //Textbox display the refined barcode
                    if (barcode.length() >= 9){     //粗看一下合法性
                        try {
                            issueThread issuethread = new issueThread();
                            if (issueLock == 0) {
                                issueLock = 1;  //加锁，不能再多开issuethread
                                disableRadioGroup(radiogroup);
                                issuethread.start();
                                say(barcode.substring(barcode.length()-4,barcode.length()));    //开始发料
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            issueLock = 0;  //开锁
                            enableRadioGroup(radiogroup);
                            sendMessage(MSG, e.getMessage());
                        }
                    }else{
                        tv_info.setText("检查发现你的输入不正确！");
                        sendMessage(alartColor,"");
                    }
                }else if(rd_move.isChecked()&&issueLock==0){  //如果在移库界面
                    String location=Utils.check_if_location(barcode);  //自动判断 barcode栏里是否是location？
                    if(location.length()>2){    //如barcode输入的是location
                        et_location.setText(location);
                        et_barcode.setText("");barcode="";
                        say("库位！");
                    }else{        //如果输入的不是location,是箱号
                        //refine barcode  但以下注释掉了
                        //barcode=Utils.refine_label(barcode);
                        et_barcode.setText(barcode);
                        location=et_location.getText().toString();  //也可能直接在et_location上输入
                        String temp_loc=location.toUpperCase();
                        if(temp_loc.contains("ASSY")||temp_loc.contains("CNC")||temp_loc.contains("VIBE")){
                            et_location.setText("");
                            sendMessage(MSG,"对现场库位 "+temp_loc+"， 需做FIFO发料！");
                            //变红色警告
                            sendMessage(alartColor,"");
                            et_barcode.selectAll();  //选用文字，以便于下次输入
                            return;
                        }
                        if(barcode.length()>=9&&location.length()>2){  //库位与箱号有效
                            System.out.println("现在开始移库！");
                            try {
                                if(issueLock==0){
                                    issueLock=1;      //加锁  现在是单线程，暂无作用
                                    disableRadioGroup(radiogroup);
                                    say(barcode.substring(barcode.length()-4,barcode.length()));   //开始移库
                                    container_move(barcode,location);   //将来可能需改到多线程
                                    issueLock=0;     //开锁
                                    enableRadioGroup(radiogroup);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                issueLock=0;
                                enableRadioGroup(radiogroup);
                                sendMessage(MSG,e.getMessage());
                            }
                        }else{
                            tv_info.setText("检查发现你的输入不正确！");
                            sendMessage(alartColor,"");
                        }
                    }
                }else if(rd_report.isChecked()&&issueLock==0){  //如果在查报表界面
                    tv_info.setText("");
                    if(barcode.length()>=5){   //如：光输入5%是不行的，不够长
                        try{
                            issueLock=1;
                            disableRadioGroup(radiogroup);
                            say("查库位！");
                            //先清理canList, cannotList
                            clear_list_data_and_UI_display();
                            sendMessage(MSG,"2,读取FIFO数据中......");
                            get_fifo_list(barcode);   //此时 barcode中放的是物料号
                            if(canlist.size()>0){
                                //此时查询 fifo报表
                                String txtPartNo=canlist.get(0).part_no;
                                String location=canlist.get(0).location;
                                tv_BarcodeInfo.setText(txtPartNo+" 在库位 "+location);
                            }
                        }
                        catch (Exception e) {
                            //显示出错信息
                            sendMessage(MSG,e.getMessage());
                            sendMessage(alartColor,"");
                            e.printStackTrace();
                        }finally {
                            issueLock=0;     //开锁
                            enableRadioGroup(radiogroup);
                        }
                    }else{
                        tv_info.setText("输入不够长！");
                    }
                }
//                et_barcode.setSelectAllOnFocus(true);   //当重新选中时，文字全选
//                et_barcode.clearFocus();
                et_barcode.selectAll();                   //文字全选
//                et_barcode.setFocusable(true);
//                et_barcode.setFocusableInTouchMode(true);
//                et_barcode.requestFocusFromTouch();
//                et_barcode.requestFocus();
//                et_barcode.findFocus();
            }
        });
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
                //ScanUtil.startScan(fifoActivity.this,scanRequestCode,null);
                Intent intent=new Intent(fifoActivity.this,DefinedActivity.class);
                startActivityForResult(intent,scanRequestCode);
            }
        });
        btn_speechRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
                // 通过intent进行识别设置。
                Intent intent = new Intent(fifoActivity.this, MLAsrCaptureActivity.class)
                        // 设置识别语言为英语，若不设置，则默认识别英语。支持设置："zh-CN":中文；"en-US":英语；"fr-FR":法语；"es-ES":西班牙语；"de-DE":德语；"it-IT":意大利语。
                        .putExtra(MLAsrCaptureConstants.LANGUAGE, "zh-CN")
                        // 设置拾音界面是否显示文字，MLAsrCaptureConstants.FEATURE_ALLINONE为不显示，MLAsrCaptureConstants.FEATURE_WORDFLUX为显示。
                        .putExtra(MLAsrCaptureConstants.FEATURE, MLAsrCaptureConstants.FEATURE_WORDFLUX);
                startActivityForResult(intent,mSpeechRecognizeCode);
            }
        });
    }

    @Override   //扫描回调
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        et_barcode.setText("");String text="";
        if (requestCode == scanRequestCode) {
            if (resultCode != RESULT_OK || data == null) {
                System.out.println("失败码："+resultCode);
                return;
            }
            HmsScan obj = data.getParcelableExtra(DefinedActivity.SCAN_RESULT);
            if (obj != null) {
                //展示解码结果
                System.out.println(obj);
                //vibrate();
                et_barcode.setText(obj.getOriginalValue()+"\n");  //加个换行，用来performclick()
            }
            return;
        }
        if(requestCode==mSpeechRecognizeCode){
            switch (resultCode) {
                // 返回值为MLAsrCaptureConstants.ASR_SUCCESS表示识别成功。
                case MLAsrCaptureConstants.ASR_SUCCESS:
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        // 获取语音识别得到的文本信息。
                        if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_RESULT)) {
                            text=bundle.getString(MLAsrCaptureConstants.ASR_RESULT);
                            // 识别得到的文本信息处理。
                        }
                        if (text != null && !"".equals(text)&&!rd_report.isChecked()) {  //在查报表界面不加前缀smmp
                            et_barcode.setText("SMMP"+text);
                        }
                    }
                    break;
                // 返回值为MLAsrCaptureConstants.ASR_FAILURE表示识别失败。
                case MLAsrCaptureConstants.ASR_FAILURE:
                    // 识别失败处理。
                    if(data != null) {
                        Bundle bundle = data.getExtras();
                        // 判断是否包含错误码。
                        if(bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_CODE)) {
                            int errorCode = bundle.getInt(MLAsrCaptureConstants.ASR_ERROR_CODE);
                            // 对错误码进行处理。
                            sendMessage(MSG,errorCode);
                            if(errorCode==11203){
                                Toast.makeText(fifoActivity.this,"本语音功能只能用于华为手机！",Toast.LENGTH_LONG).show();
                                btn_speechRec.setVisibility(View.GONE);
                            }
                        }
                        // 判断是否包含错误信息。
                        if(bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_MESSAGE)){
                            String errorMsg = bundle.getString(MLAsrCaptureConstants.ASR_ERROR_MESSAGE);
                            // 对错误信息进行处理。
                            String temp=et_barcode.getText().toString();
                            sendMessage(MSG,temp+" 错误: "+errorMsg+" ! ");
                        }
                        //判断是否包含子错误码。
                        if(bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_SUB_ERROR_CODE)) {
                            int subErrorCode = bundle.getInt(MLAsrCaptureConstants.ASR_SUB_ERROR_CODE);
                            // 对子错误码进行处理。
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    class issueThread extends Thread{
        @Override
        public void run(){
            //////每次变回发货，需清零canlist和cannotlist   ok
            //////要把货物发到一个很怪的assy库位，用以 区别  alt+34147   ok
            //////每发货成功一下，删去一个canlist记录  ok
            //////否则如果barcode不在canlist中(canlist非空)，但与canlist的物料号相同， tvinfo栏变色，提示fifo不成功
            /////    如果barcode不在canlist中，物料号也不相同，则清空txtpartno,canlist和cannotlist启动thread，全面查询
            try{
                Part_FIFO_Data scandata=new Part_FIFO_Data(barcode,"","","","");
                //如果Barcode在canlist中，直接发货
                if(canlist.contains(scandata)){
                    FIFO_issue(barcode);
                    return;    //终止运行
                }else if(cannotlist.contains(scandata)){
                    //此时查询 fifo报表
                    String txtPartNo=cannotlist.get(0).part_no;
                    //先清理canList, cannotList
                    clear_list_data_and_UI_display();
                    sendMessage(MSG,"2,读取FIFO数据中......");
                    get_fifo_list(txtPartNo);   //此时scannotlist中就保有现存的Part No
                }else if(alllist.contains(scandata)){     //即不在canlist,也不在cannotlist，但在alllist中的
                    sendMessage(MSG,"你已发货过"+barcode);
                    sendMessage(alartColor,"");
                    return;   //终止运行
                }else {  //如果Barcode不在以上所有的list中，则需全面检查
                    sendMessage(MSG, "1,正在查条码......" + barcode);
                    String url = pre_url + "/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=";
                    Map<String, String> barcodeInfo = Utils.show_container_info(cookies, url, barcode);
                    //显示查询结结果
                    String txtPartNo = barcodeInfo.get("txtPartNo");
                    sendMessage(showBarcode_info, txtPartNo + "\n激活:" + barcodeInfo.get("txtActive") + "  状态:" + barcodeInfo.get("curStatus")+ "  库位:" + barcodeInfo.get("txtLocation"));

                    //先清理canList, cannotList
                    clear_list_data_and_UI_display();
                    sendMessage(MSG, "2,读取FIFO数据中......");
                    if(txtPartNo.length()>1){
                        get_fifo_list(txtPartNo);
                    }
                }
                {
                    //前面更新fifo list后，再次试试能否发货
                    if(canlist.contains(scandata)){
                        FIFO_issue(barcode);  // 函数中会刷新显示及颜色
                    }else{
                        sendMessage(refresh_FIFOlist_on_UI,"");
                        sendMessage(MSG,barcode+" 不符合 FIFO 发货标准！");
                        sendMessage(alartColor,"");
                    }
                }
            }catch(java.lang.NullPointerException e){
                //显示出错信息
                sendMessage(MSG,"查询结果为空，请检查输入数据！");
                sendMessage(alartColor,"");
            }
            catch (Exception e) {
                //显示出错信息
                sendMessage(MSG,e.getMessage());
                sendMessage(alartColor,"");
                e.printStackTrace();
            }finally{
                issueLock=0;   //开锁
                sendMessage(enableRadioGroup,"");
//                1、finally中的代码总会被执行。
//                2、当try、catch中有return时，也会执行finally。return的时候，要注意返回值的类型，是否受到finally中代码的影响。
//                3、finally中有return时，会直接在finally中退出，导致try、catch中的return失效
            }
        }
    }

    protected void get_fifo_list(String txtPartNo) throws Exception{
        //读取FIFO数据中......
        String url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(10617)";
        clear_list_data_and_UI_display(); //清空数据及显示，因后边的查询可能会出错
        alllist=get_fifo_report(cookies,url,txtPartNo);
        split_fifo_report(alllist); //canlist和cannotlist会被充值，如Size为0，则canlist与cannotlist会被清空
    }

    void FIFO_issue(String barcode) throws Exception{
        System.out.println("--现在FIFO发货 barcode"+barcode);
        sendMessage(MSG,"--现在FIFO发货中..."+barcode);
        //以下变化组件发货库存, 以此标记是否此程序的操作
        String locationTemp="ASsy1_";
        if(movedCount%2==0){
            locationTemp="AsSy1";
        }
        HashMap<String,String> move_result=Utils.move_container(cookies,pre_url,locationTemp,barcode);  //移到 Assy1_
        if(move_result!=null){  //分析move container 返回的结果
            if(move_result.get("IsValid")=="true"){
                System.out.println("从canList中移除"+barcode);
                //每发货成功一下，删去一个canlist记录
                canlist.remove(new Part_FIFO_Data(barcode,"","","",""));  //修改放在开头，以免引起显示时的 concurrent modify报错
                //canlist.removeIf(s->s.serial.equals(barcode));   //java 1.8用法
                System.out.println(barcode+"发料成功。");
                sendMessage(MOVED,barcode+"发料成功。\n "+move_result.get("Message").trim());
                ////变白色
                sendMessage(normalColor,"");vibrate();
                sendMessage(refresh_FIFOlist_on_UI,"");  //刷新fifo list
            }else{
                sendMessage(MSG,barcode+"发料不成功！\n "+move_result.get("Message").trim());
                //变红色
                sendMessage(alartColor,"");
            }
        }else{ //如 move container 返回数据为 null
            sendMessage(MSG,barcode+"发料不成功！ 请检查原因！");
            /////变红色
            sendMessage(alartColor,"");
        }
    }

    void container_move(String barcode,String location) throws Exception{
        System.out.println("--现在移库 barcode"+barcode);
        sendMessage(MSG,"--现在移库中..."+barcode);
        HashMap<String,String> move_result=Utils.move_container(cookies,pre_url,location,barcode);
        if(move_result!=null){  //分析move container 返回的结果
            if(move_result.get("IsValid")=="true"){
                //每移库成功一下，删去一个move task list记录
                System.out.println(barcode+"移库成功。");
                sendMessage(MOVED,barcode+"移库成功。\n "+move_result.get("Message").trim());
                ////变白色
                sendMessage(normalColor,"");
                //sendMessage(refresh_FIFOlist_on_UI,"");  //刷新move task list
            }else{
                sendMessage(MSG,barcode+"移库不成功！\n "+move_result.get("Message"));
                //变红色
                sendMessage(alartColor,"");
            }
        }else{ //如 move container 返回数据为 null
            sendMessage(MSG,barcode+"移库不成功！ 请检查原因！");
            /////变红色
            sendMessage(alartColor,"");
        }
    }

    void clear_list_data_and_UI_display(){
        alllist.clear();canlist.clear();cannotlist.clear(); //初始化 数据
        sendMessage(refresh_FIFOlist_on_UI,"");
    }

    public ArrayList<Part_FIFO_Data> get_fifo_report(HashMap<String,String> cookies,String url,String part_no) throws Exception{
        Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
        //////提交Post的数据
        String strKeyHandle="172932/\\Part_No[]172933/\\Building_Key[]172934/\\Location[]172935/\\Container_Status[]172936/\\Shelf_Life_Type[]172937/\\Shelf_Life_Unit[]172938/\\Product_Type[]172939/\\Operation_Key[]172940/\\Customer_No[]172942/\\Department_Nos[]172943/\\Part_Types[]172944/\\Planner[]172945/\\Job_Key[]172960/\\Open_Release_Period[]172994/\\Show[]172995/\\Days_until_expiration";
        data.put("__EVENTTARGET","Screen"); data.put("__EVENTARGUMENT","Search");
        data.put("__LASTFOCUS","");data.put("__VIEWSTATE","/wEPDwUJOTg5NDMxNjIwZGSK//YD8K8d6i3pHog8e0fH85JbPQ==");
        data.put("__VIEWSTATEGENERATOR","2811E9B3");data.put("hdnScreenTitle","Shelf Life Report");
        data.put("hdnFilterElementsKeyHandle",strKeyHandle);data.put("ScreenParameters","");
        data.put("RequestKey","1");
        data.put("Layout1$el_172932",part_no);data.put("Layout1$el_172932_hf",part_no);data.put("Layout1$el_172932_hf_last_valid",part_no);
        data.put("Layout1$el_172933","5824");  //means building: SMMP
        data.put("Layout1$el_172935","OK");
        data.put("Layout1$el_172994","-1");
        data.put("panel_row_count_3","0");
        try{
            Connection.Response res=Utils.request_post(url,cookies,data);
            Document doc=res.parse();
            /////////这里解析结果
            //System.out.println(doc.html());
            Element element_table=doc.getElementById("GRID_PANEL_3_28");
            Elements table_rows=element_table.getElementsByTag("tbody").first().getElementsByTag("tr");
            //System.out.println("222222__________\n"+element_table.outerHtml());

            //刷新一下part_no
            //part_no=table_rows.first().getElementsByTag("td").get(3).getElementsByTag("span").first().html();
            ArrayList<Part_FIFO_Data> allList=new ArrayList<Part_FIFO_Data>();
            for (Element row : table_rows) {
                Elements columns = row.getElementsByTag("td");
                part_no=columns.get(0).getElementsByTag("span").first().html();
                String serial=columns.get(2).getElementsByTag("span").first().html();
                String QTY=columns.get(4).getElementsByTag("span").first().html();
                String location=columns.get(5).getElementsByTag("span").first().html();
                String strdate=columns.get(7).getElementsByTag("span").first().html();
                allList.add(new Part_FIFO_Data(serial,part_no,QTY,location,strdate));
            }
            return allList;
        }catch(NullPointerException e){
            throw new Exception("没有查到相关FIFO数据!");
        }catch(Exception e) {
            System.out.println("catch Exception at check_fifo.");
            e.printStackTrace();
            throw e;
        }
    }
    void split_fifo_report(ArrayList<Part_FIFO_Data> alllist){
        canlist.clear(); cannotlist.clear(); //清空
        if(alllist.size()>0){ //alllist不能为空
            //获取第一条记录
            Part_FIFO_Data first=alllist.get(0);
            String first_date=first.date;
            for(int i=0;i<alllist.size();i++){
                Part_FIFO_Data item=alllist.get(i);
                if(first_date.equals(item.date)){
                    canlist.add(item);
                }else{
                    cannotlist.add(item);
                }
            }
        }
        sendMessage(MSG,"2,已获取FIFO Report，请等待......");
        sendMessage(refresh_FIFOlist_on_UI,"");
    }
    //主线程处理消息
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.what==MSG){
                tv_info.setText(msg.obj.toString());
                tv_info.setVisibility(View.VISIBLE);
            }else if(msg.what== showBarcode_info){
                //tv_BarcodeInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);
                tv_BarcodeInfo.setText(barcode+"："+msg.obj.toString()+"\n");
            }else if(msg.what== refresh_FIFOlist_on_UI){
                //刷新显示 canlist,cannotlist  注：一旦 canlist和cannotlist有变化，需运行这个
                tv_canlist.setText("");tv_cannotlist.setText("");
                for(Part_FIFO_Data data:canlist){
                    String temp=tv_canlist.getText().toString();
                    tv_canlist.setText(temp+"\n"+data.toString());
                } //显示 cannotlist
                for(Part_FIFO_Data data:cannotlist){
                    String temp=tv_cannotlist.getText().toString();
                    tv_cannotlist.setText(temp+"\n"+data.toString());
                }
            }else if(msg.what==alartColor){
                tv_info.setBackgroundColor(Color.RED);
                et_barcode.setBackgroundColor(Color.RED);
                vibrate(500);
            }else if(msg.what==normalColor){
                tv_info.setBackgroundColor(Color.WHITE);
                et_barcode.setBackgroundColor(Color.WHITE);
            }else if(msg.what==MOVED){
                movedCount++;
                String temp=tv_movedlist.getText().toString();
                String message=msg.obj.toString();
                if(message.contains("发料")){
                    tv_movedlist.setText(movedCount+"-"+barcode+"发  "+temp);  //显示已移库的条码
                }else if(message.contains("移库")){
                    tv_movedlist.setText(movedCount+"-"+barcode+"移  "+temp);  //显示已移库的条码
                }
                tv_movedlist.setVisibility(View.VISIBLE);
                tv_info.setText(message);
                tv_info.setVisibility(View.VISIBLE);
                et_barcode.setText("");      //move成功后，去掉已成功的条码，等下一个
            }else if(msg.what==enableRadioGroup){
                enableRadioGroup(radiogroup);
            }
        }};

    //子线程向主线程发送消息
    private void sendMessage(int what,Object obj){
        Message message1 = Message.obtain();
        message1.what = what;  //1 means at newPage
        message1.obj = obj;
        mMainHandler.sendMessage(message1);
    }

    public void disableRadioGroup(RadioGroup testRadioGroup) {
        for (int i = 0; i < testRadioGroup.getChildCount(); i++) {
            testRadioGroup.getChildAt(i).setEnabled(false);
        }
        et_barcode.setEnabled(false);    //工作时，锁定输入
    }
    public void enableRadioGroup(RadioGroup testRadioGroup) {
        for (int i = 0; i < testRadioGroup.getChildCount(); i++) {
            testRadioGroup.getChildAt(i).setEnabled(true);
        }
        et_barcode.setEnabled(true);    //工作完，放开输入
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        super.onDestroy();
    }

    private void initTTS() {
        //实例化自带语音对象
        textToSpeech = new TextToSpeech(fifoActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == textToSpeech.SUCCESS) {

                    textToSpeech.setPitch(1.0f);//方法用来控制音调
                    textToSpeech.setSpeechRate(1.2f);//用来控制语速

                    //判断是否支持下面两种语言
                    //int result1 = textToSpeech.setLanguage(Locale.US);
                    int result1 = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);

                    if (result1 == TextToSpeech.LANG_MISSING_DATA || result1 == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(fifoActivity.this, "语音包丢失或语音不支持", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(fifoActivity.this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void say(String data) {
        // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
        textToSpeech.setPitch(1.0f);
        // 设置语速
        textToSpeech.setSpeechRate(1.2f);
        data=data.replace(""," ");   //加分隔符，以便读单数字
        textToSpeech.speak(data,//输入中文，若不支持的设备则不会读出来
                TextToSpeech.QUEUE_FLUSH, null,null);
    }

    //自定义数据类型，用于保存记录
    public static class Part_FIFO_Data{
        public String serial;
        public String part_no;
        public String QTY;
        public String location;
        public String date;
        public Part_FIFO_Data(String serial,String part_no,String QTY,String location,String date) {
            this.serial = serial;
            this.part_no= part_no;
            this.QTY = QTY;
            this.location = location;
            this.date = date;
        }
        @Override
//        public boolean equals(Object obj){
//            if(this == obj)
//                return true;
//            if(obj == null)
//                return false;
//            if(!(obj instanceof Part_FIFO_Data))
//                return false;
//            Part_FIFO_Data other = (Part_FIFO_Data)obj;
//            if(this.serial == null){
//                if(other.serial !=null)
//                    return false;
//            }else if(this.serial.equals(other.serial))
//                return true;
//            return false;
//        }
        public boolean equals(Object obj){
            if(this == obj)   //地址相同
                return true;
            if(obj == null)
                return false;
            if(obj instanceof Part_FIFO_Data) {  //不分大小写的比较
                return ((Part_FIFO_Data) obj).serial.toUpperCase().equals(this.serial.toUpperCase());
                //return ((Part_FIFO_Data) obj).serial.equals(this.serial);
            }
            return false;
        }

        public String toString(){
            if(this.serial==null)
                return "no data";
            //String date=Utils.getMonthTime(this.date);
            return String.format("%s日期:%s数量:%s %s",this.serial,this.date,this.QTY,location);
        }
    }

    //震动
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    //震动
    private void vibrate(int time) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    public static void main(String[] args){
        ArrayList<Part_FIFO_Data> array=new ArrayList<>();
        Part_FIFO_Data data1=new Part_FIFO_Data("smmp123456","1","","","");
        Part_FIFO_Data data2=new Part_FIFO_Data("smmP123456","2","","","");
        Part_FIFO_Data data3=new Part_FIFO_Data("smmp123457","1","","","");
        array.add(data1);
        array.add(data2);
        array.add(data3);

        System.out.println(array);
        System.out.println(array.remove(new Part_FIFO_Data("smmP123456","2","","","")));
        System.out.println(array);
        System.out.println(array.remove(new Part_FIFO_Data("smmP123456","2","","","")));
        System.out.println(array);
    }
}