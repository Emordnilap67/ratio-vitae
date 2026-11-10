package io.github.emordnilap67.ratiovitae;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class MoneyWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int w : ids) m.updateAppWidget(w, Wid.moneyNow(c));
    }
}
