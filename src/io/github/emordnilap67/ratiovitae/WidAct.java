package io.github.emordnilap67.ratiovitae;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class WidAct extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            run(getIntent());
        } catch (Throwable t) {
            Toast.makeText(getApplicationContext(), "Ratio Vitae: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void run(Intent i) {
        Context c = getApplicationContext();
        String what = i == null ? null : i.getStringExtra("do");
        String slot = i == null || i.getStringExtra("slot") == null ? "" : i.getStringExtra("slot");
        if (what == null || "open".equals(what)) {
            String act = i == null || i.getStringExtra("act") == null ? "" : i.getStringExtra("act");
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("page", i == null ? 0 : i.getIntExtra("page", 0)).putExtra("act", act));
            return;
        }
        String r = "ok";
        if ("take".equals(what)) r = Wid.take(c, slot);
        else if ("undo".equals(what)) Wid.undo(c);
        else if ("eat".equals(what)) {
            int n = 1;
            try { n = Integer.parseInt(slot); } catch (Exception ignored) { }
            r = Wid.eat(c, n);
        } else if ("eggundo".equals(what)) Wid.eggUndo(c);
        if (!"ok".equals(r)) Toast.makeText(c, "Ratio Vitae: " + r, Toast.LENGTH_LONG).show();
        else if ("take".equals(what) || "eat".equals(what)) Wid.later(c);
        Wid.updateAll(c);
    }
}
