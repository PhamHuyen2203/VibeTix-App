package com.example.vibetix.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
