package com.example.vibetix.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "vibetix_profile";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String DEFAULT_LANG = "vi";

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANG);
    }

    public static Context applyLocale(Context context) {
        String lang = getSavedLanguage(context);
        return applyLocale(context, lang);
    }

    public static Context applyLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        // updateConfiguration patches existing resources so @string/ resolves correctly
        // on API 26-32 where createConfigurationContext alone may not suffice
        res.updateConfiguration(config, res.getDisplayMetrics());

        // Also patch the application-level resources so all contexts resolve correctly
        Context appCtx = context.getApplicationContext();
        if (appCtx != null) {
            Resources appRes = appCtx.getResources();
            Configuration appConfig = new Configuration(appRes.getConfiguration());
            appConfig.setLocale(locale);
            appRes.updateConfiguration(appConfig, appRes.getDisplayMetrics());
        }

        return context.createConfigurationContext(config);
    }
}
