package com.yourmoneyhubs.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class YMWidgetProvider extends AppWidgetProvider {
    static final String PREFS = "ym_widget";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static PendingIntent link(Context c, String url, int req) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setPackage(c.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static RemoteViews build(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.ym_widget);
        boolean hide = p.getBoolean("hide", false);
        boolean has = p.contains("today");
        String now = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String zero = p.getString("zero", "RM 0.00");
        // a new day / month started since the app last opened: nothing spent yet
        boolean newDay = !now.equals(p.getString("day", now));
        boolean newMonth = !now.substring(0, 7).equals(p.getString("ym", now.substring(0, 7)));
        String today = newDay ? zero : p.getString("today", zero);
        String month = newMonth ? zero : p.getString("month", zero);
        String left = newMonth ? "" : p.getString("left", "");

        v.setTextViewText(R.id.w_label, p.getString("lToday", "今天支出"));
        v.setTextViewText(R.id.w_today, !has ? "—" : hide ? "RM ••••" : today);
        v.setTextViewText(R.id.w_month, p.getString("lMonth", "本月") + "  " + (!has ? "—" : hide ? "••••" : month));
        if (left.isEmpty()) {
            v.setViewVisibility(R.id.w_left, View.GONE);
        } else {
            v.setViewVisibility(R.id.w_left, View.VISIBLE);
            v.setTextViewText(R.id.w_left, p.getString("lLeft", "预算剩") + "  " + (hide ? "••••" : left));
        }
        v.setTextViewText(R.id.w_add, p.getString("lAdd", "＋ 记一笔"));
        v.setTextViewText(R.id.w_scan, p.getString("lScan", "扫收据"));
        v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 1));
        v.setOnClickPendingIntent(R.id.w_add, link(c, "yourmoney://quick", 2));
        v.setOnClickPendingIntent(R.id.w_scan, link(c, "yourmoney://receipt", 3));
        return v;
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, YMWidgetProvider.class));
        for (int id : ids) m.updateAppWidget(id, build(c));
    }
}
