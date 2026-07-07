package com.example.vibetix.Activities.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.vibetix.Activities.Organizer.EventHubActivity;
import com.example.vibetix.R;

public class NotificationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView txtTitle = findViewById(R.id.txtDetailTitle);
        TextView txtBody  = findViewById(R.id.txtDetailBody);
        TextView txtTime  = findViewById(R.id.txtDetailTime);
        com.google.android.material.button.MaterialButton btnAction =
                findViewById(R.id.btnNotifAction);

        String title = getIntent().getStringExtra("title");
        String body  = getIntent().getStringExtra("body");
        String time  = getIntent().getStringExtra("time");
        String type  = getIntent().getStringExtra("type");
        String refId = getIntent().getStringExtra("refId");

        if (txtTitle != null) txtTitle.setText(title != null ? title : "");
        if (txtBody  != null) txtBody.setText(body   != null ? body  : "");
        if (txtTime  != null) txtTime.setText(time   != null ? time  : "");

        // Hiện nút điều hướng tuỳ theo loại thông báo
        if (btnAction != null) {
            if ("order_confirmed".equals(type)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Xem vé của tôi");
                btnAction.setOnClickListener(v -> openTicketsTab());
            } else if ("staff_assigned".equals(type)) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Xem sự kiện của tôi");
                final String eventId = refId;
                btnAction.setOnClickListener(v -> openEventHub(eventId));
            } else {
                btnAction.setVisibility(View.GONE);
            }
        }
    }

    private void openTicketsTab() {
        Intent intent = new Intent(this, UserMainActivity.class);
        intent.putExtra("openTab", "tickets");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openEventHub(String eventId) {
        if (eventId == null || eventId.isEmpty()) return;
        Intent intent = new Intent(this, EventHubActivity.class);
        intent.putExtra(EventHubActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(EventHubActivity.EXTRA_ROLE, "check_in_staff");
        startActivity(intent);
        finish();
    }
}
