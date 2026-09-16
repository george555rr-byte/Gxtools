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

    // ========== KunTools executeRootCommand ==========
    private String executeRootCommand(String command) {
        StringBuffer output = new StringBuffer();
        Process p = null;
        DataOutputStream os = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os.writeBytes(command + "\nexit\n");
            os.flush();
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            return "";
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            try { if (br != null) br.close(); } catch (Exception ignored) {}
        }
        return output.toString().trim();
    }

    // ========== GAID ==========
    public String getGaid() {
        if (cachedGaid != null) return cachedGaid;

        String cmd = "gaid=$(grep -oE \"[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}\" " +
                     "/data/data/com.google.android.gms/shared_prefs/adid_settings.xml " +
                     "2>/dev/null | head -n 1); " +
                     "if test -z \"$gaid\"; then gaid=$(settings get secure advertising_id); fi; " +
                     "echo $gaid";

        String result = executeRootCommand(cmd).trim();
        if (result.isEmpty()) result = "Not Found";
        cachedGaid = result;
        return cachedGaid;
    }

    // ========== SCAN ALL TRACKERS (ONE su CALL) ==========
    // يرجع Map: pkg → [tracker, id]
    public Map<String, String[]> scanAllTrackers() {
        Map<String, String[]> result = new HashMap<>();

        String cmd =
            "for prefs in /data/data/*/shared_prefs; do " +
            "  pkg=$(echo \"$prefs\" | cut -d/ -f4); " +
            "  if test -f \"$prefs/appsflyer-data.xml\"; then " +
            "    id=$(sed -n 's/.*name=\"AF_INSTALLATION\">\\([^<]*\\).*/\\1/p' \"$prefs/appsflyer-data.xml\" | head -n 1); " +
            "    echo \"$pkg|AF|$id\"; " +
            "  elif test -f \"$prefs/pref-singular-id.xml\"; then " +
            "    id=$(sed -n 's/.*name=\"singular-id\">\\([^<]*\\).*/\\1/p' \"$prefs/pref-singular-id.xml\" | head -n 1); " +
            "    echo \"$pkg|SG|$id\"; " +
            "  elif test -f \"$prefs/adjust_preferences.xml\"; then " +
            "    echo \"$pkg|AD|-\"; " +
            "  fi; " +
            "done";

        String out = executeRootCommand(cmd);

        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                result.put(parts[0], new String[]{parts[1], parts[2]});
            }
        }

        return result;
    }

    // ========== LIST APPS ==========
    public List<AppData> scan() {
        List<AppData> list = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();
        String gaid = getGaid();
        Map<String, String[]> trackers = scanAllTrackers();

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

            String[] t = trackers.get(pkg);
            if (t != null) {
                if ("AF".equals(t[0])) {
                    data.tracker = "AppsFlyer";
                    data.trackerColor = 0xFFFF9800;
                    data.afId = (t[1] == null || t[1].isEmpty()) ? "-" : t[1];
                } else if ("SG".equals(t[0])) {
                    data.tracker = "Singular";
                    data.trackerColor = 0xFF00E5FF;
                    data.singularId = (t[1] == null || t[1].isEmpty()) ? "-" : t[1];
                } else if ("AD".equals(t[0])) {
                    data.tracker = "Adjust";
                    data.trackerColor = 0xFF00E676;
                }
            }

            list.add(data);
        }

        return list;
    }

    // ========== DEEP SCAN (احتياطي للضغط) ==========
    public void deepScan(AppData app) {
        Map<String, String[]> trackers = scanAllTrackers();
        app.gaid = getGaid();

        String[] t = trackers.get(app.packageName);
        if (t == null) {
            app.tracker = "None";
            app.trackerColor = 0xFF9E9E9E;
            return;
        }

        if ("AF".equals(t[0])) {
            app.tracker = "AppsFlyer";
            app.trackerColor = 0xFFFF9800;
            app.afId = (t[1] == null || t[1].isEmpty()) ? "-" : t[1];
        } else if ("SG".equals(t[0])) {
            app.tracker = "Singular";
            app.trackerColor = 0xFF00E5FF;
            app.singularId = (t[1] == null || t[1].isEmpty()) ? "-" : t[1];
        } else if ("AD".equals(t[0])) {
            app.tracker = "Adjust";
            app.trackerColor = 0xFF00E676;
        }
    }
}
