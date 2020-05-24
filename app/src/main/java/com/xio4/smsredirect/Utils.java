package com.xio4.smsredirect;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Utils {
    public static final String join(String delim, String[] strArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strArray.length; i++) {
            if (i > 0) {
                stringBuilder.append(delim);
            }
            stringBuilder.append(strArray[i]);
        }
        return stringBuilder.toString();
    }

    public static final String loadValueByKey(AppCompatActivity activity, final int key) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        return sharedPref.getString(activity.getString(key), "");
    }

    public static final void saveKeyValue(AppCompatActivity activity, final int key, final String value) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(key), value);
        editor.commit();
    }

    public static final PublicKey stringToPublicKey(String publStr) {
        PublicKey publicKey = null;

        try {
            byte[] data = Base64.decode(publStr, Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            publicKey = fact.generatePublic(spec);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static final PrivateKey stringToPrivateKey(String privStr) {
        PrivateKey privateKey = null;

        try {
            byte[] data = Base64.decode(privStr, Base64.DEFAULT);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            privateKey = fact.generatePrivate(spec);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public static final String getTextFromEditText(AppCompatActivity activity, int id) {
        EditText editText = activity.findViewById(id);
        return editText.getText().toString();
    }

    public static final void setTextToEditText(AppCompatActivity activity, int id, String value) {
        EditText editText = activity.findViewById(id);
        editText.setText(value);
    }

    public static String getDate(String milliSeconds, String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.valueOf(milliSeconds));
        return formatter.format(calendar.getTime());
    }

    public static final String RSADecryptFromBase64(final String b64text, final PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher1 = Cipher.getInstance(Constants.RSA_ALGORYTHM);

        cipher1.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher1.doFinal(Base64.decode(b64text, Base64.DEFAULT));
        String decrypted = new String(decryptedBytes);
        return decrypted;
    }

    public static final String RSAEncryptToBase64(final String plain, final PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(Constants.RSA_ALGORYTHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plain.getBytes());

        String encrypted = Base64.encodeToString(encryptedBytes, 0, encryptedBytes.length, Base64.DEFAULT);

        return encrypted;
    }

    public static final boolean keysMatch(PublicKey publicKey, PrivateKey privateKey) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        String str = "Helloxyz123";

        String encrypted = RSAEncryptToBase64(str, publicKey);
        String decrypted = RSADecryptFromBase64(encrypted, privateKey);

        return str.equals(decrypted);
    }
}
