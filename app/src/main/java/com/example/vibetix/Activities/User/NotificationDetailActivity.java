package com.example.vibetix.Activities.User;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.R;

public class NotificationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView txtTitle = findViewById(R.id.txtDetailTitle);
        TextView txtBody  = findViewById(R.id.txtDetailBody);
        TextView txtTime  = findViewById(R.id.txtDetailTime);

        String title = getIntent().getStringExtra("title");
        String body  = getIntent().getStringExtra("body");
        String time  = getIntent().getStringExtra("time");

        if (txtTitle != null) txtTitle.setText(title != null ? title : "");
        if (txtBody  != null) txtBody.setText(body   != null ? body  : "");
        if (txtTime  != null) txtTime.setText(time   != null ? time  : "");
    }
}
