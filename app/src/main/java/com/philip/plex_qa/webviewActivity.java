//这个代码主要是 研究webview开多窗口Plex，后发现只要打开webview的setJavaScriptCanOpenWindowsAutomatically(true)就可以
package com.philip.plex_qa;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class webviewActivity extends AppCompatActivity {
    WebView mWebView;
    String session_ID,firstPage;
    String url_plex="https://www.plexonline.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        //传入 applicaton context有利于解决网内存泄漏
        mWebView = new WebView(getApplicationContext());
        LinearLayout linearLayout1 = findViewById(R.id.linearlayout1);
        linearLayout1.addView(mWebView);
        init_view();
        mWebView.loadUrl(url_plex);
    }

    private void init_view(){
        //mWebView.setLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT);

        WebSettings mWebSettings=mWebView.getSettings();
        mWebSettings.setJavaScriptEnabled(true); // 设置支持javascript
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);//支持js调用window.open方法
        //mWebSettings.setSupportMultipleWindows(true);// 设置允许开启多窗口
        mWebSettings.setDomStorageEnabled(true);
        //mWebSettings.setSaveFormData(true);         //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放
        //mWebSettings.setBlockNetworkImage(true);  //  不加载图片，快些

        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebView.setWebViewClient(new WebViewClient() {
            //设置不用系统浏览器打开,直接显示在当前Webview
            @Override   //老机器用
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                System.out.println("跳转 旧: "+url);
                runOverrideUrlLoading(view,url);
                return true;
            }
            @Override    //新机器用
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                String url=request.getUrl().toString();
                System.out.println("跳转 新: "+url);
                System.out.println(request.getRequestHeaders());
                runOverrideUrlLoading(view,url);
                return true;
            }
            private void runOverrideUrlLoading(WebView view,String url){
                view.loadUrl(url);
            }

//            @Override   //以下拦截代码：会拦截每一个请求 另：测试表明两个回调函数会重复运行，只需一个
//            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//                System.out.println("拦截at 3: "+url);
//                if(url.contains("www.plexus-online")) {  //一个拦截测试
//                    return myInterruptResponse(url);
//                }
//                return super.shouldInterceptRequest(view, url);
//            }
//            @Override
//            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                System.out.println("拦截at 4: "+request.getUrl().toString());
//                return super.shouldInterceptRequest(view, request);
//            }
//            private WebResourceResponse myInterruptResponse(String url){  //拦截回调后的一个具体处理子程序
//                String targetHtml="<p>have a test</p>";
//                InputStream targetContent=new ByteArrayInputStream(targetHtml.getBytes());
//                return new WebResourceResponse("text/html","utf-8",targetContent);
//            }

            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载:"+url);
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println("结束加载:"+url);
                //获取 webview 的html,以便提取信息
                if(url.contains("/Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key=")){
                    System.out.println("准备注入 Javascript......");
                    view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }else if(url.equals("https://www.plexus-online.com/Modules/SystemAdministration/Login/Index.aspx")){     //////////////////
                    System.out.println("onPageFinished: 登录成功，存Cookie,跳转首页：");
                    String cookieString= CookieManager.getInstance().getCookie(url_plex);
                    Pattern pattern=Pattern.compile("Session_Key=\\{(.{36})\\}");    //正则：Session_Key={} 在花括号以内的任意字符
                    Matcher m=pattern.matcher(cookieString);
                    if(m.find()){
                        session_ID=m.group(1);
                    }
                    //go to inter-plant
                    view.loadUrl(url_plex+"/"+session_ID+firstPage);
                }
            }
        });

        //设置WebChromeClient类  作用：辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override   //参看： https://www.cnblogs.com/ufreedom/p/4229590.html
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                System.out.println("创建新窗口：" + isDialog + ";" + isUserGesture + ";" + resultMsg);

                WebView newWebView = new WebView(getApplicationContext());//新创建一个webview
                LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.linearlayout2);
                linearLayout2.addView(newWebView);
                //initWebView(newWebView);//初始化webview
                findViewById(R.id.linearlayout1).setVisibility(View.GONE);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;//以下的操作应该就是让新的webview去加载对应的url等操作。
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
                // return super.onCreateWindow(view, isDialog, isUserGesture,resultMsg);
            }

//            @Override   onCloseWindow
//            public void onCloseWindow(WebView window) {//html中，用js调用.close(),会回调此函数
//                super.onCloseWindow(window);
//                System.out.println("关闭当前窗口");
//                if (newWebView != null) {
//                    fl_web_activity.removeView(newWebView);
//                }

            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里:"+title);
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


    @Override
    protected void onDestroy() {
        if( mWebView!=null) {
            // 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
            // destory()
            ViewParent parent = mWebView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(mWebView);
            }
            mWebView.stopLoading();
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            mWebView.getSettings().setJavaScriptEnabled(false);
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();
            mWebView.removeAllViews();
            mWebView.destroy();
            mWebView=null;
        }
        super.onDestroy();
    }
}
