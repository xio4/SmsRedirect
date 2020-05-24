package com.xio4.smsredirect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.xio4.smsredirect.Utils.RSAEncryptToBase64;
import static com.xio4.smsredirect.Utils.getDate;
import static com.xio4.smsredirect.Utils.getTextFromEditText;
import static com.xio4.smsredirect.Utils.keysMatch;
import static com.xio4.smsredirect.Utils.loadValueByKey;
import static com.xio4.smsredirect.Utils.saveKeyValue;
import static com.xio4.smsredirect.Utils.setTextToEditText;
import static com.xio4.smsredirect.Utils.stringToPrivateKey;
import static com.xio4.smsredirect.Utils.stringToPublicKey;

public class MainActivity extends AppCompatActivity {
    public static final String INBOX = "content://sms/inbox";
    public static final int SMS_PERMISSIONS = 1;
    public static final int TIMER_PERIOD = 5000;
    private Timer timer;
    private TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        setContentView(R.layout.activity_main);

        this.loadSettings();

        try {
            if (this.getPublicKey().isEmpty() || !keysMatch(stringToPublicKey(this.getPublicKey()), stringToPrivateKey(this.getPrivateKey()))) {
                try {
                    this.generateKeyPair();
                    Toast toast = Toast.makeText(getApplicationContext(), "Attention! Key pair is regenerated!", Toast.LENGTH_LONG);
                    toast.show();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        runTimer();
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
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            }
        };

        timer.schedule(this.timerTask, TIMER_PERIOD, TIMER_PERIOD); //тикаем каждую секунду без задержки
    }

    protected void stopTimer() {
        this.timer.cancel();
    }

    protected void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.INTERNET}, SMS_PERMISSIONS);
            // Permission is not granted
        }
    }

    protected String getPublicKey() {
        return getTextFromEditText(this, R.id.editTextPublicKey).replaceAll(" ","");
    }

    protected String getPrivateKey() {
        return getTextFromEditText(this, R.id.editTextPrivateKey).replaceAll(" ","");
    }

    protected String[] getEmails() {
        return getTextFromEditText(this, R.id.editTextEmails).split("\n");
    }

    protected String getEmailSource() {
        return getTextFromEditText(this, R.id.editTextEmailSrc);
    }

    protected String getEmailPassword() {
        return getTextFromEditText(this, R.id.editTextEmailPassword);
    }

    protected String getBotToken() {
        return getTextFromEditText(this, R.id.editTextBotToken);
    }

    protected String getBotChatId() {
        return getTextFromEditText(this, R.id.editTextBotChatId);
    }

    protected void generateKeyPair() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(String.valueOf(System.currentTimeMillis()).getBytes());
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, sr);
        KeyPair kp = kpg.genKeyPair();
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        setTextToEditText(this, R.id.editTextPrivateKey, Base64.encodeToString(privateKey.getEncoded(), 0, privateKey.getEncoded().length, Base64.DEFAULT));
        setTextToEditText(this, R.id.editTextPublicKey, Base64.encodeToString(publicKey.getEncoded(), 0, publicKey.getEncoded().length, Base64.DEFAULT));
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
            String encryptedData = RSAEncryptToBase64(smsItem.get("body"), stringToPublicKey(this.getPublicKey()));

            for (String email : emails) {
                new SendMailSync(
                        this.getEmailSource(),
                        this.getEmailPassword(),
                        email,
                        subject,
                        encryptedData
                );
            }

            if (!this.getBotToken().isEmpty() && !this.getBotChatId().isEmpty()) {
                // To get chatId
                // https://api.telegram.org/bot123456789:jbd78sadvbdy63d37gda37bd8/getUpdates
                new TelegramBot(this.getBotToken(), this.getBotChatId()).sendToTelegram(String.format("%s\n %s", this.formatSubject(smsItem), encryptedData));
            }
        }

        runTimer();
    }

    protected boolean isReady() {
        return !this.getEmailPassword().isEmpty() && !this.getEmailSource().isEmpty() && this.getEmails().length > 0 && !this.getPublicKey().isEmpty() && !this.getPrivateKey().isEmpty();
    }

    protected void loadSettings() {
        setTextToEditText(this, R.id.editTextEmails, loadValueByKey(this, R.string.emails));
        setTextToEditText(this, R.id.editTextEmailSrc, loadValueByKey(this, R.string.email_source));
        setTextToEditText(this, R.id.editTextEmailPassword, loadValueByKey(this, R.string.email_password));
        setTextToEditText(this, R.id.editTextPrivateKey, loadValueByKey(this, R.string.private_key));
        setTextToEditText(this, R.id.editTextPublicKey, loadValueByKey(this, R.string.public_key));
        setTextToEditText(this, R.id.editTextBotToken, loadValueByKey(this, R.string.bot_token));
        setTextToEditText(this, R.id.editTextBotChatId, loadValueByKey(this, R.string.bot_chat_id));
    }

    public void onSaveSettings(View view) {
        saveKeyValue(this, R.string.emails, Utils.join("\n", this.getEmails()));
        saveKeyValue(this, R.string.email_source, this.getEmailSource());
        saveKeyValue(this, R.string.email_password, this.getEmailPassword());
        saveKeyValue(this, R.string.private_key, this.getPrivateKey());
        saveKeyValue(this, R.string.public_key, this.getPublicKey());
        saveKeyValue(this, R.string.bot_token, this.getBotToken());
        saveKeyValue(this, R.string.bot_chat_id, this.getBotChatId());
    }

    public void onCopyPrivateKey(View view) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("", this.getPrivateKey());

        clipboard.setPrimaryClip(clip);
    }
}
