package com.philip.comm;


import android.os.Build;
import android.os.Vibrator;
import android.webkit.CookieManager;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.VIBRATOR_SERVICE;

public class Utils {
    //把 Cookie硬塞给另一个url
    public static void set_cookie(String url, String cookieString) {
        //参考 https://blog.csdn.net/kelaker/article/details/82751287
        //CookieManager.getInstance().removeAllCookie();
        //CookieManager.getInstance().removeSessionCookie();

        String[] values = cookieString.split(";");
        for (String value : values) {
            CookieManager.getInstance().setCookie(url, value);
        }
        //同步 cookie修改
        if (Build.VERSION.SDK_INT < 21) {
            //CookieSyncManager.createInstance(this);
            //CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        //System.out.println( "饼干："+CookieManager.getInstance().getCookie(url));
    }

    public static String check_if_location(String barcode){
        barcode=barcode.toUpperCase();
        //+表示至少一次  * 可零次
        String pattern="(^WH\\S+|^OS-\\S+|^ASSY\\S*|^METAL-\\S+|^IN-\\S+|^CNC|^DCM|^EPC|^PKG\\S*|^XJ-\\S+|^WAREHOUSE\\S*|^XUJIN|^SEND TO\\S+|^RECEIVING)";
        Pattern re=Pattern.compile(pattern);
        Matcher ma=re.matcher(barcode);

        if(ma.find()){
            String result=ma.group();
            return result;
        }else{
            return "";
        }
    }

    public static String refine_label(String txtSerial_NO) {
        //String pattern="([a-zA-Z]{4}\\d{6,7}|\\d{6,7})";
        //不接受 纯数字条码：必须 首4位字母后跟6或7位数字，MLT后7位, T后8位
        txtSerial_NO=txtSerial_NO.toUpperCase();
        String pattern="(SMMP\\d{6,7}|WMLT\\d{6,7}|MLT\\d{7}|T\\d{8})";

        Pattern re=Pattern.compile(pattern);
        Matcher ma=re.matcher(txtSerial_NO);

        if (ma.find()) {
            String result=ma.group();
            if (result.length()<9) {
                //result="smmp"+result;  //此时自动补上smmp
            }
            return result;
        }else {
            return "";
        }
    }

    //把Cookie String转成Map
    public static HashMap<String, String> stringTomap(String cookieString) {
        HashMap<String, String> Cookies = new HashMap<String, String>();
        String[] values = cookieString.split(";");
        for (String value : values) {
            int index = value.indexOf('=');
            Cookies.put(value.substring(0, index).trim(), value.substring(index + 1));
        }
        //System.out.println(this.toString()+ Cookies);
        return Cookies;
    }

    public static Connection.Response request_get(String url, HashMap<String, String> cookies) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");
        //headers.put("Host",host);
        headers.put("Connection", "keep-alive");
        try {
            //trustEveryone();
            Connection con = Jsoup.connect(url);
            con.headers(headers);
            con.cookies(cookies);
            con.timeout(1000 * 20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res = con.method(Connection.Method.GET).execute();
            if (res.url().toString().toLowerCase().contains("Exception.aspx")){
                throw new Exception("系统出错或Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("is+currently+unavailable")){
                throw new Exception("Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")) {
                throw new Exception("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")) {
                throw new Exception("你的密码过期了,请在电脑上更新密码!");
            }
            return res;

        } catch (SocketTimeoutException e) {
            throw new Exception("Time Out!网络连接超时,请重试!");
        } catch (UnknownHostException e) {
            throw new Exception("网络故障，找不到主机地址！");
        } catch (Exception e) {
            System.out.println("Catch Exception at request_get");
            throw e;
        }
    }

    public static Connection.Response request_post(String url, HashMap<String, String> cookies, Map<String,String> data) throws Exception{
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");
        try {
            //trustEveryone();
            Connection con=Jsoup.connect(url);
            con.data(data);
            con.headers(headers);
            con.cookies(cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Connection.Method.POST).execute();
            if (res.url().toString().toLowerCase().contains("Exception.aspx")){
                throw new Exception("系统出错或Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("is+currently+unavailable")){
                throw new Exception("Plex不在线！");
            }
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")){
                throw new Exception("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new Exception("你的密码过期了,请在电脑上更新密码!");
            }
            return res;

        }catch(SocketTimeoutException e){
            throw new Exception("Time Out!网络连接超时,请重试!");
        }catch(UnknownHostException e){
            throw new Exception("网络故障，找不到主机地址！");
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static Map<String,String> show_container_info(HashMap<String,String> cookies,String pre_url,String txtSerial_No) throws Exception{
        //如果txtSerial_No不存在，会返回什么结果? 需处理
        String url=pre_url+txtSerial_No;
        try {  //////如果出网络出错，没有返回数据，怎么办？
            Connection.Response res=request_get(url,cookies);
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
            System.out.println("抓到报错：catch Exception at show_container_info.");
            e.printStackTrace();
            throw e;
        }
    }

    public static HashMap<String,String> move_container(HashMap<String,String> cookies,String pre_url,String location,String barcode) throws Exception{
        String url=pre_url+"/Modules/Inventory/InventoryTracking/MoveContainerHandler.ashx?ApplicationKey=165838";
        HashMap<String,String> resultMap=new HashMap<>();

        Map<String,String> data=new LinkedHashMap<>();
        data.put("Action","MoveContainer");data.put("SerialNo",barcode);data.put("Location",location);data.put("PartNo","");

        try {
            Connection.Response res = request_post(url, cookies, data);
            String jsonString=res.body();
            Map<String,Object> objectMap= JSON.parseObject(jsonString,Map.class);
            if(objectMap.get("IsValid")!=null) {  //如 收到移库成功与否的回应
                String IsValid = objectMap.get("IsValid").toString();
                String Message = objectMap.get("Message").toString();
                resultMap.put("IsValid",IsValid);resultMap.put("Message",Message);
                return resultMap;
            }else{
                return null;
            }
        }catch(Exception e) {
            System.out.println("catch Exception at move_container.");
            e.printStackTrace();
            throw e;
        }
    }

    public static void refreshTextView(TextView tv, String msg){
        //参考 https://www.cnblogs.com/tt2015-sz/p/4502341.html
        tv.append(msg);
        int offset=tv.getLineCount()*tv.getLineHeight();
        if(offset>tv.getHeight()){
            tv.scrollTo(0,offset-tv.getHeight());
        }
    }

    public static String getNowDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public static String getMonthTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static Date toGMTdate(Date date){
        Long timestamp=date.getTime();            //到 70年的毫秒数
        int offset= TimeZone.getDefault().getRawOffset();  //获取和 格林威治标准时区 的偏移值
        timestamp-=offset;     //调成GMT时间
        Date GMTdate=new Date(timestamp);
        return GMTdate;
    }
}
