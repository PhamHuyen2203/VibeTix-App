package com.example.vibetix.Fragments.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Adapters.OrganizerAdapter;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrganizerApprovalFragment extends Fragment implements OrganizerAdapter.OnOrganizerActionListener {

    private RecyclerView recyclerView;
    private OrganizerAdapter adapter;
    private List<Organizer> allOrganizers = new ArrayList<>();
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_approval, container, false);

        recyclerView = view.findViewById(R.id.rv_organizers);
        tabLayout = view.findViewById(R.id.tab_layout_organizer);

        setupRecyclerView();
        setupTabLayout();
        loadDummyData(); // Replace with real Firebase/API data later

        return view;
    }

    private void setupRecyclerView() {
        adapter = new OrganizerAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterList(tab.getPosition() == 1); // 0: Pending, 1: Verified
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadDummyData() {
        // Mock data using UUID v4
        allOrganizers.add(new Organizer(UUID.randomUUID().toString(), "user-1", "Music Show Co.", null, null, null, "contact@music.com", "0912345678"));
        allOrganizers.add(new Organizer(UUID.randomUUID().toString(), "user-2", "Vibe Events", null, null, "www.vibe.vn", "info@vibe.vn", "0988776655"));
        allOrganizers.add(new Organizer(UUID.randomUUID().toString(), "user-3", "Old Partner", null, null, "www.old.com", "old@partner.com", "0112233445"));
        // Set verified for last one
        allOrganizers.get(2).setVerified(true);

        filterList(false); // Default to Pending tab
    }

    private void filterList(boolean isVerified) {
        List<Organizer> filteredList = new ArrayList<>();
        for (Organizer o : allOrganizers) {
            if (o.isVerified() == isVerified) {
                filteredList.add(o);
            }
        }
        adapter.updateList(filteredList);
    }

    @Override
    public void onApprove(Organizer organizer) {
        // Update logic
        organizer.setVerified(true);
        Toast.makeText(getContext(), "Approved: " + organizer.getBrandName(), Toast.LENGTH_SHORT).show();
        
        // Refresh UI
        filterList(tabLayout.getSelectedTabPosition() == 1);
    }

    @Override
    public void onReject(Organizer organizer) {
        allOrganizers.remove(organizer);
        Toast.makeText(getContext(), "Rejected: " + organizer.getBrandName(), Toast.LENGTH_SHORT).show();
        
        // Refresh UI
        filterList(tabLayout.getSelectedTabPosition() == 1);
    }
}
