package com.nikanrayan.mobarakeh;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

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
        // اگر سیستم سرویس را کشت، دوباره آن را راه بینداز (استارت sticky)
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "سرویس پایش",
                    NotificationManager.IMPORTANCE_MIN // کمترین اهمیت تا مزاحم نباشد
            );
            channel.setDescription("برای جلوگیری از بسته شدن برنامه توسط سیستم");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        // فلگ‌ها برای باز شدن صحیح اکتیویتی
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // در اندروید ۱۲+ باید PendingIntent غیرقابل تغییر باشد
        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }

        builder.setContentTitle("خاموشی برق مبارکه")
               .setContentText("در حال پایش زمان قطعی برق...")
               .setSmallIcon(R.drawable.ic_launcher) // آیکون برنامه
               .setContentIntent(android.app.PendingIntent.getActivity(this, 0, notificationIntent, flags))
               .setOngoing(true) // نوتیفیکیشن غیرقابل حذف توسط کاربر (فقط با بستن اپ)
               .setPriority(NotificationCompat.PRIORITY_MIN);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // اگر سرویس متوقف شد، دوباره سعی کن روشن شود (اختیاری)
        Intent intent = new Intent(this, MonitoringService.class);
        startService(intent);
    }
}
