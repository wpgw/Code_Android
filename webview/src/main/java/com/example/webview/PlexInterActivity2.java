package com.example.webview;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

public class PlexInterActivity2 extends AppCompatActivity {
    WebView mWebview;
    TextView textview;
    String url_plex = "https://www.plexonline.com";
    //String url_mobile = "https://www.plexus-online.com"; //d056f1af-eade-4483-a749-c8d3e1280a0e/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx?Mobile=1";
    String first_page="/Interplant_Shipper/Interplant_Shipper_Form.asp?Do=Update&Interplant_Shipper_Key=513993"; //460129
    String session_ID = "";
    HashMap cookies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plex_inter2);

        mWebview = findViewById(R.id.webview);
        textview = findViewById(R.id.textView);

        //disable the strict polity that do not allows main thread network access
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        init_view();         //初始化 view
        mWebview.loadUrl(url_plex);  //开始登录
    }

    @SuppressLint("SetJavaScriptEnabled")  //标记，让不报错
    private void init_view() {
        textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        textview.setHeight(100);
        textview.setVisibility(View.GONE);

        WebSettings mWebSettings=mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true); // 设置支持javascript
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);//支持js调用window.open方法
        //mWebSettings.setSupportMultipleWindows(true);// 设置允许开启多窗口，在WebChromeClient.onCreateWindow中处理
        mWebSettings.setDomStorageEnabled(true);
        //mWebSettings.setSaveFormData(true);         //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放
        //mWebSettings.setBlockNetworkImage(true);  //  不加载图片，快些

        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebview.setWebViewClient(new WebViewClient() {
            //设置不用系统浏览器打开,直接显示在当前Webview
            @Override   //老机器用
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                System.out.println("转向旧: " + url);
                //登录成功后，保存cookie,跳转首页
                runOverrideUrlLoading(view, url);
                return true;
            }
            @Override    //新机器用
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                System.out.println("转向新: " + url);
                //登录成功后，保存cookie,跳转首页
                runOverrideUrlLoading(view, url);
                return true;
            }
            private void runOverrideUrlLoading(WebView view, String url){
                //如mobile界面登录成功,保存cookie,跳转首页
                if (url.contains("/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx")) {
                    System.out.println("登录成功,准备跳转：\n");
                    Uri uri = Uri.parse(url);
                    session_ID = uri.getPathSegments().get(0);
                    String cookieString = CookieManager.getInstance().getCookie(url_plex);
                    //登录成功后，把 mobile 的cookie转给 www
                    set_cookie(url_plex, cookieString);
                    cookies = stringTomap(cookieString);
                    //go to inter-plant
                    view.loadUrl(url_plex + "/" + session_ID + first_page);
                }
             else {
                    view.loadUrl(url);
                }
            }

            @Override
            /*
            以下拦截代码：会拦截每一个请求 另：测试表明两个回调函数会重复运行，只需一个
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                System.out.println("拦截 旧: "+url);
                if(url.contains("/Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")) {
                        return myInterruptResponse(url);
                }
                return super.shouldInterceptRequest(view, url);
            }
            */
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url=request.getUrl().toString();
                System.out.println("拦截 新: "+url);
                //如果是 interplant加载界面，就拦截后，直接返回
                if(url.contains("/Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")) {
                    return myInterPlant(url);
                }
                return super.shouldInterceptRequest(view, request);
            }
            private WebResourceResponse myInterPlant(String url){
                //访问页面，并修改后，返回一个WebResourceReponse给拦截器
                String html="";
                try{
                    Element doc=getUrl(url,cookies);  //这里有调用网络操作，可能网络出错
                    html=dealwith_interPlant(doc);
                }catch(Exception e){
                    //如网络操作出错，显示出错原因，并返回一个url当前跳转
                    System.out.println("网络操作出错，显示出错原因，并返回一个url当前跳转");
                    html=e.getMessage()+ "<br><a href=\"" +url+"\">"+url+"</a>";
                }
                InputStream targetContent=new ByteArrayInputStream(html.getBytes());
                return new WebResourceResponse("text/html","utf-8",targetContent);
            }

            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载:" + url);
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println("结束加载:" + url);
            }
        });
        //设置WebChromeClient类  作用：辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里:" + title);
            }

            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    String progress = newProgress + "%";
                } else if (newProgress == 100) {
                    String progress = newProgress + "%";
                }
            }
        });
    }

    private String dealwith_interPlant(Element element) {
        //主表格上的两个框框
        Element ele_containers=element.getElementById("hdnContainerView").parent();  //标题框：用于改颜色，显箱数
        Element input_table=element.getElementById("ContainerLoadingFilterTable")
                                   .getElementsByTag("tbody").first();  //用于改颜色
        //获得主表格
        Element element_table = element.getElementById("MainContainerLoadingGridTable");
        if (element_table != null) {          //如果有 加载表格，处理表格
            //表头的 最后两列去掉
            Elements header=element_table.getElementsByTag("thead").first().getElementsByTag("th");
            header.last().previousElementSibling().remove();   header.last().remove();
            //去掉左上角的 Print，Wiki按钮
            element.select("ul[title=Print]").remove();
            element.select("ul[title=Wiki]").remove();
            //主表格的各数据行
            Elements table_rows = element_table.getElementsByTag("tbody").first().getElementsByTag("tr");
            //显示箱数
            int count=(table_rows.size()-1); ele_containers.text(count+" Containers");
            //处理表格明细
            for (Element row : table_rows) {
                Elements columns = row.getElementsByTag("td");
                //表格行的最后两列去掉
                columns.last().previousElementSibling().remove();
                columns.last().remove();
                //如果发现在EPC的箱号，变红色
                if (columns.get(3).text().contains("EPC")) {
                    //warning message at textview
                    //textview.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    //textview.setHeight(textview.getHeight() + 50);
                    vibrate(1000);
                    //问题行变红
                    row.attr("style","background-color:red");
                    //其它有关表头框框等变红
                    input_table.removeAttr("style");  //先去掉style，再变红
                    input_table.attr("style","background-color:red");
                    ele_containers.attr("style","background-color:red");
                }else if(columns.get(1).text().contains("Totals")){  //在底行第二列加上总箱数
                    columns.get(1).text("Totals: "+count);
                }
            }
        }
        return element.outerHtml();
    }

    //点击返回上一页面而不是退出Activity
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebview.canGoBack()) {
            mWebview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //销毁Webview
    @Override
    protected void onDestroy() {
        if (mWebview != null) {
            mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebview.clearHistory();

            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.destroy();
            mWebview = null;
        }
        super.onDestroy();
    }

    public void set_cookie(String url, String cookieString) {
        //参考 https://blog.csdn.net/kelaker/article/details/82751287
        //CookieManager.getInstance().removeAllCookie();
        //CookieManager.getInstance().removeSessionCookie();

        String[] values = cookieString.split(";");
        for (String value : values) {
            CookieManager.getInstance().setCookie(url, value);
        }
        //同步 cookie修改
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(this);
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        //System.out.println( "饼干："+CookieManager.getInstance().getCookie(url));
    }

    //把Cookie String转成Map
    public HashMap<String, String> stringTomap(String cookieString) {
        HashMap<String, String> Cookies = new HashMap<String, String>();
        String[] values = cookieString.split(";");
        for (String value : values) {
            int index = value.indexOf('=');
            Cookies.put(value.substring(0, index), value.substring(index + 1));
        }
        System.out.println(this.toString()+ Cookies);
        return Cookies;
    }

    public void vibrate(int time) {
        Vibrator vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    public Connection.Response request_get(String url, HashMap<String, String> cookies) throws Exception {
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

    //访问指定的网站
    public Element getUrl(String url, HashMap<String, String> cookies) throws Exception {
        try {
            Connection.Response res = this.request_get(url, cookies);
            Document doc = res.parse();
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getInterplantContainer运行不成功 Exception。");
            throw e;
        }
    }
}