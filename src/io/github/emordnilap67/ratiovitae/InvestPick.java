package io.github.emordnilap67.ratiovitae;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class InvestPick extends Activity {
    private int wid = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        wid = in == null ? AppWidgetManager.INVALID_APPWIDGET_ID
                : in.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        final float dp = getResources().getDisplayMetrics().density;
        LinearLayout L = new LinearLayout(this);
        L.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * dp);
        L.setPadding(p, p, p, (int) (12 * dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B0E1C);
        bg.setCornerRadius(22 * dp);
        L.setBackground(bg);

        TextView t = new TextView(this);
        t.setText("Which account?");
        t.setTextColor(0xFF4FC38A);
        t.setTextSize(20);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        L.addView(t);
        TextView s = new TextView(this);
        s.setText("This copy of the Investments widget shows one account. Put another copy on your home screen for another one.");
        s.setTextColor(0xFF9AA0C0);
        s.setTextSize(13.5f);
        s.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        L.addView(s);

        JSONObject iv = Wid.data(this).optJSONObject("invest");
        JSONArray A = iv == null ? null : iv.optJSONArray("accounts");
        String cur = Wid.investKey(this, wid);
        int n = 0;
        for (int k = 0; A != null && k < A.length(); k++) {
            JSONObject a = A.optJSONObject(k);
            if (a == null || a.optString("n").isEmpty()) continue;
            L.addView(row(a.optString("n"), a.optString("v") + " · " + a.optString("s"), a.optString("n"), a.optString("n").equals(cur), dp));
            n++;
        }
        if (n == 0) {
            TextView e = new TextView(this);
            e.setText(iv == null ? "Open Ratio Vitae once so it can list your accounts - for now this copy shows everything."
                    : "No investment accounts yet - tap Import or + Account in the widget.");
            e.setTextColor(0xFFFFD75F);
            e.setTextSize(13);
            e.setPadding(0, 0, 0, (int) (10 * dp));
            L.addView(e);
        }
        L.addView(row("All investments", "every account added up" + (iv != null && !iv.optString("total").isEmpty() ? " · " + iv.optString("total") : ""), "", "".equals(cur), dp));

        ScrollView sv = new ScrollView(this);
        sv.addView(L);
        setContentView(sv);
    }

    private View row(String name, String sub, final String key, boolean on, float dp) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (14 * dp);
        r.setPadding(p, (int) (11 * dp), p, (int) (11 * dp));
        GradientDrawable g = new GradientDrawable();
        g.setColor(on ? 0x334FC38A : 0x14FFFFFF);
        g.setStroke((int) Math.max(1, dp), on ? 0xFF4FC38A : 0x33FFFFFF);
        g.setCornerRadius(16 * dp);
        r.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * dp);
        r.setLayoutParams(lp);
        TextView a = new TextView(this);
        a.setText((on ? "✓ " : "") + name);
        a.setTextColor(Color.WHITE);
        a.setTextSize(16);
        a.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(a);
        TextView b = new TextView(this);
        b.setText(sub);
        b.setTextColor(0xFF9AA0C0);
        b.setTextSize(12.5f);
        b.setMaxLines(2);
        r.addView(b);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setClickable(true);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(key);
            }
        });
        return r;
    }

    private void pick(String key) {
        Wid.investKey(this, wid, key);
        AppWidgetManager.getInstance(this).updateAppWidget(wid, Wid.investNow(this, wid));
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
        finish();
    }
}
