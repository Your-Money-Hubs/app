package com.yourmoneyhubs.app;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.regex.Pattern;

/** 只把“看起来像付款”的通知（含 RM 金额）暂存在本机，App 打开时交给网页识别。不上传。 */
public class PayNotifService extends NotificationListenerService {
    static final String PREFS = "ym_paynotif";
    static final String KEY = "queue";
    private static final Pattern MONEY = Pattern.compile("(?i)(RM|MYR)\\s?[0-9]");
    static PayNotifService instance;

    @Override public void onListenerConnected() { instance = this; }
    @Override public void onListenerDisconnected() { instance = null; }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;
            Notification n = sbn.getNotification();
            if (n == null) return;
            Bundle ex = n.extras;
            CharSequence t = ex.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence tx = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if (tx == null) tx = ex.getCharSequence(Notification.EXTRA_TEXT);
            String title = t == null ? "" : t.toString();
            String text = tx == null ? "" : tx.toString();
            if (!MONEY.matcher(title + " " + text).find()) return;
            add(this, pkg, title, text, sbn.getPostTime(), sbn.getKey());
            PayNotifPlugin.notifyNew();
        } catch (Exception ignored) {}
    }

    static synchronized void add(Context c, String pkg, String title, String text, long time, String key) throws Exception {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray q = new JSONArray(sp.getString(KEY, "[]"));
        for (int i = 0; i < q.length(); i++) {
            JSONObject o = q.getJSONObject(i);
            if (key != null && key.equals(o.optString("key")) && text.equals(o.optString("text"))) return;
        }
        JSONObject o = new JSONObject();
        o.put("pkg", pkg); o.put("title", title); o.put("text", text); o.put("time", time); o.put("key", key);
        q.put(o);
        while (q.length() > 50) q.remove(0);
        sp.edit().putString(KEY, q.toString()).apply();
    }

    static synchronized JSONArray takeAll(Context c) throws Exception {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray q = new JSONArray(sp.getString(KEY, "[]"));
        sp.edit().putString(KEY, "[]").apply();
        return q;
    }
}
