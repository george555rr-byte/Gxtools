package com.gx.id.tracker;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerApps;
    private TextView tvAppsCount, tvTrackersCount, tvStatus;
    private EditText etSearch;
    private TextView btnFilterAll, btnFilterAf, btnFilterSin, btnFilterAdj;
    private AppAdapter adapter;
    private Scanner scanner;

    private List<AppData> allApps = new ArrayList<>();
    private List<AppData> shownApps = new ArrayList<>();
    private String currentFilter = "ALL";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerApps = findViewById(R.id.recyclerApps);
        tvAppsCount = findViewById(R.id.tvAppsCount);
        tvTrackersCount = findViewById(R.id.tvTrackersCount);
        tvStatus = findViewById(R.id.tvStatus);
        etSearch = findViewById(R.id.etSearch);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterAf = findViewById(R.id.btnFilterAf);
        btnFilterSin = findViewById(R.id.btnFilterSin);
        btnFilterAdj = findViewById(R.id.btnFilterAdj);

        recyclerApps.setLayoutManager(new GridLayoutManager(this, 3));

        scanner = new Scanner(this);

        adapter = new AppAdapter(app -> {
            ProgressDialog pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Reading " + app.name + " ...");
            pd.setCancelable(false);
            pd.show();

            new Thread(() -> {
                scanner.deepScan(app);
                new Handler(Looper.getMainLooper()).post(() -> {
                    pd.dismiss();
                    refreshCounts();
                    adapter.notifyDataSetChanged();
                    IdDialog.show(MainActivity.this, app);
                });
            }).start();
        });
        recyclerApps.setAdapter(adapter);

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                currentQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filters
        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterAf.setOnClickListener(v -> setFilter("AppsFlyer"));
        btnFilterSin.setOnClickListener(v -> setFilter("Singular"));
        btnFilterAdj.setOnClickListener(v -> setFilter("Adjust"));

        loadApps();
    }

    private void setFilter(String f) {
        currentFilter = f;
        applyFilters();
    }

    private void applyFilters() {
        shownApps.clear();
        for (AppData a : allApps) {
            boolean matchQuery = currentQuery.isEmpty()
                    || a.name.toLowerCase().contains(currentQuery)
                    || a.packageName.toLowerCase().contains(currentQuery);

            boolean matchFilter = "ALL".equals(currentFilter)
                    || a.tracker.equals(currentFilter);

            if (matchQuery && matchFilter) shownApps.add(a);
        }
        adapter.setData(shownApps);
        tvAppsCount.setText(String.valueOf(shownApps.size()));
    }

    private void refreshCounts() {
        int trackersFound = 0;
        for (AppData a : allApps) {
            if (!"None".equals(a.tracker)) trackersFound++;
        }
        tvTrackersCount.setText(String.valueOf(trackersFound));
    }

    private void loadApps() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Scanning tracking files...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            final List<AppData> apps = scanner.scan();
            new Handler(Looper.getMainLooper()).post(() -> {
                allApps.clear();
                allApps.addAll(apps);
                tvStatus.setText("ARMED");
                applyFilters();
                refreshCounts();
                pd.dismiss();
            });
        }).start();
    }
}
