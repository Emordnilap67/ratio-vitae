package io.github.emordnilap67.ratiovitae;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PillsWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) m.updateAppWidget(w, Wid.pillsNow(c));
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (Wid.TAKE.equals(a)) {
            String r = Wid.take(c, i.getStringExtra("slot") == null ? "" : i.getStringExtra("slot"));
            if (!"ok".equals(r)) Toast.makeText(c, "Ratio Vitae: " + r, Toast.LENGTH_LONG).show();
            else Wid.later(c);
            Wid.updateAll(c);
            return;
        }
        if (Wid.UNDO.equals(a)) {
            Wid.undo(c);
            Wid.updateAll(c);
            return;
        }
        if (Wid.EGG.equals(a)) {
            int n = 1;
            try { n = Integer.parseInt(i.getStringExtra("slot")); } catch (Exception ignored) { }
            String r = Wid.eat(c, n);
            if (!"ok".equals(r)) Toast.makeText(c, "Ratio Vitae: " + r, Toast.LENGTH_LONG).show();
            else Wid.later(c);
            Wid.updateAll(c);
            return;
        }
        if (Wid.EGGUNDO.equals(a)) {
            Wid.eggUndo(c);
            Wid.updateAll(c);
            return;
        }
        if (Wid.REFRESH.equals(a)) {
            Wid.updateAll(c);
            return;
        }
        super.onReceive(c, i);
    }
}
