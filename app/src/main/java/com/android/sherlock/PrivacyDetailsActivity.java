package com.android.sherlock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import hb.sherlock.R;

/**
 * 堆栈详情
 */
public class PrivacyDetailsActivity extends AppCompatActivity {

    public static void start(Context context, String details) {
        Intent starter = new Intent(context, PrivacyDetailsActivity.class);
        starter.putExtra("details", details);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_details);


        // 显示堆栈详情信息
        String details = getIntent().getStringExtra("details");
        TextView tvDetails = findViewById(R.id.tvDetails);
        tvDetails.setText(details);

    }
}