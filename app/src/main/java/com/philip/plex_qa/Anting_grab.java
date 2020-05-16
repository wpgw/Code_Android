package com.philip.plex_qa;

import java.text.SimpleDateFormat;
import java.util.Calendar;
//import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
//import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Anting_grab {
	public Map<String,String> headers =new HashMap<>();

	// ���µ�ַ �������ź�����֧��
	//String url_pay="http://www.antingwentimap.cn//pay/stadiumpayorder?urid=1246&paymethod=4&paytype=2&orderid=sps201903092155519892655549951&returnurl=%2Forder&ordertype=1&latitude=31.309418&longitude=121.21487&_=1552139942872";
	//http://h5.antingwentimap.cn/order/payment?orderno=sps201905101248029669950999749&ordertype=3
	
	//���캯��
	public Anting_grab() {  
		headers.put("User-Agent","Mozilla/5.0 (Linux; Android 4.4.4; SAMSUNG-SM-N900A Build/tt) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Safari/537.36 MicroMessenger/6.6.3.1260(0x26060336) NetType/WIFI Language/zh_CN");
		headers.put("Referer","http://h5.antingwentimap.cn/venue/bookview?type=1&stid=23&itemtitleid=25&subitemid=170&stname=%25E5%25AE%2589%25E4%25BA%25AD%25E6%2596%2587%25E4%25BD%2593%25E6%25B4%25BB%25E5%258A%25A8%25E4%25B8%25AD%25E5%25BF%2583&itemtitle=%25E7%25BE%25BD%25E6%25AF%259B%25E7%2590%2583");		
	}
	
	//��ü��������� yyyy-MM-dd
	public String getdateAfter(int days) {
		String tdate;
		Calendar cal=Calendar.getInstance();
		SimpleDateFormat datetemple=new SimpleDateFormat("yyyy-MM-dd");	
		cal.add(Calendar.DATE, days);   //Instruction at site: www.runoob.com/java/java-date-time.html
		tdate=datetemple.format(cal.getTime());
		return tdate;
	}
	
	private String getURL(int field_id,int time_id) {
		//urid=1387  ָ17771456517�ʺ�    1246ָ13986065186�ʺ�  Ҫ�������ط�
		// ���µ�ַ ��������������
		String url_base_pre= "http://h5.antingwentimap.cn/pay/stadiumprepayorder?urid=1246&stid=23&stname=%E5%AE%89%E4%BA%AD%E6%96%87%E4%BD%93%E6%B4%BB%E5%8A%A8%E4%B8%AD%E5%BF%83&itemtitleid=25&itemtitle=%E7%BE%BD%E6%AF%9B%E7%90%83&ordertype=1&";
		String url1_pre= "subiteminfos=%5B%7B%22orderdate%22%3A%222019-01-11%22%2C%22subid%22%3A%22176%22%2C%22subname%22%3A%22%E7%BE%BD%E6%AF%9B%E7%90%837%22%2C%22timeid%22%3A%2239%22%2C%22";
		String url2_pre= "num%22%3A1%7D%5D&passwordsalt=5y2h7730y0nhqjkh&latitude=31.309418&longitude=121.21487&_=1546739274770";
		
		// change date
		url1_pre=url1_pre.replace("orderdate%22%3A%222019-01-11", "orderdate%22%3A%22" + getdateAfter(6));  //������
		url1_pre=url1_pre.replace("subid%22%3A%22176", "subid%22%3A%22" + (field_id+169));   //�����غ�
		url1_pre=url1_pre.replace("timeid%22%3A%2239", "timeid%22%3A%22" + (2*time_id+1));   //��ʱ��
		//�ĳ��ص���������,���򷵻صĳ�����������ȷ
		url1_pre=url1_pre.replace("subname%22%3A%22%E7%BE%BD%E6%AF%9B%E7%90%837","subname%22%3A%22%E7%BE%BD%E6%AF%9B%E7%90%83"+field_id);
        return url_base_pre+url1_pre+url2_pre;
	}
	
	public void pay(String orderno){
		// ���µ�ַ �������ź�����֧��
		String url_pay="http://www.antingwentimap.cn//pay/stadiumpayorder?urid=1246&paymethod=4&paytype=2&orderid=sps201903092155519892655549951&returnurl=%2Forder&ordertype=1&latitude=31.309418&longitude=121.21487&_=1552139942872";
		// http://h5.antingwentimap.cn/order/payment?orderno=sps201905101248029669950999749&ordertype=3
		
		url_pay=url_pay.replace("sps201903092155519892655549951", orderno); 
		
		try {	
			Connection con=Jsoup.connect(url_pay);

			con.headers(this.headers);
			con.timeout(1000*10);
			con.ignoreHttpErrors(true);
			con.ignoreContentType(true);

			Connection.Response res=con.method(Method.GET).execute();
			Document doc=res.parse();		
			
			JSONObject jsonObj=JSONObject.parseObject(doc.body().text());
			String message=jsonObj.getString("message");
			String status=jsonObj.getString("status");

			if (status.equals("200")){
				int pay_state=jsonObj.getJSONObject("data").getIntValue("paystate");
				if (pay_state==2) {
					System.out.println("      �õㄻ����ɹ�!");
				}else if(pay_state==1) {
					System.out.println("    �ㄻ��������΢���ֹ�֧��!");
				}else {
					System.out.println("    �����ɹ�����֧�������⣬����!");
				}
			}else {
				System.out.println("    δ����������15���Ӻ��ų���");
				System.out.println("    "+ message);
			}
			
		}catch(Exception e) {
			System.out.println(e.getStackTrace());
			e.printStackTrace();
		}	
	}
	
	public String reserve(String url){
		try {	
			Connection con=Jsoup.connect(url);

			con.headers(this.headers);
			con.timeout(1000*10);
			con.ignoreHttpErrors(true);
			con.ignoreContentType(true);

			Connection.Response res=con.method(Method.GET).execute();
			// System.out.println("status:"+res.statusCode());
			Document doc=res.parse();		
			
			JSONObject jsonObj=JSONObject.parseObject(doc.body().text());
			//String txt="{\"status\":\"200\",\"message\":\"�ɹ�\",\"data\":{\"urid\":\"1246\",\"stname\":\"��ͤ��������\",\"stid\":\"23\",\"itemtitle\":\"��ë��\",\"itemtitleid\":\"25\",\"orderno\":\"sps201911091857018855210056100\",\"price\":20,\"coin\":20,\"payprice\":0,\"state\":1,\"mode\":1,\"st_orderid\":30204,\"orderdetail\":[{\"id\":29965,\"st_orderid\":30204,\"subid\":\"175\",\"subname\":\"��ë��6\",\"timeid\":\"19\",\"orderdate\":\"2019-11-15\",\"price\":\"20\",\"orderstate\":1,\"timeinfos\":\"09:00 - 10:00\",\"amount\":1}],\"lasttime\":1573297921},\"sys_time\":1573297021}";
			//JSONObject jsonObj=JSONObject.parseObject(txt);
			
			String message=jsonObj.getString("message");
			String status=jsonObj.getString("status");
			System.out.println("Status:"+ status);
			
			if (status.equals("200")) {
				//String server_sys_time=jsonObj.getString("sys_time");
				int timeid=jsonObj.getJSONObject("data").getJSONArray("orderdetail").getJSONObject(0).getIntValue("timeid");
				int fieldid=jsonObj.getJSONObject("data").getJSONArray("orderdetail").getJSONObject(0).getIntValue("subid");
				String orderno=jsonObj.getJSONObject("data").getString("orderno");     //orderno����֧������
				String orderid=jsonObj.getJSONObject("data").getString("st_orderid");  // orderid����ͤϵͳ���ã����ڶ�����ѯ��				
				
				System.out.println("    �����������ţ�"+orderno+" OrderID:"+orderid + " ʱ�䣺"+ (int)((timeid-1)/2) + "  ���أ�"+ (fieldid-169));
				return orderno;
			}
			else {
				System.out.println("��������ʧ��!");
				System.out.println("      "+ message);
				return null;
			}
		}catch(Exception e) {
			System.out.println(e.getStackTrace());
			e.printStackTrace();
			return null;
		}				
	}
	
	
	public static void main(String[] args) {
		Anting_grab grab=new Anting_grab();
		System.out.println("Ŀ�����ڣ�"+grab.getdateAfter(6));
		
		String url=grab.getURL(6,19);
		String orderno=grab.reserve(url);
		
		if (!(orderno==null||orderno.isEmpty())) {
			grab.pay(orderno);
		}
	}
}
