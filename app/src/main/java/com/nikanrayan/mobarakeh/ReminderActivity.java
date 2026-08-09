package com.nikanrayan.mobarakeh;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

public class ReminderActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Runnable vibrateRunnable;
    private boolean isAlertActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تنظیمات پنجره: تمام صفحه، بالای همه چیز، حتی وقتی قفل است
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                             android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // دریافت پیام
        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("body");

        // ساخت Layout اصلی
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#1a1a1a")); // پس‌زمینه تیره
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 60, 40, 60);

        // عنوان
        TextView titleView = new TextView(this);
        titleView.setText("⚡ " + (title != null ? title : "یادآوری خاموشی برق"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        titleView.setTextColor(Color.parseColor("#ffeb3b")); // زرد روشن
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);
        root.addView(titleView);

        // متن بدنه
        TextView bodyView = new TextView(this);
        bodyView.setText(body != null ? body : "زمان خاموشی نزدیک است!");
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        bodyView.setTextColor(Color.WHITE);
        bodyView.setGravity(Gravity.CENTER);
        bodyView.setPadding(0, 0, 0, 50);
        root.addView(bodyView);

        // دکمه بستن
        Button closeBtn = new Button(this);
        closeBtn.setText("متوجه شدم (توقف آلارم)");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setBackgroundColor(Color.parseColor("#4CAF50")); // سبز
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(60, 20, 60, 20);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        closeBtn.setLayoutParams(btnParams);
        closeBtn.setOnClickListener(v -> {
            stopAlert();
            finish();
        });
        root.addView(closeBtn);

        setContentView(root);

        // شروع هشدار (صدا + ویبره)
        startAlert();
    }

    private void startAlert() {
        // ۱. پخش صدای آژیر از فایل alert.mp3
        try {
            // R.raw.alert اشاره به فایل app/src/main/res/raw/alert.mp3 دارد
            mediaPlayer = MediaPlayer.create(this, R.raw.alert);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true); // تکرار بی‌نهایت
                mediaPlayer.setVolume(1.0f, 1.0f); // حداکثر صدا
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // اگر فایل پیدا نشد یا خطایی داد، لاگ بگیر (در محیط واقعی)
            e.printStackTrace();
        }

        // ۲. شروع ویبره تکرارشونده
        vibrateStrong();
        vibrateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAlertActive) {
                    vibrateStrong();
                    handler.postDelayed(this, 2000); // هر ۲ ثانیه ویبره
                }
            }
        };
        handler.postDelayed(vibrateRunnable, 500);
    }

    private void stopAlert() {
        isAlertActive = false;
        
        // توقف صدا
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }

        // توقف ویبره
        handler.removeCallbacks(vibrateRunnable);
    }

    private void vibrateStrong() {
        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= 26) {
            // الگوی ویبره: ۵۰۰ میلی‌ثانیه روشن، ۲۰۰ خاموش، ۵۰۰ روشن...
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlert(); // اطمینان از توقف صدا هنگام خروج
    }

    @Override
    public void onBackPressed() {
        // کاربر مجبور است دکمه را بزند تا متوقف شود (اختیاری: می‌توانی اجازه بدهی با Back هم بسته شود)
        stopAlert();
        super.onBackPressed();
    }
}
