package com.xio4.smsredirect;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final Map SMS_KEYS = new HashMap<String, String>() {{
        put("address", "name");
        put("body", "body");
        put("date", "date");
        put("service_center", "serviceCenter");
        put("_id", "id");
    }};

    public static final String LOG_TAG = "SmsRedirect";
    public static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";
    public static final String RSA_ALGORYTHM ="RSA/ECB/PKCS1Padding";
    public static final String SHARED_PREFERENCES_NAME = "smsredirect";
}
