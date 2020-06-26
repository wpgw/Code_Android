package com.example.webview;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class buildMasterActivity extends AppCompatActivity {
    WebView mWebview;
    EditText etMaster,etSerial;
    TextView tvMessage,tvList;
    LinearLayout newPage;
    String url_plex = "https://mobile.plexus-online.com";
    String first_page="/Mobile/Inventory/Mobile_Build_Master_Unit.asp?Node=530174"; //build new master label

    String session_ID = "";
    HashMap cookies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_master1);

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
        mWebview = findViewById(R.id.webview);

        newPage=findViewById(R.id.newPage);
        newPage.setVisibility(View.GONE);
        etMaster=findViewById(R.id.etMaster);
        etSerial=findViewById(R.id.etSerial);

        tvMessage=findViewById(R.id.tvMessage);
        tvMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvList=findViewById(R.id.tvList);

        WebSettings mWebSettings=mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true); // 设置支持javascript
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);//支持js调用window.open方法
        //mWebSettings.setSupportMultipleWindows(true);// 设置允许开启多窗口，在WebChromeClient.onCreateWindow中处理
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setSaveFormData(true);         //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放
        //mWebSettings.setBlockNetworkImage(true);  //  不加载图片，快些
        mWebSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        //不能加，加了不能登录Plex, 需查原因
        //mWebSettings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");

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
            private void runOverrideUrlLoading(WebView view, String url) {
                //如mobile界面登录成功,保存cookie,跳转首页
                if (url.contains("/Modules/SystemAdministration/MenuSystem/Mobile/Menu.aspx?Node=")) {
                    System.out.println("登录成功,准备跳转：\n");
                    Uri uri = Uri.parse(url);
                    session_ID = uri.getPathSegments().get(0);
                    String cookieString = CookieManager.getInstance().getCookie(url_plex);
                    cookies = Utils.stringTomap(cookieString);
                    //go to next Activity
                    //view.loadUrl(url_plex+"/"+session_ID+first_page);
                    view.loadUrl(url);
                }else{
                    view.loadUrl(url);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri=request.getUrl();
                String url=uri.toString();
                System.out.println("拦截："+uri);
                if(url.contains("/Mobile/Inventory/Mobile_Build_Master_Unit_Container.asp?MasterUnit=")){
                    //获得master label号
                    String masterUnit=uri.getQueryParameter("MasterUnit");
                    //newPage.setVisibility(View.VISIBLE);
                    //etMaster.setText(uri.getQueryParameter("MasterUnit"));
                    try {
                        masterUnitHandler(session_ID,"M022654","smmp123456");      ///////////////////////////////////
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            private WebResourceResponse myInterPlant(String url){
                //访问页面，并修改后，返回一个WebResourceReponse给拦截器
                String html="";
                try{
                    //Map<String,String> data=new LinkedHashMap<>();   //这个保证顺序
                    //data.put("RequestID","0");
                    html=Utils.request_get(url,cookies);  //这里有调用网络操作，可能网络出错
                    //html=dealwith_interPlant(doc);
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

    public void vibrate(int time) {
        Vibrator vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    void masterUnitHandler(String session_ID,String master,String serial) throws Exception {
        String url="https://www.plexus-online.com/"+session_ID+"/Modules/Inventory/MasterUnits/MasterUnitHandler.ashx?ApplicationKey=166143";
        String html;
        //查有关master label的基本数据
        HashMap<String,String> data=new HashMap<>();
        data.put("Action","GetMasterUnit");data.put("MasterUnitNo",master);
        html=Utils.request_post(url, cookies, data);
        System.out.println("读master基本数据：");
        System.out.println(html);

        //查有关箱号是否在master label中
        data.clear();
        data.put("Action","ValidateContainer");data.put("SerialNo",serial);
        html=Utils.request_post(url, cookies, data);
        System.out.println("验证：");
        System.out.println(html);

        //把相关箱号加入到指定的master label中
        data.clear();
        data.put("Action","BuildMasterUnit");data.put("MasterUnitKey","");data.put("MasterUnitNo","");
        data.put("MasterUnitTypeKey","4605467");data.put("Location","");data.put("SerialNo",serial);
        //html=Utils.request_post(url,cookies,data);
    }

}
