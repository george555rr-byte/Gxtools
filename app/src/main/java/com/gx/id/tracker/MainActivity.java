package com.gx.id.tracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerApps;
    private TextView tvAppsCount, tvTrackersCount;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerApps = findViewById(R.id.recyclerApps);
        tvAppsCount = findViewById(R.id.tvAppsCount);
        tvTrackersCount = findViewById(R.id.tvTrackersCount);

        recyclerApps.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppAdapter(app -> IdDialog.show(MainActivity.this, app));
        recyclerApps.setAdapter(adapter);

        new Thread(() -> {
            Scanner scanner = new Scanner(MainActivity.this);
            List<AppData> apps = scanner.scan();

            int trackersFound = 0;
            for (AppData a : apps) {
                if (!"None".equals(a.tracker)) trackersFound++;
            }
            final int finalTrackers = trackersFound;

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.setData(apps);
                tvAppsCount.setText(String.valueOf(apps.size()));
                tvTrackersCount.setText(String.valueOf(finalTrackers));
            });
        }).start();
    }
}
