package com.philip.plex_qa;

//本程序参考 https://blog.csdn.net/weixin_40438421/article/details/85700109  建框架，其中很多加载进度等功能view被hide了

import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class ActivityContainerHistory extends AppCompatActivity{
    WebView mWebview;
    WebSettings mWebSettings;
    TextView beginLoading,endLoading,loading,mtitle;

    //sessions data
    Map<String,String> cookies=new HashMap<>();  //should get cookies if it is null
    Plex_qa plex_qa;
    String Session_Key,host,base_url;  //host may be test DB or production DB
    String barcode,function;
    String html="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("ActivityContainerHistory On create!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_history);

        //get cookies and session_id from Intent
        Bundle bundle = getIntent().getExtras();
        this.host=bundle.getString("host");
        this.barcode=bundle.getString("barcode");
        this.function=bundle.getString("function");
        this.cookies=(Map<String,String>)bundle.getSerializable("cookies");
        this.Session_Key=this.cookies.get("Session_Key");
        this.Session_Key=Session_Key.substring(1,Session_Key.length()-1);  //去掉头尾的字符{}
        this.base_url="https://"+this.host+"/"+Session_Key;

        // init a plex_qa class instance
        plex_qa =new Plex_qa(this.host);
        plex_qa.cookies=this.cookies;   // transfer cookies to plex_qa instance

        mWebview = (WebView) findViewById(R.id.webView1);
        beginLoading = (TextView) findViewById(R.id.text_beginLoading);
        endLoading = (TextView) findViewById(R.id.text_endLoading);
        loading = (TextView) findViewById(R.id.text_Loading);
        mtitle = (TextView) findViewById(R.id.title);
        beginLoading.setVisibility(View.GONE);  // hide the views
        endLoading.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        mtitle.setVisibility(View.GONE);

        mWebSettings = mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSaveFormData(true);  //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放

        //用于 运行jascript, 获取 webview的当前html
        mWebview.addJavascriptInterface(new InJavaScriptLocalObj(),"java_obj");
        mWebview.setWebViewClient(new WebViewClient()); //此行代码可以保证JavaScript的Alert弹窗正常弹出


        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebview.setWebViewClient(new WebViewClient() {
            //设置不用系统浏览器打开,直接显示在当前Webview
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
                beginLoading.setText("开始加载了");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                //获取 webview 的html
                view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                System.out.println("结束加载了");
                endLoading.setText("结束加载了");
            }
        });

        //设置WebChromeClient类  作用：辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里");
                mtitle.setText(title);
            }
            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    String progress = newProgress + "%";
                    loading.setText(progress);
                } else if (newProgress == 100) {
                    String progress = newProgress + "%";
                    loading.setText(progress);
                }
            }
        });

        if (html.length()==0) {  //如果 html没有内容
            try {
                if (this.function.equals("Inventory_History")) {
                    System.out.println("开始显示inventory history");
                    html = plex_qa.get_container_history(Session_Key, barcode);
                } else {  //以下查 已加载物料
                    Plex_qa.result="";   //reset result
                    html=plex_qa.get_loaded_multiThread(Session_Key);
                    html=html.replace("Material",""); //去掉多余字段
                    html=html.replace("Heat","");
                    html=html.replace("No Source Inventory Loaded","<p align=\"left\">No Source Inventory Loaded</p>");
                    html=html.replace("Currently Loaded","");
                }
                html=html.replace("\"../","\"https://"+host+"/"+Session_Key+"/");  //换成绝对地址
                //这个BaseURL没作用，要研究下
                mWebview.loadDataWithBaseURL(this.base_url, html, "text/html", "utf-8", null);
                //mWebview.loadData(history_html,"text/html", "utf-8");
                //mWebview.loadUrl(this.base_url+"/Interplant_Shipper/Interplant_Shipper.asp");
                set_cookie();  // 保存Cookie
            } catch (Exception e) {
                Toast.makeText(ActivityContainerHistory.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
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

    public void set_cookie(){
        //参考 https://blog.csdn.net/kelaker/article/details/82751287
        //delete cookies
//        CookieManager.getInstance().removeAllCookie();
//        CookieManager.getInstance().removeSessionCookie();

        //得到向URL中添加的Cookie的值
        String cookieString="";
        for(Map.Entry<String, String> item:this.cookies.entrySet()){
            cookieString=item.getKey()+"="+item.getValue()+";path=/";
            //添加Cookie
            CookieManager.getInstance().setCookie("https://"+host, cookieString);
        }
        //同步 cookie修改
        CookieSyncManager.createInstance(this);
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        System.out.println( CookieManager.getInstance().getCookie("https://"+host));
    }

    final class InJavaScriptLocalObj{   //有关获取webview 的html, 在onPageFinished中引用
        @JavascriptInterface
        public void getSource(String html){
            System.out.println("嘿嘿html=\n"+html);
        }
    }
}
