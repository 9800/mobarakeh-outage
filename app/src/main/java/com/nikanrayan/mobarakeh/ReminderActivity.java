package com.nikanrayan.mobarakeh;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
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
    private SoundPool soundPool;
    private int alertSoundId;
    private Handler handler = new Handler();
    private Runnable alertRunnable;
    private boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تنظیمات پنجره: تمام صفحه، بالای همه چیز
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
        root.setBackgroundColor(Color.parseColor("#1a1a1a"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 60, 40, 60);

        // عنوان
        TextView titleView = new TextView(this);
        titleView.setText("⚡ " + (title != null ? title : "یادآوری خاموشی برق"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        titleView.setTextColor(Color.parseColor("#ffeb3b"));
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
        closeBtn.setText("متوجه شدم");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
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

        // شروع هشدار صوتی و لرزش
        startAlert();
    }

    private void startAlert() {
        // ویبره قوی
        vibrateStrong();

        // تنظیم SoundPool برای صدای آژیر
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        soundPool = new SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build();

        // تولید صدای آژیر با فرکانس متغیر
        alertSoundId = soundPool.load(createAlertSound(), 1);
        
        // پخش تکراری
        alertRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    soundPool.play(alertSoundId, 1.0f, 1.0f, 0, -1, 1.0f);
                    vibrateStrong();
                    handler.postDelayed(this, 2000); // هر ۲ ثانیه تکرار
                }
            }
        };
        handler.postDelayed(alertRunnable, 500);
    }

    private void stopAlert() {
        isPlaying = false;
        handler.removeCallbacks(alertRunnable);
        if (soundPool != null) {
            soundPool.stop(alertSoundId);
            soundPool.release();
        }
    }

    private void vibrateStrong() {
        if (Build.VERSION.SDK_INT >= 26) {
            long[] pattern = {0, 500, 200, 500, 200, 500}; // ویبره طولانی با وقفه
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            }
        } else {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    // تولید صدای آژیر دیجیتالی
    private int createAlertSound() {
        // اینجا از یک الگوی سادهٔ موج سینوسی استفاده می‌کنیم
        // در نسخه‌های بعدی می‌توان فایل صوتی واقعی اضافه کرد
        return 0; // Placeholder - در عمل باید فایل صوتی لود شود
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlert();
    }

    @Override
    public void onBackPressed() {
        // جلوگیری از بستن با دکمه Back تا کاربر دکمه را بزند
        // یا می‌توان اجازه داد که ببندد:
        stopAlert();
        super.onBackPressed();
    }
}
