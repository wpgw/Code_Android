package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class fifoActivity extends AppCompatActivity {
    HashMap<String,String> cookies=new HashMap<>();
    String pre_url,Session_Key;
    final int MSG=1,showBarcode_PartNo=2,refreshFIFOlist=3,alartColor=4,normalColor=5;
    //init Views
    TextView tv_info,tv_message,tv_canlist,tv_cannotlist;
    EditText et_barcode,et_location;
    Button btn_confirm;
    ImageButton btn_scan;
    Button btn_move,btn_issue;
    RadioGroup radiogroup;

    String barcode;   //用于存当前处理的条码号，传给thread
    ArrayList<Part_FIFO_Data> alllist=new ArrayList<Part_FIFO_Data>();
    ArrayList<Part_FIFO_Data> canlist=new ArrayList<Part_FIFO_Data>();
    ArrayList<Part_FIFO_Data> cannotlist=new ArrayList<Part_FIFO_Data>();  //fifo允许的物料及不允许的物料

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
        tv_canlist.setMovementMethod(ScrollingMovementMethod.getInstance());tv_cannotlist.setMovementMethod(ScrollingMovementMethod.getInstance());

        //开始时不显示有关控件
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
                clear_list_and_display();  //每次变化，都初始化fifo数据与显示
                //containerActive="否";  // 此时不能作任何操作 onhold/scrap
                if (checkedId==R.id.rd_move){
                    //移库
                    tv_canlist.setVisibility(View.GONE);
                    tv_cannotlist.setVisibility(View.GONE);
                    et_location.setText("");
                    et_location.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                    //et_location.setEnabled(true);
                    //tv_info.setPadding(dip2px(5),0,0,0);
                }else if(checkedId==R.id.rd_fifo_issue){
                    //发货
                    tv_canlist.setVisibility(View.VISIBLE);
                    tv_cannotlist.setVisibility(View.VISIBLE);
                    et_location.setText("Assy");
                    et_location.setEnabled(false);
                    //tv_info.setPadding(dip2px(5),dip2px(20),0,0);
                }
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId()==R.id.btn_confirm){
                    //refine barcode
                    barcode=et_barcode.getText().toString().toUpperCase();
                    barcode=Utils.refine_label(barcode);
                    et_barcode.setText(barcode);    //Textbox display the refined barcode

                    try {
                        ChildThread thread=new ChildThread();
                        thread.start();
//                        tv_info.setText("正在查条码......"+barcode);
//                        String url=pre_url+"/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=";
//                        Map<String,String> info=Utils.show_container_info(cookies,url,barcode);
//                        System.out.println("条码如下：");
//                        System.out.println(info.get("barcode")+"  "+info.get("txtPartNo")+"   "+info.get("txtQTY"));
//                        String txtPartNo=info.get("txtPartNo");
//                        url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(10617)";
//                        Utils.get_fifo_report(cookies,url,txtPartNo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //check_container_info_fifo(barcode);
                }
            }
        });
    }

    class ChildThread extends Thread{
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
                }else{  //如果Barcode不在canlist中，则需检查
                    sendMessage(MSG,"1,正在查条码......"+barcode);
                    String url=pre_url+"/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=";
                    Map<String,String> barcodeInfo=Utils.show_container_info(cookies,url,barcode);
                    //显示查询结结果
                    String txtPartNo=barcodeInfo.get("txtPartNo");
                    sendMessage(showBarcode_PartNo,txtPartNo+" "+barcodeInfo.get("txtLocation")+"  Active:"+barcodeInfo.get("txtActive"));

                    sendMessage(MSG,"2,读取FIFO数据中......");
                    url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(10617)";
                    clear_list_and_display(); //清空数据及显示，因后边的查询可能会出错
                    alllist=get_fifo_report(cookies,url,txtPartNo);
                    split_fifo_report(alllist); //canlist和cannotlist会被充值，如Size为0，则canlist与cannotlist会被清空
                    if(canlist.contains(scandata)){
                        FIFO_issue(barcode);  // 函数中会刷新显示及颜色
                    }else{
                        sendMessage(refreshFIFOlist,"");
                        sendMessage(MSG,barcode+" 不符合 FIFO 发货标准！");
                        sendMessage(alartColor,"");
                    }
                }
            } catch (Exception e) {
                //要清理canList, cannotList   ???如果只是 发料 时出错，则不必清理
                clear_list_and_display();
                //显示出错信息
                sendMessage(MSG,e.getMessage());
                sendMessage(alartColor,"");
                e.printStackTrace();
            }
        }
    }

    void FIFO_issue(String barcode) throws Exception{
        System.out.println("--FIFO发货 barcode"+barcode);
        HashMap<String,String> move_result=Utils.move_container(cookies,pre_url,"ASSY1",barcode);  //Assy卌
        if(move_result!=null){  //分析move container 返回的结果
            if(move_result.get("IsValid")=="true"){
                System.out.println(barcode+"发料成功。");
                sendMessage(MSG,barcode+"发料成功。\n "+move_result.get("Message"));
                System.out.println("从canList中移除"+barcode);
                canlist.remove(new Part_FIFO_Data(barcode,"","","",""));//每发货成功一下，删去一个canlist记录
                //canlist.removeIf(s->s.serial.equals(barcode));
                sendMessage(refreshFIFOlist,"");  //刷新
                ////变白色
                sendMessage(normalColor,"");
            }else{
                sendMessage(MSG,barcode+"发料不成功！\n "+move_result.get("Message"));
                /////变红色
                sendMessage(alartColor,"");
            }
        }else{ //如 move container 返回数据 null
            sendMessage(MSG,barcode+"发料不成功！ 请检查原因！");
            /////变红色
            sendMessage(alartColor,"");
        }
    }

    void clear_list_and_display(){
        alllist.clear();canlist.clear();cannotlist.clear(); //初始化 数据
        sendMessage(refreshFIFOlist,"");
    }

    //主线程处理消息
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.what==MSG){
                tv_info.setText(msg.obj.toString());
            }else if(msg.what==showBarcode_PartNo){
                et_location.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);
                et_location.setText(barcode+"："+msg.obj.toString()+"\n");
            }else if(msg.what==refreshFIFOlist){
                //显示 canlist
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
            }else if(msg.what==normalColor){
                tv_info.setBackgroundColor(Color.WHITE);
                et_barcode.setBackgroundColor(Color.WHITE);
            }
        }};

    //子线程向主线程发送消息
    private void sendMessage(int what,Object obj){
        Message message1 = Message.obtain();
        message1.what = what;  //1 means at newPage
        message1.obj = obj;
        mMainHandler.sendMessage(message1);
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

            ArrayList<Part_FIFO_Data> allList=new ArrayList<Part_FIFO_Data>();
            for (Element row : table_rows) {
                Elements columns = row.getElementsByTag("td");
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
        canlist.clear();  //清空
        canlist.clear();
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
            return String.format("%s 日期：%s 数量：%s %s",this.serial,this.date,this.QTY,location);
        }
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