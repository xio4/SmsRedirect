package com.xio4.smsredirect;

import android.util.Log;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.xio4.smsredirect.Constants.LOG_TAG;

public class SendMailSync {
    public SendMailSync(final String srcEmail, final String srcPassword, String email, String subject, String message){
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

//        props.put("mail.smtp.host", "smtp.yandex.ru");
//        props.put("mail.smtp.socketFactory.port", "465");
//        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.port", "465");
//
        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(srcEmail, srcPassword);
            }
        });

        try {
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress(srcEmail));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            mm.setSubject(subject);
            mm.setText(message);
            Transport.send(mm);
        }
        catch (MessagingException e) {
            Log.e(LOG_TAG, "Received an exception", e);
            e.printStackTrace();
        }
    }
}
