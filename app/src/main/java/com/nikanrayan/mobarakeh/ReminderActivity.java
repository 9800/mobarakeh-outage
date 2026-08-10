package com.nikanrayan.mobarakeh;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReminderActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private Ringtone fallbackRingtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("body");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#1a1a1a"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 60, 40, 60);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ " + (title != null ? title : "یادآوری خاموشی برق"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        titleView.setTextColor(Color.parseColor("#ffeb3b"));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);
        root.addView(titleView);

        TextView bodyView = new TextView(this);
        bodyView.setText(body != null ? body : "زمان خاموشی نزدیک است!");
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        bodyView.setTextColor(Color.WHITE);
        bodyView.setGravity(Gravity.CENTER);
        bodyView.setPadding(0, 0, 0, 50);
        root.addView(bodyView);

        Button closeBtn = new Button(this);
        closeBtn.setText("متوجه شدم (توقف آلارم)");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(60, 20, 60, 20);
        closeBtn.setOnClickListener(v -> { stopAlert(); finish(); });
        root.addView(closeBtn);

        setContentView(root);
        startAlert();
    }

    private void startAlert() {
        startVibration();

        boolean started = false;
        try {
            AssetFileDescriptor afd = getAssets().openFd("alert.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            started = true;
        } catch (Exception e) {
            started = false;
        }
        if (!started) {
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                fallbackRingtone = RingtoneManager.getRingtone(this, uri);
                if (fallbackRingtone != null) fallbackRingtone.play();
            } catch (Exception ignored) {}
        }
    }

    /* ✅ ویبرهٔ ریتمیکِ بی‌پایان تا زمانی که stopAlert صدا زده شود */
    private void startVibration() {
        if (vibrator == null) return;
        long[] pattern = {0, 600, 300, 600, 300, 600, 1000};
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlert() {
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
        try { if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.release(); mediaPlayer = null; } } catch (Exception ignored) {}
        try { if (fallbackRingtone != null) { fallbackRingtone.stop(); fallbackRingtone = null; } } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        stopAlert();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // فقط دکمهٔ «متوجه شدم» آلارم را می‌بندد
    }
}
