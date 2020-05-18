package com.philip.plex_qa;

//本程序参考 https://blog.csdn.net/weixin_40438421/article/details/85700109  建框架，其中很多加载进度等功能view被hide了

import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ActivityContainerHistory extends AppCompatActivity{
    WebView mWebview;
    WebSettings mWebSettings;
    TextView beginLoading,endLoading,loading,mtitle;

    //sessions data
    Map<String,String> cookies=new HashMap<>();  //should get cookies if it is null
    Plex_qa plex_qa;
    String Session_Key,host;  //host may be test DB or production DB
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
        //mWebSettings.setSaveFormData(true);
        mWebSettings.setBuiltInZoomControls(true);  // 看看是否可缩放？

        //设置不用系统浏览器打开,直接显示在当前Webview
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebview.setWebViewClient(new WebViewClient() {
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
                beginLoading.setText("开始加载了");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                //如：可以在这里获取cookie
                //CookieManager cookieManager = CookieManager.getInstance();
                //String CookieStr = cookieManager.getCookie(url);
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
                //mWebview.loadData(history_html,"text/html", "utf-8");
                mWebview.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
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


}
