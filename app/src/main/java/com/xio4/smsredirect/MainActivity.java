package com.xio4.smsredirect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.xio4.smsredirect.Utils.getTextFromEditText;
import static com.xio4.smsredirect.Utils.keysMatch;
import static com.xio4.smsredirect.Utils.loadValueByKey;
import static com.xio4.smsredirect.Utils.saveKeyValue;
import static com.xio4.smsredirect.Utils.setTextToEditText;
import static com.xio4.smsredirect.Utils.stringToPrivateKey;
import static com.xio4.smsredirect.Utils.stringToPublicKey;

public class MainActivity extends AppCompatActivity {
    public static final int SMS_PERMISSIONS = 1;
    private boolean started;
    private Map<Integer, String> settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.started = false;
        checkPermissions();
        setContentView(R.layout.activity_main);
        this.settings = new HashMap<>();

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

    private void loadValue(int id, int key) {
        String value = loadValueByKey(this, key);
        this.settings.put(key, value);

        setTextToEditText(this, id, value);
    }

    private void saveValue(int key, String value) {
        this.settings.put(key, value);

        saveKeyValue(this, key, value);
    }

    protected void loadSettings() {
        loadValue(R.id.editTextEmails, R.string.emails);
        loadValue(R.id.editTextEmails, R.string.emails);
        loadValue(R.id.editTextEmailSrc, R.string.email_source);
        loadValue(R.id.editTextEmailPassword, R.string.email_password);
        loadValue(R.id.editTextPrivateKey, R.string.private_key);
        loadValue(R.id.editTextPublicKey, R.string.public_key);
        loadValue(R.id.editTextBotToken, R.string.bot_token);
        loadValue(R.id.editTextBotChatId, R.string.bot_chat_id);
    }

    public void onSaveSettings(View view) {
        saveValue(R.string.emails, Utils.join("\n", this.getEmails()));
        saveValue(R.string.email_source, this.getEmailSource());
        saveValue(R.string.email_password, this.getEmailPassword());
        saveValue(R.string.private_key, this.getPrivateKey());
        saveValue(R.string.public_key, this.getPublicKey());
        saveValue(R.string.bot_token, this.getBotToken());
        saveValue(R.string.bot_chat_id, this.getBotChatId());
    }

    public void onCopyPrivateKey(View view) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("", this.getPrivateKey());

        clipboard.setPrimaryClip(clip);
    }

    public void onStart(View view) {
        Button btnStart = findViewById(R.id.buttonStart);

        this.started = !this.started;

        if (this.started) {
            Intent intent = new Intent(MainActivity.this, MainService.class);
            intent.putExtra("settings", (Serializable)this.settings);
            startService(intent);
            btnStart.setText(R.string.button_stop);
        } else {
            stopService(new Intent(MainActivity.this, MainService.class));
            btnStart.setText(R.string.button_start);
        }
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(MainActivity.this, MainService.class));
        super.onDestroy();
    }
}
