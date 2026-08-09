package com.nikanrayan.mobarakeh;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView webView;
    private AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        createChannel();
        askNotificationPermission();

        // اگر از ReminderActivity آمده‌ایم، مستقیم برگرد
        if (getIntent().getBooleanExtra("fromReminder", false)) {
            finish();
            return;
        }

        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("باشه", (d, w) -> result.confirm())
                        .setCancelable(false)
                        .show();
                return true;
            }
        });
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "outage", "یادآوری خاموشی برق", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("هشدار قطعی برق با صدا و باز شدن اپ");
            ch.enableVibration(true);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }

    /* دریافت مستقیم صفحهٔ کانال از سمت جاوا — بدون محدودیت CORS و بدون پراکسی */
    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        in.close();
        return bos.toString("UTF-8");
    }

    private class Bridge {
        @JavascriptInterface
        public void fetchChannel() {
            new Thread(new Runnable() {
                @Override public void run() {
                    String html = "";
                    try { html = httpGet("https://eitaa.com/epedcmobarake"); } catch (Exception e) { html = ""; }
                    final String out = html;
                    webView.post(new Runnable() {
                        @Override public void run() {
                            webView.evaluateJavascript(
                                "window.onNativeFetch(" + JSONObject.quote(out) + ");", null);
                        }
                    });
                }
            }).start();
        }

        @JavascriptInterface
        public void scheduleAt(long epochMs, int id, String title, String body) {
            if (epochMs < System.currentTimeMillis()) return;
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            i.putExtra("title", title);
            i.putExtra("body", body);
            i.putExtra("id", id);
            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, id, i, pendingFlags());
            try {
                if (Build.VERSION.SDK_INT >= 23)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMs, pi);
                else
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, epochMs, pi);
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, epochMs, pi);
            }
        }

        @JavascriptInterface
        public void cancel(int id) {
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, id, i, pendingFlags());
            alarmManager.cancel(pi);
        }

        @JavascriptInterface
        public void notifyNow(String title, String body) {
            postNotification(title, body, 999);
        }
    }

    void postNotification(String title, String body, int id) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(MainActivity.this, "outage");
        } else {
            b = new Notification.Builder(MainActivity.this);
        }
        // Intent برای باز کردن ReminderActivity هنگام لمس نوتیفیکیشن
        Intent intent = new Intent(MainActivity.this, ReminderActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.this, id + 10000, intent, pendingFlags());
        
        b.setContentTitle(title).setContentText(body)
         .setSmallIcon(R.drawable.ic_launcher)
         .setAutoCancel(true)
         .setContentIntent(pi)
         .setDefaults(Notification.DEFAULT_ALL)
         .setPriority(Notification.PRIORITY_MAX);
         
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
