package com.gx.id.tracker;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("GX_ID Test");
        tv.setTextColor(0xFFF0F0F0);
        tv.setBackgroundColor(0xFF0A0A0F);
        tv.setTextSize(24);
        tv.setPadding(50, 200, 50, 50);
        setContentView(tv);
    }
}
