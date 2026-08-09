package com.nikanrayan.mobarakeh;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class MonitoringService extends Service {
    private static final int NOTIFICATION_ID = 998877;
    private static final String CHANNEL_ID = "monitoring_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "سرویس پایش", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("برای جلوگیری از بسته شدن برنامه توسط سیستم");
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(this, CHANNEL_ID);
        else b = new Notification.Builder(this);

        b.setContentTitle("خاموشی برق مبارکه")
         .setContentText("در حال پایش زمان قطعی برق...")
         .setSmallIcon(R.drawable.ic_launcher)
         .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, flags))
         .setOngoing(true)
         .setPriority(Notification.PRIORITY_MIN);
        return b.build();
    }
}
