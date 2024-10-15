package com.peach.common.mail;

import com.peach.common.config.EmailConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/14 10:12
 */
@Slf4j
@Component
public class EmailSendService {

    @Autowired
    @Qualifier("mailSender")
    private JavaMailSender mailSender;

    @Autowired
    private EmailConfig emailProps;

    /**
     * 简单邮件发送
     * @param to
     * @param title
     * @param content
     * @throws Exception
     */
    public void sendSimpleMail(String to,String title,String content) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailProps.getForm());
        message.setTo(to);
        message.setSubject(title);
        message.setText(content);
        mailSender.send(message);
        log.info("简单邮件发送成功");
    }

    /**
     * 带附件邮件发送
     * @param to
     * @param title
     * @param cotent
     * @param fileList
     * @throws Exception
     */
    public void sendAttachmentsMail(String to, String title, String cotent, List<File> fileList) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(emailProps.getForm());
        helper.setTo(to);
        helper.setSubject(title);
        helper.setText(cotent);
        String fileName = null;
        for (File file : fileList) {
            fileName = MimeUtility.encodeText(file.getName(), "GB2312", "B");
            helper.addAttachment(fileName, file);
        }
        mailSender.send(message);
        log.info("带附件邮件发送成功");
    }

    /**
     * 发送带附件的邮件，并指出html解决
     * @param title
     * @param htmlContent
     * @param fileList
     * @param email
     * @param isSuportHtml
     * @throws Exception
     */
    public void sendInlineMail( String title,String htmlContent, List<File> fileList,String email,boolean isSuportHtml) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(emailProps.getForm());
        helper.setTo(email);
        helper.setSubject(title);
        helper.setText(htmlContent,isSuportHtml);
        helper.addInline("logo",new ClassPathResource("email/logo.png"));
        String fileName = null;
        for (File file : fileList) {
            fileName = MimeUtility.encodeText(file.getName(), "GB2312", "B");
            helper.addAttachment(fileName, file);
        }
        mailSender.send(message);
        log.info("带附件邮件发送成功");
    }
}
