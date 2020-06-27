package com.example.webview;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Utils {
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

    //把Cookie String转成Map
    public static HashMap<String, String> stringTomap(String cookieString) {
        HashMap<String, String> Cookies = new HashMap<String, String>();
        String[] values = cookieString.split(";");
        for (String value : values) {
            int index = value.indexOf('=');
            Cookies.put(value.substring(0, index), value.substring(index + 1));
        }
        //System.out.println(this.toString()+ Cookies);
        return Cookies;
    }

    public static String request_get(String url, HashMap<String, String> cookies) throws Exception {
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

            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")) {
                throw new Exception("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")) {
                throw new Exception("你的密码过期了,请在电脑上更新密码!");
            }
            return res.parse().html();

        } catch (SocketTimeoutException e) {
            throw new Exception("Time Out!网络连接超时,请重试!");
        } catch (UnknownHostException e) {
            throw new Exception("网络故障，找不到主机地址！");
        } catch (Exception e) {
            System.out.println("Catch Exception at request_get");
            throw e;
        }
    }

    public static String request_post(String url, HashMap<String, String> cookies, Map<String,String> data) throws Exception{
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
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")){
                throw new Exception("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new Exception("你的密码过期了,请在电脑上更新密码!");
            }
            return res.parse().html();

        }catch(SocketTimeoutException e){
            throw new Exception("Time Out!网络连接超时,请重试!");
        }catch(UnknownHostException e){
            throw new Exception("网络故障，找不到主机地址！");
        }catch(Exception e) {
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

    public static String getDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}
