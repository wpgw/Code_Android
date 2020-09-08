package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
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
    final int MSG=1,showBarcodeInfo=2,showFIFOreport=3,STOP=5;
    //init Views
    TextView tv_info,tv_message,tv_canlist,tv_cannotlist;
    EditText et_barcode,et_location;
    Button btn_confirm;
    ImageButton btn_scan;
    Button btn_move,btn_issue;
    RadioGroup radiogroup;

    String barcode;   //用于存当前处理的条码号，传给thread
    String part_no_inList;  //记录canlist中的物料号，用于扫描后判断当前canlist是否可用
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
                    barcode=et_barcode.getText().toString();
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
            //////如果Barcode在canlist中，直接发货
            //////否则如果barcode与canlist的物料号相同，barcode栏变色，提示fifo不成功
            /////    如物料号也不相同，则清空txtpartno,canlist和cannotlist启动thread
            try {
                sendMessage(MSG,"1,正在查条码......"+barcode);
                String url=pre_url+"/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=";
                Map<String,String> barcodeInfo=Utils.show_container_info(cookies,url,barcode);
                //显示查询结结果
                String txtPartNo=barcodeInfo.get("txtPartNo");
                sendMessage(showBarcodeInfo,txtPartNo);

                sendMessage(MSG,"2,读取FIFO数据中......");
                url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(10617)";
                ArrayList<Part_FIFO_Data> allList=get_fifo_report(cookies,url,txtPartNo);
                //////如果Size为0，则可能 无FIFO库存，可能物料 fifo没有设置，需处理
                System.out.println("size:"+ allList.size());
                System.out.println(allList);

                canlist.addAll(allList);  ////////////////////////要拆分allList
                sendMessage(showFIFOreport,"");
                sendMessage(MSG,"2,读取FIFO数据完成");
                /////应把结果分类为 canList and cannotList
                /////如果 barcode在canList中，直接操作移库
                /////否则 barcode栏，变色，sendMessage说不能移库，需扫canlist中的barcode
            } catch (Exception e) {
                //要显示出错信息
                //要清理txtPartNo,canList, cannotList   ???如果只是没有发料，则不必清理
                e.printStackTrace();
            }
        }
    }

    //主线程处理消息
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.what==MSG){
                tv_info.setText(msg.obj.toString());
            }else if(msg.what==showBarcodeInfo){
                tv_info.setText("当前料号："+msg.obj.toString());
            }else if(msg.what==showFIFOreport){
                for(Part_FIFO_Data data:canlist){
                    String temp=tv_canlist.getText().toString();
                    tv_canlist.setText(temp+"\n"+data.toString());
                }
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
        try{
            Connection.Response res=Utils.request_post(url,cookies,data);
            Document doc=res.parse();
            /////////这里解析结果
            Element element_table=doc.getElementById("GRID_PANEL_3_28");
            Elements table_rows=element_table.getElementsByTag("tbody").first().getElementsByTag("tr");
            //System.out.println("222222__________\n"+element_table.outerHtml());

            ArrayList<Part_FIFO_Data> allList=new ArrayList<Part_FIFO_Data>();
            for (Element row : table_rows) {
                Elements columns = row.getElementsByTag("td");
                String serial=columns.get(2).getElementsByTag("span").first().html();
                String QTY=columns.get(4).getElementsByTag("span").first().html();
                String location=columns.get(5).getElementsByTag("span").first().html();
                String data1=columns.get(7).getElementsByTag("span").first().html();
                allList.add(new Part_FIFO_Data(serial,QTY,location,data1));
            }
            return allList;
        }catch(Exception e) {
            System.out.println("catch Exception at check_fifo.");
            e.printStackTrace();
            throw e;
        }
    }

    //此程序不成功！！！！！！！！！ 没作用，每次运行，只能修改最先一条记录  奇怪！！！！！！
    //此程序 批量 激活 相关part no的shelf life功能。
    //part no放在以下的part_map数据表中
    //此为一次性程序
    public void set_part_shelf_life(HashMap<String,String> cookies){
        String url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(15562)ScreenParameters(Do%3dUpdate%7cPart_Key%3d5037897%7cPart_No%3d5-100-001-711%7cImage%3d..%2fimages%2fblankbar.gif%7cFrom_Part_Menu%3dTrue)";
        Map<String,String> part_map =new LinkedHashMap<>();   //这个保证顺序
        //数据源 要修改的Part列在这里
        {
            //part_map.put("5-073-001-008","4315829");
            //part_map.put("5-100-001-700","5030786");
            //part_map.put("5-100-001-707","5030793");
            //part_map.put("5-100-001-708","5030794");
            //part_map.put("5-100-001-710","5030796");
            //part_map.put("5-100-001-711","5037897");
            //part_map.put("5-100-001-712","5040844");
            //part_map.put("5-100-001-812","4478365");
            //part_map.put("5-100-001-813","4478366");
            //part_map.put("5-100-001-814","4478367");
            //part_map.put("5-100-001-816","4478369");
            //part_map.put("5-100-001-817","4478370");
            part_map.put("5-100-001-818","4478371");
            part_map.put("5-100-001-819","4478372");
            part_map.put("5-100-001-820","4478373");
            part_map.put("5-100-001-822","4478375");
            part_map.put("5-100-001-823","4478376");
            part_map.put("5-100-001-824","4478377");
            part_map.put("5-100-001-828","4478381");
            part_map.put("5-100-001-829","4478382"); //
            part_map.put("5-100-001-830","4478383");
            part_map.put("5-100-001-839","4499984");
            part_map.put("5-100-001-840","4499985");
            part_map.put("5-100-001-841","4499986");
            part_map.put("5-100-001-842","4499987");
            part_map.put("5-100-001-843","4499988");
            part_map.put("5-100-001-844","4499989");
            part_map.put("5-100-001-845","4499990");
            part_map.put("5-100-001-846","4499991");
            part_map.put("5-100-001-847","4499992");
            part_map.put("5-100-001-848","4499993");
            part_map.put("5-100-001-849","4499994");
            part_map.put("5-100-001-850","4499995");
            part_map.put("5-100-001-851","4499996");
            part_map.put("5-100-001-852","4499997");
            part_map.put("5-100-001-853","4499998");
            part_map.put("5-100-001-854","4499999");
            part_map.put("5-100-001-855","4500000");
            part_map.put("5-100-001-856","4500001");
            part_map.put("5-100-001-857","4500002");
            part_map.put("5-100-001-858","4500003");
            part_map.put("5-100-001-859","4500004");
            part_map.put("5-100-001-861","4500006");
            part_map.put("5-100-001-862","4500007");
            part_map.put("5-100-001-863","4500008");
            part_map.put("5-100-001-864","4500009");
            part_map.put("5-100-001-865","4500010");
            part_map.put("5-100-001-866","4500011");
            part_map.put("5-100-001-867","4500012");
            part_map.put("5-100-001-868","4500013");
            part_map.put("5-100-001-869","4500014");
            part_map.put("5-100-001-870","4500015");
            part_map.put("5-100-001-871","4500016");
            part_map.put("5-100-001-872","4500017");
            part_map.put("5-100-001-877","4500022");
            part_map.put("5-100-001-878","4500023");
            part_map.put("5-100-001-880","4500025");
            part_map.put("5-100-001-882","4500027");
            part_map.put("5-100-001-883","4500028");
            part_map.put("5-100-001-884","4500029");
            part_map.put("5-100-001-885","4500030");
            part_map.put("5-100-001-886","4500031");
            part_map.put("5-100-001-887","4549284");
            part_map.put("5-100-001-888","4549286");
            part_map.put("5-100-001-889","4552703");
            part_map.put("5-100-001-890","4552704");
            part_map.put("5-100-001-893","4563338");
            part_map.put("5-100-001-896","4563341");
            part_map.put("5-100-001-902","4565696");
            part_map.put("5-100-001-903","4568872");
            part_map.put("5-100-001-904","4572330");
            part_map.put("5-100-001-905","4585867");
            part_map.put("5-100-001-906","4585868");
            part_map.put("5-100-001-907","4585869");
            part_map.put("5-100-001-908","4585870");
            part_map.put("5-100-001-909","4585871");
            part_map.put("5-100-001-910","4585872");
            part_map.put("5-100-001-911","4585873");
            part_map.put("5-100-001-912","4585874");
            part_map.put("5-100-001-913","4585875");
            part_map.put("5-100-001-914","4585876");
            part_map.put("5-100-001-915","4585877");
            part_map.put("5-100-001-917","4605192");
            part_map.put("5-100-001-918","4605193");
            part_map.put("5-100-001-920","4605194");
            part_map.put("5-100-001-922","4736537");
            part_map.put("5-100-001-923","4736538");
            part_map.put("5-100-001-924","4736539");
            part_map.put("5-100-001-925","4736540");
            part_map.put("5-100-001-926","4736543");
            part_map.put("5-100-001-927","4736553");
            part_map.put("5-100-001-928","4737167");
            part_map.put("5-100-001-929","4737168");
            part_map.put("5-100-001-930","4737169");
            part_map.put("5-100-001-931","4740459");
            part_map.put("5-100-001-932","4744880");
            part_map.put("5-100-001-933","4744881");
            part_map.put("5-100-001-934","4744882");
            part_map.put("5-100-001-935","4744883");
            part_map.put("5-100-001-936","4744884");
            part_map.put("5-100-001-948","4808815");
            part_map.put("5-100-001-950","4809306");
            part_map.put("5-100-001-951","4809307");
            part_map.put("5-100-001-956","4829019");
            part_map.put("5-100-001-957","4829020");
            part_map.put("5-100-001-958","4829021");
            part_map.put("5-100-001-959","4829022");
            part_map.put("5-100-001-960","4829024");
            part_map.put("5-100-001-961","4829025");
            part_map.put("5-100-001-962","4829026");
            part_map.put("5-100-001-963","4829027");
            part_map.put("5-100-001-964","4829028");
            part_map.put("5-100-001-965","4829029");
            part_map.put("5-100-001-966","4829030");
            part_map.put("5-100-001-967","4829031");
            part_map.put("5-100-001-971","4853094");
            part_map.put("5-100-001-972","4853095");
            part_map.put("5-100-001-973","4853096");
            part_map.put("5-100-001-974","4859642");
            part_map.put("5-100-001-975","5201150");
            part_map.put("5-100-001-982","4878065");
            part_map.put("5-100-001-984","4878066");
            part_map.put("5-100-001-985","4878068");
            part_map.put("5-100-001-986","4878067");
            part_map.put("5-100-001-987","4961129");
            part_map.put("5-100-001-989","5025319");
            part_map.put("5-100-001-990","5034578");
            part_map.put("5-101-001-714","5082339");
            part_map.put("5-101-001-715","5082340");
            part_map.put("5-101-001-716","5082341");
            part_map.put("5-101-001-717","5082342");
            part_map.put("5-101-001-723","5104687");
            part_map.put("5-101-001-724","5104688");
            part_map.put("5-180-001-979","5201741");
            part_map.put("5-180-019-976","5201444");
            part_map.put("5-180-019-977","5201445");
            part_map.put("5-180-019-978","5201446");
            part_map.put("5-190-001-622","4489761");
            part_map.put("5-190-001-623","4489785");
            part_map.put("5-190-001-624","4489793");
            part_map.put("5-300-001-718","5082344");
            part_map.put("5-300-001-719","5082345");
            part_map.put("5-300-001-720","5103134");
            part_map.put("5-300-001-728","5106465");
            part_map.put("5-300-001-730","5114309");
            part_map.put("5-300-001-731","5114310");
            part_map.put("5-300-001-732","5114311");
            part_map.put("5-300-001-733","5114312");
            part_map.put("5-300-001-734","5114313");
            part_map.put("5-300-001-735","5114314");
            part_map.put("5-300-001-736","5114315");
            part_map.put("5-320-001-740","5131063");
        }
        int i=0;
        for( Map.Entry<String,String> item:part_map.entrySet()){
            i++;
            String part_key=item.getValue(); String part_no=item.getKey();
            url=url.replace("5037897",part_key);
            url=url.replace("5-100-001-711",part_no);
            Map<String,String> data=new LinkedHashMap<>();
            data.put("__EVENTTARGET","Screen");data.put("__EVENTARGUMENT","Update");
            data.put("__VIEWSTATE","/wEPDwUJOTg5NDMxNjIwZGSK//YD8K8d6i3pHog8e0fH85JbPQ==");data.put("__VIEWSTATEGENERATOR","2811E9B3");
            data.put("hdnScreenTitle","Part Shelf Life");data.put("hdnFilterElementsKeyHandle","259995/\\Shelf_Life[]259996/\\Unit[]259997/\\Note[]259999/\\Shelf_Life_Type_Key[]260007/\\Supplier_Shelf_Life[]260008/\\Supplier_Shelf_Life_Unit");
            data.put("ScreenParameters","Part_Key="+part_key+"|Part_Shelf_Life_Key=|");data.put("RequestKey","1");
            data.put("Layout1$el_259995","365");data.put("Layout1$el_259996","day");
            data.put("Layout1$el_259996_txt_val","");data.put("Layout1$el_259999","355");
            data.put("panel_row_count_6","0");data.put("panel_row_count_7","0");
            try {
                System.out.println(i+"--设置FIFO： "+part_key+"  "+part_no);
                Connection.Response res=Utils.request_post(url,cookies,data);
                Thread.sleep(1000);
                data.put("__EVENTARGUMENT","Back");
                res=Utils.request_post(url,cookies,data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }




    }

    //自定义数据类型，用于保存记录
    public static class Part_FIFO_Data{
        public String serial;
        public String QTY;
        public String location;
        public String date;
        public Part_FIFO_Data(String serial,String QTY,String location,String date) {
            this.serial = serial;
            this.QTY = QTY;
            this.location = location;
            this.date = date;
        }
        @Override
        public boolean equals(Object obj){
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(!(obj instanceof Part_FIFO_Data))
                return false;
            Part_FIFO_Data other = (Part_FIFO_Data)obj;
            if(this.serial == null){
                if(other.serial !=null)
                    return false;
            }else if(this.serial.equals(other.serial))
                return true;
            return false;
        }
        public String toString(){
            if(this.serial==null)
                return "no data";
            //String date=Utils.getMonthTime(this.date);
            return String.format("%s %s %sKg %s",this.serial,this.date,this.QTY,location);
        }
    }

}