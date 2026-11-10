package io.github.emordnilap67.ratiovitae;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final String HOST = "ratiovitae.app";
    static final String ORIGIN = "https://ratiovitae.app/";

    static final String FOLDER = "Ratio Vitae";
    static final int BG = Color.rgb(5, 7, 15);
    static final int REQ_READ = 3;
    static final int REQ_NOTE = 4;
    static final int REQ_FILE = 5;

    static final int REQ_PHOTO = 6;
    Uri photoUri = null;

    static final int FILE_MAX = 25 * 1024 * 1024;

    static final String[] WRITABLE = {"pills_taken.csv", "weight_log.csv", "pills_log.csv",

            "budget_log.csv", "groceries_trips.csv", "food_log.csv", "cards.csv",

            "categories.csv", "credit_log.csv",

            "pills_fills.csv",

            "rdsp_holdings.csv", "rdsp_cash.csv", "crypto_holdings.csv", "bp_log.csv",

            "investments.csv",

            "goals.csv", "networth_log.csv", "grocery_list.csv",

            "temp_log.csv"};

    static final String[] READ_EXTRA = {"fx.txt"};

    private WebView web;
    private final Handler h = new Handler(Looper.getMainLooper());

    private volatile int pendingPage = -1;
    private volatile String pendingAct = "";
    private boolean askedNote = false;

    private final java.util.ArrayDeque<Object[]> inQ = new java.util.ArrayDeque<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                v.setPadding(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(),
                        in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom());
                return in;
            }
        });

        web = new WebView(this);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setBackgroundColor(BG);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "Native");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return serve(r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !HOST.equals(r.getUrl().getHost());
            }

            @Override
            public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                recreate();
                return true;
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            back();
                        }
                    });
        }
        pendingPage = getIntent() == null ? -1 : getIntent().getIntExtra("page", -1);
        pendingAct = getIntent() == null || getIntent().getStringExtra("act") == null ? "" : getIntent().getStringExtra("act");
        takeIntentFile(getIntent());
        web.loadUrl(ORIGIN);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        pendingPage = i.getIntExtra("page", -1);
        pendingAct = i.getStringExtra("act") == null ? "" : i.getStringExtra("act");
        takeIntentFile(i);
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == REQ_FILE && result == RESULT_OK && data != null) {
            if (data.getClipData() != null) for (int k = 0; k < data.getClipData().getItemCount(); k++) readIn(data.getClipData().getItemAt(k).getUri());
            else if (data.getData() != null) readIn(data.getData());
        }
        if (code == REQ_PHOTO && photoUri != null) {
            Uri u = photoUri;
            photoUri = null;
            if (result == RESULT_OK) readIn(u);
            try { getContentResolver().delete(u, null, null); } catch (Exception ignored) { }
        }
    }

    private void takeIntentFile(Intent i) {
        if (i == null) return;
        Uri u = null;
        if (Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())) {
            java.util.ArrayList<android.os.Parcelable> list = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list == null) return;
            for (android.os.Parcelable p : list) if (p instanceof Uri) readIn((Uri) p);
            i.setAction(Intent.ACTION_MAIN);
            i.removeExtra(Intent.EXTRA_STREAM);
            return;
        }
        if (Intent.ACTION_VIEW.equals(i.getAction())) u = i.getData();
        else if (Intent.ACTION_SEND.equals(i.getAction())) {
            Object x = i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (x instanceof Uri) u = (Uri) x;
        }
        if (u == null) return;
        readIn(u);
        i.setAction(Intent.ACTION_MAIN);
        i.setData(null);
        i.removeExtra(Intent.EXTRA_STREAM);
    }

    private void readIn(Uri u) {
        String name = "file";
        byte[] bytes = null;
        String err = null;
        try {
            Cursor c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst() && c.getString(0) != null) name = c.getString(0);
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {
            if (u.getLastPathSegment() != null) name = u.getLastPathSegment();
        }
        try {
            InputStream in = getContentResolver().openInputStream(u);
            if (in == null) throw new java.io.IOException("it could not be opened");
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[65536];
                int n;
                while ((n = in.read(b)) > 0) {
                    out.write(b, 0, n);
                    if (out.size() > FILE_MAX) throw new java.io.IOException("it is bigger than 25 MB");
                }
                bytes = out.toByteArray();
            } finally {
                in.close();
            }
        } catch (Exception e) {
            bytes = null;
            err = e.getMessage() == null ? "it could not be read" : e.getMessage();
        }
        synchronized (inQ) { inQ.addLast(new Object[]{name, bytes, err}); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();

            js("window.onResumeApp&&onResumeApp()");
        }
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (web == null) {
            moveTaskToBack(true);
            return;
        }
        web.evaluateJavascript("window.appBack?appBack():''", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String r) {
                if ("\"1\"".equals(r)) return;
                moveTaskToBack(true);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQ_READ || code == REQ_NOTE) js("window.onResumeApp&&onResumeApp()");
    }

    private void js(String code) {
        if (web != null) web.evaluateJavascript(code, null);
    }

    /** a new phone starts with every file empty - just its column names - in its own "Ratio Vitae" folder */
    static final String[][] HEADS = {
            {"budget_log.csv", "month,kind,group,label,amount,note"},
            {"cards.csv", "name,kind,rate,balance,limit,payment,left,note"},
            {"pills_taken.csv", "date,time,medication,amount,unit,tablets,note"},
            {"pills_fills.csv", "date,medication,quantity,unit,rx,pharmacy,note"},
            {"grocery_list.csv", "item,qty,each,added"},
            {"crypto_holdings.csv", "platform,holding,quantity,money_in,worth_now,funded,note"},
            {"rdsp_holdings.csv", "ticker,name,price_usd,shares,category,target,currency"},
            {"groceries_trips.csv", "month,date,store,channel,subtotal,tax,fees,tip,discounts,adjustments,total,note,eggs_brand,eggs_pack,eggs_price,eggs_have,eggs_asof,eggs_stock"},
            {"categories.csv", "kind,category,subcategory,icon,colour,match"},
            {"goals.csv", "name,what,target,by,note"},
            {"pills_log.csv", "date,medication,form,strength,unit,morning,night,flex,interval_days,status,prescriber,purpose,note"},
            {"investments.csv", "name,kind,value,money_in,date,note"},
            {"networth_log.csv", "date,net_worth,have,owe"},
            {"food_log.csv", "month,date,place,item,amount,note"},
            {"weight_log.csv", "month,pounds,note"},
            {"credit_log.csv", "month,equifax,transunion"},
            {"temp_log.csv", "date,time,celsius,note"},
            {"rdsp_cash.csv", "date,cash,note"},
            {"bp_log.csv", "date,time,systolic,diastolic,pulse,device,note"},
            {"grocery_items.csv", "month,date,store,item,qty,each,amount,note"},
            {"falls_log.csv", "date,time,severity,landed,injury,help,recovery,note"},
            {"dividends.csv", "ticker,rate,freq,yield_pct,currency,ex_div,pay_date"},
    };
    private static boolean seeded;

    static File folder() {
        File d = new File(Environment.getExternalStorageDirectory(), FOLDER);
        if (!seeded) {
            try {
                if (!d.isDirectory()) d.mkdirs();
                if (d.isDirectory()) {
                    for (String[] h : HEADS) {
                        File f = new File(d, h[0]);
                        if (!f.exists()) writeWhole(f, h[1] + "\n");
                    }
                    File fx = new File(d, "fx.txt");
                    if (!fx.exists()) writeWhole(fx, "FX = 1.400000   # US dollars -> Canadian dollars; a TD holdings import sets it\n");
                    seeded = true;
                }
            } catch (Exception ignored) {
                // no file access yet - asked on the first screen
            }
        }
        return d;
    }

    boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    void askAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Exception ignored) {

                }
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_READ);
        }
    }

    static boolean okName(String name) {
        return name != null && name.matches("[A-Za-z0-9_.\\-]{1,80}\\.csv") && !name.contains("..");
    }

    static void writeWhole(File f, String text) throws java.io.IOException {
        byte[] b = text.getBytes("UTF-8");
        File tmp = new File(f.getParentFile(), "." + f.getName() + ".rv");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(b);
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (!tmp.renameTo(f)) {
            FileOutputStream o2 = new FileOutputStream(f);
            try {
                o2.write(b);
                o2.getFD().sync();
            } finally {
                o2.close();
            }
            tmp.delete();
        }
    }

    static String readText(File f) throws java.io.IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    static boolean writable(String name) {
        if (!okName(name)) return false;
        for (String w : WRITABLE) if (w.equals(name)) return true;
        return false;
    }

    static String checkRows(String head, String rows) {
        int cols = head.split(",", -1).length;
        if (rows == null || rows.length() < 2 || !rows.endsWith("\n")) return "no rows to add";
        String[] ls = rows.substring(0, rows.length() - 1).split("\n", -1);
        for (String l : ls) {
            if (l.trim().isEmpty() || l.indexOf('"') >= 0 || l.indexOf('\r') >= 0) return "a row is not plain csv: " + l;
            if (l.split(",", -1).length != cols) return "a row has the wrong number of fields: " + l;
        }
        return null;
    }

    class Bridge {
        @JavascriptInterface
        public boolean hasAccess() {
            return MainActivity.this.hasAccess();
        }

        @JavascriptInterface
        public void askAccess() {
            h.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.askAccess();
                }
            });
        }

        @JavascriptInterface
        public String read(String name) {
            boolean extra = false;
            for (String x : READ_EXTRA) if (x.equals(name)) extra = true;
            if (!okName(name) && !extra) return "";
            File f = new File(folder(), name);
            if (!f.isFile()) return "";
            try {
                return readText(f);
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public synchronized String append(String name, String rows) {
            if (!writable(name)) return "the app cannot write to " + name;
            if (!MainActivity.this.hasAccess()) return "no file access - allow it in Settings";
            File f = new File(folder(), name);
            try {
                if (!f.isFile()) return name + " was not found in " + FOLDER;
                String text = readText(f);
                int nl = text.indexOf('\n');
                String head = (nl < 0 ? text : text.substring(0, nl)).replace("\r", "");
                String bad = checkRows(head, rows);
                if (bad != null) return bad;
                String add = (text.isEmpty() || text.endsWith("\n")) ? rows : "\n" + rows;
                FileOutputStream out = new FileOutputStream(f, true);
                try {
                    out.write(add.getBytes("UTF-8"));
                    out.getFD().sync();
                } finally {
                    out.close();
                }
                return "ok";
            } catch (Exception e) {
                return "could not write " + name + ": " + e.getMessage();
            }
        }

        @JavascriptInterface
        public synchronized String apply(String json) {
            if (!MainActivity.this.hasAccess()) return "no file access - allow it in Settings";
            try {
                JSONArray ops = new JSONArray(json);
                Map<String, List<String>> files = new LinkedHashMap<String, List<String>>();
                for (int i = 0; i < ops.length(); i++) {
                    JSONObject o = ops.getJSONObject(i);
                    String name = o.getString("f");
                    boolean fxOp = "fx.txt".equals(name) && "fx".equals(o.optString("op"));
                    if (!writable(name) && !fxOp) return "the app cannot write to " + name;
                    if ("fx.txt".equals(name) && !fxOp) return "the app cannot write to " + name;
                    List<String> lines = files.get(name);
                    if (lines == null) {
                        File f = new File(folder(), name);
                        if (!f.isFile()) return name + " was not found in " + FOLDER;
                        String text = readText(f);
                        if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
                        lines = new ArrayList<String>(Arrays.asList(text.split("\n", -1)));
                        files.put(name, lines);
                    }
                    String head = lines.get(0);
                    String op = o.getString("op");
                    if (op.equals("append")) {
                        String rows = o.getString("rows");
                        String bad = checkRows(head, rows);
                        if (bad != null) return bad;
                        lines.addAll(Arrays.asList(rows.substring(0, rows.length() - 1).split("\n", -1)));
                    } else if (op.equals("replace") || op.equals("delete")) {
                        String old = o.getString("old");
                        int at = -1, n = 0;
                        for (int k = 1; k < lines.size(); k++) if (lines.get(k).equals(old)) { at = k; n++; }
                        if (n != 1) return name + " changed since it was read (" + (n == 0 ? "line not found" : "line there " + n + " times") + ") - nothing written";
                        if (op.equals("delete")) lines.remove(at);
                        else {
                            String nw = o.getString("rows");
                            String bad = checkRows(head, nw + "\n");
                            if (bad != null) return bad;
                            lines.set(at, nw);
                        }
                    } else if (op.equals("insert")) {

                        String after = o.getString("after");
                        int at = -1, n = 0;
                        for (int k = 0; k < lines.size(); k++) if (lines.get(k).equals(after)) { at = k; n++; }
                        if (n != 1) return name + " changed since it was read - nothing written";
                        String nw = o.getString("rows");
                        String bad = checkRows(head, nw + "\n");
                        if (bad != null) return bad;
                        lines.add(at + 1, nw);
                    } else if (op.equals("fx")) {

                        String old = o.getString("old"), nw = o.getString("rows");
                        if (!nw.matches("\\d{1,2}\\.\\d{6}") || !old.matches("\\d{1,2}\\.\\d{1,8}")) return "not a rate: " + nw;
                        int at = -1, n = 0;
                        for (int k = 0; k < lines.size(); k++) {
                            String l = lines.get(k);
                            if (l.startsWith("FX = ")) {
                                n++;
                                if (l.startsWith("FX = " + old) && (l.length() == 5 + old.length()
                                        || Character.isWhitespace(l.charAt(5 + old.length())))) at = k;
                            }
                        }
                        if (n != 1 || at < 0) return "fx.txt changed since it was read - nothing written";
                        String l = lines.get(at);
                        lines.set(at, "FX = " + nw + l.substring(5 + old.length()));
                    } else return "unknown change: " + op;
                }
                for (Map.Entry<String, List<String>> e : files.entrySet()) {
                    StringBuilder b = new StringBuilder();
                    for (String l : e.getValue()) b.append(l).append('\n');
                    writeWhole(new File(folder(), e.getKey()), b.toString());
                }
                return "ok";
            } catch (Exception e) {
                return "could not write: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public synchronized String takeBack(String name, String rows) {
            if (!writable(name)) return "the app cannot write to " + name;
            File f = new File(folder(), name);
            try {
                String text = readText(f);
                if (rows == null || rows.isEmpty() || !text.endsWith(rows)) return "the file changed since - nothing taken back";
                byte[] keep = text.substring(0, text.length() - rows.length()).getBytes("UTF-8");
                RandomAccessFile r = new RandomAccessFile(f, "rw");
                try {
                    r.setLength(keep.length);
                } finally {
                    r.close();
                }
                return "ok";
            } catch (Exception e) {
                return "could not undo: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public void plan(String json) {
            Alerts.setPlan(MainActivity.this, json);
            if (!Alerts.allowed(MainActivity.this) && !askedNote) {
                askedNote = true;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTE);
                    }
                });
            }
        }

        @JavascriptInterface
        public void postNote(String id, String ch, String title, String text, int page) {
            Alerts.post(MainActivity.this, id, ch, title, text, page);
        }

        @JavascriptInterface
        public void cancelNote(String id) {
            Alerts.cancel(MainActivity.this, id);
        }

        @JavascriptInterface
        public boolean canNotify() {
            return Alerts.allowed(MainActivity.this);
        }

        @JavascriptInterface
        public void widget(String json) {
            Wid.setData(MainActivity.this, json);
        }

        @JavascriptInterface
        public String takeAct() {
            String a = pendingAct;
            pendingAct = "";
            return a == null ? "" : a;
        }

        @JavascriptInterface
        public int takePage() {
            int p = pendingPage;
            pendingPage = -1;
            return p;
        }

        @JavascriptInterface
        public void pickFile() {
            h.post(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "text/csv", "text/comma-separated-values",
                            "application/csv", "text/plain", "application/vnd.ms-excel", "application/octet-stream",

                            "image/png", "image/jpeg", "image/webp",

                            "application/zip", "application/x-zip-compressed"});
                    try {
                        startActivityForResult(i, REQ_FILE);
                    } catch (Exception e) {
                        js("window.onFileErr&&onFileErr('this phone has no file picker')");
                    }
                }
            });
        }

        @JavascriptInterface
        public void takePhoto() {
            h.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT < 29) throw new Exception("old Android");
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_" + System.currentTimeMillis() + ".jpg");
                        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Ratio Vitae");
                        photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                        if (photoUri == null) throw new Exception("no picture");
                        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        i.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivityForResult(i, REQ_PHOTO);
                    } catch (Exception e) {
                        if (photoUri != null) { try { getContentResolver().delete(photoUri, null, null); } catch (Exception ignored) { } photoUri = null; }
                        js("window.onFileErr&&onFileErr('The camera did not open - pick the picture with Import instead')");
                    }
                }
            });
        }

        @JavascriptInterface
        public void printPdf(final String title) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        android.print.PrintManager pm = (android.print.PrintManager) getSystemService(PRINT_SERVICE);
                        pm.print(title, web.createPrintDocumentAdapter(title), new android.print.PrintAttributes.Builder().build());
                    } catch (Exception e) {
                        js("window.onFileErr&&onFileErr('The print screen did not open')");
                    }
                }
            });
        }

        @JavascriptInterface
        public int filesLeft() {
            synchronized (inQ) { return inQ.size(); }
        }

        @JavascriptInterface
        public String takeFile() {
            Object[] f;
            synchronized (inQ) { f = inQ.pollFirst(); }
            if (f == null) return "";
            String n = (String) f[0];
            byte[] b = (byte[]) f[1];
            String err = (String) f[2];
            try {
                JSONObject o = new JSONObject();
                o.put("name", n);
                if (b != null) o.put("data", Base64.encodeToString(b, Base64.NO_WRAP));
                else o.put("err", err == null ? "it could not be read" : err);
                return o.toString();
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String folderPath() {
            return folder().getAbsolutePath();
        }

        @JavascriptInterface
        public String version() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "";
            }
        }
    }

    private WebResourceResponse serve(Uri u) {
        if (!HOST.equals(u.getHost())) return null;
        String p = u.getPath() == null ? "/" : u.getPath();
        try {
            if (p.equals("/") || p.equals("/index.html")) return asset("index.html");
            String name = p.substring(1);
            if (!name.isEmpty() && !name.contains("..")) return asset(name);
        } catch (Exception e) {

        }
        return missing();
    }

    private WebResourceResponse asset(String name) throws java.io.IOException {
        InputStream in = getAssets().open(name);
        String type = "application/octet-stream";
        if (name.endsWith(".html")) type = "text/html";
        else if (name.endsWith(".woff")) type = "font/woff";
        else if (name.endsWith(".js") || name.endsWith(".mjs")) type = "text/javascript";
        else if (name.endsWith(".wasm")) type = "application/wasm";
        else if (name.endsWith(".css")) type = "text/css";
        else if (name.endsWith(".svg")) type = "image/svg+xml";
        else if (name.endsWith(".png")) type = "image/png";
        Map<String, String> head = new HashMap<String, String>();
        head.put("Cache-Control", name.endsWith(".html") ? "no-store" : "max-age=604800");
        return new WebResourceResponse(type, type.startsWith("text/") ? "UTF-8" : null, 200, "OK", head, in);
    }

    private WebResourceResponse missing() {
        Map<String, String> head = new HashMap<String, String>();
        head.put("Cache-Control", "no-store");
        return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", head,
                new ByteArrayInputStream(new byte[0]));
    }
}
