package com.example.webview;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class PlexInterActivity extends AppCompatActivity {
    WebView mWebview;
    TextView textview;
    WebSettings mWebSettings;
    String url_plex="https://www.plexus-online.com";
    String url_mobile="https://mobile.plexus-online.com"; //d056f1af-eade-4483-a749-c8d3e1280a0e/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx?Mobile=1";
    String firstPage="/Interplant_Shipper/Interplant_Shipper_Form.asp?Do=Update&Interplant_Shipper_Key=513993";  //460129
    String session_ID="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plex_inter);

        mWebview=findViewById(R.id.webview);
        textview=findViewById(R.id.textView);

        init_view();
        mWebview.loadUrl(url_mobile);
    }

    @SuppressLint("SetJavaScriptEnabled")         //不报错
    private void init_view(){
        textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        textview.setHeight(100);

        mWebSettings = mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSaveFormData(true);         //看一看有无作用？
        mWebSettings.setBuiltInZoomControls(true);  // 可缩放
        //mWebSettings.setBlockNetworkImage(true);  //  不加载图片，快些
        mWebSettings.setSavePassword(true);

        //用于 运行jascript, 获取 webview的当前html
        mWebview.addJavascriptInterface(new InJavaScriptLocalObj(),"java_obj");
        mWebview.setWebViewClient(new WebViewClient()); //此行代码可以保证JavaScript的Alert弹窗正常弹出

        //设置WebViewClient类  作用：处理各种通知 & 请求事件
        mWebview.setWebViewClient(new WebViewClient() {
            //设置不用系统浏览器打开,直接显示在当前Webview
            @Override   //老机器用
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                System.out.println("拦截 旧: "+url);
                runOverrideUrlLoading(view,url);
                return true;
            }
            @Override    //新机器用
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                String url=request.getUrl().toString();
                System.out.println("拦截 新: "+url);
                runOverrideUrlLoading(view,url);
                return true;
            }
            private void runOverrideUrlLoading(WebView view,String url){
                if(url.contains("/Modules/SystemAdministration/MenuSystem/MenuCustomer.aspx")){   //如mobile界面登录成功
                    System.out.println("登录成功，存Cookie,跳转首页：");
                    Uri uri=Uri.parse(url);
                    session_ID=uri.getPathSegments().get(0);
                    String cookieString=CookieManager.getInstance().getCookie(url_mobile);
                    //登录成功后，把mobile 的cookie转给 www
                    set_cookie(url_plex,cookieString);
                    //go to inter-plant
                    view.loadUrl(url_plex+"/"+session_ID+firstPage);
                }else{
                    view.loadUrl(url);
                }
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
                //获取 webview 的html
                if(url.contains("/Interplant_Shipper/Interplant_Shipper_Modify.asp?Do=Load_Container&Interplant_Shipper_Key=513993&Serial_No=123456")){
                    view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }

//                if(url.contains("Interplant_Shipper/Interplant_Shipper_Form.asp?Mode=Containers&Do=Update&Interplant_Shipper_Key")){  //如在Inter-Plant扫描加载界面
//                        //注入javascript，然后页面刷新
//                        view.loadUrl("javascript:window.java_obj.getSource('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
//                }else if(url.contains("https://www.plexus-online.com/modules/systemadministration/login/index.aspx")){                //如果帐号过期，会转到这里
//                    mWebview.loadUrl("https://mobile.plexus-online.com/modules/systemadministration/login/index.aspx");
//                }
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

    final class InJavaScriptLocalObj{   //获取webview 的html并jsoup解读, 在onPageFinished中注入javascript
        @JavascriptInterface
        public void getSource(final String html){
            //System.out.println("内容:\n"+html);
            mWebview.post(new Runnable() {
                @Override
                public void run() {
                    //mWebview.loadDataWithBaseURL(url_plex,html, "text/html", "utf-8", null);
                    Document document=Jsoup.parse(html);
                    Element element_table=document.getElementById("MainContainerLoadingGridTable");
                    Boolean flag_error=false;
                    //init info at textview
                    textview.setText("   信息栏： ");  //clear textview
                    textview.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    if(element_table!=null){          //如果有 加载表格，处理表格
                        Elements elements=element_table.getElementsByTag("tbody").first().getElementsByTag("tr");

                        for(Element element:elements){
                            Elements eles=element.getElementsByTag("td");
                            //System.out.println(eles.get(3).text());
                            if(eles.get(3).text().contains("EPC")){
                                //warning message at textview
                                textview.setBackgroundColor(getResources().getColor(R.color.colorRed));
                                textview.setHeight(textview.getHeight());
                                textview.setText("   信息栏： "+eles.get(1).text());
                                vibrate(500);
                                flag_error=true;
                            }
                        }
                        //mWebview.loadDataWithBaseURL(url_plex,document.outerHtml(), "text/html", "utf-8", null);
                        //mWebview.reload();
                        if(!flag_error){
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

    public void set_cookie(String url, String cookieString){
        //参考 https://blog.csdn.net/kelaker/article/details/82751287
        //CookieManager.getInstance().removeAllCookie();
        //CookieManager.getInstance().removeSessionCookie();

        String[] values=cookieString.split(";");
        for(String value:values) {
            CookieManager.getInstance().setCookie(url, value);
        }
        //同步 cookie修改
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.createInstance(this);
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        //System.out.println( CookieManager.getInstance().getCookie("https://"+host));
    }

    public void vibrate(int time){
        Vibrator vibrator=(Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }
}
