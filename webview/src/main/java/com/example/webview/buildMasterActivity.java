package com.example.webview;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import org.json.JSONObject;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class buildMasterActivity extends AppCompatActivity {
    final int atPAGE=1,leftPAGE=2;
    WebView mWebview;
    EditText etMaster,etSerial;
    TextView tvMessage,tvList;
    Button button;
    LinearLayout newPage;
    String url_plex = "https://mobile.plexus-online.com";
    String first_page="/Mobile/Inventory/Mobile_Build_Master_Unit.asp?Node=530174"; //build new master label

    String session_ID = "";
    HashMap cookies;
    Handler mChildHandler;
    Thread childThread;
    //ConcurrentHashMap<String,ScanData> scandataMap=new ConcurrentHashMap();  //这个内容没有顺序
    //ConcurrentSkipListMap<String,ScanData> scandataMap=new ConcurrentSkipListMap(new Comparator() {
    TreeMap<String,ScanData> scandataMap=new TreeMap(new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return 1;  //去掉默认的排序功能
        }
    });



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
        mWebview.loadUrl(url_plex);  //开始登录Plex

        childThread=new ChildThread();
        childThread.start();
    }

    @SuppressLint("SetJavaScriptEnabled")  //标记，让不报错
    private void init_view() {
        mWebview = findViewById(R.id.webview);

        newPage=findViewById(R.id.newPage);
        newPage.setVisibility(View.GONE);

        etMaster=findViewById(R.id.etMaster);
        etSerial=findViewById(R.id.etSerial);
        //监听扫描数据的输入
        etSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serial = etSerial.getText().toString();
                String master = etMaster.getText().toString();
                if (serial.length() > 7 && master.length() > 5) {   //粗粗检查一下合法性  //////////////
                    ScanData scandata = new ScanData(new Date(), master);
                    //加入前，判断是否已经扫过了，在列表中，如在，需提醒一下
                    if(scandataMap.get(serial)!=null){
                        vibrate(200);
                        scandataMap.remove(serial);     //去掉旧数据
                    }
                    scandataMap.put(serial, scandata);  //加入新数据
                    refresh_list();
                    //System.out.println("嘿嘿：" + scandataMap);     ///////////////////////////
                    etSerial.requestFocus();     //条码框获得焦点
                    etSerial.setText("");    //清空条码框
                 }else if(serial.length()>0){   //发现清空后，还会激发一次click, 这次不报警
                    Toast.makeText(getApplicationContext(),"可能输入数据无效！",Toast.LENGTH_SHORT).show();
                    vibrate(500);      //输入无效，报警
                }
                etSerial.setSelection(etSerial.getText().length());  //如果有文字，光标移到未尾
            }
        });

        button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //System.out.println("按钮：------------");
                etSerial.performClick();     //通过执行EditText的onClick
            }
        });

        tvMessage = findViewById(R.id.tvMessage);
        tvMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvList=findViewById(R.id.tvList);
        tvList.setMovementMethod(ScrollingMovementMethod.getInstance());

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
                //System.out.println("转向旧: " + url);
                //登录成功后，保存cookie,跳转首页
                runOverrideUrlLoading(view, url);
                return true;
            }
            @Override    //新机器用
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                //System.out.println("转向新: " + url);
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
                    //子线程向主线程发消息
                    sendMessage(leftPAGE,null);
                    //go to next Activity
                    view.loadUrl(url_plex+"/"+session_ID+first_page);
                }else if(url.contains("/Mobile/Inventory/Mobile_Build_Master_Unit.asp?Node=")){
                    //点了Back退出，不在build master扫描界面
                    sendMessage(leftPAGE,null);
                    view.loadUrl(url);
                } else{
                    view.loadUrl(url);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri=request.getUrl();
                String url=uri.toString();
                //如查在Build Master扫描界面
                if(url.contains("/Mobile/Inventory/Mobile_Build_Master_Unit_Container.asp?MasterUnit=")){
                    //获得 master label号
                    String masterUnit=uri.getQueryParameter("MasterUnit");
                    //发消息，在UI显示 自定义的扫描页面
                    sendMessage(atPAGE,masterUnit);
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
                //System.out.println("开始加载:" + url);
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                //System.out.println("结束加载:" + url);
            }
        });
        //设置WebChromeClient类  作用：辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                //System.out.println("标题在这里:" + title);
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

    //点击让webview返回上一页面, webview不能退了才退出Activity
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

    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            //处理从子线程中来的消息
            if(msg.what==atPAGE){
                newPage.setVisibility(View.VISIBLE);
                etMaster.setText(msg.obj.toString());
                etSerial.setText("");   // clear条码框
                etSerial.requestFocus();  //条码框 获得焦点
            }else if(msg.what==leftPAGE){
                newPage.setVisibility(View.GONE);
                etSerial.setText("");
            }
        }};

    //向主线程发送消息
    private void sendMessage(int what,Object obj){
        Message message1 = Message.obtain();
        message1.what = what;  //1 means at newPage
        message1.obj = obj;
        mMainHandler.sendMessage(message1);
    }

    //自定义数据，用于保存 scan data
    private class ScanData{
        public Date date;
        String master;
        public ScanData(Date date,String master){
            this.date=date;this.master=master;
        }
    }

    private class ScanData1{
        public String serial;
        public String master;
        public Date date;
        public int count;   //记录入队列的次数
        public ScanData1(String serial,String master,Date date,int count) {
            this.serial = serial;
            this.master = master;
            this.date = date;
            this.count = count;
        }
        @Override
        public boolean equals(Object obj){
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(!(obj instanceof ScanData1))
                return false;
            ScanData1 other = (ScanData1)obj;
            if(this.serial == null){
                if(other.serial !=null)
                    return false;
            }else if(this.serial.equals(other.serial))
                return true;
            return false;
        }
    }

    //显示 扫描任务清单
    private void refresh_list(){
        String strlist="";
        int count=scandataMap.size();
        for(Map.Entry<String,ScanData> entry: scandataMap.entrySet()){
            String date=Utils.getMonthTime(entry.getValue().date);
            strlist+=String.format("%s  %s  时间：%s  %s\n",entry.getKey(),entry.getValue().master,date,count);
        }
        tvList.setText(strlist);
    }

    class ChildThread extends Thread{
        private static final String Child_TAG="ChildThread";
        boolean stopChild;   //子线程 停不停的标记

        public ChildThread(){
            this.stopChild=false;
        }

        public void setStop(boolean stopChild){
            this.stopChild=stopChild;
        }

        @Override
        public void run(){
            this.setName("ChildThread");
            while(!stopChild){
                try {
                    if(scandataMap.size()>0){
                        System.out.println("调试："+scandataMap);
                        Set<String> list=scandataMap.keySet();
                        for(String serial:list) {
                            Thread.sleep(1000);
                            String master=scandataMap.get(serial).master;
                            //String serial=entry.getKey();
                            //scandataMap.remove(serial);     //这里从队列中删除数据，如果处理失败，再加入队列
                            System.out.println("调试2："+scandataMap.size());
                            //refresh_list();         //需在UI中刷新
                            if(session_ID.length()>0) {  //session_ID是登录成功后才有值,肯定在scandataMap之前获得数据
                                //操作 Build MasterUnit
                                boolean success=masterUnitHandler(session_ID, master, serial);
                                if(!success){
                                    //不成功，加进行，再来再处理
                                    scandataMap.put(serial,new ScanData(new Date(),master));
                                    //refresh_list();  //需在UI中刷新
                                }
                            }
                        }
                    }
                    Thread.sleep(5000);
                    System.out.println("Child Thread is running."+session_ID);   ////////////////////////////////
                } catch (Exception e) {
                    System.out.println("子线程处理Exception.");
                    e.printStackTrace();
                }
            }
        }

        private boolean masterUnitHandler(String session_ID,String master,String serial) throws Exception {
            String url="https://www.plexus-online.com/"+session_ID+"/Modules/Inventory/MasterUnits/MasterUnitHandler.ashx?ApplicationKey=166143";
            String jsonString;
            Connection.Response res;

            //查有关master label的基本数据
            HashMap<String,String> data=new HashMap<>();
            data.put("Action","GetMasterUnit");data.put("MasterUnitNo",master);
            res=Utils.request_post(url, cookies, data);
            jsonString=res.body();
            System.out.println("读master label基本数据：");
            Map<String,Object> objectMap= JSON.parseObject(jsonString,Map.class);
            String strMasterUnitKey=objectMap.get("MasterUnitKey").toString();
            String strMasterUnitNo=objectMap.get("MasterUnitNo").toString();
            String strLocation=objectMap.get("Location").toString();
            String strMasterUnitTypeKey=objectMap.get("MasterUnitTypeKey").toString();
            boolean Active=(boolean)objectMap.get("Active");
            System.out.println("嘿嘿Json1:"+strMasterUnitKey);

            //如果 StrMasterUnitKey为空，则报错      /////////////////////
            if(strMasterUnitKey.length()==0){
                return false;
            }

            //查有关箱号是否在master label中
            data.clear();objectMap.clear();
            data.put("Action","ValidateContainer");data.put("SerialNo",serial);
            res=Utils.request_post(url, cookies, data);
            jsonString=res.body();
            System.out.println("验证：");
            System.out.println(jsonString);
            objectMap= JSON.parseObject(jsonString,Map.class);
            String strSerialNo=objectMap.get("SerialNo").toString();
            boolean IsValid=(boolean)objectMap.get("IsValid");
            System.out.println("嘿嘿Json2:"+strSerialNo);

            //如果IsValid是真，报错      //////////////////////////
            if(IsValid){
                return false;
            }

            //把相关箱号加入到指定的master label中
            data.clear();objectMap.clear();
            data.put("Action","BuildMasterUnit");data.put("MasterUnitKey",strMasterUnitKey);data.put("MasterUnitNo",strMasterUnitNo);
            data.put("MasterUnitTypeKey",strMasterUnitTypeKey);data.put("Location",strLocation);data.put("SerialNo",serial);
            res=Utils.request_post(url,cookies,data);
            jsonString=res.body();
            System.out.println("执行：");
            objectMap= JSON.parseObject(jsonString,Map.class);
            boolean isSuccess =(boolean)objectMap.get("IsValid");
            String strContainerCount="",strMessage="";
            System.out.println("嘿嘿Json3:"+isSuccess);
            if(isSuccess){
                strContainerCount=objectMap.get("ContainerCount").toString();
                //这里需发成功消息     /////////////////////////////////////
                return true;
            }else{
                strMessage=objectMap.get("Message").toString();
                //这里需发失败消息//////////////////////////////////////////
                return false;

            }
        }
    }
}
