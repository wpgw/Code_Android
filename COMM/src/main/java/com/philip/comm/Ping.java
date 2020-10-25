package com.philip.comm;

import java.net.InetAddress;
import java.util.Date;

public class Ping {
    public  static  boolean ping(String ipAddress)  throws Exception {
        int  timeOut =  1000 ;   // 超时应该在1钞以上
        boolean status = InetAddress.getByName(ipAddress).isReachable(timeOut);      //  当返回值是true时，说明host是可用的，false则不可。
        return status;
    }

    public static void main(String[] args) {
        Integer count=0;
        while (true){
            try{
                String ipAdd="10.70.60.25";
                if(Ping.ping(ipAdd)){
                    if(count%1800==0){
                        System.out.println("Ping "+ipAdd+" 成功！"+(new Date()));
                    }
                }else{
                    System.out.println("失败！"+(new Date()));
                    myQQmail mymail=new myQQmail("pwang@meridian-mag.com,pwang@meridian-mag.com","this is from Java Ping","Ping "+ipAdd+" 失败！"+(new Date()));
                    mymail.send();
                }
                count++;
                Thread.sleep(1000);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
