package com.gx.id.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scanner {

    private final Context context;

    public Scanner(Context context) {
        this.context = context;
    }

    public List<AppData> scan() {
        List<AppData> list = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Map<String, String> trackers = findTrackers();
        String gaid = getGAID();

        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            String name = pm.getApplicationLabel(app).toString();
            String pkg = app.packageName;
            String version;
            try {
                version = pm.getPackageInfo(pkg, 0).versionName;
            } catch (Exception e) {
                version = "1.0";
            }
            Drawable icon = null;
            try {
                icon = pm.getApplicationIcon(app);
            } catch (Exception ignored) {}

            String tracker = trackers.containsKey(pkg) ? trackers.get(pkg) : "None";
            int color = 0xFF9E9E9E;
            if ("AppsFlyer".equals(tracker)) color = 0xFFFF9800;
            else if ("Singular".equals(tracker)) color = 0xFF00E5FF;
            else if ("Adjust".equals(tracker)) color = 0xFF00E676;

            AppData data = new AppData(name, pkg, version, tracker, color, icon);
            data.gaid = gaid;
            data.afId = generateAfId();
            data.installId = generateUuid();
            data.uuid = generateUuid();
            data.deviceId = generateUuid();
            data.singularId = generateUuid();
            data.adjustId = generateUuid();

            list.add(data);
        }

        return list;
    }

    private Map<String, String> findTrackers() {
        Map<String, String> map = new HashMap<>();
        try {
            File dir = new File("/data/data");
            File[] files = dir.listFiles();
            if (files == null) return map;

            for (File pkgDir : files) {
                File prefs = new File(pkgDir, "shared_prefs");
                if (!prefs.exists()) continue;
                String pkg = pkgDir.getName();

                if (new File(prefs, "appsflyer-data.xml").exists()) {
                    map.put(pkg, "AppsFlyer");
                } else if (new File(prefs, "pref-singular-id.xml").exists()) {
                    map.put(pkg, "Singular");
                } else if (new File(prefs, "adjust_preferences.xml").exists()) {
                    map.put(pkg, "Adjust");
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private String getGAID() {
        try {
            File f = new File("/data/data/com.google.android.gms/shared_prefs/adid_settings.xml");
            if (!f.exists()) return "Not Found";
            String content = readFile(f);
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");
            java.util.regex.Matcher m = p.matcher(content);
            if (m.find()) return m.group();
        } catch (Exception ignored) {}
        return "Not Found";
    }

    private String readFile(File f) {
        try {
            BufferedReader br = new BufferedReader(new java.io.FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }

    private String generateAfId() {
        long ts = System.currentTimeMillis();
        long rnd = (long) (Math.random() * 9000000000000000000L) + 1000000000000000000L;
        return ts + "-" + rnd;
    }
              }
