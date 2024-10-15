package com.peach.common.config;

import com.peach.common.constant.MailConstant;
import com.peach.common.util.StringUtil;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //接收属性值，初始化Bean
 * @CreateTime 2024/10/12 18:15
 */
@Slf4j
@Data
@Configuration
public class EmailConfig {

    /**
     * 邮件服务器地址
     */
    @Value("${email.host:smtp.qq.com}")
    private String host;

    /**
     * 发件邮箱，此处以qq为例
     */
    @Value("${email.from:445623047@qq.com}")
    private String form;

    /**
     * 邮箱授权码
     */
    @Value("${email.password:jsmunjtmgrdsbjbb}")
    private String password;

    /**
     * 邮件服务端口
     */
    @Value("${email.port:465}")
    private String port;

    /**
     * 发送协议
     */
    @Value("${email.protocol:smtp}")
    public String protocol;

    /**
     * 是否ssl加密端口
     */
    @Value("${email.isSSL:1}")
    private Integer isSSL;

    /**
     * 默认编码
     */
    @Value("${email.defaultEncoding:UTF-8}")
    private String defaultEncoding;


    @Bean("mailSender")
    public JavaMailSender javaMailSender() throws GeneralSecurityException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setDefaultEncoding(defaultEncoding);
        mailSender.setPassword(password);
        mailSender.setUsername(form);
        mailSender.setProtocol(protocol);
        Properties javaMailProperties = new Properties();
        log.info("邮箱发送加密方式：[{}]" ,isSSL);
        if (MailConstant.MAIL_AUTH_TYPE_SSL.equals(isSSL)) {
            //阿里:使用SSL，企业邮箱必需！ 开启安全协议
            port = StringUtil.isBlank(port) ? "465" : port;
            MailSSLSocketFactory sf = null;
            sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.ssl.socketFactory", sf);
        } else if (MailConstant.MAIL_AUTH_TYPE_TLS.equals(isSSL)) {
            //阿里:使用TLS，企业邮箱必需！ 开启安全协议
            port = StringUtil.isBlank(port) ? "587" : port;
            javaMailProperties.put("mail.smtp.ssl.trust", "*");
            javaMailProperties.put("mail.smtp.starttls.required", "true");
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
        } else {
            port = StringUtil.isBlank(port) ? "25" : port;
        }
        mailSender.setPort(Integer.parseInt(port));
        //开启认证
        javaMailProperties.setProperty("mail.smtp.auth", "true");
        //启用调试
        javaMailProperties.setProperty("mail.debug", "true");
        //设置链接超时
        javaMailProperties.setProperty("mail.smtp.timeout", "5000");
        //设置端口
        javaMailProperties.setProperty("mail.smtp.port", port);
        if (MailConstant.MAIL_AUTH_TYPE_SSL.equals(isSSL)) {
            //阿里
            //使用SSL，企业邮箱必需！
            //开启安全协议
            //设置SSL端口
            javaMailProperties.setProperty("mail.smtp.socketFactory.port", port);
            javaMailProperties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        javaMailProperties.setProperty("mail.smtp.socketFactory.fallback", "false");
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;
    }

}
