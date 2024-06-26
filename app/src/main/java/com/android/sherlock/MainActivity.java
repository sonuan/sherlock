package com.android.sherlock;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import java.io.File;

import hb.sherlock.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 测试
        findViewById(R.id.tvInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrivacyCheckResultActivity.launch(v.getContext(), "/");
            }
        });

        FloatingViewUtils.install(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FloatingViewUtils.showFloat(this, new File(""));
    }
}