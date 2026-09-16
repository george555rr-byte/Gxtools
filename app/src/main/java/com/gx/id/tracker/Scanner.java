package com.gx.id.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {

    private final Context ctx;
    private String cachedGaid = null;

    public Scanner(Context ctx) {
        this.ctx = ctx;
    }

    private String sh(String cmd) {
        StringBuilder out = new StringBuilder();
        Process p = null;
        DataOutputStream os = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os.writeBytes(cmd + "\nexit\n");
            os.flush();
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            return "";
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            try { if (br != null) br.close(); } catch (Exception ignored) {}
        }
        return out.toString().trim();
    }

    public String pullGaid() {
        if (cachedGaid != null) return cachedGaid;

        String[] candidates = {
            "/data/data/com.google.android.gms/shared_prefs/adid_settings.xml",
            "/data/user/0/com.google.android.gms/shared_prefs/adid_settings.xml"
        };

        for (String path : candidates) {
            String raw = sh("cat \"" + path + "\" 2>/dev/null");
            if (raw.isEmpty()) continue;
            String found = matchUuid(raw);
            if (found != null) {
                cachedGaid = found;
                return found;
            }
        }

        String sec = sh("settings get secure advertising_id 2>/dev/null");
        if (sec != null && !sec.isEmpty() && !sec.equals("null")) {
            cachedGaid = sec.trim();
            return cachedGaid;
        }

        cachedGaid = "Not Found";
        return cachedGaid;
    }

    // ============ DEEP SCAN ALL APPS (ONE ROOT CALL) ============
    public void deepScanAll(List<AppData> apps) {
        pullGaid();

        StringBuilder script = new StringBuilder();
        for (AppData a : apps) {
            String pkg = a.packageName;
            script.append("P=\"/data/data/").append(pkg).append("/shared_prefs\"; ");
            script.append("A=\"/data/user/0/").append(pkg).append("/shared_prefs\"; ");
            script.append("[ -d \"$P\" ] || P=\"$A\"; ");
            script.append("AF=\"$P/appsflyer-data.xml\"; ");
            script.append("SG=\"$P/pref-singular-id.xml\"; ");
            script.append("AJ=\"$P/adjust_preferences.xml\"; ");
            script.append("if [ -f \"$AF\" ]; then ");
            script.append("V=$(sed -n 's/.*name=\"AF_INSTALLATION\">\\([^<]*\\).*/\\1/p' \"$AF\" | head -n1); ");
            script.append("echo \"JT|").append(pkg).append("|AF|$V\"; ");
            script.append("elif [ -f \"$SG\" ]; then ");
            script.append("V=$(sed -n 's/.*name=\"singular-id\">\\([^<]*\\).*/\\1/p' \"$SG\" | head -n1); ");
            script.append("echo \"JT|").append(pkg).append("|SG|$V\"; ");
            script.append("elif [ -f \"$AJ\" ]; then ");
            script.append("echo \"JT|").append(pkg).append("|AD|-\"; ");
            script.append("else ");
            script.append("echo \"JT|").append(pkg).append("|NO|-\"; ");
            script.append("fi; ");
        }

        String out = sh(script.toString());
        Map<String, String[]> results = new HashMap<>();

        for (String line : out.split("\n")) {
            if (!line.startsWith("JT|")) continue;
            String[] parts = line.split("\\|");
            if (parts.length < 4) continue;
            results.put(parts[1], new String[]{parts[2], parts[3]});
        }

        for (AppData a : apps) {
            String[] r = results.get(a.packageName);
            a.gaid = cachedGaid;
            if (r == null) {
                a.tracker = "None";
                a.trackerColor = 0xFF9E9E9E;
            } else if ("AF".equals(r[0])) {
                a.tracker = "AppsFlyer";
                a.trackerColor = 0xFFFF9800;
                a.afId = r[1].isEmpty() ? "-" : r[1];
            } else if ("SG".equals(r[0])) {
                a.tracker = "Singular";
                a.trackerColor = 0xFF00E5FF;
                a.singularId = r[1].isEmpty() ? "-" : r[1];
            } else if ("AD".equals(r[0])) {
                a.tracker = "Adjust";
                a.trackerColor = 0xFF00E676;
            } else {
                a.tracker = "None";
                a.trackerColor = 0xFF9E9E9E;
            }
        }
    }

    private String matchUuid(String input) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
            java.util.regex.Matcher m = p.matcher(input);
            if (m.find()) return m.group();
        } catch (Exception ignored) {}
        return null;
    }

    public List<AppData> scan() {
        List<AppData> list = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();

        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            String name = pm.getApplicationLabel(app).toString();
            String pkg = app.packageName;
            String version;
            try { version = pm.getPackageInfo(pkg, 0).versionName; }
            catch (Exception e) { version = "1.0"; }

            Drawable icon = null;
            try { icon = pm.getApplicationIcon(app); }
            catch (Exception ignored) {}

            AppData data = new AppData(name, pkg, version, "None", 0xFF9E9E9E, icon);
            list.add(data);
        }

        return list;
    }
}
