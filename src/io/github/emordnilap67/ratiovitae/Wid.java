package io.github.emordnilap67.ratiovitae;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Locale;

public class Wid {
    static final String TAKE = "io.github.emordnilap67.ratiovitae.WTAKE";
    static final String UNDO = "io.github.emordnilap67.ratiovitae.WUNDO";
    static final String REFRESH = "io.github.emordnilap67.ratiovitae.WREFRESH";
    static final long UNDO_MS = 10000L;
    static final String EGG = "io.github.emordnilap67.ratiovitae.WEGG";
    static final String EGGUNDO = "io.github.emordnilap67.ratiovitae.WEGGUNDO";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("widget", Context.MODE_PRIVATE);
    }

    static int res(Context c, String name, String type) {
        return c.getResources().getIdentifier(name, type, c.getPackageName());
    }

    static int id(Context c, String name) {
        return res(c, name, "id");
    }

    static void setData(Context c, String json) {
        try {
            new JSONObject(json);
        } catch (Exception e) {
            return;
        }
        prefs(c).edit().putString("data", json).apply();
        updateAll(c);
    }

    static JSONObject data(Context c) {
        try {
            return new JSONObject(prefs(c).getString("data", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int w : m.getAppWidgetIds(new ComponentName(c, MoneyWidget.class))) m.updateAppWidget(w, moneyNow(c));
        for (int w : m.getAppWidgetIds(new ComponentName(c, PillsWidget.class))) m.updateAppWidget(w, pillsNow(c));
    }

    static boolean scrolls() {
        return android.os.Build.VERSION.SDK_INT >= 31;
    }

    static RemoteViews moneyNow(Context c) {
        if (scrolls()) {
            try {
                return money2(c);
            } catch (Throwable t) {

            }
        }
        return money(c);
    }

    static RemoteViews pillsNow(Context c) {
        if (scrolls()) {
            try {
                return pills2(c);
            } catch (Throwable t) {

            }
        }
        return pills(c);
    }

    static String pillDay() {
        Calendar k = Calendar.getInstance();
        if (k.get(Calendar.HOUR_OF_DAY) < 4) k.add(Calendar.DAY_OF_MONTH, -1);
        return String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    static String hm() {
        Calendar k = Calendar.getInstance();
        return String.format(Locale.US, "%02d:%02d", k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE));
    }

    static String clock() {
        Calendar k = Calendar.getInstance();
        int h = k.get(Calendar.HOUR_OF_DAY);
        return ((h + 11) % 12 + 1) + ":" + String.format(Locale.US, "%02d", k.get(Calendar.MINUTE)) + (h < 12 ? " AM" : " PM");
    }

    static String today() {
        Calendar k = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    static int colour(String hex, int dflt) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return dflt;
        }
    }

    static PendingIntent open(Context c, int req, int page, String act) {
        Intent i = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("page", page).putExtra("act", act);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent cast(Context c, int req, String action, String slot) {
        Intent i = new Intent(c, PillsWidget.class).setAction(action).putExtra("slot", slot);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String asOf(JSONObject d) {
        String day = d.optString("atDay", "");
        if (day.isEmpty()) return "open the app once";
        return day.equals(today()) ? "as of " + d.optString("atText", "") : "as of " + day.substring(5).replace('-', '/') + " - open the app";
    }

    static RemoteViews money(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), res(c, "w_money", "layout"));
        JSONObject d = data(c), m = d.optJSONObject("money");
        v.setTextViewText(id(c, "w_at"), asOf(d));
        if (m == null) {
            v.setTextViewText(id(c, "w_left"), "—");
            v.setTextViewText(id(c, "w_tier"), "open Ratio Vitae once");
        } else {
            v.setTextViewText(id(c, "w_lk"), ("LEFT NOW · " + m.optString("month", "")).toUpperCase(Locale.US));
            v.setTextViewText(id(c, "w_left"), m.optString("left", "—"));
            v.setTextColor(id(c, "w_left"), colour(m.optString("leftHex"), Color.WHITE));
            v.setTextViewText(id(c, "w_tier"), m.optString("inOut", ""));
            v.setTextViewText(id(c, "w_kk"), m.optString("main", "Main account"));
            v.setTextViewText(id(c, "w_koho"), m.optString("koho", "—"));
            v.setTextColor(id(c, "w_koho"), colour(m.optString("kohoHex"), Color.WHITE));
            v.setTextViewText(id(c, "w_after"), m.optString("after", "—"));
            v.setTextColor(id(c, "w_after"), colour(m.optString("afterHex"), Color.WHITE));
            String np = m.optString("nextPay", "");
            v.setTextViewText(id(c, "w_next"), "Next: " + m.optString("next", "") + (np.isEmpty() ? "" : "  ›"));
            v.setOnClickPendingIntent(id(c, "w_next"), open(c, 305, 0, np.isEmpty() ? "bills" : "pay:" + np));
            String low = m.optString("low", "");
            v.setTextViewText(id(c, "w_low"), low.isEmpty() ? "" : "Lowest: " + low);
            v.setTextColor(id(c, "w_low"), colour(m.optString("lowHex"), 0xFFD8D4F0));
        }
        v.setOnClickPendingIntent(id(c, "w_open"), open(c, 300, 0, ""));
        v.setOnClickPendingIntent(id(c, "w_b1"), open(c, 301, 0, "spent"));
        v.setOnClickPendingIntent(id(c, "w_b2"), open(c, 302, 0, "in"));
        v.setOnClickPendingIntent(id(c, "w_b3"), open(c, 303, 0, "bills"));
        v.setOnClickPendingIntent(id(c, "w_b4"), open(c, 304, 0, "import"));
        return v;
    }

    static JSONObject set(JSONObject p, String slot) {
        JSONArray a = p == null ? null : p.optJSONArray("sets");
        if (a == null) return null;
        for (int k = 0; k < a.length(); k++) {
            JSONObject s = a.optJSONObject(k);
            if (s != null && slot.equals(s.optString("slot"))) return s;
        }
        return null;
    }

    static RemoteViews pills(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), res(c, "w_pills", "layout"));
        JSONObject d = data(c), p = d.optJSONObject("pills");
        SharedPreferences sp = prefs(c);
        String day = pillDay();
        boolean sameDay = p != null && day.equals(p.optString("day"));
        v.setTextViewText(id(c, "w_at"), asOf(d));
        v.setTextViewText(id(c, "w_ph"), p == null ? "Pills - open Ratio Vitae once" : "Pills · " + (sameDay ? p.optString("dayText", "") : "a new day"));
        String[] slots = {"am", "pm"};
        for (int k = 0; k < 2; k++) {
            int n = k + 1;
            JSONObject s = set(p, slots[k]);
            int row = id(c, "w_r" + n), btn = id(c, "w_t" + n);
            if (s == null) {
                v.setViewVisibility(row, android.view.View.GONE);
                continue;
            }
            v.setViewVisibility(row, android.view.View.VISIBLE);
            String took = sp.getString("took_" + day + "_" + slots[k], "");
            boolean undo = slots[k].equals(sp.getString("undo_slot", "")) && System.currentTimeMillis() < sp.getLong("undo_until", 0);
            boolean done = !took.isEmpty() || (sameDay && s.optInt("left", 1) == 0);
            int count = s.optInt("n", 0);
            int left = sameDay ? s.optInt("left", count) : count;
            v.setTextViewText(id(c, "w_n" + n), s.optString("label") + " · " + count + (count == 1 ? " pill" : " pills"));
            v.setTextViewText(id(c, "w_s" + n), done ? "taken" + (took.isEmpty() ? "" : " from the widget") : (left < count ? left + " still to take · " : "") + s.optString("names"));
            if (undo) {
                v.setTextViewText(btn, "Undo");
                v.setInt(btn, "setBackgroundResource", res(c, "wbtn", "drawable"));
                v.setTextColor(btn, Color.WHITE);
                v.setOnClickPendingIntent(btn, cast(c, 410 + k, UNDO, slots[k]));
            } else if (done) {
                String when = took.isEmpty() ? s.optString("done", "") : took;
                v.setTextViewText(btn, "✓ " + (when.isEmpty() ? "Done" : when));
                v.setInt(btn, "setBackgroundResource", res(c, "wdone", "drawable"));
                v.setTextColor(btn, 0xFF4FC38A);
                v.setOnClickPendingIntent(btn, open(c, 420 + k, 4, "health"));
            } else {
                v.setTextViewText(btn, left < count ? "Take the rest" : "Take all");
                v.setInt(btn, "setBackgroundResource", res(c, "wtake", "drawable"));
                v.setTextColor(btn, 0xFF1A0606);
                v.setOnClickPendingIntent(btn, cast(c, 400 + k, TAKE, slots[k]));
            }
        }
        StringBuilder low = new StringBuilder();
        JSONArray la = p == null ? null : p.optJSONArray("low");
        if (la != null) for (int k = 0; k < la.length(); k++) low.append(k == 0 ? "" : "  ·  ").append(la.optString(k));
        v.setTextViewText(id(c, "w_low"), low.length() == 0 ? "" : "Order: " + low);
        StringBuilder sh = new StringBuilder();
        JSONArray sa = p == null ? null : p.optJSONArray("shots");
        if (sa != null) for (int k = 0; k < sa.length(); k++) sh.append(k == 0 ? "" : "  ·  ").append(sa.optString(k));
        v.setTextViewText(id(c, "w_shot"), sh.toString());
        v.setOnClickPendingIntent(id(c, "w_open"), open(c, 430, 4, "health"));
        eggsRow(c, v, d.optJSONObject("eggs"));
        return v;
    }

    static final String[] MON = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    static Calendar day(String iso) {
        Calendar k = Calendar.getInstance();
        try {
            String[] p = iso.split("-");
            k.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2].substring(0, 2)), 12, 0, 0);
        } catch (Exception e) {
            k.set(Calendar.HOUR_OF_DAY, 12);
        }
        k.set(Calendar.MILLISECOND, 0);
        return k;
    }

    static void eggsRow(Context c, RemoteViews v, JSONObject e) {
        int row = id(c, "w_re");
        if (e == null) {
            v.setViewVisibility(row, android.view.View.GONE);
            return;
        }
        v.setViewVisibility(row, android.view.View.VISIBLE);
        SharedPreferences sp = prefs(c);
        long have = Math.round(e.optDouble("have", 0));
        v.setTextViewText(id(c, "w_en"), have + (have == 1 ? " egg" : " eggs"));
        String sub;
        int col = 0xFF9AA0C0;
        if (have <= 0) {
            sub = "none left";
            col = colour(e.optString("red"), 0xFFFF5F5F);
        } else {
            Calendar around = day(e.optString("asof", today()));
            around.add(Calendar.DAY_OF_MONTH, (int) (have / 3) - 3);
            long gone = Math.round((around.getTimeInMillis() - day(today()).getTimeInMillis()) / 86400000.0);
            sub = "lasts " + (have / 4) + "–" + (have / 2) + " days · order ~" + MON[around.get(Calendar.MONTH)] + " " + around.get(Calendar.DAY_OF_MONTH);
            if (gone <= 0) col = colour(e.optString("red"), 0xFFFF5F5F);
            else if (gone <= 3) col = colour(e.optString("warn"), 0xFFFFAF5F);
        }
        boolean undo = System.currentTimeMillis() < sp.getLong("egg_until", 0);
        if (undo) sub = "was " + sp.getLong("egg_was", 0) + " · " + sub;
        v.setTextViewText(id(c, "w_es"), sub);
        v.setTextColor(id(c, "w_es"), col);
        v.setOnClickPendingIntent(id(c, "w_eo"), open(c, 460, 4, "eggs"));
        for (int k = 1; k <= 3; k++) {
            int b = id(c, "w_e" + k);
            if (undo) {
                v.setViewVisibility(b, k == 1 ? android.view.View.VISIBLE : android.view.View.GONE);
                if (k == 1) {
                    v.setTextViewText(b, "Undo");
                    v.setOnClickPendingIntent(b, cast(c, 470, EGGUNDO, ""));
                }
            } else {
                v.setViewVisibility(b, android.view.View.VISIBLE);
                v.setTextViewText(b, "Ate " + k);
                v.setOnClickPendingIntent(b, cast(c, 450 + k, EGG, String.valueOf(k)));
            }
        }
    }

    static int lineAt(String[] L, String line) {
        int at = -1;
        for (int k = 0; k < L.length; k++) {
            String s = L[k].endsWith("\r") ? L[k].substring(0, L[k].length() - 1) : L[k];
            if (s.equals(line)) {
                if (at >= 0) return -2;
                at = k;
            }
        }
        return at;
    }

    static String joinLines(String[] L) {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < L.length; k++) b.append(k == 0 ? "" : "\n").append(L[k]);
        return b.toString();
    }

    static boolean swapLine(String from, String to) throws Exception {
        File f = new File(MainActivity.folder(), "groceries_trips.csv");
        String[] L = readText(f).split("\n", -1);
        int at = lineAt(L, from);
        if (at < 0) return false;
        L[at] = to + (L[at].endsWith("\r") ? "\r" : "");
        write(f, joinLines(L), false);
        return true;
    }

    static String eat(Context c, int n) {
        JSONObject d = data(c), e = d.optJSONObject("eggs");
        if (e == null) return "open Ratio Vitae once";
        if (!e.optBoolean("plain", false)) return "a grocery trip row is not plain csv - open Ratio Vitae";
        String line = e.optString("line", "");
        int hi = e.optInt("hi", -1), ai = e.optInt("ai", -1);
        long have = Math.round(e.optDouble("have", -1));
        if (line.isEmpty() || hi < 0) return "open Ratio Vitae once";
        if (have <= 0) return "no eggs left to take off";
        String[] fl = line.split(",", -1);
        if (hi >= fl.length || ai >= fl.length) return "the eggs row changed - open Ratio Vitae";
        long nw = Math.max(0, have - n);
        String was = e.optString("asof", "");
        fl[hi] = String.valueOf(nw);
        if (ai >= 0) fl[ai] = today();
        StringBuilder nl = new StringBuilder();
        for (int k = 0; k < fl.length; k++) nl.append(k == 0 ? "" : ",").append(fl[k]);
        try {
            if (!swapLine(line, nl.toString())) return "the eggs row changed - open Ratio Vitae";
            e.put("have", nw).put("asof", today()).put("line", nl.toString());
            d.put("eggs", e);
            prefs(c).edit().putString("data", d.toString()).putString("egg_old", line).putString("egg_new", nl.toString())
                    .putLong("egg_was", have).putString("egg_wasasof", was).putLong("egg_until", System.currentTimeMillis() + UNDO_MS).apply();
            return "ok";
        } catch (Exception x) {
            return "could not write: " + x.getMessage();
        }
    }

    static void eggUndo(Context c) {
        SharedPreferences sp = prefs(c);
        String old = sp.getString("egg_old", ""), nw = sp.getString("egg_new", "");
        if (old.isEmpty() || System.currentTimeMillis() > sp.getLong("egg_until", 0) + 5000) return;
        try {
            if (swapLine(nw, old)) {
                JSONObject d = data(c), e = d.optJSONObject("eggs");
                if (e != null) {
                    e.put("have", sp.getLong("egg_was", 0)).put("asof", sp.getString("egg_wasasof", today())).put("line", old);
                    d.put("eggs", e);
                    sp.edit().putString("data", d.toString()).apply();
                }
            }
        } catch (Exception ignored) {

        }
        sp.edit().remove("egg_old").remove("egg_new").remove("egg_until").apply();
    }

    static String readText(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) b.write(buf, 0, r);
            return b.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    static void write(File f, String text, boolean append) throws Exception {
        FileOutputStream out = new FileOutputStream(f, append);
        try {
            out.write(text.getBytes("UTF-8"));
            out.getFD().sync();
        } finally {
            out.close();
        }
    }

    static String take(Context c, String slot) {
        JSONObject p = data(c).optJSONObject("pills"), s = set(p, slot);
        if (s == null) return "open Ratio Vitae once";
        String day = pillDay();
        boolean sameDay = day.equals(p.optString("day"));
        JSONArray tails = sameDay ? s.optJSONArray("todo") : s.optJSONArray("rows");
        if (tails == null || tails.length() == 0) return "nothing left to take";
        String time = hm();
        StringBuilder rows = new StringBuilder();
        for (int k = 0; k < tails.length(); k++) {
            String t = tails.optString(k);
            if (t.split(",", -1).length != 5) return "the set changed - open Ratio Vitae";
            rows.append(day).append(',').append(time).append(',').append(t).append('\n');
        }
        try {
            File f = new File(MainActivity.folder(), "pills_taken.csv");
            if (!f.isFile()) return "pills_taken.csv was not found";
            String text = readText(f);
            String add = (text.isEmpty() || text.endsWith("\n")) ? rows.toString() : "\n" + rows;
            write(f, add, true);
            prefs(c).edit().putString("took_" + day + "_" + slot, clock())
                    .putString("undo_slot", slot).putString("undo_rows", add).putString("undo_day", day)
                    .putLong("undo_until", System.currentTimeMillis() + UNDO_MS).apply();
            Alerts.post(c, "pilltook", "logged", "Pills logged · " + s.optString("label"),
                    s.optString("names") + "\nfrom the widget at " + clock(), 4);
            return "ok";
        } catch (Exception e) {
            return "could not write: " + e.getMessage();
        }
    }

    static void undo(Context c) {
        SharedPreferences sp = prefs(c);
        String rows = sp.getString("undo_rows", ""), slot = sp.getString("undo_slot", ""), day = sp.getString("undo_day", "");
        if (rows.isEmpty() || System.currentTimeMillis() > sp.getLong("undo_until", 0) + 5000) return;
        try {
            File f = new File(MainActivity.folder(), "pills_taken.csv");
            String text = readText(f);
            if (text.endsWith(rows)) write(f, text.substring(0, text.length() - rows.length()), false);
        } catch (Exception ignored) {

        }
        sp.edit().remove("took_" + day + "_" + slot).remove("undo_rows").remove("undo_slot").remove("undo_until").apply();
        Alerts.cancel(c, "pilltook");
    }

    static void later(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.set(AlarmManager.RTC, System.currentTimeMillis() + UNDO_MS + 300, cast(c, 440, REFRESH, ""));
    }

    static PendingIntent template(Context c, int req) {
        Intent i = new Intent(c, WidAct.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    static Intent goTo(int page, String act) {
        return new Intent().putExtra("do", "open").putExtra("page", page).putExtra("act", act);
    }

    static Intent doIt(String what, String slot) {
        return new Intent().putExtra("do", what).putExtra("slot", slot);
    }

    static RemoteViews item(Context c, String layout) {
        return new RemoteViews(c.getPackageName(), res(c, layout, "layout"));
    }

    static RemoteViews line(Context c, String icon, int iconCol, String title, String value, int valueCol, String sub, Intent tap,
                            String btn, Intent btnTap) {
        RemoteViews v = item(c, "wi_line");
        v.setTextViewText(id(c, "wi_ic"), icon);
        v.setTextColor(id(c, "wi_ic"), iconCol);
        v.setTextViewText(id(c, "wi_t"), title);
        v.setTextViewText(id(c, "wi_v"), value);
        v.setTextColor(id(c, "wi_v"), valueCol);
        v.setTextViewText(id(c, "wi_s"), sub);
        v.setViewVisibility(id(c, "wi_s"), sub.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        if (tap != null) v.setOnClickFillInIntent(id(c, "wi_root"), tap);
        if (btn != null) {
            v.setViewVisibility(id(c, "wi_b"), android.view.View.VISIBLE);
            v.setTextViewText(id(c, "wi_b"), btn);
            v.setOnClickFillInIntent(id(c, "wi_b"), btnTap);
        }
        return v;
    }

    static RemoteViews frame(Context c, String layout, String title, int listReq, RemoteViews.RemoteCollectionItems items, PendingIntent head) {
        RemoteViews v = new RemoteViews(c.getPackageName(), res(c, layout, "layout"));
        v.setTextViewText(id(c, "w_ph"), title);
        v.setTextViewText(id(c, "w_at"), asOf(data(c)));
        v.setOnClickPendingIntent(id(c, "w_open"), head);
        v.setRemoteAdapter(id(c, "w_list"), items);
        v.setPendingIntentTemplate(id(c, "w_list"), template(c, listReq));
        return v;
    }

    static RemoteViews money2(Context c) {
        JSONObject d = data(c), m = d.optJSONObject("money");
        RemoteViews.RemoteCollectionItems.Builder b = new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(7);

        RemoteViews L = item(c, "wi_left");
        if (m == null) {
            L.setTextViewText(id(c, "w_left"), "—");
            L.setTextViewText(id(c, "w_tier"), "open Ratio Vitae once");
        } else {
            L.setTextViewText(id(c, "w_lk"), ("LEFT NOW · " + m.optString("month", "")).toUpperCase(Locale.US));
            L.setTextViewText(id(c, "w_left"), m.optString("left", "—"));
            L.setTextColor(id(c, "w_left"), colour(m.optString("leftHex"), Color.WHITE));
            L.setTextViewText(id(c, "w_tier"), m.optString("inOut", ""));
            L.setTextViewText(id(c, "w_kk"), m.optString("main", "Main account"));
            L.setTextViewText(id(c, "w_koho"), m.optString("koho", "—"));
            L.setTextColor(id(c, "w_koho"), colour(m.optString("kohoHex"), Color.WHITE));
            L.setTextViewText(id(c, "w_after"), m.optString("after", "—"));
            L.setTextColor(id(c, "w_after"), colour(m.optString("afterHex"), Color.WHITE));
        }
        L.setOnClickFillInIntent(id(c, "wi_root"), goTo(0, "budget"));
        b.addItem(1, L);

        RemoteViews B = item(c, "wi_btns");
        B.setOnClickFillInIntent(id(c, "w_b1"), goTo(0, "spent"));
        B.setOnClickFillInIntent(id(c, "w_b2"), goTo(0, "in"));
        B.setOnClickFillInIntent(id(c, "w_b3"), goTo(0, "bills"));
        B.setOnClickFillInIntent(id(c, "w_b4"), goTo(0, "import"));
        b.addItem(2, B);

        JSONArray cats = m == null ? null : m.optJSONArray("cats");
        if (cats != null && cats.length() > 0) {
            RemoteViews K = item(c, "wi_cats");
            String[] ids = {"w_c1", "w_c2", "w_c3", "w_c4"};
            for (int k = 0; k < ids.length; k++) {
                JSONObject o = k < cats.length() ? cats.optJSONObject(k) : null;
                if (o == null) { K.setViewVisibility(id(c, ids[k]), android.view.View.INVISIBLE); continue; }
                String nm = o.optString("n", "");
                android.text.SpannableString sp = new android.text.SpannableString("● " + nm);
                sp.setSpan(new android.text.style.ForegroundColorSpan(colour(o.optString("col"), 0xFF9AA0C0)), 0, 1, 0);
                K.setTextViewText(id(c, ids[k]), sp);
                K.setOnClickFillInIntent(id(c, ids[k]), goTo(0, "cat:" + nm));
            }
            b.addItem(6, K);
        }
        if (m != null) {
            String np = m.optString("nextPay", "");
            b.addItem(3, line(c, "📅", 0xFFAFAFFF, "NEXT BILL", m.optString("next", "no bills left to pay"), 0xFFFFFFFF, np.isEmpty() ? "" : "tap to pay it",
                    goTo(0, np.isEmpty() ? "bills" : "pay:" + np), null, null));
            String low = m.optString("low", "");
            if (!low.isEmpty()) b.addItem(4, line(c, "↘", colour(m.optString("lowHex"), 0xFFD8D4F0), "LOWEST POINT", low,
                    colour(m.optString("lowHex"), 0xFFD8D4F0), "before the next payday", goTo(0, "bills"), null, null));
        }

        RemoteViews E = eggsItem(c, d.optJSONObject("eggs"));
        if (E != null) b.addItem(5, E);
        String month = m == null ? "" : m.optString("month", "");
        return frame(c, "w_money2", "Money" + (month.isEmpty() ? "" : " · " + month), 500, b.build(), open(c, 300, 0, ""));
    }

    static RemoteViews eggsItem(Context c, JSONObject e) {
        if (e == null) return null;
        RemoteViews v = item(c, "wi_eggs");
        SharedPreferences sp = prefs(c);
        long have = Math.round(e.optDouble("have", 0));
        v.setTextViewText(id(c, "w_en"), have + (have == 1 ? " egg" : " eggs"));
        String sub;
        int col = 0xFF9AA0C0;
        if (have <= 0) {
            sub = "none left";
            col = colour(e.optString("red"), 0xFFFF5F5F);
        } else {
            Calendar around = day(e.optString("asof", today()));
            around.add(Calendar.DAY_OF_MONTH, (int) (have / 3) - 3);
            long gone = Math.round((around.getTimeInMillis() - day(today()).getTimeInMillis()) / 86400000.0);
            sub = "lasts " + (have / 4) + "–" + (have / 2) + " days · order ~" + MON[around.get(Calendar.MONTH)] + " " + around.get(Calendar.DAY_OF_MONTH);
            if (gone <= 0) col = colour(e.optString("red"), 0xFFFF5F5F);
            else if (gone <= 3) col = colour(e.optString("warn"), 0xFFFFAF5F);
        }
        boolean undo = System.currentTimeMillis() < sp.getLong("egg_until", 0);
        if (undo) sub = "was " + sp.getLong("egg_was", 0) + " · " + sub;
        v.setTextViewText(id(c, "w_es"), sub);
        v.setTextColor(id(c, "w_es"), col);
        v.setOnClickFillInIntent(id(c, "wi_root"), goTo(4, "eggs"));
        for (int k = 1; k <= 4; k++) {
            int b = id(c, "w_e" + k);
            if (undo) {
                v.setViewVisibility(b, k == 1 ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
                if (k == 1) {
                    v.setTextViewText(b, "Undo");
                    v.setOnClickFillInIntent(b, doIt("eggundo", ""));
                }
            } else {
                v.setViewVisibility(b, android.view.View.VISIBLE);
                v.setTextViewText(b, "Ate " + k);
                v.setOnClickFillInIntent(b, doIt("eat", String.valueOf(k)));
            }
        }
        return v;
    }

    static RemoteViews pills2(Context c) {
        JSONObject d = data(c), p = d.optJSONObject("pills"), hl = d.optJSONObject("health");
        SharedPreferences sp = prefs(c);
        String pday = pillDay();
        boolean sameDay = p != null && pday.equals(p.optString("day"));
        RemoteViews.RemoteCollectionItems.Builder b = new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(6);
        String[] slots = {"am", "pm"}, icons = {"☀", "☾"};
        int[] icol = {0xFFFFD75F, 0xFFAFAFFF};
        for (int k = 0; k < 2; k++) {
            JSONObject s = set(p, slots[k]);
            if (s == null) continue;
            RemoteViews v = item(c, "wi_set");
            String took = sp.getString("took_" + pday + "_" + slots[k], "");
            boolean undo = slots[k].equals(sp.getString("undo_slot", "")) && System.currentTimeMillis() < sp.getLong("undo_until", 0);
            boolean done = !took.isEmpty() || (sameDay && s.optInt("left", 1) == 0);
            int count = s.optInt("n", 0), left = sameDay ? s.optInt("left", count) : count;
            v.setTextViewText(id(c, "wi_ic"), icons[k]);
            v.setTextColor(id(c, "wi_ic"), icol[k]);
            v.setTextViewText(id(c, "wi_v"), s.optString("label") + " · " + count + (count == 1 ? " pill" : " pills"));
            v.setTextViewText(id(c, "wi_s"), done ? "taken" + (took.isEmpty() ? "" : " from the widget") + " · " + s.optString("names")
                    : (left < count ? left + " still to take · " : "") + s.optString("names"));
            int btn = id(c, "wi_b");
            if (undo) {
                v.setTextViewText(btn, "Undo");
                v.setInt(btn, "setBackgroundResource", res(c, "wbtn", "drawable"));
                v.setTextColor(btn, Color.WHITE);
                v.setOnClickFillInIntent(btn, doIt("undo", slots[k]));
            } else if (done) {
                String when = took.isEmpty() ? s.optString("done", "") : took;
                v.setTextViewText(btn, "✓ " + (when.isEmpty() ? "Done" : "Taken " + when));
                v.setInt(btn, "setBackgroundResource", res(c, "wdone", "drawable"));
                v.setTextColor(btn, 0xFF4FC38A);
                v.setOnClickFillInIntent(btn, goTo(4, "health"));
            } else {
                v.setTextViewText(btn, left < count ? "Take the rest" : "Take all");
                v.setInt(btn, "setBackgroundResource", res(c, "wtake", "drawable"));
                v.setTextColor(btn, 0xFF1A0606);
                v.setOnClickFillInIntent(btn, doIt("take", slots[k]));
            }
            v.setOnClickFillInIntent(id(c, "wi_root"), goTo(4, "health"));
            b.addItem(10 + k, v);
        }
        JSONObject bp = hl == null ? null : hl.optJSONObject("bp"), pu = hl == null ? null : hl.optJSONObject("pulse"),
                tp = hl == null ? null : hl.optJSONObject("temp");
        b.addItem(20, line(c, "♥", 0xFFFF8787, "BLOOD PRESSURE", bp == null ? "no readings yet" : bp.optString("text"),
                bp == null ? 0xFF9AA0C0 : colour(bp.optString("hex"), Color.WHITE), bp == null ? "" : bp.optString("sub"),
                goTo(4, "bpcard"), "+ BP", goTo(4, "bp")));
        if (pu != null) b.addItem(21, line(c, "〰", 0xFFFFAFD7, "HEART RATE", pu.optString("text"), Color.WHITE, pu.optString("sub"),
                goTo(4, "bpcard"), null, null));
        b.addItem(22, line(c, "🌡", 0xFFFFAF5F, "TEMPERATURE", tp == null ? "no readings yet" : tp.optString("text"),
                tp == null ? 0xFF9AA0C0 : colour(tp.optString("hex"), Color.WHITE), tp == null ? "" : tp.optString("sub"),
                goTo(4, "tempcard"), "+ Temp", goTo(4, "temp")));
        JSONArray la = p == null ? null : p.optJSONArray("low");
        if (la != null && la.length() > 0) {
            StringBuilder low = new StringBuilder();
            for (int k = 0; k < la.length(); k++) low.append(k == 0 ? "" : "\n").append(la.optString(k));
            b.addItem(23, line(c, "℞", 0xFFFFD75F, "ORDER SOON", la.length() == 1 ? la.optString(0) : la.length() + " bottles", 0xFFFFD75F,
                    la.length() == 1 ? "" : low.toString(), goTo(4, "health"), null, null));
        }
        JSONArray sa = p == null ? null : p.optJSONArray("shots");
        if (sa != null && sa.length() > 0) {
            StringBuilder sh = new StringBuilder();
            for (int k = 0; k < sa.length(); k++) sh.append(k == 0 ? "" : "  ·  ").append(sa.optString(k));
            b.addItem(24, line(c, "💉", 0xFFAFAFFF, "INJECTIONS", sh.toString(), 0xFFAFAFFF, "", goTo(4, "health"), null, null));
        }
        String title = p == null ? "Pills - open Ratio Vitae once" : "Pills · " + (sameDay ? p.optString("dayText", "") : "a new day");
        return frame(c, "w_pills2", title, 600, b.build(), open(c, 430, 4, "health"));
    }
}
