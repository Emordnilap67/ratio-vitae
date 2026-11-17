package io.github.emordnilap67.ratiovitae;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

public class Fresh extends BroadcastReceiver {
    static final String TICK = "io.github.emordnilap67.ratiovitae.FRESH";
    static final long EVERY = 15 * 60 * 1000L;
    static final long GIVE_UP_MS = 25000L;

    static final String[] FILES = {"budget_log.csv", "cards.csv", "categories.csv", "groceries_trips.csv", "food_log.csv",
            "pills_log.csv", "pills_taken.csv", "pills_fills.csv", "bp_log.csv", "temp_log.csv", "weight_log.csv",
            "rdsp_holdings.csv", "rdsp_cash.csv", "fx.txt", "dividends.csv", "invest_log.csv", "investments.csv",
            "crypto_holdings.csv", "schedule.csv"};

    private static WebView web;
    private static BroadcastReceiver.PendingResult held;
    private static Runnable giveUp;

    @Override
    public void onReceive(Context c, Intent i) {
        schedule(c);
        check(c, goAsync());
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("fresh", Context.MODE_PRIVATE);
    }

    static boolean hasAccess() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    static boolean anyWidget(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (m == null) return false;
        return m.getAppWidgetIds(new ComponentName(c, MoneyWidget.class)).length
                + m.getAppWidgetIds(new ComponentName(c, PillsWidget.class)).length
                + m.getAppWidgetIds(new ComponentName(c, InvestWidget.class)).length > 0;
    }

    static String sig() {
        CRC32 crc = new CRC32();
        byte[] buf = new byte[16384];
        for (String n : FILES) {
            crc.update(n.getBytes());
            File f = new File(MainActivity.folder(), n);
            if (!f.isFile()) continue;
            try {
                InputStream in = new FileInputStream(f);
                try {
                    int r;
                    while ((r = in.read(buf)) > 0) crc.update(buf, 0, r);
                } finally {
                    in.close();
                }
            } catch (Exception e) {
                crc.update(7);
            }
        }
        return Long.toHexString(crc.getValue());
    }

    static String day(long t) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t));
    }

    static void mark(Context c) {
        prefs(c).edit().putString("sig", sig()).putLong("at", System.currentTimeMillis()).apply();
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, Fresh.class).setAction(TICK);
        PendingIntent p = PendingIntent.getBroadcast(c, 470, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + EVERY, EVERY, p);
    }

    static void kick(Context c) {
        c.sendBroadcast(new Intent(c, Fresh.class).setAction(TICK));
    }

    static void check(Context c, BroadcastReceiver.PendingResult pr) {
        final Context a = c.getApplicationContext();
        try {
            if (web != null || !anyWidget(a) || !hasAccess()) {
                pr.finish();
                return;
            }
            SharedPreferences sp = prefs(a);
            long at = sp.getLong("at", 0), now = System.currentTimeMillis();
            boolean same = sig().equals(sp.getString("sig", "")) && day(at).equals(day(now)) && now - at < 6 * 3600000L;
            if (same) {
                pr.finish();
                return;
            }
            run(a, pr);
        } catch (Throwable t) {
            try { pr.finish(); } catch (Throwable ignored) { }
        }
    }

    static void run(final Context a, BroadcastReceiver.PendingResult pr) {
        final Handler h = new Handler(Looper.getMainLooper());
        held = pr;
        WebView w = new WebView(a);
        web = w;
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        w.addJavascriptInterface(new Bridge(a, h), "Native");
        w.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return serve(a, r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                end();
                return true;
            }
        });
        giveUp = new Runnable() {
            @Override
            public void run() {
                end();
            }
        };
        h.postDelayed(giveUp, GIVE_UP_MS);
        w.loadUrl(MainActivity.ORIGIN + "#widget");
    }

    static void end() {
        if (giveUp != null) new Handler(Looper.getMainLooper()).removeCallbacks(giveUp);
        giveUp = null;
        if (web != null) {
            try {
                web.stopLoading();
                web.destroy();
            } catch (Throwable ignored) { }
            web = null;
        }
        if (held != null) {
            try { held.finish(); } catch (Throwable ignored) { }
            held = null;
        }
    }

    static WebResourceResponse serve(Context a, Uri u) {
        if (!MainActivity.HOST.equals(u.getHost())) return null;
        String p = u.getPath() == null ? "/" : u.getPath();
        String name = p.equals("/") || p.equals("/index.html") ? "index.html" : p.substring(1);
        try {
            if (name.isEmpty() || name.contains("..")) throw new Exception("no");
            InputStream in = a.getAssets().open(name);
            String type = "application/octet-stream";
            if (name.endsWith(".html")) type = "text/html";
            else if (name.endsWith(".woff")) type = "font/woff";
            else if (name.endsWith(".js") || name.endsWith(".mjs")) type = "text/javascript";
            else if (name.endsWith(".css")) type = "text/css";
            else if (name.endsWith(".svg")) type = "image/svg+xml";
            else if (name.endsWith(".png")) type = "image/png";
            Map<String, String> head = new HashMap<String, String>();
            head.put("Cache-Control", "no-store");
            return new WebResourceResponse(type, type.startsWith("text/") ? "UTF-8" : null, 200, "OK", head, in);
        } catch (Exception e) {
            return new WebResourceResponse("text/plain", "UTF-8", 404, "Not found", new HashMap<String, String>(),
                    new ByteArrayInputStream(new byte[0]));
        }
    }

    static class Bridge {
        final Context a;
        final Handler h;

        Bridge(Context a, Handler h) {
            this.a = a;
            this.h = h;
        }

        @JavascriptInterface
        public boolean hasAccess() {
            return Fresh.hasAccess();
        }

        @JavascriptInterface
        public String read(String name) {
            return MainActivity.readSafe(name);
        }

        @JavascriptInterface
        public void widget(String json) {
            Wid.setData(a, json);
            mark(a);
        }

        @JavascriptInterface
        public void plan(String json) {
            Alerts.setPlan(a, json);
        }

        @JavascriptInterface
        public boolean canNotify() {
            return Alerts.allowed(a);
        }

        @JavascriptInterface
        public void done() {
            h.post(new Runnable() {
                @Override
                public void run() {
                    end();
                }
            });
        }
    }
}
