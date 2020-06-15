package com.example.webview;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.view.KeyEvent;
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
    WebSettings mWebSettings;
    String url_plex = "https://www.plexus-online.com";
    String url_mobile = "https://mobile.plexus-online.com"; //d056f1af-eade-4483-a749-c8d3e1280a0e/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx?Mobile=1";
    String session_ID = "";
    HashMap cookies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plex_inter);

        mWebview = findViewById(R.id.webview);
        textview = findViewById(R.id.textView);

        //disable the strict polity that do not allows main thread network access
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        init_view();
        mWebview.loadUrl(url_mobile);
    }

    @SuppressLint("SetJavaScriptEnabled")         //不报错
    private void init_view() {
        textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        textview.setHeight(100);

        mWebSettings = mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSaveFormData(true);         //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放
        //mWebSettings.setBlockNetworkImage(true);  //  不加载图片，快些

        //用于 运行jascript, 获取 webview的当前html
        mWebview.addJavascriptInterface(new InJavaScriptLocalObj(), "java_obj");
        //mWebview.setWebViewClient(new WebViewClient()); //此行代码可以保证JavaScript的Alert弹窗正常弹出

        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebview.setWebViewClient(new WebViewClient() {
            //设置不用系统浏览器打开,直接显示在当前Webview
            @Override   //老机器用
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                System.out.println("拦截at 1: " + url);
                try {
                    runOverrideUrlLoading(view, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override    //新机器用
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                System.out.println("拦截at 2: " + url);
                try {
                    //可能会拦截
                    runOverrideUrlLoading(view, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            private void runOverrideUrlLoading(WebView view, String url) throws Exception {
                if (url.contains("/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx")) {   //如mobile界面登录成功
                    System.out.println("准备跳转：");
                    Uri uri = Uri.parse(url);
                    session_ID = uri.getPathSegments().get(0);
                    String cookieString = CookieManager.getInstance().getCookie(url_mobile);
                    //登录成功后，把mobile 的cookie转给 www
                    set_cookie(url_plex, cookieString);
                    cookies = stringTomap(cookieString);
                    //go to inter-plant
                    view.loadUrl(url_plex + "/" + session_ID + "/Interplant_Shipper/Interplant_Shipper_Form.asp?Do=Update&Interplant_Shipper_Key=513993");  //460129
                }
             else {
                    view.loadUrl(url);
                }
            }

            @Override   //以下拦截代码：会拦截每一个请求 另：测试表明两个回调函数会重复运行，只需一个
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                System.out.println("拦截at 3: "+url);
                if(url.contains("/Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")) {  //一个拦截测试
                    try {
                        return myInterruptResponse(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            //            @Override
//            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                System.out.println("拦截at 4: "+request.getUrl().toString());
//                return super.shouldInterceptRequest(view, request);
//            }
            private WebResourceResponse myInterruptResponse(String url) throws Exception {  //拦截回调后的一个具体处理子程序
                String targetHtml=dealwith_interplant(getInterplantContainer(url,cookies));
                InputStream targetContent=new ByteArrayInputStream(targetHtml.getBytes());
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
                //如在Inter-Plant扫描加载界面, 注入javascript，获取 webview 的html
                if (url.contains("Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")) {
                    //view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    //如果帐号过期，会转到Test
                } else if (url.contains("https://www.plexus-online.com/modules/systemadministration/login/index.aspx")) {
                    mWebview.loadUrl("https://mobile.plexus-online.com/modules/systemadministration/login/index.aspx");
                }
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

    final class InJavaScriptLocalObj {   //获取webview 的html并jsoup解读, 在onPageFinished中注入javascript
        @JavascriptInterface
        public void getSource(final String html) {
            //System.out.println("内容:\n"+html);
            mWebview.post(new Runnable() {
                @Override
                public void run() {
                    Document document = Jsoup.parse(html);
                    Element element_table = document.getElementById("MainContainerLoadingGridTable");
                    Boolean flag_error = false;
                    //init info at textview
                    textview.setText("   信息栏： ");  //clear textview
                    textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    if (element_table != null) {          //如果有 加载表格，处理表格
                        Elements elements = element_table.getElementsByTag("tbody").first().getElementsByTag("tr");

                        for (Element element : elements) {
                            Elements eles = element.getElementsByTag("td");
                            //System.out.println(eles.get(3).text());
                            if (eles.get(3).text().contains("EPC")) {
                                //warning message at textview
                                textview.setBackgroundColor(getResources().getColor(R.color.colorRed));
                                textview.setHeight(textview.getHeight() + 50);
                                textview.setText("   信息栏： " + eles.get(1).text());
                                vibrate(500);
                                flag_error = true;
                            }
                        }
                        //mWebview.loadDataWithBaseURL(url_plex,document.outerHtml(), "text/html", "utf-8", null);
                        //mWebview.reload();
                        if (!flag_error) {
                            textview.setText("   信息栏： " + (elements.size() - 1));
                        }

                    }
                }
            });
        }
    }

    //点击返回上一页面而不是退出浏览器
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

    public HashMap<String, String> stringTomap(String cookieString) {
        HashMap<String, String> Cookies = new HashMap<String, String>();
        String[] values = cookieString.split(";");
        for (String value : values) {
            int index = value.indexOf('=');
            Cookies.put(value.substring(0, index), value.substring(index + 1));
        }
        System.out.println(Cookies);
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

    public Element getInterplantContainer(String url, HashMap<String, String> cookies) throws Exception {
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

    private String dealwith_interplant(Element element) {
        Element element_table = element.getElementById("MainContainerLoadingGridTable");
        Boolean flag_error = false;
        //init info at textview  !!!!!!!!!!!此线程不能直接操作View
//        textview.setText("   信息栏inter： ");  //clear textview
//        textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        if (element_table != null) {          //如果有 加载表格，处理表格
            Elements header=element_table.getElementsByTag("thead").first().getElementsByTag("th");
            header.last().previousElementSibling().remove();
            header.last().remove();

            Elements table_rows = element_table.getElementsByTag("tbody").first().getElementsByTag("tr");
            for (Element row : table_rows) {
                Elements colums = row.getElementsByTag("td");
                colums.last().previousElementSibling().remove();
                colums.last().remove();

                //System.out.println(eles.get(3).text());
                if (colums.get(3).text().contains("EPC")) {
                    //warning message at textview
//                    textview.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                    textview.setHeight(textview.getHeight() + 50);
//                    textview.setText("   信息栏error： " + colums.get(1).text());
                    vibrate(500);
                    flag_error = true;
                }else if(colums.get(1).text().contains("Totals")){
                    colums.get(1).text("Totals: "+(table_rows.size()-1));
                }
            }
            if (!flag_error) {
//                textview.setText("   信息栏： " + (table.size() - 1));
            }
        }
        return element.outerHtml();
    }

}