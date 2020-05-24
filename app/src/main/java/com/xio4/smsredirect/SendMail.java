package com.xio4.smsredirect;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail extends AsyncTask {
    private Context context;
    private Session session;
    private String email;
    private String subject;
    private String message;
    private String srcEmail;
    private String srcPassword;
    private ProgressDialog progressDialog;

    public SendMail(Context context, String srcEmail, String srcPassword, String email, String subject, String message){
        this.context = context;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.srcEmail = srcEmail;
        this.srcPassword = srcPassword;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(context,"Sending message","Please wait...",false,false);
    }

    @Override
    protected void onPostExecute(Object aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
        Toast.makeText(context,"Message Sent", Toast.LENGTH_LONG).show();
    }

    @Override
    protected Object doInBackground(Object[] params) {
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
        final String srcEmail = this.srcEmail;
        final String srcPassword = this.srcPassword;
        session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
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
            e.printStackTrace();
        }
        return null;
    }
}
