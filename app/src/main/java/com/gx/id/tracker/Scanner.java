package com.gx.id.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Scanner {

    private final Context ctx;
    private String cachedGaid = null;

    public Scanner(Context ctx) {
        this.ctx = ctx;
    }

    // ============ ROOT EXEC - su -c ============
    private String executeRootCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process p = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            return "";
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignored) {}
            try { if (p != null) p.destroy(); } catch (Exception ignored) {}
        }
        return output.toString().trim();
    }

    // ============ GAID ============
    public String getGaid() {
        if (cachedGaid != null) return cachedGaid;

        String result = executeRootCommand(
            "grep -oE \"[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}\" " +
            "/data/data/com.google.android.gms/shared_prefs/adid_settings.xml 2>/dev/null | head -n 1"
        );

        if (!result.isEmpty()) {
            cachedGaid = result;
            return cachedGaid;
        }

        // احتياطي
        String alt = executeRootCommand("settings get secure advertising_id");
        if (alt != null && !alt.isEmpty() && !alt.equals("null")) {
            cachedGaid = alt;
            return cachedGaid;
        }

        cachedGaid = "Not Found";
        return cachedGaid;
    }

    // ============ DEEP SCAN ONE APP ============
    public void deepScan(AppData app) {
        String pkg = app.packageName;

        String cmd =
            "prefs=\"/data/data/" + pkg + "/shared_prefs\"; " +
            "[ -d \"$prefs\" ] || prefs=\"/data/user/0/" + pkg + "/shared_prefs\"; " +
            "AF=\"$prefs/appsflyer-data.xml\"; " +
            "SG=\"$prefs/pref-singular-id.xml\"; " +
            "AJ=\"$prefs/adjust_preferences.xml\"; " +
            "if [ -f \"$AF\" ]; then " +
            "  echo \"TAG=AF\"; " +
            "  sed -n 's/.*name=\"AF_INSTALLATION\">\\([^<]*\\).*/\\1/p' \"$AF\" | head -n 1; " +
            "elif [ -f \"$SG\" ]; then " +
            "  echo \"TAG=SG\"; " +
            "  sed -n 's/.*name=\"singular-id\">\\([^<]*\\).*/\\1/p' \"$SG\" | head -n 1; " +
            "elif [ -f \"$AJ\" ]; then " +
            "  echo \"TAG=AD\"; " +
            "  echo \"-\"; " +
            "else " +
            "  echo \"TAG=NO\"; " +
            "  echo \"-\"; " +
            "fi";

        String result = executeRootCommand(cmd);
        app.gaid = getGaid();

        String[] lines = result.split("\n");
        String tag = "";
        String value = "";

        for (String line : lines) {
            if (line.startsWith("TAG=")) {
                tag = line.substring(4).trim();
            } else if (!line.isEmpty()) {
                value = line.trim();
            }
        }

        if ("AF".equals(tag)) {
            app.tracker = "AppsFlyer";
            app.trackerColor = 0xFFFF9800;
            app.afId = value.isEmpty() ? "-" : value;
        } else if ("SG".equals(tag)) {
            app.tracker = "Singular";
            app.trackerColor = 0xFF00E5FF;
            app.singularId = value.isEmpty() ? "-" : value;
        } else if ("AD".equals(tag)) {
            app.tracker = "Adjust";
            app.trackerColor = 0xFF00E676;
        } else {
            app.tracker = "None";
            app.trackerColor = 0xFF9E9E9E;
        }
    }

    // ============ LIST APPS ============
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
