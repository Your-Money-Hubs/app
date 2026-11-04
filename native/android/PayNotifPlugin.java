package com.yourmoneyhubs.app;

import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;

@CapacitorPlugin(name = "PayNotif")
public class PayNotifPlugin extends Plugin {
    static PayNotifPlugin current;

    @Override public void load() { current = this; }

    static void notifyNew() {
        PayNotifPlugin p = current;
        if (p != null) p.notifyListeners("payNotif", new JSObject());
    }

    private boolean enabled() {
        String flat = Settings.Secure.getString(getContext().getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        ComponentName me = new ComponentName(getContext(), PayNotifService.class);
        for (String s : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(s);
            if (cn != null && cn.equals(me)) return true;
        }
        return false;
    }

    @PluginMethod public void isEnabled(PluginCall call) {
        JSObject r = new JSObject(); r.put("enabled", enabled()); call.resolve(r);
    }

    @PluginMethod public void openSettings(PluginCall call) {
        Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod public void takeAll(PluginCall call) {
        try {
            JSONArray q = PayNotifService.takeAll(getContext());
            JSObject r = new JSObject(); r.put("items", new JSArray(q.toString())); call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage()); }
    }
}
