package com.philip.fifo;

import com.philip.plex_qa.Plex_login;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class set_part_shelf_life {
    //此程序 批量 激活 相关part no的shelf life功能。
    //part no放在以下的part_map数据表中
    //!!!!!此程序 2020-9-8，直接在 Plex_login中运行成功，但在这里并不能运行 报error:Dependent features configured but no package ID was set.
    public static void set_part_shelf_life(){
        String host="www.plexus-online.com";
        Map<String,String> part_map =new LinkedHashMap<>();   //这个保证顺序
        //数据源 要修改的Part列在这里
        {
            part_map.put("5-073-001-008","4315829");
            //part_map.put("5-100-001-700","5030786");
            //part_map.put("5-100-001-707","5030793");
            //part_map.put("5-100-001-708","5030794");
            //part_map.put("5-100-001-710","5030796");
            //part_map.put("5-100-001-711","5037897");
            //part_map.put("5-100-001-712","5040844");
            //part_map.put("5-100-001-812","4478365");
            //part_map.put("5-100-001-813","4478366");
            //part_map.put("5-100-001-814","4478367");
            //part_map.put("5-100-001-816","4478369");
            //part_map.put("5-100-001-817","4478370");
//            part_map.put("5-100-001-818","4478371");
//            part_map.put("5-100-001-819","4478372");
//            part_map.put("5-100-001-820","4478373");
//            part_map.put("5-100-001-822","4478375");
//            part_map.put("5-100-001-823","4478376");
//            part_map.put("5-100-001-824","4478377");
//            part_map.put("5-100-001-828","4478381");
//            part_map.put("5-100-001-829","4478382"); //
//            part_map.put("5-100-001-830","4478383");
//            part_map.put("5-100-001-839","4499984");
//            part_map.put("5-100-001-840","4499985");
//            part_map.put("5-100-001-841","4499986");
//            part_map.put("5-100-001-842","4499987");
//            part_map.put("5-100-001-843","4499988");
//            part_map.put("5-100-001-844","4499989");
//            part_map.put("5-100-001-845","4499990");
//            part_map.put("5-100-001-846","4499991");
//            part_map.put("5-100-001-847","4499992");
//            part_map.put("5-100-001-848","4499993");
//            part_map.put("5-100-001-849","4499994");
//            part_map.put("5-100-001-850","4499995");
//            part_map.put("5-100-001-851","4499996");
//            part_map.put("5-100-001-852","4499997");
//            part_map.put("5-100-001-853","4499998");
//            part_map.put("5-100-001-854","4499999");
//            part_map.put("5-100-001-855","4500000");
//            part_map.put("5-100-001-856","4500001");
//            part_map.put("5-100-001-857","4500002");
//            part_map.put("5-100-001-858","4500003");
//            part_map.put("5-100-001-859","4500004");
//            part_map.put("5-100-001-861","4500006");
//            part_map.put("5-100-001-862","4500007");
//            part_map.put("5-100-001-863","4500008");
//            part_map.put("5-100-001-864","4500009");
//            part_map.put("5-100-001-865","4500010");
//            part_map.put("5-100-001-866","4500011");
//            part_map.put("5-100-001-867","4500012");
//            part_map.put("5-100-001-868","4500013");
//            part_map.put("5-100-001-869","4500014");
//            part_map.put("5-100-001-870","4500015");
//            part_map.put("5-100-001-871","4500016");
//            part_map.put("5-100-001-872","4500017");
//            part_map.put("5-100-001-877","4500022");
//            part_map.put("5-100-001-878","4500023");
//            part_map.put("5-100-001-880","4500025");
//            part_map.put("5-100-001-882","4500027");
//            part_map.put("5-100-001-883","4500028");
//            part_map.put("5-100-001-884","4500029");
//            part_map.put("5-100-001-885","4500030");
//            part_map.put("5-100-001-886","4500031");
//            part_map.put("5-100-001-887","4549284");
//            part_map.put("5-100-001-888","4549286");
//            part_map.put("5-100-001-889","4552703");
//            part_map.put("5-100-001-890","4552704");
//            part_map.put("5-100-001-893","4563338");
//            part_map.put("5-100-001-896","4563341");
//            part_map.put("5-100-001-902","4565696");
//            part_map.put("5-100-001-903","4568872");
//            part_map.put("5-100-001-904","4572330");
//            part_map.put("5-100-001-905","4585867");
//            part_map.put("5-100-001-906","4585868");
//            part_map.put("5-100-001-907","4585869");
//            part_map.put("5-100-001-908","4585870");
//            part_map.put("5-100-001-909","4585871");
//            part_map.put("5-100-001-910","4585872");
//            part_map.put("5-100-001-911","4585873");
//            part_map.put("5-100-001-912","4585874");
//            part_map.put("5-100-001-913","4585875");
//            part_map.put("5-100-001-914","4585876");
//            part_map.put("5-100-001-915","4585877");
//            part_map.put("5-100-001-917","4605192");
//            part_map.put("5-100-001-918","4605193");
//            part_map.put("5-100-001-920","4605194");
//            part_map.put("5-100-001-922","4736537");
//            part_map.put("5-100-001-923","4736538");
//            part_map.put("5-100-001-924","4736539");
//            part_map.put("5-100-001-925","4736540");
//            part_map.put("5-100-001-926","4736543");
//            part_map.put("5-100-001-927","4736553");
//            part_map.put("5-100-001-928","4737167");
//            part_map.put("5-100-001-929","4737168");
//            part_map.put("5-100-001-930","4737169");
//            part_map.put("5-100-001-931","4740459");
//            part_map.put("5-100-001-932","4744880");
//            part_map.put("5-100-001-933","4744881");
//            part_map.put("5-100-001-934","4744882");
//            part_map.put("5-100-001-935","4744883");
//            part_map.put("5-100-001-936","4744884");
//            part_map.put("5-100-001-948","4808815");
//            part_map.put("5-100-001-950","4809306");
//            part_map.put("5-100-001-951","4809307");
//            part_map.put("5-100-001-956","4829019");
//            part_map.put("5-100-001-957","4829020");
//            part_map.put("5-100-001-958","4829021");
//            part_map.put("5-100-001-959","4829022");
//            part_map.put("5-100-001-960","4829024");
//            part_map.put("5-100-001-961","4829025");
//            part_map.put("5-100-001-962","4829026");
//            part_map.put("5-100-001-963","4829027");
//            part_map.put("5-100-001-964","4829028");
//            part_map.put("5-100-001-965","4829029");
//            part_map.put("5-100-001-966","4829030");
//            part_map.put("5-100-001-967","4829031");
//            part_map.put("5-100-001-971","4853094");
//            part_map.put("5-100-001-972","4853095");
//            part_map.put("5-100-001-973","4853096");
//            part_map.put("5-100-001-974","4859642");
//            part_map.put("5-100-001-975","5201150");
//            part_map.put("5-100-001-982","4878065");
//            part_map.put("5-100-001-984","4878066");
//            part_map.put("5-100-001-985","4878068");
//            part_map.put("5-100-001-986","4878067");
//            part_map.put("5-100-001-987","4961129");
//            part_map.put("5-100-001-989","5025319");
//            part_map.put("5-100-001-990","5034578");
//            part_map.put("5-101-001-714","5082339");
//            part_map.put("5-101-001-715","5082340");
//            part_map.put("5-101-001-716","5082341");
//            part_map.put("5-101-001-717","5082342");
//            part_map.put("5-101-001-723","5104687");
//            part_map.put("5-101-001-724","5104688");
//            part_map.put("5-180-001-979","5201741");
//            part_map.put("5-180-019-976","5201444");
//            part_map.put("5-180-019-977","5201445");
//            part_map.put("5-180-019-978","5201446");
//            part_map.put("5-190-001-622","4489761");
//            part_map.put("5-190-001-623","4489785");
//            part_map.put("5-190-001-624","4489793");
//            part_map.put("5-300-001-718","5082344");
//            part_map.put("5-300-001-719","5082345");
//            part_map.put("5-300-001-720","5103134");
//            part_map.put("5-300-001-728","5106465");
//            part_map.put("5-300-001-730","5114309");
//            part_map.put("5-300-001-731","5114310");
//            part_map.put("5-300-001-732","5114311");
//            part_map.put("5-300-001-733","5114312");
//            part_map.put("5-300-001-734","5114313");
//            part_map.put("5-300-001-735","5114314");
//            part_map.put("5-300-001-736","5114315");
//            part_map.put("5-320-001-740","5131063");
        }

        int k=0;
        for( Map.Entry<String,String> item:part_map.entrySet()){
            k++;System.out.println("第---"+k+"  "+item.getKey());
            //login to get cookies
            Plex_login login=new Plex_login(host);
            try {
                if (login.login("smmp.pwang","88776655","smmp")!=null) {//cookie在类login的public变量中
                    String session_key=login.cookies.get("Session_Key").toString();
                    session_key=session_key.substring(1,session_key.length()-1);  //去掉头尾的{}
                    System.out.println("\n "+session_key);

                    String pre_url="https://"+host+"/"+session_key;
                    String url=pre_url+"/Rendering_Engine/default.aspx?Request=Show&RequestData=SourceType(Screen)SourceKey(15562)ScreenParameters(Do%3dUpdate%7cPart_Key%3d5037897%7cPart_No%3d5-100-001-711%7cImage%3d..%2fimages%2fblankbar.gif%7cFrom_Part_Menu%3dTrue)";

                    String part_key=item.getValue(); String part_no=item.getKey();
                    url=url.replace("5037897",part_key);
                    url=url.replace("5-100-001-711",part_no);
                    Map<String,String> data=new LinkedHashMap<>();
                    data.put("__EVENTTARGET","Screen");data.put("__EVENTARGUMENT","Update");
                    data.put("__VIEWSTATE","/wEPDwUJOTg5NDMxNjIwZGSK//YD8K8d6i3pHog8e0fH85JbPQ==");data.put("__VIEWSTATEGENERATOR","2811E9B3");
                    data.put("hdnScreenTitle","Part Shelf Life");data.put("hdnFilterElementsKeyHandle","259995/\\Shelf_Life[]259996/\\Unit[]259997/\\Note[]259999/\\Shelf_Life_Type_Key[]260007/\\Supplier_Shelf_Life[]260008/\\Supplier_Shelf_Life_Unit");
                    data.put("ScreenParameters","Part_Key="+part_key+"|Part_Shelf_Life_Key=|");data.put("RequestKey","1");
                    data.put("Layout1$el_259995","365");data.put("Layout1$el_259996","day");
                    data.put("Layout1$el_259996_txt_val","");data.put("Layout1$el_259999","355");
                    data.put("panel_row_count_6","0");data.put("panel_row_count_7","0");
                    try {
                        System.out.println("    "+k+"--设置FIFO： "+part_key+"  "+part_no);
                        Connection.Response res=request_post(url,login.cookies,data);   ////////////////////
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("  1没有成功");
                }
            } catch (Exception e) {
                System.out.println("  2没有成功");
                e.printStackTrace();
            }
        }
    }
    public static Connection.Response request_post(String url, Map<String, String> cookies, Map<String,String> data) throws Exception{
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36");
        try {
            //trustEveryone();
            Connection con= Jsoup.connect(url);
            con.data(data);
            con.headers(headers);
            con.cookies(cookies);
            con.timeout(1000*20);
            con.ignoreHttpErrors(true);
            con.ignoreContentType(true);
            //con.proxy("127.0.0.1",8888);    //The settings is for Charles
            //System.setProperty("javax.net.ssl.trustStore", "D:\\Code\\Java\\plex.jks");

            Connection.Response res=con.method(Connection.Method.POST).execute();
            if (res.url().toString().toLowerCase().contains("systemadministration/login/index.asp")){
                throw new Exception("你空闲时间过长,需重新登陆了!");
            }
            if (res.url().toString().toLowerCase().contains("change_password")){
                throw new Exception("你的密码过期了,请在电脑上更新密码!");
            }
            if (res.url().toString().toLowerCase().contains("unavailable")){
                throw new Exception("系统正在维护!");
            }
            return res;

        }catch(SocketTimeoutException e){
            throw new Exception("Time Out!网络连接超时,请重试!");
        }catch(UnknownHostException e){
            throw new Exception("网络故障，找不到主机地址！");
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] argw){
        set_part_shelf_life();
    }
}
