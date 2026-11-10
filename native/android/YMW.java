package com.yourmoneyhubs.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Shared code for all YourMoney home screen widgets. The web app sends one JSON blob ("data"). */
public final class YMW {
    static final String PREFS = "ym_widget";
    private static final Class<?>[] PROVIDERS = {
        YMWidgetProvider.class, YMWSmall.class, YMWBudget.class, YMWRecent.class, YMWWallets.class,
        YMWButtons.class, YMWDebts.class, YMWWeek.class, YMWMonth.class
    };

    private YMW() {}

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int i = 0; i < PROVIDERS.length; i++) {
            int[] ids = m.getAppWidgetIds(new ComponentName(c, PROVIDERS[i]));
            if (ids != null && ids.length > 0) update(c, m, ids, i + 1);
        }
    }

    static void update(Context c, AppWidgetManager m, int[] ids, int which) {
        JSONObject d = data(c);
        for (int id : ids) {
            try {
                m.updateAppWidget(id, build(c, m, id, which, d));
            } catch (Exception e) {
                // never crash the launcher; leave the previous view
            }
        }
    }

    static JSONObject data(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try { return new JSONObject(p.getString("data", "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    static String L(JSONObject d, String k, String def) {
        JSONObject l = d.optJSONObject("L");
        String v = l == null ? null : l.optString(k, null);
        return v == null || v.isEmpty() ? def : v;
    }

    static PendingIntent link(Context c, String url, int req) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setPackage(c.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String nowDay() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    /** a new day / month started since the app last opened */
    private static boolean newDay(JSONObject d) { String day = d.optString("day", ""); return !day.isEmpty() && !day.equals(nowDay()); }
    private static boolean newMonth(JSONObject d) { String ym = d.optString("ym", ""); return !ym.isEmpty() && !ym.equals(nowDay().substring(0, 7)); }

    private static int[] sizePx(Context c, AppWidgetManager m, int id, int defW, int defH) {
        float dens = c.getResources().getDisplayMetrics().density;
        int w = defW, h = defH;
        try {
            Bundle o = m.getAppWidgetOptions(id);
            int mw = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int mh = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            if (mw > 0) w = mw;
            if (mh > 0) h = mh;
        } catch (Exception ignored) {}
        return new int[]{Math.max(80, (int) (w * dens)), Math.max(60, (int) (h * dens)), (int) (dens * 1000)};
    }

    static RemoteViews build(Context c, AppWidgetManager m, int id, int which, JSONObject d) {
        boolean has = d.has("today");
        String zero = d.optString("zero", "RM 0.00");
        String dash = "—";
        RemoteViews v;
        switch (which) {
            case 2: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_small);
                v.setTextViewText(R.id.w_label, L(d, "lToday", "今天支出"));
                v.setTextViewText(R.id.w_today, !has ? dash : newDay(d) ? zero : d.optString("today"));
                v.setTextViewText(R.id.w_month, L(d, "lMonth", "本月") + " " + (!has ? dash : newMonth(d) ? zero : d.optString("monthShort")));
                v.setTextViewText(R.id.w_add, L(d, "lAdd", "＋ 记一笔"));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 20));
                v.setOnClickPendingIntent(R.id.w_add, link(c, "yourmoney://quick", 21));
                return v;
            }
            case 3: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_budget);
                int pct = d.optInt("pct", -1);
                v.setTextViewText(R.id.w_label, L(d, "lBudget", "本月预算"));
                v.setTextViewText(R.id.w_pct, pct < 0 ? dash : pct + "%");
                v.setTextViewText(R.id.w_used, d.optString("usedTxt", L(d, "lNoBudget", "还没设预算")));
                v.setTextViewText(R.id.w_remain_l, L(d, "lRemain", "还剩") + " ");
                String left = d.optString("left", "");
                v.setTextViewText(R.id.w_remain, left.isEmpty() ? dash : left);
                v.setTextColor(R.id.w_remain, c.getColor(left.startsWith("RM -") ? R.color.ym_w_red : R.color.ym_w_green));
                int[] s = sizePx(c, m, id, 150, 160);
                float dens = s[2] / 1000f;
                v.setImageViewBitmap(R.id.w_chart, progress(c, (int) (s[0] - 28 * dens), (int) (52 * dens), pct < 0 ? 0 : pct, dens));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://budget", 30));
                return v;
            }
            case 4: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_recent);
                v.setTextViewText(R.id.w_label, L(d, "lRecent", "最近记账"));
                JSONArray r = d.optJSONArray("recent");
                int n = r == null ? 0 : Math.min(3, r.length());
                int[] rows = {R.id.w_r0, R.id.w_r1, R.id.w_r2}, ts = {R.id.w_t0, R.id.w_t1, R.id.w_t2},
                    ss = {R.id.w_s0, R.id.w_s1, R.id.w_s2}, as = {R.id.w_a0, R.id.w_a1, R.id.w_a2};
                int[] divs = {0, R.id.w_div1, R.id.w_div2};
                for (int i = 0; i < 3; i++) {
                    boolean show = i < n;
                    v.setViewVisibility(rows[i], show ? View.VISIBLE : View.INVISIBLE);
                    if (i > 0) v.setViewVisibility(divs[i], show ? View.VISIBLE : View.INVISIBLE);
                    if (!show) continue;
                    JSONObject t = r.optJSONObject(i);
                    v.setTextViewText(ts[i], t.optString("t"));
                    v.setTextViewText(ss[i], t.optString("s"));
                    v.setTextViewText(as[i], t.optString("a"));
                    v.setTextColor(as[i], c.getColor(t.optBoolean("inc") ? R.color.ym_w_green : R.color.ym_w_red));
                }
                v.setViewVisibility(R.id.w_empty, n == 0 ? View.VISIBLE : View.GONE);
                v.setTextViewText(R.id.w_empty, L(d, "lEmpty", "还没有记账"));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 40));
                v.setOnClickPendingIntent(R.id.w_add, link(c, "yourmoney://quick", 41));
                return v;
            }
            case 5: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_wallets);
                v.setTextViewText(R.id.w_label, L(d, "lWallets", "钱包余额"));
                JSONArray w = d.optJSONArray("wallets");
                int n = w == null ? 0 : Math.min(4, w.length());
                int[] cs = {R.id.w_c0, R.id.w_c1, R.id.w_c2, R.id.w_c3}, ns = {R.id.w_n0, R.id.w_n1, R.id.w_n2, R.id.w_n3},
                    vs = {R.id.w_v0, R.id.w_v1, R.id.w_v2, R.id.w_v3};
                for (int i = 0; i < 4; i++) {
                    boolean show = i < n;
                    v.setViewVisibility(cs[i], show ? View.VISIBLE : View.INVISIBLE);
                    if (!show) continue;
                    JSONObject a = w.optJSONObject(i);
                    v.setTextViewText(ns[i], a.optString("n"));
                    String val = a.optString("v");
                    v.setTextViewText(vs[i], val);
                    v.setTextColor(vs[i], c.getColor(val.startsWith("RM -") ? R.color.ym_w_red : R.color.ym_w_text));
                }
                v.setViewVisibility(R.id.w_empty, n == 0 ? View.VISIBLE : View.GONE);
                v.setTextViewText(R.id.w_empty, L(d, "lEmpty", "还没有记账"));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 50));
                return v;
            }
            case 6: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_buttons);
                v.setTextViewText(R.id.w_add, L(d, "lAdd", "＋ 记一笔"));
                v.setTextViewText(R.id.w_scan, L(d, "lScan", "扫收据"));
                v.setTextViewText(R.id.w_aa, L(d, "lAA", "AA 分账"));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 60));
                v.setOnClickPendingIntent(R.id.w_add, link(c, "yourmoney://quick", 61));
                v.setOnClickPendingIntent(R.id.w_scan, link(c, "yourmoney://receipt", 62));
                v.setOnClickPendingIntent(R.id.w_aa, link(c, "yourmoney://debts", 63));
                return v;
            }
            case 7: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_debts);
                v.setTextViewText(R.id.w_l1, L(d, "lOwedMe", "朋友欠你"));
                v.setTextViewText(R.id.w_l2, L(d, "lIOwe", "你欠别人"));
                v.setTextViewText(R.id.w_v1, d.optString("owedMe", dash));
                v.setTextViewText(R.id.w_v2, d.optString("iOwe", dash));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://debts", 70));
                return v;
            }
            case 8: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_week);
                v.setTextViewText(R.id.w_label, L(d, "lWeek", "近 7 天支出"));
                v.setTextViewText(R.id.w_total, d.optString("weekTotal", dash));
                if (d.isNull("vs") || !d.has("vs")) {
                    v.setViewVisibility(R.id.w_vs, View.GONE);
                } else {
                    int vs = d.optInt("vs");
                    v.setViewVisibility(R.id.w_vs, View.VISIBLE);
                    v.setTextViewText(R.id.w_vs, L(d, "lVsWeek", "比上周") + " " + (vs <= 0 ? "↓ " : "↑ ") + Math.abs(vs) + "%");
                    v.setTextColor(R.id.w_vs, c.getColor(vs <= 0 ? R.color.ym_w_green : R.color.ym_w_red));
                }
                int[] s = sizePx(c, m, id, 250, 150);
                float dens = s[2] / 1000f;
                v.setImageViewBitmap(R.id.w_chart, bars(c, (int) (s[0] - 28 * dens), (int) Math.max(60 * dens, s[1] - 84 * dens),
                    d.optJSONArray("week"), d.optJSONArray("labels"), dens));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 80));
                return v;
            }
            case 9: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_w_month);
                v.setTextViewText(R.id.w_label, L(d, "lMonthCum", "本月累计支出"));
                v.setTextViewText(R.id.w_total, d.optString("month", dash));
                v.setTextViewText(R.id.w_legend, "━ " + L(d, "lThis", "本月") + "   ┅ " + L(d, "lLastSame", "上月同期") + " " + d.optString("lastSame", ""));
                int[] s = sizePx(c, m, id, 150, 160);
                float dens = s[2] / 1000f;
                v.setImageViewBitmap(R.id.w_chart, line(c, (int) (s[0] - 28 * dens), (int) Math.max(50 * dens, s[1] - 96 * dens),
                    d.optJSONArray("cur"), d.optJSONArray("last"), dens));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 90));
                return v;
            }
            default: {
                v = new RemoteViews(c.getPackageName(), R.layout.ym_widget);
                boolean nm = newMonth(d);
                String left = nm ? "" : d.optString("left", "");
                v.setTextViewText(R.id.w_label, L(d, "lToday", "今天支出"));
                v.setTextViewText(R.id.w_today, !has ? dash : newDay(d) ? zero : d.optString("today"));
                v.setTextViewText(R.id.w_month, L(d, "lMonth", "本月") + " " + (!has ? dash : nm ? zero : d.optString("month")));
                v.setViewVisibility(R.id.w_left, left.isEmpty() ? View.GONE : View.VISIBLE);
                v.setTextViewText(R.id.w_left, "  ·  " + L(d, "lLeft", "预算剩") + " " + left);
                v.setTextViewText(R.id.w_add, L(d, "lAdd", "＋ 记一笔"));
                v.setTextViewText(R.id.w_scan, L(d, "lScan", "扫收据"));
                v.setOnClickPendingIntent(R.id.w_root, link(c, "yourmoney://home", 1));
                v.setOnClickPendingIntent(R.id.w_add, link(c, "yourmoney://quick", 2));
                v.setOnClickPendingIntent(R.id.w_scan, link(c, "yourmoney://receipt", 3));
                return v;
            }
        }
    }

    // ---------- drawing ----------

    private static Bitmap dino(Context c, int heightPx, boolean facingRight) {
        Bitmap src = BitmapFactory.decodeResource(c.getResources(), facingRight ? R.drawable.ym_dino_r : R.drawable.ym_dino);
        if (src == null) return null;
        int w = Math.max(1, Math.round(src.getWidth() * (heightPx / (float) src.getHeight())));
        return Bitmap.createScaledBitmap(src, w, Math.max(1, heightPx), true);
    }

    private static Bitmap coin(Context c, int sizePx) {
        Bitmap src = BitmapFactory.decodeResource(c.getResources(), R.drawable.ym_coin);
        if (src == null) return null;
        return Bitmap.createScaledBitmap(src, Math.max(1, sizePx), Math.max(1, sizePx), true);
    }

    /** budget bar with the dino walking towards the coin */
    static Bitmap progress(Context c, int w, int h, int pct, float dens) {
        w = Math.max(w, 60); h = Math.max(h, 30);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float barH = 8 * dens, y1 = h - 4 * dens, y0 = y1 - barH, r = barH / 2;
        int coinS = (int) (22 * dens);
        float trackEnd = w - coinS * 0.5f;
        p.setColor(c.getColor(R.color.ym_w_track));
        cv.drawRoundRect(new RectF(0, y0, trackEnd, y1), r, r, p);
        float f = Math.max(0, Math.min(100, pct)) / 100f;
        p.setColor(c.getColor(pct > 100 ? R.color.ym_w_red : R.color.ym_w_gold));
        if (f > 0) cv.drawRoundRect(new RectF(0, y0, Math.max(barH, trackEnd * f), y1), r, r, p);
        Bitmap cn = coin(c, coinS);
        if (cn != null) cv.drawBitmap(cn, w - coinS, y1 - coinS, null);
        Bitmap dn = dino(c, (int) Math.min(h - barH - 6 * dens, 40 * dens), true);
        if (dn != null) {
            float x = trackEnd * f - dn.getWidth() / 2f;
            x = Math.max(0, Math.min(x, w - coinS - dn.getWidth()));
            cv.drawBitmap(dn, x, y0 - dn.getHeight() - 1 * dens, null);
        }
        return b;
    }

    /** last 7 days bars, today in coin gold with the dino on top */
    static Bitmap bars(Context c, int w, int h, JSONArray vals, JSONArray labels, float dens) {
        w = Math.max(w, 100); h = Math.max(h, 60);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int n = 7;
        double max = 0.01;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) { v[i] = vals == null ? 0 : vals.optDouble(i, 0); max = Math.max(max, v[i]); }
        float labelH = 16 * dens, dinoH = Math.min(36 * dens, h * 0.35f), gap = 8 * dens;
        float bw = (w - gap * (n - 1)) / n, base = h - labelH, top = dinoH + 2 * dens, r = 6 * dens;
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(c.getColor(R.color.ym_w_sub));
        tp.setTextSize(11 * dens);
        tp.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float x = i * (bw + gap);
            float bh = (float) Math.max(3 * dens, (base - top) * (v[i] / max));
            p.setColor(c.getColor(i == n - 1 ? R.color.ym_w_gold : R.color.ym_w_bar));
            cv.drawRoundRect(new RectF(x, base - bh, x + bw, base), r, r, p);
            cv.drawRect(new RectF(x, base - Math.min(bh, r), x + bw, base), p);
            String lb = labels == null ? "" : labels.optString(i, "");
            cv.drawText(lb, x + bw / 2, h - 3 * dens, tp);
            if (i == n - 1) {
                Bitmap dn = dino(c, (int) dinoH, true);
                if (dn != null) cv.drawBitmap(dn, x + bw / 2 - dn.getWidth() / 2f, base - bh - dn.getHeight(), null);
            }
        }
        return b;
    }

    /** cumulative spending this month (solid) vs last month (dashed); dino at the end of this month's line */
    static Bitmap line(Context c, int w, int h, JSONArray cur, JSONArray last, float dens) {
        w = Math.max(w, 80); h = Math.max(h, 50);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        int nc = cur == null ? 0 : cur.length(), nl = last == null ? 0 : last.length();
        int days = Math.max(28, Math.max(nl, nc));
        double max = 1;
        for (int i = 0; i < nc; i++) max = Math.max(max, cur.optDouble(i, 0));
        for (int i = 0; i < nl; i++) max = Math.max(max, last.optDouble(i, 0));
        max *= 1.08;
        float dinoH = Math.min(30 * dens, h * 0.4f), padTop = dinoH, padR = 8 * dens, padB = 4 * dens;
        float cw = w - padR, ch = h - padTop - padB;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        if (nl > 1) {
            Path pl = new Path();
            for (int i = 0; i < nl; i++) {
                float x = cw * i / (days - 1f), y = (float) (padTop + ch - ch * last.optDouble(i, 0) / max);
                if (i == 0) pl.moveTo(x, y); else pl.lineTo(x, y);
            }
            p.setColor(c.getColor(R.color.ym_w_line2));
            p.setStrokeWidth(2 * dens);
            p.setPathEffect(new DashPathEffect(new float[]{4 * dens, 4 * dens}, 0));
            cv.drawPath(pl, p);
            p.setPathEffect(null);
        }
        float ex = 0, ey = padTop + ch;
        if (nc > 0) {
            Path pc = new Path();
            for (int i = 0; i < nc; i++) {
                float x = cw * i / (days - 1f), y = (float) (padTop + ch - ch * cur.optDouble(i, 0) / max);
                if (i == 0) pc.moveTo(x, y); else pc.lineTo(x, y);
                ex = x; ey = y;
            }
            p.setColor(c.getColor(R.color.ym_w_text));
            p.setStrokeWidth(3 * dens);
            cv.drawPath(pc, p);
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(c.getColor(R.color.ym_w_text));
            cv.drawCircle(ex, ey, 4 * dens, dot);
        }
        Bitmap dn = dino(c, (int) dinoH, true);
        if (dn != null) {
            float x = Math.max(0, Math.min(ex - dn.getWidth() / 2f, w - dn.getWidth()));
            cv.drawBitmap(dn, x, Math.max(0, ey - dn.getHeight() - 3 * dens), null);
        }
        return b;
    }
}
