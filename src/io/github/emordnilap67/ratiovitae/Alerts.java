package io.github.emordnilap67.ratiovitae;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class Alerts extends BroadcastReceiver {
    static final String FIRE = "io.github.emordnilap67.ratiovitae.ALERT";
    static final long LATE_OK = 6 * 3600000L;

    static final String[][] CHANNELS = {
            {"bills", "Bills coming up", "The day before a bill is due, at 9 AM"},
            {"pills", "Pills today", "Every day at 8 AM: the morning and night sets, and what to order"},
            {"logged", "Pills logged", "Each time pills are logged in the app"}};

    @Override
    public void onReceive(Context c, Intent i) {
        if (FIRE.equals(i.getAction())) fire(c);
        schedule(c);
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("alerts", Context.MODE_PRIVATE);
    }

    static void setPlan(Context c, String json) {
        try {
            new JSONArray(json);
        } catch (Exception e) {
            return;
        }
        prefs(c).edit().putString("plan", json).apply();
        fire(c);
        schedule(c);
    }

    static boolean has(JSONArray a, String s) throws Exception {
        for (int k = 0; k < a.length(); k++) if (s.equals(a.getString(k))) return true;
        return false;
    }

    static void fire(Context c) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        try {
            JSONArray plan = new JSONArray(p.getString("plan", "[]"));
            JSONArray shown = new JSONArray(p.getString("shown", "[]"));
            JSONArray keep = new JSONArray();
            for (int k = 0; k < shown.length(); k++) {
                String s = shown.getString(k);
                long at = Long.parseLong(s.substring(s.lastIndexOf('@') + 1));
                if (at > now - 3 * 86400000L) keep.put(s);
            }
            for (int k = 0; k < plan.length(); k++) {
                JSONObject o = plan.getJSONObject(k);
                long at = o.getLong("at");
                if (at > now || at < now - LATE_OK) continue;
                String key = o.getString("id") + "@" + at;
                if (has(keep, key)) continue;
                post(c, o.getString("id"), o.optString("ch", "bills"), o.getString("title"), o.getString("text"), o.optInt("page", 0));
                keep.put(key);
            }
            p.edit().putString("shown", keep.toString()).apply();
        } catch (Exception ignored) {

        }
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, 7, new Intent(c, Alerts.class).setAction(FIRE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        long next = Long.MAX_VALUE, now = System.currentTimeMillis();
        try {
            JSONArray plan = new JSONArray(prefs(c).getString("plan", "[]"));
            for (int k = 0; k < plan.length(); k++) {
                long at = plan.getJSONObject(k).getLong("at");
                if (at > now && at < next) next = at;
            }
        } catch (Exception ignored) {
            return;
        }
        if (next == Long.MAX_VALUE) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
    }

    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 33
                || c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    static void post(Context c, String id, String ch, String title, String text, int page) {
        if (!allowed(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        for (String[] k : CHANNELS) {
            NotificationChannel nc = new NotificationChannel(k[0], k[1], NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(k[2]);
            nm.createNotificationChannel(nc);
        }
        Intent open = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("page", page);
        PendingIntent pi = PendingIntent.getActivity(c, id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int icon = c.getResources().getIdentifier("ic_note", "drawable", c.getPackageName());
        int nl = text.indexOf('\n');
        Notification n = new Notification.Builder(c, ch)
                .setSmallIcon(icon != 0 ? icon : android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(nl >= 0 ? text.substring(0, nl) : text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor("bills".equals(ch) ? 0xFFFFD75F : 0xFFFF8787)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        nm.notify(id, 1, n);
    }

    static void cancel(Context c, String id) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(id, 1);
    }
}
