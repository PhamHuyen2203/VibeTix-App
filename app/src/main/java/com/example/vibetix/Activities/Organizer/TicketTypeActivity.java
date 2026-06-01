package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.vibetix.Fragments.Organizer.TicketTypeListFragment;
import com.example.vibetix.R;

public class TicketTypeActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_type);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            finish();
            return;
        }

        setupToolbar();

        if (savedInstanceState == null) {
            TicketTypeListFragment fragment = new TicketTypeListFragment();
            Bundle args = new Bundle();
            args.putString(TicketTypeListFragment.ARG_EVENT_ID, eventId);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerTicketType, fragment)
                    .commit();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarTicketType);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }
}
