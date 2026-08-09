package com.nikanrayan.mobarakeh;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {
    private WebView webView;
    private AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        createChannel();
        askNotificationPermission();

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
            ch.setDescription("هشدار قطعی برق با صدا");
            ch.enableVibration(true);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void scheduleAt(long epochMs, int id, String title, String body) {
            if (epochMs < System.currentTimeMillis()) return;
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            i.putExtra("title", title);
            i.putExtra("body", body);
            i.putExtra("id", id);
            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, id, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            try {
                if (Build.VERSION.SDK_INT >= 23)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMs, pi);
                else
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, epochMs, pi);
            } catch (SecurityException e) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMs, pi);
            }
        }

        @JavascriptInterface
        public void cancel(int id) {
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, id, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pi);
        }

        @JavascriptInterface
        public void notifyNow(String title, String body) {
            postNotification(title, body, 999);
        }
    }

    void postNotification(String title, String body, int id) {
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this, "outage")
                : new Notification.Builder(this);
        b.setContentTitle(title).setContentText(body)
         .setSmallIcon(R.drawable.ic_launcher).setAutoCancel(true);
        getSystemService(NotificationManager.class).notify(id, b.build());
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
