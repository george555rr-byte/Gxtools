package com.gx.id.tracker;

import android.graphics.drawable.Drawable;

public class AppData {
    public String name;
    public String packageName;
    public String version;
    public String tracker;
    public int trackerColor;
    public Drawable icon;
    public String gaid = "";
    public String afId = "";
    public String installId = "";
    public String uuid = "";
    public String deviceId = "";
    public String singularId = "";
    public String adjustId = "";

    public AppData(String name, String packageName, String version,
                   String tracker, int trackerColor, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.version = version;
        this.tracker = tracker;
        this.trackerColor = trackerColor;
        this.icon = icon;
    }
}
