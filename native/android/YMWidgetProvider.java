package com.yourmoneyhubs.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class YMWidgetProvider extends AppWidgetProvider {
    static final String PREFS = YMW.PREFS;
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { YMW.update(c, m, ids, 1); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { YMW.update(c, m, new int[]{id}, 1); }
    static void refresh(Context c) { YMW.refreshAll(c); }
}
