
package com.philip.plex_qa;

import android.content.SyncStatusObserver;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;  // readline
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//以下import 只用于for method trustEveryone，for enable Proxy
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import static java.lang.Thread.*;

public class Plex_qa{
    public static String path="";   //may point to android /data/data app private path

    public Map<String,String> headers =new HashMap<>();
    public Map<String,String> cookies;
    String host,url_pre;
    static String result;  // this variable is for multi-thread method of loaded_container

    //构造函数
    public Plex_qa(String host) {      //构造函数
        this.host=host;this.url_pre="https://"+this.host+"/";

        headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");
        headers.put("Host",this.host);
        headers.put("Connection","keep-alive");
    }

    //获得Cookies
    public Map<String,String> get_cookies(String userID,String Password) throws Exception{
        Plex_login login=new Plex_login(host);
        try {
            this.cookies=login.login(userID, Password,"smmp");   //此cookies为本类成员所共用
            return this.cookies;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void trustEveryone() {  //only used for Charles analysis, to avoid the shakehand error
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public Connection.Response request_get(String url) throws Exception{
        try {
            //trustEveryone();
            Connection con=Jsoup.connect(url);
            con.headers(this.headers);
            con.cookies(this.cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Method.GET).execute();

            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp"))
            //if(res.parse().title().contains("Login"))
            {
                throw new MyException("你空闲时间过长,需重新登陆了!");
            }

            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new MyException("你的密码过期了,请在电脑上更新密码!");
            }
            return res;

        }catch(SocketTimeoutException e){
            throw new MyException("Time Out!网络连接超时,请重试!");
        }catch(UnknownHostException e){
            throw new MyException("网络故障，找不到主机地址！");
        }catch(Exception e) {
            System.out.println("Catch Exception at request_get");
            //e.printStackTrace();
            throw e;
        }
    }

    public Connection.Response request_post(String url,Map<String,String> data) throws Exception{
        try {
            //trustEveryone();
            Connection con=Jsoup.connect(url);
            con.data(data);
            con.headers(this.headers);
            con.cookies(this.cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Method.POST).execute();
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")){
                throw new MyException("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new MyException("你的密码过期了,请在电脑上更新密码!");
            }
            return res;

        }catch(SocketTimeoutException e){
            throw new MyException("Time Out!网络连接超时,请重试!");
        }catch(UnknownHostException e){
            throw new MyException("网络故障，找不到主机地址！");
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<String> get_defect_list(String Session_Key) throws Exception{
        String path= Plex_qa.path +"/.defect_list.xml";
        File file=new File(path);

        try {
            Document doc= null;
            if (!file.exists()) {
                String url=url_pre + Session_Key + "/Quality/Defect_Code.asp";
                Connection.Response res=request_get(url);
                //不该每次到网上下清单
                doc = res.parse();
                //JXDocument jxd=JXDocument.create(doc);			//用JsoupXpath
                //List<Object> list=jxd.sel("//tr/td[contains(text(),'Core')]/following-sibling::td[5]/text()");  //顺数第5个

                //save to file with code UTF-8
                OutputStreamWriter out=new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                out.write(doc.html());
                out.close();
            }
            //get list from file
            doc=Jsoup.parse(file, "UTF-8");
            Elements elements=doc.select("tr").select("td:contains(core)");
            List<String> list=new ArrayList<>();
            for(Element item:elements) {
                list.add(item.lastElementSibling().previousElementSibling().text());  //取倒数第二个
            }
            return list;

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Map<String,String> get_scrap_reason_file(String Session_Key) throws Exception{
        String path= Plex_qa.path +"/.plex_scrap_reason.xml";
        File file =new File(path);
        //file.delete();
        try {
            if (!file.exists()) {  //if file not exist, then get list from url and save
                System.out.println("从web读scrap reason...");
                String url= url_pre + Session_Key + "/ssi/combobox.asp";  //采自 container scrap

                Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
                data.put("RequestID","0"); data.put("DatabaseName","Part");
                data.put("ProcedureName","Scrap_Reasons_Link_Picker_Get"); //数字与下边的intput parameters对应
                data.put("MaxReturn","200");          //数字与下边的output parameters对应
                data.put("ParameterCount","6");  data.put("FieldCount","2");
                data.put("Plexus_Customer_No INT","291348"); data.put("Scrap SMALLINT","");
                data.put("Reason VARCHAR(50)","");data.put("Include_Inactive SMALLINT","0");data.put("Operation_Key INT","0");
                data.put("Workcenter_Key INT","69430");data.put("Field0","Scrap_Reason");data.put("Field1","Scrap_Reason_Key");

                Connection.Response res=this.request_post(url, data);
                Document doc1=res.parse();

                //save to file with code UTF-8
                OutputStreamWriter out=new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                out.write(doc1.html());
                out.close();
            }

            //get list from file
            Document doc=Jsoup.parse(file, "UTF-8");
            Elements elements=doc.select("record");

            Map<String,String> map=new LinkedHashMap<>();
            for(Element item:elements) {
                String scrap_reason=item.select("field").first().text();
                String scrap_key=item.select("field").last().text();
                map.put(scrap_reason,scrap_key);
            }
            return map;

        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    public TreeMap<String,String> get_workcenter_list(String Session_Key) throws Exception{
        String path= Plex_qa.path +"/.workcenter_list.xml";  //path to save file
        File file=new File(path);
        try {
            Document doc= null;
            if (!file.exists()) {
                String url=url_pre + Session_Key + "/Part/Scrap_Container_Add_Form.asp";
                Connection.Response res=request_get(url);
                doc = res.parse();

                //save to file with code UTF-8
                OutputStreamWriter out=new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                out.write(doc.html());
                out.close();
            }
            //get list from file
            doc=Jsoup.parse(file, "UTF-8");
            Elements elements=doc.select("td").select("option");
            TreeMap<String,String> map=new TreeMap<>();

            //装入需排除的workcenter
            List<String> exclude_list =new ArrayList<String>();
            exclude_list.add("Gene");exclude_list.add("LEAK");//exclude_list.add("A-En");

            for(Element item:elements) {
                String temp=item.text();
                if(temp.length()>4 && !exclude_list.contains(item.text().substring(0, 4)) ) {  //长度大于4且是有效机器名
                    map.put(item.text(),item.attr("value") );  //map format { 机器名:机器id }
                }
            }
            //System.out.println(map);
            return map;
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Map<String,String> show_container_info(String Session_Key,String txtSerial_No) throws Exception{
        //如果txtSerial_No不存在，会返回什么结果? 需处理
        String url= url_pre + Session_Key + "/Modules/Inventory/InventoryTracking/ContainerForm.aspx?Do=Update&Serial_No=" + txtSerial_No;
        try {
            Connection.Response res=request_get(url);

            Document doc=res.parse();

            Elements el_input=doc.select("input");

            Map<String,String> map=new LinkedHashMap<>();
            map.put("barcode",txtSerial_No);
            map.put("txtPartNo", el_input.select("input[name=txtPartNo]").first().attr("value").trim());
            map.put("txtQTY", el_input.select("input[name=numQuantity]").first().attr("value"));
            map.put("txtLocation", el_input.select("input[name=txtLocation]").first().attr("value"));

            //获得并转换 chkActive
            String txtActive=el_input.select("input[name=chkActive]").first().attr("checked");
            txtActive=(txtActive.equals("checked"))?"true":"false";  //三元运算
            map.put("txtActive", txtActive);

            map.put("curStatus", doc.select("option[selected=selected]").first().text());
            map.put("txtNote",doc.select("textarea[id=txtNote]").first().text());

            //System.out.println(map);
            return map;

        }catch(Exception e) {
            System.out.println("catch Exception at show_container_info.");
            e.printStackTrace();
            throw e;
        }
    }

    public String get_container_workcenter(String Session_Key, String txtSerial_No) throws Exception{
        String url= url_pre + Session_Key + "/Ajax/AjaxPost.asp";  //采自 container scrap

        Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
        data.put("DatabaseName","Part"); data.put("ProcedureName","dbo.Container_Quantities_Get");
        data.put("ParameterCount","1"); //数字与下边的intput parameters对应
        data.put("FieldCount","1");          //数字与下边的output parameters对应
        data.put("MaxRows","1");  data.put("ExecutingPCN","0");
        data.put("ExecutingServerKey","0"); data.put("PCN","1");
        //input parameter
        data.put("Serial_No VARCHAR(25)",txtSerial_No);
        //output parameters
        data.put("Field0","Workcenter_Key");

        try {
            Connection.Response res=this.request_post(url, data);
            Document doc=res.parse();
            Element output=doc.select("field").first();

            return output.text();

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    //单线程 读取loaded containers
    public String get_loaded_all(String Session_Key) throws Exception{
        String result="";
        String name="";
        try{
            //遍历workcenter_list, 一个一个的查询其loaded containers
            for(TreeMap.Entry<String, String> item:get_workcenter_list(Session_Key).entrySet()){
                Element table=get_loaded_container(Session_Key,item.getValue());
                //add workcenter name at Table Head
                name=item.getKey();
                table.getElementsByTag("th").first().prepend("<br><p align=\"left\"><u>"+name+"</u></p>");
                System.out.println(table.outerHtml());

                result+=table.outerHtml();
            }
        }catch(Exception e) {//处理下边传来的 Exception
                result+=name+ " at get_loaded_all出错。\n"+e.getMessage();  //如果有错，清除exception. 记录下来，然后循环
        }finally {
            return result;
        }

    }

    //多线程 读取loaded containers
    public String get_loaded_multiThread(String Session_key) throws Exception{
        Plex_qa.result="";  //初始化
        Vector<Thread> threadVector=new Vector<Thread>();
        try {
            //遍历workcenter_list, 一个一个的查询其loaded containers
            for(TreeMap.Entry<String, String> item:get_workcenter_list(Session_key).entrySet()) {
                cls_get_loaded cls = new cls_get_loaded(Session_key);
                cls.workcenter_key=item.getValue();cls.workcenter_name=item.getKey();

                Thread thread = new Thread(cls);
                thread.start();
                threadVector.add(thread);
                Thread.sleep(100);
            }
            for (Thread thread: threadVector){  //主线程在子线程之后停止
                thread.join();
            }
            System.out.println("--------Main Thread End---------");

            return Plex_qa.result;
        }catch (Exception e) {
            System.out.println("get_loaded_multiTread Exception");
            //e.printStackTrace();
            throw e;
        }
    }

    //多线程 读取loaded containers的子线程定义
    public class cls_get_loaded implements Runnable{
        //public String result="";
        public String Session_Key;
        public String workcenter_key,workcenter_name;

        //构造函数
        public cls_get_loaded(String Session_Key){
            this.Session_Key=Session_Key;
        }

        public void run(){
            try{
                Element table=get_loaded_container(Session_Key,workcenter_key);
                //add workcenter name at Table Head
                table.getElementsByTag("th").first().prepend("<br><p align=\"left\"><u>"+workcenter_name+"</u></p>");
                synchronized ("") {Plex_qa.result+=table.outerHtml();}
                System.out.println("_________"+ workcenter_name +" got data_________");

            }catch(Exception e) {
                e.printStackTrace();
                //result+="<p>读 "+ workcenter_name + " 一次出错</p>";
                Element table=null;
                //如果出错，就再读一次数据
                try {
                    table=get_loaded_container(Session_Key,workcenter_key);
                    //add workcenter name at Table Head
                    table.getElementsByTag("th").first().prepend("<br><p align=\"left\"><u>"+workcenter_name+"</u></p>");
                    synchronized ("") {Plex_qa.result+=table.outerHtml();}
                    System.out.println("_________"+ workcenter_name +" tried again to got data_________");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("__________读 "+ workcenter_name + " 二次都出错了</p>");
                    result+="<p>读 "+ workcenter_name + " 二次都出错了</p>";
                }
            }
        }
    }

    public Element get_loaded_container(String Session_Key,String workcenter_Key) throws Exception{
        //String url= url_pre + Session_Key+"/Control_Panel/Control_Panel_Dispatch.asp?Workcenter_Key=65916&Workcenter_Code=PORSCHE+ROBOT+DEBURR&Serial=&Setup_Key=185498953&Job_Op_Key=178537785&Previous_Operation=0&Location=&Status=Production+%7B%E7%94%9F%E4%BA%A7%7D";
        String url= url_pre + Session_Key+"/Control_Panel/Control_Panel_Dispatch.asp?Workcenter_Key="+workcenter_Key;  //66615, 65916

        try {
            Connection.Response res=this.request_get(url);

                Document doc=res.parse();
                //其tbody含有有用的tr
                Element table=doc.getElementsByTag("table").last();
                return table;

        }catch(Exception e) {
            e.printStackTrace();
            System.out.println("get_loaded_container运行不成功 Exception。");
            throw e;
        }
    }

    public String get_inventory_adjustment(String Session_Key) throws Exception{
        String url= url_pre + Session_Key+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(1732)";

        Map<String,String> data=new LinkedHashMap<>();
        data.put("undefined__EVENTTARGET","");
        data.put("Layout1$el_148572","Custom");  //可能不要

        try {
            //第一次get
            Connection.Response res=this.request_get(url);

            Document doc=res.parse();
            Elements output=doc.select("input");
            for(Element e:output){
                //System.out.println(e.attr("name")+"  __  "+e.attr("value"));
                if(e.attr("value").length()>0){
                    data.put(e.attr("name"),e.attr("value"));
                }
            }
            data.put("Layout1$el_23432",getFirstDayOfMonth());//"3/1/2020"
            data.put("Layout1$el_23432_time","12:00 AM");
            data.put("Layout1$el_23433",getLastDayOfMonth());
            data.put("Layout1$el_23433_time","11:59 PM");
            //第二次post
            Connection.Response res2=this.request_post(url, data);
            Document doc2=res2.parse();
            //grid title
            String grid_title="<head><meta charset=\"utf-8\"><title>Inventory Adjustment</title><style type=\"text/css\">.GridBody{background-color:#dddddd;} th{background-color:#ccccaa;}</style></head>";
            grid_title+= "<h3>Inv Adj "+ getFirstDayOfMonth()+" to "+getLastDayOfMonth()+"</h3>";
            //change the grid data
            Element grid=doc2.getElementById("GRID_PANEL_3_28");
            Elements grid_trs=grid.getElementsByTag("tr");  //表的行集合
            for(Element tr:grid_trs){ //tr.child 指行中的列数据td
                tr.child(5).text(""); //tr.child(13).text("");//去掉 Unit Cost and  Part Group栏 column
                if(tr.child(2).text().length()==0){        //去掉没有修改数量的记录行 row
                    tr.remove();
                }
            }
            return grid_title+grid.outerHtml();

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String get_container_history(String Session_Key,String txtSerial_No) throws Exception{
        String path= Plex_qa.path +"/.history.html";
        File file=new File(path);
        try {
            Document doc= null;
            if (!file.exists()||file.exists()) {     //这里要改
                String url=url_pre + Session_Key + "/Part/Container_History2.asp?Serial_No="+ txtSerial_No;
                Connection.Response res=request_get(url);
                doc = res.parse();

                //save to file with code UTF-8
                OutputStreamWriter out=new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                out.write(doc.html());
                out.close();
            }
            //get html from file
            doc=Jsoup.parse(file, "UTF-8");

            //set css style for grid
            String head="<head><meta charset=\"utf-8\"><style type=\"text/css\">.AltColor{background-color:#cccccc;} th{background-color:#cccccc;}</style></head><body>";
            //get grid title
            Elements grid_title=doc.getElementsByClass("StandardForm");
            grid_title.attr("style","width:100%;");      //原始值 style="width:30%;"
            String html=head+grid_title.first().outerHtml()+"<br>\n";
            //change the grid data
            Element grid=doc.getElementsByClass("StandardGrid").first();  //表
            Elements grid_trs=grid.getElementsByTag("tr");  //表的行集合
            for(Element tr:grid_trs){
                tr.child(10).text("");tr.child(11).text(""); tr.child(16).text("");  //clear Net/Len/Type column

                tr.child(4).text(tr.child(12).text());  //Operation栏<<<--Qty
                tr.child(5).text(tr.child(13).text());  //Material栏<<<--Loaction
                tr.child(6).text(tr.child(14).text());  //Heat栏改成Status
                tr.child(7).text(tr.child(17).text()); //tracking No栏改成 Last Action
                tr.child(8).text(tr.child(19).text());  //Gross 栏改成 Loaded
                tr.child(9).text(tr.child(20).text());  //Tare栏改成Active
                tr.child(12).text("");tr.child(13).text("");tr.child(14).text("");// clear redundent column
                tr.child(17).text("");tr.child(19).text("");tr.child(20).text("");
            }

            //对Element grid加个tfoot, 与thead 内容一样
            Element thead=grid.getElementsByTag("thead").first();
            grid.append("<tfoot>"+thead.html()+"</tfoot>");

            html+=grid.outerHtml()+"</body>";  //.replace("Material","").replace("Heat","");
            return html;

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String refine_label(String txtSerial_NO) {
        String pattern="([a-zA-Z]{4}\\d{6,7}|\\d{6,7})";

        Pattern re=Pattern.compile(pattern);
        Matcher ma=re.matcher(txtSerial_NO);

        if (ma.find()) {
            String result=ma.group();
            if (result.length()<9) {
                result="smmp"+result;  //此时自动补上smmp
            }
            return result;
        }else {
            return "";
        }
    }

    //Call之前，一定要先 show_info, 获得当时的status, active, Note等
    public boolean change_status(String Session_Key,String Serial,String Status,String Reason,String Note){
        String url=url_pre + Session_Key + "/Rejection/Sort_Container_Modify.asp?Do=Update";

        Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
        data.put("txtSerial_NoRQD",Serial);data.put("rdoAction","1091");data.put("rdoQuantity","Full_Container");
        data.put("rdoResult","Change_Status");data.put("lstContainer_Status",Status);data.put("pkrDefect_Type",Reason);
        data.put("txaNote_TXA_200",Note);   // should pay attention to note if it can be ""

        try{
            Connection.Response res=this.request_post(url, data);
            Document doc=res.parse();

            //正则表达式
            String pattern="uccessfully";
            Pattern re=Pattern.compile(pattern);
            Matcher ma=re.matcher(doc.html());

            if (ma.find()) {
                print("成功修改 "+Serial+" 状态为:"+Status+"  "+Reason);
                return true;
            }else {
                print("修改"+Serial+"的状态失败！");
                return false;
            }
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //发现不能用scrap_full，Plex针对它的报表不正确
    //有效的条码才能报废: status_Valid_list=["ok", "hold", "warehouse receive status", "suspect", "defective","scrap"]  且 active
    public boolean scrap_container(String Session_Key,String Serial,String Scrap_reason_key,String workcenter_key,String qty) throws MyException{
        String url=url_pre+Session_Key+"/Part/Scrap_Container_Add_Modify.asp?Do=Add&From=&Shift_Key=&Production_No=&Rejection_Key=0&Convert_Part_Weight=0&Scrap_Mode=Quantity";
        String date=this.getdateAfter(0);
        String time=this.getTime();

        // 按数量与报废原因生成xml
        String ScrapXML=" <ScrapTable><Scrap><RowNo>T1</RowNo><Quantity><![CDATA[qty94782]]></Quantity><Reason><![CDATA[rea94782]]></Reason><Note></Note></Scrap></ScrapTable>";
        ScrapXML=ScrapXML.replace("qty94782", qty).replace("rea94782",Scrap_reason_key);

        Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
        data.put("txtSerial_NoRQD",Serial);data.put("lstWorkcenter_Key",workcenter_key);
        data.put("txtScrap_Date_DTE_RQD",date);data.put("txtScrap_Time_TME_RQD",time);
        data.put("ScrapXML", ScrapXML);
        try {
            Connection.Response res=this.request_post(url, data);

            Document doc=res.parse();
            Elements elements=doc.select("title");

            String title=elements.first().text();

            if (title.equals("Error")) {
                print("报废条码 "+Serial+" 不成功！");
                return false;
            }else {
                print("已报废条码 "+Serial+"，请检查是否成功！");
                return true;
            }
        }catch(MyException e){
            throw e;
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //获得几天后的日期 yyyy-MM-dd
    public String getdateAfter(int days) {
        String tdate;
        Calendar cal=Calendar.getInstance();
        SimpleDateFormat datetemple=new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.DATE, days);   //Instruction at site: www.runoob.com/java/java-date-time.html
        tdate=datetemple.format(cal.getTime());
        return tdate;
    }

    public String getTime() {
        String tdate;
        Calendar cal=Calendar.getInstance();
        SimpleDateFormat datetemple=new SimpleDateFormat("HH:mm");
        tdate=datetemple.format(cal.getTime());
        return tdate;
    }

    public static String getFirstDayOfMonth() {
        //获取当月第一天日历
        Calendar cal = Calendar.getInstance();

        // 获取某月第一天
        int firstDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
        // 设置日历中月份的最小天数
        cal.set(Calendar.DAY_OF_MONTH, firstDay);
        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String firstDayOfMonth = sdf.format(cal.getTime());
        return firstDayOfMonth;
    }

    public static String getLastDayOfMonth() {
        //获取当月最后一天
        Calendar cal = Calendar.getInstance();
        // 设置月份
        //cal.set(Calendar.MONTH, month - 1);
        // 获取某月最大天数
        int lastDay=cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 设置日历中月份的最大天数
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastDayOfMonth = sdf.format(cal.getTime());
        return lastDayOfMonth;
    }

    void print(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        Plex_qa app=new Plex_qa("www.plexus-online.com");
        try {
            Map<String,String> cookies= app.get_cookies("smmp.pwang", "99887766");

            String Session_Key=cookies.get("Session_Key");
            Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}

            String label="smmp123456";

            System.out.println("waiting...");
            //currentThread().sleep(120 * 60000);

            //System.out.println(app.get_container_history(Session_Key, label));
            //System.out.println(app.show_container_info(Session_Key,label));
            System.out.println(app.get_loaded_multiThread(Session_Key));
            //System.out.println(app.get_container_history(Session_Key,label));
            //System.out.println(app.get_inventory_adjustment(Session_Key));
            //System.out.println(app.get_workcenter_list(Session_Key));
            //System.out.println(app.get_loaded_container(Session_Key,"66615"));
            //System.out.println(app.get_loaded_all(Session_Key));
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        //Map<String,String> scrap_reason=app.get_scrap_reason_file(Session_Key);
        //System.out.println(scrap_reason);
        //System.out.println(scrap_reason.get("D01-冷模报废 Start-up scrap"));

        //System.out.println(Environment.getDataDirectory());

        //app.show_container_info(Session_Key, label);
        //app.change_status(Session_Key, label, "ok", "","test2");
    }
}
