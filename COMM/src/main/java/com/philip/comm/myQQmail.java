package com.philip.comm;
import com.sun.mail.util.MailSSLSocketFactory;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Properties;

public class myQQmail {
    String receptions,subject,content;
    public myQQmail(String receiptions,String subject,String content){
        this.receptions =receiptions;
        this.subject=subject;
        this.content=content;
    }

    public void send() throws MessagingException, GeneralSecurityException{
        //javamail官网：https://javaee.github.io/javamail/
        //          注：这里应用android版的jar包，而不应是标准的java版的包
        //参考 https://blog.csdn.net/baolingye/article/details/96598222
        // Android发送邮件 https://blog.csdn.net/fukaimei/article/details/87717995

        //创建一个配置文件并保存
        Properties properties = new Properties();
        properties.setProperty("mail.host","smtp.qq.com");
        properties.setProperty("mail.transport.protocol","smtp");
        properties.setProperty("mail.smtp.auth","true");

        //QQ存在一个特性设置SSL加密
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.socketFactory", sf);

        //创建一个session对象
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("13986065186@qq.com","tcewijpuxnqmdihh");
            }
        });

        //开启debug模式
        session.setDebug(true);
        //获取连接对象
        Transport transport = session.getTransport();
        //连接服务器
        transport.connect("smtp.qq.com","13986065186@qq.com","tcewijpuxnqmdihh");
        //创建邮件对象
        MimeMessage mimeMessage = new MimeMessage(session);
        //邮件发送人
        mimeMessage.setFrom(new InternetAddress("13986065186@qq.com"));
        //邮件接收人
        //mimeMessage.setRecipient(Message.RecipientType.TO,new InternetAddress(receiption));
        mimeMessage.setRecipients(Message.RecipientType.TO, receptions);
        //邮件标题
        mimeMessage.setSubject(subject);
        //邮件内容
        mimeMessage.setContent(content,"text/html;charset=UTF-8");
        //发送邮件
        transport.sendMessage(mimeMessage,mimeMessage.getAllRecipients());
        //关闭连接
        transport.close();
    }

    public static void main(String[] args) throws MessagingException, GeneralSecurityException {
        myQQmail mymail=new myQQmail("pwang@meridian-mag.com,pwang@meridian-mag.com","this is from receitions","this is the content");
        mymail.send();
    }
}

