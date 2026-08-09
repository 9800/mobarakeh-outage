package com.nikanrayan.mobarakeh;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("id", 1);

        // باز کردن ReminderActivity به صورت خودکار
        Intent activityIntent = new Intent(context, ReminderActivity.class);
        activityIntent.putExtra("title", title);
        activityIntent.putExtra("body", body);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(activityIntent);

        // ارسال نوتیفیکیشن همزمان
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(context, "outage");
        } else {
            b = new Notification.Builder(context);
        }
        
        // Intent برای باز کردن ReminderActivity هنگام لمس نوتیفیکیشن
        Intent notifIntent = new Intent(context, ReminderActivity.class);
        notifIntent.putExtra("title", title);
        notifIntent.putExtra("body", body);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(context, id + 10000, notifIntent, flags);
        
        b.setContentTitle(title).setContentText(body)
         .setSmallIcon(R.drawable.ic_launcher)
         .setAutoCancel(true)
         .setContentIntent(pi)
         .setDefaults(Notification.DEFAULT_ALL)
         .setPriority(Notification.PRIORITY_MAX);
         
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }
}
