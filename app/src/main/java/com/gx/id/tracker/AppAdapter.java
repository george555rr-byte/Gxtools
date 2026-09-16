package com.gx.id.tracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {

    public interface OnItemClick {
        void onClick(AppData app);
    }

    private List<AppData> data = new ArrayList<>();
    private final OnItemClick listener;

    public AppAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    public void setData(List<AppData> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppData app = data.get(position);
        h.tvName.setText(app.name);
        h.tvPackage.setText(app.packageName);
        h.tvVersion.setText("v" + app.version);
        h.tvTracker.setText(app.tracker);

        if (app.icon != null) {
            h.imgIcon.setImageDrawable(app.icon);
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20f);
        bg.setColor((app.trackerColor & 0x00FFFFFF) | 0x22000000);
        bg.setStroke(2, (app.trackerColor & 0x00FFFFFF) | 0x80000000);
        h.tvTracker.setBackground(bg);
        h.tvTracker.setTextColor(app.trackerColor);

        h.itemView.setOnClickListener(v -> listener.onClick(app));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName, tvPackage, tvVersion, tvTracker;

        VH(View v) {
            super(v);
            imgIcon = v.findViewById(R.id.imgIcon);
            tvName = v.findViewById(R.id.tvName);
            tvPackage = v.findViewById(R.id.tvPackage);
            tvVersion = v.findViewById(R.id.tvVersion);
            tvTracker = v.findViewById(R.id.tvTracker);
        }
    }
          }
