package com.gx.id.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Scanner {

    private final Context ctx;
    private String cachedGaid = null;

    public Scanner(Context ctx) {
        this.ctx = ctx;
    }

    // ============ KunTools executeRootCommand - حرفياً ============
    private String executeRootCommand(String command) {
        StringBuilder output = new StringBuilder();
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

    // ============ KunTools extractPayloadSync - حرفياً ============
    // يفحص تطبيق واحد ويحدد نوع المنصة والمعرف
    public String extractPayloadSync(String pkg) {
        String command =
            "pkg=\"" + pkg + "\"; " +
            "prefs=\"/data/data/$pkg/shared_prefs\"; " +
            "af_xml=\"$prefs/appsflyer-data.xml\"; " +
            "singular_xml=\"$prefs/pref-singular-id.xml\"; " +
            "adjust_xml=\"$prefs/adjust_preferences.xml\"; " +
            "if test -f \"$af_xml\"; then " +
            "  id=$(sed -n \"s/.*name=\\\"AF_INSTALLATION\\\">\\([^<]*\\).*/\\1/p\" \"$af_xml\" | head -n 1); " +
            "  echo \"AF|$id\"; " +
            "elif test -f \"$singular_xml\"; then " +
            "  id=$(sed -n \"s/.*name=\\\"singular-id\\\">\\([^<]*\\).*/\\1/p\" \"$singular_xml\" | head -n 1); " +
            "  echo \"SG|$id\"; " +
            "elif test -f \"$adjust_xml\"; then " +
            "  echo \"AD|-\"; " +
            "else " +
            "  echo \"NO|-\"; " +
            "fi";

        return executeRootCommand(command);
    }

    // ============ GAID ============
    public String getGaid() {
        if (cachedGaid != null) return cachedGaid;

        String result = executeRootCommand(
            "cat /data/data/com.google.android.gms/shared_prefs/adid_settings.xml 2>/dev/null"
        );

        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
            ).matcher(result);
            if (m.find()) {
                cachedGaid = m.group();
                return cachedGaid;
            }
        } catch (Exception ignored) {}

        cachedGaid = "Not Found";
        return cachedGaid;
    }

    // ============ DEEP SCAN ONE APP (ON CLICK) ============
    public void deepScan(AppData app) {
        String result = extractPayloadSync(app.packageName);

        app.gaid = getGaid();

        if (result.startsWith("AF|")) {
            app.tracker = "AppsFlyer";
            app.trackerColor = 0xFFFF9800;
            app.afId = result.substring(3).trim();
            if (app.afId.isEmpty()) app.afId = "-";
        } else if (result.startsWith("SG|")) {
            app.tracker = "Singular";
            app.trackerColor = 0xFF00E5FF;
            app.singularId = result.substring(3).trim();
            if (app.singularId.isEmpty()) app.singularId = "-";
        } else if (result.startsWith("AD|")) {
            app.tracker = "Adjust";
            app.trackerColor = 0xFF00E676;
        } else {
            app.tracker = "None";
            app.trackerColor = 0xFF9E9E9E;
        }
    }

    // ============ QUICK SCAN ============
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
