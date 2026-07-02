package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.ResaleEventGroupAdapter;
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketTransferRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình "Vé bán lại" — list các sự kiện đang có vé bán lại (group theo sự kiện).
 * Mở từ "Xem thêm" ở mục Resale trên Homepage.
 */
public class ResaleMarketplaceFragment extends Fragment {

    private RecyclerView rvGroups;
    private TextView txtEmpty;
    private ResaleEventGroupAdapter adapter;
    private final List<ResaleEventGroupAdapter.ResaleGroup> groups = new ArrayList<>();
    private final TicketTransferRepository repo = new TicketTransferRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resale_marketplace, container, false);

        View header = view.findViewById(R.id.layoutResaleMpHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top + dp(8), v.getPaddingRight(), dp(14));
                return insets;
            });
        }

        ImageView btnBack = view.findViewById(R.id.btnResaleMpBack);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        rvGroups = view.findViewById(R.id.rvResaleGroups);
        txtEmpty = view.findViewById(R.id.txtResaleMpEmpty);

        adapter = new ResaleEventGroupAdapter(requireContext(), groups, this::openEventDetail);
        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGroups.setAdapter(adapter);

        loadResaleGroups();
        return view;
    }

    private void loadResaleGroups() {
        repo.getAllPendingTransfers(new TicketTransferRepository.OnTransfersLoadedListener() {
            @Override
            public void onSuccess(List<TicketTransfer> transfers) {
                if (!isAdded()) return;
                groups.clear();
                // Group theo eventId
                Map<String, ResaleEventGroupAdapter.ResaleGroup> map = new LinkedHashMap<>();
                for (TicketTransfer t : transfers) {
                    String eid = t.getEventId() != null ? t.getEventId() : t.getTransferId();
                    ResaleEventGroupAdapter.ResaleGroup g = map.get(eid);
                    if (g == null) {
                        g = new ResaleEventGroupAdapter.ResaleGroup();
                        g.eventId = eid;
                        g.title = t.getEventTitle();
                        g.date = t.getEventDate();
                        g.imageUrl = t.getEventImageUrl();
                        g.location = t.getEventLocation();
                        g.count = 0;
                        g.minPrice = Long.MAX_VALUE;
                        map.put(eid, g);
                    }
                    g.count++;
                    if (t.getPrice() > 0 && t.getPrice() < g.minPrice) g.minPrice = t.getPrice();
                }
                for (ResaleEventGroupAdapter.ResaleGroup g : map.values()) {
                    if (g.minPrice == Long.MAX_VALUE) g.minPrice = 0;
                    groups.add(g);
                }
                adapter.notifyDataSetChanged();
                updateEmpty();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                updateEmpty();
            }
        });
    }

    private void updateEmpty() {
        boolean empty = groups.isEmpty();
        txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvGroups.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openEventDetail(ResaleEventGroupAdapter.ResaleGroup group) {
        ResaleEventDetailFragment frag = ResaleEventDetailFragment.newInstance(group.eventId);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, frag)
                .addToBackStack("resale_event_detail")
                .commit();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
