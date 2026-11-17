package io.github.emordnilap67.ratiovitae;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class InvestWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) m.updateAppWidget(w, Wid.investNow(c, w));
        Fresh.schedule(c);
        Fresh.kick(c);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        for (int w : ids) Wid.investForget(c, w);
    }
}
