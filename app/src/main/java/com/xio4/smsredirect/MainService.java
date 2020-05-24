package com.xio4.smsredirect;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.xio4.smsredirect.Constants.LOG_TAG;
import static com.xio4.smsredirect.Utils.RSAEncryptToBase64;
import static com.xio4.smsredirect.Utils.getDate;
import static com.xio4.smsredirect.Utils.loadValueByKey;
import static com.xio4.smsredirect.Utils.saveKeyValue;
import static com.xio4.smsredirect.Utils.stringToPublicKey;

public class MainService extends Service {
    public static final String INBOX = "content://sms/inbox";
    public static final int SMS_PERMISSIONS = 1;
    public static final int TIMER_PERIOD = 5000;
    private Timer timer;
    private TimerTask timerTask;
    private Map<Integer, String> settings;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Sms redirect is started",
                Toast.LENGTH_SHORT).show();
        this.settings = (Map)intent.getSerializableExtra("settings");
        this.runTimer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopTimer();
        super.onDestroy();
        Toast.makeText(this, "Sms redirect is stopped",
                Toast.LENGTH_SHORT).show();
    }

    protected void runTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                try {
                    if (isReady()) {
                        send();
                    }
                } catch (InvalidKeyException e) {
                    Log.e(LOG_TAG, "Received an exception", e);
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    Log.e(LOG_TAG, "Received an exception", e);
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    Log.e(LOG_TAG, "Received an exception", e);
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    Log.e(LOG_TAG, "Received an exception", e);
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    Log.e(LOG_TAG, "Received an exception", e);
                    e.printStackTrace();
                }
            }
        };

        timer.schedule(this.timerTask, TIMER_PERIOD, TIMER_PERIOD); //тикаем каждую секунду без задержки
    }

    protected void stopTimer() {
        this.timer.cancel();
    }

    public String formatSubject(SmsItem item) {
        return String.format("SMS N:%s S:%s %s", item.get("name"), item.get("serviceCenter"), getDate(item.get("date"), Constants.DATE_TIME_FORMAT));
    }

    // TODO: Needs refactor
    protected List<SmsItem> readSMS() {
        Cursor cursor = getContentResolver().query(Uri.parse(INBOX), null, null, null, null);
        String lastId = loadValueByKey(this, R.string.last_sent);
        List<SmsItem> smsList = new ArrayList<>();

        SmsItem smsItem = null;
        int fill = 0;
        boolean finish = false;

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                    String key = cursor.getColumnName(idx);
                    String value = cursor.getString(idx);

                    if (fill == 0) {
                        smsItem = new SmsItem();
                    }

                    if (Constants.SMS_KEYS.containsKey(key)) {
                        smsItem.put((String)Constants.SMS_KEYS.get(key), value);
                        fill++;
                    }

                    if (key.equals("_id")) {
                        if (lastId.equals(value)) {
                            finish = true;
                            break;
                        }
                    }

                    if (fill == 5) {
                        fill = 0;
                        smsList.add(smsItem);
                        break;
                    }
                }
            } while (!finish && cursor.moveToNext());
        } else {
            // empty box, no SMS
        }

        if (smsList.size() > 0) {
            saveKeyValue(this, R.string.last_sent, smsList.get(smsList.size() - 1).get("id"));
        }

        return smsList;
    }

    protected void send() throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        stopTimer();

        String[] emails = this.getEmails();
        List<SmsItem> smsList = this.readSMS();

        if (!smsList.isEmpty()) {
            SmsItem smsItem = smsList.get(smsList.size() - 1);
            String subject = this.formatSubject(smsItem);
            String encryptedData = RSAEncryptToBase64(smsItem.get("body"), stringToPublicKey(this.settings.get(R.string.public_key)));

            for (String email : emails) {
                new SendMailSync(
                        this.settings.get(R.string.email_source),
                        this.settings.get(R.string.email_password),
                        email,
                        subject,
                        encryptedData
                );
            }

            final String botToken = this.settings.get(R.string.bot_token);
            final String botChatId = this.settings.get(R.string.bot_chat_id);

            if (!botToken.isEmpty() && !botChatId.isEmpty()) {
                // To get chatId
                // https://api.telegram.org/bot123456789:jbd78sadvbdy63d37gda37bd8/getUpdates
                new TelegramBot(botToken, botChatId).sendToTelegram(String.format("%s\n %s", this.formatSubject(smsItem), encryptedData));
            }
        }

        runTimer();
    }

    protected String[] getEmails() {
        return this.settings.get(R.string.emails).split("\n");
    }

    protected boolean isReady() {
        return !this.settings.get(R.string.email_password).isEmpty() && !this.settings.get(R.string.email_source).isEmpty()
                && this.getEmails().length > 0 && !this.settings.get(R.string.public_key).isEmpty() && !this.settings.get(R.string.private_key).isEmpty();
    }
}
