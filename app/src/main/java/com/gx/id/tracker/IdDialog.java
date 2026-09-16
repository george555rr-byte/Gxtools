package com.gx.id.tracker;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class IdDialog {

    public static void show(Context ctx, AppData app) {
        View root = LayoutInflater.from(ctx).inflate(R.layout.dialog_ids, null);

        TextView tvAppName = root.findViewById(R.id.tvAppName);
        TextView tvAppPkg = root.findViewById(R.id.tvAppPkg);
        LinearLayout container = root.findViewById(R.id.containerIds);
        TextView btnClose = root.findViewById(R.id.btnClose);
        View btnCopyAll = root.findViewById(R.id.btnCopyAll);

        tvAppName.setText(app.name);
        tvAppPkg.setText(app.packageName);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // ============ SHOW IDs BASED ON TRACKER ============
        if ("AppsFlyer".equals(app.tracker)) {
            addRow(ctx, container, "GAID", safe(app.gaid));
            addRow(ctx, container, "AF_ID", safe(app.afId));
        } else if ("Singular".equals(app.tracker)) {
            addRow(ctx, container, "GAID", safe(app.gaid));
            addRow(ctx, container, "SINGULAR_ID", safe(app.singularId));
        } else if ("Adjust".equals(app.tracker)) {
            addRow(ctx, container, "GAID", safe(app.gaid));
            addRow(ctx, container, "ADJUST_ID", "-");
        } else {
            addRow(ctx, container, "GAID", safe(app.gaid));
        }

        btnCopyAll.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            sb.append("App: ").append(app.name).append("\n");
            sb.append("Package: ").append(app.packageName).append("\n");
            sb.append("Tracker: ").append(app.tracker).append("\n");
            sb.append("GAID: ").append(safe(app.gaid)).append("\n");

            if ("AppsFlyer".equals(app.tracker)) {
                sb.append("AF_ID: ").append(safe(app.afId));
            } else if ("Singular".equals(app.tracker)) {
                sb.append("SINGULAR_ID: ").append(safe(app.singularId));
            } else if ("Adjust".equals(app.tracker)) {
                sb.append("ADJUST_ID: -");
            }

            copy(ctx, "GX_ID", sb.toString().trim());
            Toast.makeText(ctx, "All IDs copied!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private static void addRow(Context ctx, LinearLayout container, String label, String value) {
        View row = LayoutInflater.from(ctx).inflate(R.layout.row_id, container, false);
        TextView tvLabel = row.findViewById(R.id.tvLabel);
        TextView tvValue = row.findViewById(R.id.tvValue);
        TextView btnCopy = row.findViewById(R.id.btnCopy);

        tvLabel.setText(label);
        tvValue.setText(value);
        btnCopy.setOnClickListener(v -> {
            copy(ctx, label, value);
            Toast.makeText(ctx, label + " copied!", Toast.LENGTH_SHORT).show();
        });

        container.addView(row);
    }

    private static void copy(Context ctx, String label, String value) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
    }
                }
