package com.nikanrayan.mobarakeh;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("id", 1);

        // ✅ بیدار کردن صفحهٔ خاموش/قفل و نگه‌داشتن CPU
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "mobarakeh:alarm");
            wl.acquire(10 * 60 * 1000L); // ۱۰ دقیقه
        }

        Intent act = new Intent(context, ReminderActivity.class);
        act.putExtra("title", title);
        act.putExtra("body", body);
        act.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fsPi = PendingIntent.getActivity(context, id + 10000, act, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(context, "outage");
        else b = new Notification.Builder(context);

        b.setContentTitle(title).setContentText(body)
         .setSmallIcon(R.drawable.ic_launcher)
         .setAutoCancel(true)
         .setCategory(Notification.CATEGORY_ALARM)
         .setVisibility(Notification.VISIBILITY_PUBLIC)
         .setFullScreenIntent(fsPi, true)   // ✅ کلید اصلی: نمایش تمام‌صفحه روی صفحهٔ قفل
         .setContentIntent(fsPi)
         .setDefaults(Notification.DEFAULT_ALL)
         .setPriority(Notification.PRIORITY_MAX);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());

        // اگر گوشی باز بود، مستقیم هم Activity را بالا بیاور
        try { context.startActivity(act); } catch (Exception ignored) {}
    }
}
