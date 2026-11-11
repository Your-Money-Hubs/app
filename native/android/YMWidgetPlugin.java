package com.yourmoneyhubs.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "YMWidget")
public class YMWidgetPlugin extends Plugin {
    private static final String[] KEYS = {"today", "month", "left", "zero", "day", "ym", "lToday", "lMonth", "lLeft", "lAdd", "lScan", "data"};

    @PluginMethod
    public void update(PluginCall call) {
        SharedPreferences.Editor e = getContext().getSharedPreferences(YMW.PREFS, Context.MODE_PRIVATE).edit();
        for (String k : KEYS) {
            String s = call.getString(k);
            if (s != null) e.putString(k, s); else e.remove(k);
        }
        Boolean hide = call.getBoolean("hide", false);
        e.putBoolean("hide", hide != null && hide);
        e.apply();
        YMW.refreshAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void pin(PluginCall call) {
        Integer w = call.getInt("which", 1);
        boolean ok = false;
        try { ok = YMW.pin(getContext(), w == null ? 1 : w); } catch (Exception ignored) {}
        JSObject r = new JSObject();
        r.put("ok", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void info(PluginCall call) {
        JSObject r = new JSObject();
        r.put("canPin", YMW.canPin(getContext()));
        r.put("styles", 9);
        call.resolve(r);
    }
}
