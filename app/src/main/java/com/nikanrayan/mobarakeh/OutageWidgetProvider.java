package com.nikanrayan.mobarakeh;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutageWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.nikanrayan.mobarakeh.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        Context app = context.getApplicationContext();
        for (int id : ids) render(app, mgr, id);
        fetchAsync(app);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            final Context app = context.getApplicationContext();
            final PendingResult pr = goAsync();
            final AppWidgetManager mgr = AppWidgetManager.getInstance(app);
            final int[] ids = mgr.getAppWidgetIds(new ComponentName(app, OutageWidgetProvider.class));
            for (int id : ids) render(app, mgr, id);
            new Thread(new Runnable() {
                @Override public void run() {
                    try { fetchAndSave(app); } catch (Exception ignored) {}
                    for (int id : ids) render(app, mgr, id);
                    pr.finish();
                }
            }).start();
        }
    }

    static void fetchAsync(final Context app) {
        new Thread(new Runnable() {
            @Override public void run() {
                try { fetchAndSave(app); } catch (Exception ignored) {}
                AppWidgetManager mgr = AppWidgetManager.getInstance(app);
                int[] ids = mgr.getAppWidgetIds(new ComponentName(app, OutageWidgetProvider.class));
                for (int id : ids) render(app, mgr, id);
            }
        }).start();
    }

    static void fetchAndSave(Context app) throws Exception {
        String html = httpGet("https://eitaa.com/epedcmobarake");
        if (html != null && html.length() > 300) {
            app.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
               .edit().putString("html", html).putLong("time", System.currentTimeMillis()).apply();
        }
    }

    private static int pFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    static void render(Context app, AppWidgetManager mgr, int id) {
        try {
            RemoteViews v = new RemoteViews(app.getPackageName(), R.layout.widget_layout);

            Intent open = new Intent(app, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.setOnClickPendingIntent(R.id.widgetRoot, PendingIntent.getActivity(app, 0, open, pFlags()));

            Intent rf = new Intent(app, OutageWidgetProvider.class);
            rf.setAction(ACTION_REFRESH);
            v.setOnClickPendingIntent(R.id.wRefresh, PendingIntent.getBroadcast(app, 1, rf, pFlags()));

            SharedPreferences sp = app.getSharedPreferences("widget_cache", Context.MODE_PRIVATE);
            String html = sp.getString("html", null);
            long t = sp.getLong("time", 0);

            if (html == null) {
                v.setTextViewText(R.id.wStatus, "⏳ در حال دریافت جدول…");
                v.setTextViewText(R.id.wRows, "");
            } else {
                Model m = buildModel(html);
                if (m == null) {
                    v.setTextViewText(R.id.wStatus, "⚠️ جدول معتبر نیست — 🔄 را بزنید");
                    v.setTextViewText(R.id.wRows, "");
                } else {
                    v.setTextViewText(R.id.wStatus, m.status);
                    v.setTextViewText(R.id.wRows, m.rows);
                }
            }
            v.setTextViewText(R.id.wUpdate, "آخرین به‌روزرسانی: " + faTime(t));
            mgr.updateAppWidget(id, v);
        } catch (Exception ignored) {}
    }

    static String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        in.close();
        return bos.toString("UTF-8");
    }

    static String toEn(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch >= 0x06F0 && ch <= 0x06F9) sb.append((char) ('0' + (ch - 0x06F0)));
            else if (ch >= 0x0660 && ch <= 0x0669) sb.append((char) ('0' + (ch - 0x0660)));
            else sb.append(ch);
        }
        return sb.toString();
    }

    static String toFa(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') sb.append((char) (0x06F0 + (ch - '0')));
            else sb.append(ch);
        }
        return sb.toString();
    }

    static String faTime(long t) {
        if (t == 0) return "—";
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        String hh = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        String mm = String.valueOf(c.get(Calendar.MINUTE));
        if (mm.length() == 1) mm = "0" + mm;
        return toFa(hh + ":" + mm);
    }

    static int[] jalaliToGregorian(int jy, int jm, int jd) {
        jy += 1595;
        long days = -355668L + (365L * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) + jd + ((jm < 7) ? (jm - 1) * 31 : ((jm - 7) * 30) + 186);
        int gy = (int) (400 * (days / 146097)); days %= 146097;
        if (days > 36524) { gy += 100 * ((--days) / 36524); days %= 36524; if (days >= 365) days++; }
        gy += 4 * (days / 1461); days %= 1461;
        if (days > 365) { gy += (days - 1) / 365; days = (days - 1) % 365; }
        int gd = (int) days + 1;
        int[] sal = {0, 31, ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int gm = 0;
        for (gm = 0; gm < 13 && gd > sal[gm]; gm++) gd -= sal[gm];
        return new int[]{gy, gm, gd};
    }

    static final String[] MONTHS = {"فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند"};

    static class Row { int s, e; String groups; int dateIdx; }
    static class Model { String status; String rows; }

    static int[] parseJalali(String line) {
        String alt = "اردیبهشت|فروردین|شهریور|اسفند|مرداد|خرداد|بهمن|دی|تیر|مهر|آبان|آذر";
        Matcher dm = Pattern.compile("(\\d{1,2})\\s*(?:" + alt + ")").matcher(line);
        Matcher ym = Pattern.compile("(1[34]\\d{2})").matcher(line);
        int mn = 0;
        for (int i = 0; i < 12; i++) if (line.contains(MONTHS[i])) { mn = i + 1; break; }
        if (!dm.find() || mn == 0 || !ym.find()) return null;
        return new int[]{Integer.parseInt(ym.group(1)), mn, Integer.parseInt(dm.group(1))};
    }

    static String fmtGroups(String g) {
        return g.replaceAll("\\s+", "").replace("-", "، ");
    }

    static Model buildModel(String html) {
        try {
            String clean = html.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");
            String text = clean.replaceAll("<[^>]+>", "");
            String norm = toEn(text);

            List<Integer> dateIdx = new ArrayList<>();
            List<String> dateLines = new ArrayList<>();
            Matcher dm = Pattern.compile("مورخ\\s*([^\\n✅🔴]{3,40})").matcher(norm);
            while (dm.find()) { dateIdx.add(dm.start()); dateLines.add(dm.group(1).trim()); }

            List<Row> rows = new ArrayList<>();
            Matcher rm = Pattern.compile("ساعت\\s*(\\d{1,2})\\s*تا\\s*(\\d{1,2})\\s*[:：]?\\s*گروه\\s*(\\d[\\d\\s\\-،,]*)").matcher(norm);
            while (rm.find()) {
                Row r = new Row();
                r.s = Integer.parseInt(rm.group(1));
                r.e = Integer.parseInt(rm.group(2));
                r.groups = rm.group(3).trim();
                r.dateIdx = dateLines.size() - 1;
                for (int i = dateIdx.size() - 1; i >= 0; i--) {
                    if (dateIdx.get(i) < rm.start()) { r.dateIdx = i; break; }
                }
                rows.add(r);
            }
            if (rows.isEmpty() || dateLines.isEmpty()) return null;

            Calendar now = Calendar.getInstance();
            long d2 = now.get(Calendar.YEAR) * 372L + (now.get(Calendar.MONTH) + 1) * 31L + now.get(Calendar.DAY_OF_MONTH);

            int bestBlock = 0; long bestDiff = Long.MAX_VALUE;
            for (int i = 0; i < dateLines.size(); i++) {
                long diff = 999999;
                int[] j = parseJalali(dateLines.get(i));
                if (j != null) {
                    int[] g = jalaliToGregorian(j[0], j[1], j[2]);
                    diff = (g[0] * 372L + g[1] * 31L + g[2]) - d2;
                }
                if (diff == 0) { bestBlock = i; bestDiff = 0; break; }
                if (Math.abs(diff) < Math.abs(bestDiff)) { bestDiff = diff; bestBlock = i; }
            }

            List<Row> blockRows = new ArrayList<>();
            for (Row r : rows) if (r.dateIdx == bestBlock) blockRows.add(r);
            if (blockRows.isEmpty()) blockRows = rows;

            int hour = now.get(Calendar.HOUR_OF_DAY);
            Row active = null, next = null;
            for (Row r : blockRows) {
                if (hour >= r.s && hour < r.e) active = r;
                else if (r.s > hour && next == null) next = r;
            }

            Model m = new Model();
            if (bestDiff != 0) {
                m.status = "🟢 برنامهٔ امروز منتشر نشده | آخرین: " + toFa(dateLines.get(bestBlock));
            } else if (active != null) {
                m.status = "🔴 الان قطع تا ساعت " + toFa(String.valueOf(active.e)) + " | گروه " + toFa(fmtGroups(active.groups));
            } else {
                m.status = "🟢 برق وصل است" + (next != null ? " | بعدی: " + toFa(String.valueOf(next.s)) + " تا " + toFa(String.valueOf(next.e)) : "");
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Row r : blockRows) {
                if (count >= 4) break;
                sb.append("ساعت ").append(toFa(String.valueOf(r.s))).append(" تا ").append(toFa(String.valueOf(r.e)))
                  .append(": گروه ").append(toFa(fmtGroups(r.groups))).append("\n");
                count++;
            }
            m.rows = sb.toString();
            return m;
        } catch (Exception e) {
            return null;
        }
    }
}
