package com.example.webview;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PlexInterActivity extends AppCompatActivity {
    WebView mWebview;
    TextView textview;
    WebSettings mWebSettings;
    String url_plex="https://www.plexus-online.com";
    String url_mobile="https://mobile.plexus-online.com";
    String session_ID="";
    boolean flag_changed=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plex_inter);

        mWebview=findViewById(R.id.webview);
        textview=findViewById(R.id.textView);

        init_view();
        mWebview.loadUrl(url_mobile);
    }

    private void init_view(){
        textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        textview.setHeight(100);

        mWebSettings=mWebview.getSettings();
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
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println("结束加载了");
                //获取 webview 的html
                if(url.contains("Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")){     //在Inter-Plant扫描界面
                        //执行javascript:
                        view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");

                }else if(url.contains("mobile.plexus-online.com")){                //登录中mobile中......
                    Uri uri=Uri.parse(url);
                    session_ID=uri.getPathSegments().get(0);
                    String cookieString=CookieManager.getInstance().getCookie(url_mobile);
                    //如果mobile界面登录成功
                    if((session_ID.length()==36)&&uri.getPath().contains("Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx")){
                        set_cookie(url_plex,cookieString);    //把mobile 的cookie转到 www
                        //cookieString=CookieManager.getInstance().getCookie("https://www.plexus-online.com");
                        //go to inter-plant
                        view.loadUrl(url_plex+"/"+session_ID+"/Interplant_Shipper/Interplant_Shipper_Form.asp?Do=Update&Interplant_Shipper_Key=460129");  //513993
                    }
                }else if(url.contains("https://www.plexus-online.com/modules/systemadministration/login/index.aspx")){  //帐号过期，会转到这里
                    mWebview.loadUrl("https://mobile.plexus-online.com/modules/systemadministration/login/index.aspx");
                }
                System.out.println(url);
            }
        });

        //设置WebChromeClient类  作用：辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等
        mWebview.setWebChromeClient(new WebChromeClient() {
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

    final class InJavaScriptLocalObj{   //有关获取webview 的html, 在onPageFinished中引用
        @JavascriptInterface
        public void getSource(final String html){
            System.out.println("嘿嘿 html=\n");
            mWebview.post(new Runnable() {
                @Override
                public void run() {
                    Document document=Jsoup.parse(html);
                    Element element_table=document.getElementById("MainContainerLoadingGridTable");
                    textview.setText("   信息栏： ");  //clear textview
                    textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    if(element_table!=null){
                        Elements elements=element_table.getElementsByTag("tbody").first().getElementsByTag("tr");
                        System.out.println("元素个数"+elements.size());

                        for(Element element:elements){
                            Elements eles=element.getElementsByTag("td");
                            System.out.println(eles.get(3).text());
                            if(eles.get(3).text().contains("EPC")){
                                textview.setBackgroundColor(getResources().getColor(R.color.colorRed));
                                textview.setHeight(textview.getHeight()+50);
                            }
                        }
                        //mWebview.loadDataWithBaseURL(url_plex,document.outerHtml(), "text/html", "utf-8", null);
                        //mWebview.reload();
                        textview.setText("   信息栏： "+(elements.size()-1));
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

    public void set_cookie(String url, String cookieString){
        //参考 https://blog.csdn.net/kelaker/article/details/82751287
        //CookieManager.getInstance().removeAllCookie();
        //CookieManager.getInstance().removeSessionCookie();

        String[] values=cookieString.split(";");
        for(String value:values) {
            CookieManager.getInstance().setCookie(url, value);
        }
        //同步 cookie修改
        CookieSyncManager.createInstance(this);
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        //System.out.println( CookieManager.getInstance().getCookie("https://"+host));
    }
}
