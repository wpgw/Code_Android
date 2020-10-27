package com.philip.comm;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class Plex_add_Note {
    public Map<String,String> headers =new HashMap<>();
    public Map<String,String> cookies;
    String host,url_pre;
    static String result;  // for multi-thread method of loaded_container


    //构造函数
    public Plex_add_Note(String host) {      //构造函数
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

    private Connection.Response request_get(String url) throws Exception{
        try {
            //trustEveryone();
            Connection con= Jsoup.connect(url);
            con.headers(this.headers);
            con.cookies(this.cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Connection.Method.GET).execute();
            if (res.url().toString().toLowerCase().contains("Exception.aspx")){
                throw new Exception("系统出错或Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("is+currently+unavailable")){
                throw new MyException("Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp"))            {
                throw new MyException("你空闲时间过长,需重新登陆了!");
            } else if (res.url().toString().toLowerCase().contains("change_password")){
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

    private Connection.Response request_post(String url,Map<String,String> data) throws Exception{
        try {
            trustEveryone();
            Connection con=Jsoup.connect(url);
            con.data(data);
            con.headers(this.headers);
            con.cookies(this.cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            con.proxy("127.0.0.1",8888);    //The settings is for Charles
            System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Connection.Method.POST).execute();
            if (res.url().toString().toLowerCase().contains("Exception.aspx")){
                throw new Exception("系统出错或Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("is+currently+unavailable")){
                throw new MyException("Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")){
                throw new MyException("你空闲时间过长,需重新登陆了!");
            }else if (res.url().toString().toLowerCase().contains("change_password")){
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

    public String add_note(String Session_Key) throws Exception{
        String temp_str="2338/\\Serial_No[]3196/\\Operation_Key[]3200/\\Operation_Type[]3204/\\Active[]3205/\\Part_Key[]3214/\\Container_Status[]6100/\\[]6101/\\Job_No[]6102/\\Location[]6103/\\Material_Code[]6104/\\Supplier[]6105/\\Tracking_No[]23761/\\Part_Type[]23762/\\Heat_Code[]23764/\\Customer_No[]23765/\\Exclude_Raw_Material[]23766/\\Shippable[]23768/\\Lot_Key[]23769/\\Job_Template_Key[]23771/\\Inventory_Type_Only[]23772/\\Accounting_Job_Key[]23773/\\Order_No[]24071/\\Building[]81016/\\Group By[]213974/\\Master_Unit_Key[]285725/\\Part_No_Begins[]309088/\\Operation_Types_Picker[]337533/\\Heat_No_new[]385621/\\From Date[]385622/\\End Date[]392678/\\Building_New[]926731/\\Container_Types_Multi";
        String url= url_pre + Session_Key+"/Rendering_Engine/Default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(245)";
        Map<String,String> data=new LinkedHashMap<>();
        data.put("__EVENTTARGET","Screen");
        data.put("__EVENTARGUMENT","Search");
        data.put("__LASTFOCUS","");
        data.put("__VIEWSTATE","/wEPDwUJOTg5NDMxNjIwZBgBBR5fX0NvbnRyb2xzUmVxdWlyZVBvc3RCYWNrS2V5X18WAgUPTGF5b3V0MSRlbF8zMjA0BRBMYXlvdXQxJGVsXzIzNzY222RkO28Ho/O49sKnDCSvE2T0QpM=");
        data.put("__VIEWSTATEGENERATOR","2811E9B3");
        data.put("hdnScreenTitle","Inventory");
        data.put("hdnFilterElementsKeyHandle",temp_str);
        data.put("ScreenParameters","Accounting_Job_Key=|Accounting_Job_No=|Active=|AutoSearch=|Building_Code=|Building_Key=|Class=|Container_Note=|Container_Status=|Container_Type=|Customer Part No=|Description=|Exact_Match=|ExcludeRawMaterial=|Group_By=|Heat_Code=|Heat_Key=|Job_Key=|Job_No=|Job_Template_Key=|Job_Template_Name=|Location=|Lot_Key=|Lot_No=|Master_Unit_Key=|Master_Unit_No=|Material_Code=|Material_Key=|Operation_Code=|Operation_Key=|Operation_Type=|Part Name=|Part_Key=|Part_No=|Part_Status=|Part_Type=|Quantity=|Serial_No=|Shippable=|Supplier_Code=|Supplier_No=|Tracking_No=|");
        data.put("RequestKey","1");
        data.put("Layout1$el_285725","");
        data.put("Layout1$el_285725_hf","");
        data.put("Layout1$el_285725_hf_last_valid","");
        data.put("Layout1$el_2338","");
        data.put("Layout1$el_3196","");
        data.put("Layout1$el_3196_hf","");
        data.put("Layout1$el_3196_hf_last_valid","");
        data.put("Layout1$el_6103","");
        data.put("Layout1$el_6103_hf","");
        data.put("Layout1$el_6103_hf_last_valid","");
        data.put("Layout1$el_6101","");
        data.put("Layout1$el_6101_hf","");
        data.put("Layout1$el_6101_hf_last_valid","");
        data.put("Layout1$el_6105","");
        data.put("Layout1$el_3214","Suspect");
        data.put("Layout1$el_6102","cycle i");
        data.put("Layout1$el_6102_hf","");
        data.put("Layout1$el_6102_hf_last_valid","");
        data.put("Layout1$el_24071","");
        data.put("Layout1$el_24071_hf","");
        data.put("Layout1$el_24071_hf_last_valid","");
        data.put("Layout1$el_3200","");
        data.put("Layout1$el_6104","");
        data.put("Layout1$el_6104_hf","-1");
        data.put("Layout1$el_6104_hf_last_valid","");
        data.put("Layout1$el_81016","P");
        data.put("Layout1$el_213974","");
        data.put("Layout1$el_213974_hf","");
        data.put("Layout1$el_213974_hf_last_valid","");
        data.put("Layout1$el_3204","on");
        data.put("Layout1$el_385621","10/25/2015");
        data.put("Layout1$el_385622","");
        data.put("Layout1$el_926731","");
        data.put("Layout1$el_926731_hf","");
        data.put("Layout1$el_926731_hf_last_valid","");
        data.put("Layout1_el_254822","");
        data.put("panel_row_count_3","0");

        try {
            //post
            Connection.Response res=this.request_post(url, data);
            Document doc=res.parse();
            //change the grid data
            //Element grid=doc.getElementById("GRID_PANEL_3_28");
//            Elements grid_trs=grid.getElementsByTag("tr");  //表的行集合
//            for(Element tr:grid_trs){ //tr.child 指行中的列数据td
//                tr.child(5).text(""); //tr.child(13).text("");//去掉 Unit Cost and  Part Group栏 column
//                if(tr.child(2).text().length()==0){        //去掉没有修改数量的记录行 row
//                    tr.remove();
//                }
//            }
            return doc.outerHtml();

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        Plex_add_Note app=new Plex_add_Note("www.plexonline.com");
        try {
            Map<String,String> cookies= app.get_cookies("smmp.pwang", "77665544");

            String Session_Key=cookies.get("Session_Key");
            Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}

            System.out.println("waiting...");

            System.out.println(app.add_note(Session_Key));
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
