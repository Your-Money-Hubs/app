package com.yourmoneyhubs.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "YMWidget")
public class YMWidgetPlugin extends Plugin {
    private static final String[] KEYS = {"today", "month", "left", "zero", "day", "ym", "lToday", "lMonth", "lLeft", "lAdd", "lScan"};

    @PluginMethod
    public void update(PluginCall call) {
        SharedPreferences.Editor e = getContext().getSharedPreferences(YMWidgetProvider.PREFS, Context.MODE_PRIVATE).edit();
        for (String k : KEYS) {
            String s = call.getString(k);
            if (s != null) e.putString(k, s); else e.remove(k);
        }
        Boolean hide = call.getBoolean("hide", false);
        e.putBoolean("hide", hide != null && hide);
        e.apply();
        YMWidgetProvider.refresh(getContext());
        call.resolve();
    }
}
