package com.philip.fifo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.philip.comm.Utils;

import java.io.Serializable;
import java.util.HashMap;

public class webloginActivity extends AppCompatActivity {
    WebView mWebview;
    String host,url_plex = "https://mobile.plexus-online.com";
    Intent intent=null;
    String first_page="/Interplant_Shipper/Interplant_Shipper.asp"; //_Form?Do=Update&Interplant_Shipper_Key=513993"; //460129

    String session_ID = "";
    HashMap cookies;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weblogin);

        mWebview = findViewById(R.id.webview);

        host="www.plexus-online.com";
        init_view();         //初始化 view
        mWebview.loadUrl(url_plex);  //开始登录
        intent=new Intent(this,fifoActivity.class);
    }

    @SuppressLint("SetJavaScriptEnabled")  //标记，让不报错
    private void init_view() {
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
            private void runOverrideUrlLoading(WebView view, String url){
                //如mobile界面登录成功,保存cookie,跳转首页
                if (url.contains("/Modules/SystemAdministration/MenuSystem/Mobile/Menu.aspx?Node=")) {
                    System.out.println("登录成功,准备跳转：\n");
                    Uri uri = Uri.parse(url);
                    session_ID = uri.getPathSegments().get(0);
                    String cookieString = CookieManager.getInstance().getCookie(url_plex);
                    cookies = Utils.stringTomap(cookieString);
                    //go to next Activity
                    System.out.println("Go to next page...-----------------------------");
                    // Transfer to next page
                    Bundle bundle = new Bundle();
                    bundle.putString("host", host);
                    bundle.putSerializable("cookies", (Serializable) cookies);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }else {
                    view.loadUrl(url);
                }
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
}
