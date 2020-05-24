package com.xio4.smsredirect;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class TelegramBot {
    public static final String URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
    private String apiToken;
    private String chatId;

    public TelegramBot(String apiToken, String chatId) {
        this.apiToken = apiToken;
        this.chatId = chatId;
    }

    public void sendToTelegram(String text) {
        String urlString = String.format(URL_TEMPLATE, this.apiToken, this.chatId, text);

        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
