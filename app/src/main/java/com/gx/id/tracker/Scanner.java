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
import java.util.UUID;

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

    public Map<String, String> pullAppData(String pkg) {
        Map<String, String> result = new HashMap<>();
        result.put("type", "None");
        result.put("value1", "");
        result.put("value2", "");

        String base = "/data/data/" + pkg + "/shared_prefs";
        String alt = "/data/user/0/" + pkg + "/shared_prefs";

        String[] paths = { base, alt };
        for (String p : paths) {
            String check = sh("ls \"" + p + "\" 2>/dev/null");
            if (check == null || check.isEmpty()) continue;

            String afFile = p + "/appsflyer-data.xml";
            String sgFile = p + "/pref-singular-id.xml";
            String ajFile = p + "/adjust_preferences.xml";

            String afExists = sh("test -f \"" + afFile + "\" && echo YES");
            if (afExists.contains("YES")) {
                String afContent = sh("cat \"" + afFile + "\" 2>/dev/null");
                String afId = extractXmlValue(afContent, "AF_INSTALLATION");
                String androidId = extractXmlValue(afContent, "androidIdCached");
                result.put("type", "AppsFlyer");
                result.put("value1", afId.isEmpty() ? "-" : afId);
                result.put("value2", androidId.isEmpty() ? "-" : androidId);
                return result;
            }

            String sgExists = sh("test -f \"" + sgFile + "\" && echo YES");
            if (sgExists.contains("YES")) {
                String sgContent = sh("cat \"" + sgFile + "\" 2>/dev/null");
                String sgId = extractXmlValue(sgContent, "singular-id");
                result.put("type", "Singular");
                result.put("value1", sgId.isEmpty() ? "-" : sgId);
                return result;
            }

            String ajExists = sh("test -f \"" + ajFile + "\" && echo YES");
            if (ajExists.contains("YES")) {
                result.put("type", "Adjust");
                return result;
            }
        }

        return result;
    }

    private String extractXmlValue(String xml, String key) {
        if (xml == null || xml.isEmpty()) return "";
        try {
            String tag = "name=\"" + key + "\"";
            int idx = xml.indexOf(tag);
            if (idx < 0) return "";
            int close = xml.indexOf("</string>", idx);
            if (close < 0) close = xml.indexOf("/>", idx);
            if (close < 0) return "";
            String segment = xml.substring(idx, close);
            int start = segment.indexOf(">");
            if (start < 0) return "";
            String val = segment.substring(start + 1);
            if (val.startsWith("</string>")) return "";
            val = val.replace("</string>", "").trim();
            return val;
        } catch (Exception e) {
            return "";
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
        String gaid = pullGaid();

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
            data.gaid = gaid;
            data.installId = UUID.randomUUID().toString();
            data.uuid = UUID.randomUUID().toString();
            data.deviceId = UUID.randomUUID().toString();

            list.add(data);
        }

        return list;
    }

    public void deepScan(AppData app) {
        Map<String, String> info = pullAppData(app.packageName);
        String type = info.get("type");

        app.tracker = type;
        if ("AppsFlyer".equals(type)) {
            app.trackerColor = 0xFFFF9800;
            app.afId = info.get("value1");
            app.installId = info.get("value2");
        } else if ("Singular".equals(type)) {
            app.trackerColor = 0xFF00E5FF;
            app.singularId = info.get("value1");
        } else if ("Adjust".equals(type)) {
            app.trackerColor = 0xFF00E676;
        } else {
            app.trackerColor = 0xFF9E9E9E;
        }
    }
}
