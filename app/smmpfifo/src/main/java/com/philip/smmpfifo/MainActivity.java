package com.philip.smmpfifo;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.webview.myDBHelper;

import org.jsoup.Connection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

public class MainActivity extends AppCompatActivity {
    Context mActvity;
    Menu mMenu;
    final int atPAGE=1,leftPAGE=2,REFRESH=3,MSG=4,STOP=5,bigINFO=6;
    RadioButton rdOld,rdNew;  //想用于切换界面
    LinearLayout layoutUpper;
    WebView mWebview;
    EditText etMaster,etSerial;
    TextView tvMessage,tvList,tvBackPlex,tvMasterUnitMessage;
    Button button;
    LinearLayout newPage;
    String url_plex = "https://mobile.plexus-online.com";
    String first_page="/Mobile/Inventory/Mobile_Build_Master_Unit.asp?Node=530174"; //build new master label

    String session_ID = "";
    HashMap cookies;
    ChildThread childThread;

    //queue用于暂存数据
    LinkedBlockingDeque<buildMasterActivity.ScanData1> queue=new LinkedBlockingDeque<buildMasterActivity.ScanData1>();
    //sqlite用于长存数据
    myDBHelper mydbhelper;
    private TextToSpeech textToSpeech = null;//创建自带语音对象
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //打开数据库
        mydbhelper=myDBHelper.getInstance(this,1);
        mydbhelper.openWriteLink();

//        mydbhelper.delete("serial=?",new String[]{"smmp123456"});    //clear database
//        //增加一行数据
//        buildMasterActivity2.ScanData1 scandata=new buildMasterActivity2.ScanData1("smmp1234587","m12121",null,0);
//        mydbhelper.insert(scandata);
        mydbhelper.query();
//        mydbhelper.closeLink();   /////////////////////////////////////


        //disable the strict polity that do not allows main thread network access
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        init();         //初始化 view
        init_webview();
        initTTS();          //初始化 语音

        mWebview.loadUrl(url_plex);  //开始登录Plex
    }

    private void init() {
        mActvity=this;
        layoutUpper=findViewById(R.id.layout_upper);  //原始Plex界面
        //缩小下边的Info区
        layoutUpper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1.2f));
        mWebview = findViewById(R.id.webview);

        newPage=findViewById(R.id.newPage); //新的扫描
        newPage.setVisibility(View.GONE);   //hide 新界面

        etMaster=findViewById(R.id.etMaster);
        etSerial=findViewById(R.id.etSerial);
        //监听扫描数据的输入
        etSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String say_word="";
                String serial = etSerial.getText().toString();
                String master = etMaster.getText().toString();
                serial=Utils.refine_label(serial);                  // 规范化读取的 bacode, 如barcode无效，将会是空""
                if ((serial.length()>=9) && master.length() == 7) {   //粗粗检查一下合法性
                    //加入前，判断是否已经扫过了，如在列表中发现重复，提醒，并删除重复
                    buildMasterActivity.ScanData1 scandata1= new buildMasterActivity.ScanData1(serial,master,null,0);  //date取数据库默认值
                    if(mydbhelper.contains(serial)){
                        say_word=",重复了!";  //说 重复了
                        vibrate(400);
                        mydbhelper.delete("serial=?",new String[]{serial});
                    }
                    //加入新数据
                    mydbhelper.insert(scandata1);
                    //queue.offer(scandata1);    //poll(出)与offer(入)相互对应, 满会返回false   poll -->【若队列为空，返回null】
                    tvMasterUnitMessage.setText("你的最后成功一扫：" + serial);   // 显示最新的扫描结果
                    //语音提示
                    say_word=serial.substring(serial.length()-4,serial.length())+say_word;
                    say(say_word);   //读出最后4个数字
                    refresh_list("加入新条码");
                    //System.out.println("嘿嘿：" + scandataMap);     ///////////////////////////
                    etSerial.requestFocus();     //条码框获得焦点
                }else if(serial.length()>0){     //另注：这个会激发两次click, 后一次serial为空，这次不报警
                    tvMasterUnitMessage.setText(serial+"扫描错误！");   // 显示最新的扫描结果
                    say(serial+"错误！");
                    Toast.makeText(getApplicationContext(),"可能输入数据无效！"+serial,Toast.LENGTH_SHORT).show();
                    vibrate(500);      //输入无效，报警
                }
                etSerial.setText("");    //清空条码框
                etSerial.setSelection(etSerial.getText().length());  //如果有文字，光标移到未尾
            }
        });

        button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSerial.performClick();     //通过执行EditText的onClick
            }
        });

        tvMasterUnitMessage=findViewById(R.id.tvMasterUnitMessage);
        tvMessage = findViewById(R.id.tvMessage);
        tvMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvList=findViewById(R.id.tvList);
        tvList.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvBackPlex=findViewById(R.id.tvBackPlex);
        tvBackPlex.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        tvBackPlex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebview.setVisibility(View.VISIBLE);  //显原始Plex界面
                newPage.setVisibility(View.GONE);      //hide新界面
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")  //标记，让不报错
    private void init_webview(){
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

                    refresh_list("查看数据库！");
                    childThread=new ChildThread();
                    childThread.start();   //启动子线程开始上传扫描数据
                    //go to next Activity
                    view.loadUrl(url_plex+"/"+session_ID+first_page);
                    //发消息，放大 信息栏
                    sendMessage(bigINFO,null);
                }else if(url.contains("/Mobile/Inventory/Mobile_Build_Master_Unit.asp?Node=")){
                    //点了Back退出，离开build master扫描界面
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
            private WebResourceResponse myInterPlant(String url){   //这个没用
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

    @Override   //显示 选项菜单
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.option_menu, menu);
        mMenu=menu;
        return true;
    }
    @Override  //有关 选项菜单
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.navigation_stop_child:
                if(childThread!=null && childThread.isAlive()) {
                    //childThread.interrupt();   //发现用Interrupt不好控制退出点，可能使Jsoup中断，在随机位置返回Null
                    childThread.flag=false;      //标志位控制 子线程的开关（不会立即关，在下一个循环时关）
                    sendMessage(STOP,"准备手工中断上传......");
                }
                if(childThread!=null && !childThread.isAlive()){      //确认 子线程已关闭后，才同步菜单项
                    item.setEnabled(false);
                    mMenu.findItem(R.id.navigation_start_child).setEnabled(true);  //激活 启动线程 菜单项
                    childThread=null;
                    sendMessage(STOP,"已停止上传!\n");
                }
                break;
            case R.id.navigation_start_child:
                if(childThread==null) {
                    childThread=new buildMasterActivity2.ChildThread();
                    childThread.start();
                    item.setEnabled(false);
                    mMenu.findItem(R.id.navigation_stop_child).setEnabled(true);
                    sendMessage(MSG,"已启动了上传。\n");
                    //System.out.println(childThread.getState());   //terminated or runable
                }
                break;
            case R.id.navigation_end:
                //System.exit(0);    //比较下 finish, activity.onDestroy
                //onDestroy();
                finish();   //测试表明，这里finish, 也执行 onDestory
                break;
            case R.id.navigation_clearDB1:
                //准备清除数据
                item.setEnabled(false);   //关step 1
                mMenu.findItem(R.id.navigation_clearDB2).setEnabled(true);//打开step 2
                break;
            case R.id.navigation_clearDB2:
                //清除数据
                mydbhelper.delete(null,null);
                //queue.clear();
                refresh_list("清理数据");
                item.setEnabled(false);  //关step 2
                mMenu.findItem(R.id.navigation_clearDB1).setEnabled(true); //打开step 1
                break;
            default:
        }
        return true;
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

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        //销毁Webview
        if (mWebview != null) {
            mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebview.clearHistory();

            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.destroy();
            mWebview = null;
        }
        //childThread.interrupt();  //中断子线程：子线程会产生interrupt exception,跳出loop
        if(childThread!=null){
            childThread.flag=false;
        }
        //关闭数据库
        mydbhelper.closeLink();

        super.onDestroy();
    }

    public void vibrate(int time) {
        Vibrator vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    //主线程处理消息
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            //处理从子线程中来的消息
            if(msg.what==atPAGE){    //显示新页面，去掉老Plex界面
                newPage.setVisibility(View.VISIBLE);
                mWebview.setVisibility(View.GONE);
                etMaster.setText(msg.obj.toString());
                etSerial.setText("");   // clear条码框
                etSerial.requestFocus();  //条码框 获得焦点
                //扩大下边的info区
                layoutUpper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,0.6f));
            }else if(msg.what==leftPAGE){   //离开新页面
                newPage.setVisibility(View.GONE);
                mWebview.setVisibility(View.VISIBLE);
                etSerial.setText("");
                //缩小下边的info区
                layoutUpper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1.2f));
            }else if(msg.what==REFRESH){
                if(msg.obj!=null){
                    refresh_list(msg.obj.toString());
                }else{
                    refresh_list("");
                }
            }else if(msg.what==STOP){
                if(msg.obj!=null){
                    String temp="\n"+tvList.getText().toString();
                    tvList.setText(msg.obj.toString()+temp);
                }
            }else if(msg.what==MSG){          // 显示操作信息
                String message=tvMessage.getText().toString();
                if(message.length()>2000){    // 控制文字长度
                    message.substring(0,2000);
                }
                tvMessage.setText(msg.obj.toString()+"\n"+message);
            }else if(msg.what==bigINFO){
                //扩大下方的info区
                layoutUpper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,0.6f));
            }
        }};

    //向主线程发送消息
    private void sendMessage(int what,Object obj){
        Message message1 = Message.obtain();
        message1.what = what;  //1 means at newPage
        message1.obj = obj;
        mMainHandler.sendMessage(message1);
    }

    //自定义数据类型，用于保存 scan data
    public static class ScanData1{
        public String serial;
        public String master;
        public Date date;
        public Integer count;   //记录入队列的次数
        public ScanData1(String serial,String master,Date date,Integer count) {
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
            if(!(obj instanceof buildMasterActivity.ScanData1))
                return false;
            buildMasterActivity.ScanData1 other = (buildMasterActivity.ScanData1)obj;
            if(this.serial == null){
                if(other.serial !=null)
                    return false;
            }else if(this.serial.equals(other.serial))
                return true;
            return false;
        }
        public String toString(){
            if(this.serial==null)
                return "no data";
            String date=Utils.getMonthTime(this.date);
            return String.format("%s-->%s 时间:%s 记数:%s\n",this.serial,this.master,date,this.count);
        }
    }

    //清队列，查数据库，更新队列，更新显示tvlist(扫描任务清单)
    private void refresh_list(String head){
        System.out.println("刷新refresh_list!");
        queue.clear();    //先清空, 接着填充
        ArrayList<buildMasterActivity.ScanData1> list=new ArrayList<buildMasterActivity.ScanData1>();
        list=mydbhelper.query();
        if(!list.isEmpty()){
            queue.addAll(list);   //再填充  ///////////需检查 数据集是否为空吗？
        }
        int count=queue.size();
        String strlist=head+" 任务数："+count+"\n";

        //遍历队列
        for (buildMasterActivity.ScanData1 scandata1 : queue) {
            strlist+=scandata1.toString();
            if(scandata1.count>999){  //上传记数1000以上，会被停传
                strlist+="停";
            }
            strlist+="\n";  //换行
        }  //显示出来
        tvList.setText(strlist);
    }

    class ChildThread extends Thread{
        volatile boolean flag=true;
        int sleepTime=2000;  //正常时，每次上传间隔时间 (这个2000不起作用，后面有改)
        @Override
        public void run(){
            while(flag){     //子程序可被Interrupt停止
                buildMasterActivity.ScanData1 scanData1=queue.peek(); //poll(出)与offer(入)相互对应, 满会返回false 另：peek不会去掉队首元素

                if(scanData1!=null){              //poll(出)：若队列为空，返回null
                    //System.out.println("子线程发现数据："+scanData1.toString());
                    String serial=scanData1.serial;
                    String master=scanData1.master;
                    int success=0;  //初始化 success 结果状态
                    if(scanData1.count<1000){  //上传次数太多的，算无效条码，不再上传
                        try {
                            sleepTime=2000;
                            sendMessage(REFRESH,"干活........");
                            success = masterUnitHandler(session_ID, master, serial); //返回0：失败, 1：成功 或 1000:条码失效
                        }catch(Exception e){
                            e.printStackTrace();
                            sendMessage(MSG,e.getMessage());     //发出 出错信息
                        }
                    }else{
                        success=999;     //999代表，发现条码已设置停止上传了，本次没有上传
                        sleepTime=2000;  //如果没有上传，等下一个的间隔时间可变短些（但太短，屏幕会跳得厉害）
                    }
                    //已处理完，就去掉这个 serial
                    mydbhelper.delete("serial=?",new String[]{serial});    //删除本次尝试过上传的条码
                    //queue.poll();     //去掉数据
                    if(success==0){     //0代表上传不成功  1 代表上传成功
                        scanData1.count++;   //数据的失败记录加1
                        mydbhelper.insert(scanData1);  //加数据在末尾，准备重传
                        //queue.poll(); queue.offer(scanData1);
                    }else if(success==1000){          //1000代表，子程序发现条码无效
                        scanData1.count=2000;         //做标记，下次不再上传
                        mydbhelper.insert(scanData1);  //加数据在末尾, 但因count大，不会重传
                    }else if(success==999){
                        mydbhelper.insert(scanData1);  //本次没有上传，只是记录在数据库末尾
                    }
                }
                try {
                    sendMessage(REFRESH,"休息中......"+sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    sendMessage(MSG,"子线程被Interrupt！");   //////这里要改菜单项
                    break;      //停止本线程
                }
            }
            if(flag==false){
                System.out.println("子线程退出!");
            }
        }

        private int masterUnitHandler(String session_ID,String master,String serial) throws Exception {
            String url="https://www.plexus-online.com/"+session_ID+"/Modules/Inventory/MasterUnits/MasterUnitHandler.ashx?ApplicationKey=166143";
            String jsonString;
            Connection.Response res;
            String strMasterUnitKey,strMasterUnitNo,strLocation,strMasterUnitTypeKey;

            //查有关master label的基本数据
            HashMap<String,String> data=new HashMap<>();
            data.put("Action","GetMasterUnit");data.put("MasterUnitNo",master);
            res=Utils.request_post(url, cookies, data);
            jsonString=res.body();
            System.out.println("读master label基本数据：");
            Map<String,Object> objectMap= JSON.parseObject(jsonString,Map.class);
            if(objectMap.get("MasterUnitKey")!=null){
                strMasterUnitKey=objectMap.get("MasterUnitKey").toString();
                strMasterUnitNo=objectMap.get("MasterUnitNo").toString();
                strLocation=objectMap.get("Location").toString();
                strMasterUnitTypeKey=objectMap.get("MasterUnitTypeKey").toString();
                boolean Active=(boolean)objectMap.get("Active");
                System.out.println("嘿嘿Json1:"+strMasterUnitNo);
            }else{  //如果 StrMasterUnitKey为空，则报错
                sendMessage(MSG,master+"出错：主条码号有问题！\n");
                vibrate(300);
                //不成功，返回0
                return 0;
            }
            { //查有关箱号是否在master label中
                data.clear();
                objectMap.clear();
                data.put("Action", "ValidateContainer");
                data.put("SerialNo", serial);
                res = Utils.request_post(url, cookies, data);
                jsonString = res.body();
                //System.out.println("验证：");System.out.println(jsonString);
                objectMap = JSON.parseObject(jsonString, Map.class);

                //以下分析 查询结果：
                if (objectMap.get("MasterUnitNo") != null) {    //masterUnit不一定会有      //发现这里会抛出 null 异常, 且子程序被手工中断时会掉数据  ////////////////////////////////////
                    String strMasterUnitNo_raw = objectMap.get("MasterUnitNo").toString();
                    sendMessage(MSG, String.format("%s在已主条码%s中。\n", serial, strMasterUnitNo_raw));
                }

                boolean IsValid = (boolean) objectMap.get("IsValid");
                //System.out.println("嘿嘿Json2:" + serial);
                //如果IsValid不是真，报错
                if (!IsValid) {
                    vibrate(300);
                    String strMessage = objectMap.get("Message").toString();
                    sendMessage(MSG, serial + "出错：条码号有问题！\n      " + strMessage + "\n");
                    return 1000;  //发现条码无效，返回1000
                }
            }
            //把相关箱号加入到指定的master label中
            data.clear();objectMap.clear();
            data.put("Action","BuildMasterUnit");data.put("MasterUnitKey",strMasterUnitKey);data.put("MasterUnitNo",strMasterUnitNo);
            data.put("MasterUnitTypeKey",strMasterUnitTypeKey);data.put("Location",strLocation);data.put("SerialNo",serial);
            res=Utils.request_post(url,cookies,data);
            jsonString=res.body();
            System.out.println("执行：");
            objectMap= JSON.parseObject(jsonString,Map.class);
            //以下处理上传结果 objectMap
            boolean isSuccess =(boolean)objectMap.get("IsValid"); //发现这里会抛出 null 异常, 子程序被手工interrupt时会掉数据
            System.out.println("嘿嘿Json3:"+isSuccess);
            if(isSuccess){
                String strContainerCount=objectMap.get("ContainerCount").toString();
                sendMessage(MSG,"成功："+serial+"成功加入"+master+".\n            主码箱数："+strContainerCount+"  库位："+strLocation+"\n");
                return 1;   //成功，返回1
            }
            else{
                String strMessage=objectMap.get("Message").toString();
                sendMessage(MSG,"失败："+serial+"加入"+master+"失败。 "+Utils.getMonthTime(new Date())+"\n            "+strMessage+"\n");
                if (strMessage.contains("not a valid container")||strMessage.contains("cannot be added")){
                    return 1000;  //发现条码无效，返回1000
                }else{
                    return 0;    //不成功，返回0}
                }
            }
        }
    }

    private void initTTS() {
        //实例化自带语音对象
        textToSpeech = new TextToSpeech(mActvity, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == textToSpeech.SUCCESS) {

                    textToSpeech.setPitch(1.0f);//方法用来控制音调
                    textToSpeech.setSpeechRate(1.2f);//用来控制语速

                    //判断是否支持下面两种语言
                    //int result1 = textToSpeech.setLanguage(Locale.US);
                    int result1 = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);

                    if (result1 == TextToSpeech.LANG_MISSING_DATA || result1 == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(mActvity, "语音包丢失或语音不支持", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mActvity, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void say(String data) {
        // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
        textToSpeech.setPitch(1.0f);
        // 设置语速
        textToSpeech.setSpeechRate(1.2f);
        data=data.replace(""," ");   //加分隔符，以便读单数字
        textToSpeech.speak(data,//输入中文，若不支持的设备则不会读出来
                TextToSpeech.QUEUE_FLUSH, null,null);
    }
}