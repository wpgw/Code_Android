package com.philip.plex_qa;

import java.io.BufferedReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Plex_login {
    public Map<String,String> headers =new HashMap<>();
    public Map<String,String> cookies =new HashMap<>();
    String host,url;

    String viewstate="";
    String response_url="";   //to check if the Request is successful
    public static String path="";

    //构造函数
    public Plex_login(String host) {
        this.host=host; url="https://"+host;

        headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");
        headers.put("Host",this.host);
        headers.put("Connection","keep-alive");

        cookies.put("PlexSystems","CompanyCode=smmp");
        cookies.put("POLThemeKey","1");
    }

    public void get_post(String url,Map<String,String> data,Connection.Method method) throws Exception{
        try {
            Connection con=Jsoup.connect(url);
            if (method.equals(Method.POST)) {
                con.data(data);
            }
            con.headers(this.headers);
            con.cookies(this.cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(method).execute();
            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new MyException("你的密码过期了,请在电脑上更新密码!");
            }
            this.cookies.putAll(res.cookies());

            Document doc=res.parse();
            if(doc.title().equals("Plex Online Maintenance")){
                throw new MyException("Plex停机维护，请等待，");
            }

            //System.out.println("set cookies: " + res.cookies());
            viewstate=doc.select("input[id=__VIEWSTATE]").get(0).attr("value");
            response_url=res.url().toString();
            //System.out.println("viewstate:"+viewstate);
            //System.out.println("The cookies is: "+this.cookies);
            //System.out.println("response url:" + res.url().toString());
            //System.out.println(doc.html());
            //System.out.println("");
        }catch(SocketTimeoutException e){
            throw new MyException("Time Out!网络连接超时,请重试!");
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Map<String,String> login(String userID,String Password,String CompanyCode) throws Exception{

        //String url= "https://mobile.plexus-online.com";
//        if (Password.length()<19) {
//            Password=ExecuteJS("foggy.js",Password);
//        }

        try {
            //the 1 and 2 step: get the viewstate
            this.get_post(url, null, Method.GET);
            System.out.print(" 1: 2:done!");


            //the 3 step: post
            Map<String,String> data = new HashMap<>();
            data.put("__VIEWSTATE",viewstate);
            data.put("txtUserID",userID);
            data.put("txtPassword",Password);
            data.put("txtCompanyCode",CompanyCode);
            data.put("hdnUseSslAfterLogin","1");

            String url_index=url+"/Modules/SystemAdministration/Login/Index.aspx";
            this.get_post(url_index, data, Method.POST);
            System.out.print(" 3:done!");

            if (response_url.toLowerCase().indexOf("+valid+")!=-1){   //注意字符串比较的大小写
                System.out.println("  帐号或密码不正确！");
                //vibrate()                             //需声音 报警
                throw new MyException("帐号或密码不正确！");
            }
            //the 4 step: get the viewstate
            String url_login=url+"/Modules/SystemAdministration/Login/Login.aspx";
            this.get_post(url_login, data, Method.GET);
            System.out.print(" 4:done!");

            //the 5 step: post
            data.clear();
            data.put("__VIEWSTATE",viewstate);
            data.put("browserMinorVersion","undefined");
            data.put("screenHeight","768");
            data.put("screenWidth","1366");
            data.put("screenDepth","24");
            data.put("browserName","Netscape");
            data.put("browswerVersion","5.0(Windows NT 6.1;x64) Chrome/76.0.3809.100");

            this.get_post(url_login, data, Method.POST);
            // System.out.println(doc4.html());
            System.out.print(" 5:done!");

            // Need to save the userID and Password
            save_userInfo(userID,Password);
            return cookies;

        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

//    public String ExecuteJS(String JSfile,String password) {
//        //use the javascript foggy.js to foggy password
//        ScriptEngineManager manager=new ScriptEngineManager();
//        ScriptEngine engine=manager.getEngineByName("js");
//        try {
//            String path = Plex_login.class.getResource("").getPath();
//
//            engine.eval(new FileReader(path+JSfile));
//            Invocable invocable=(Invocable)engine;
//            String arg[] = {password}; //使用Invocable调用脚本函数，传入String参数
//            String result= invocable.invokeFunction("Foggy",arg).toString();
//            return result;
//        }catch(Exception e) {
//            e.printStackTrace();
//            return "";
//        }
//    }

    public void save_userInfo(String userID,String password) {
        //String path = Plex_login.class.getResource("").getPath();
        File file=new File(this.path+"/.plex");
        if(!file.exists()) {
            try {
                file.createNewFile();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        try {
            FileWriter out=new FileWriter(file);
            out.write(userID);
            //out.write("\n");
            //out.write(password);
            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //从文件中获得用户名与密码
    public List<String> get_userInfo(){
        File file=new File(this.path+"/.plex");
        List<String> list=new ArrayList<>();
        try {
            if (file.exists()) {
                FileReader fr=new FileReader(file);
                BufferedReader bufr=new BufferedReader(fr);
                list.add(bufr.readLine());    //read userid
                //list.add(bufr.readLine()); //don't read password
                bufr.close();
                fr.close();
                return list;
            } else {
                list.add("");
                return list;
            }
        }catch(Exception e){
            e.printStackTrace();
            list.add("");
            return list;
        }
    }

    public static void main(String[] args) {
        String path = Plex_login.class.getResource("").getPath();
        System.out.println("Plex_login class Path:"+path);
        Plex_login login=new Plex_login("test.plexus-online.com");

        try {
            if (login.login("smmp.pwang","99887766","smmp")!=null) {//cookie在类login的public变量中
                String session_key=login.cookies.get("Session_Key").toString();
                session_key=session_key.substring(1,session_key.length()-1);  //去掉头尾的{}
                System.out.println("\n "+session_key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

