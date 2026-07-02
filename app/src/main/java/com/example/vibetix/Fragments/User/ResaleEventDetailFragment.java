package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.ResaleTicketListAdapter;
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketTransferRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Chi tiết sự kiện + danh sách vé bán lại. Bấm "Mua lại" → vào flow điền thông tin + thanh toán.
 */
public class ResaleEventDetailFragment extends Fragment {

    private String eventId = "";
    private RecyclerView rvListings;
    private TextView txtEmpty, txtTitle, txtDate, txtLocation;
    private ImageView imvBanner;
    private ResaleTicketListAdapter adapter;
    private final List<TicketTransfer> listings = new ArrayList<>();
    private final TicketTransferRepository repo = new TicketTransferRepository();

    public static ResaleEventDetailFragment newInstance(String eventId) {
        ResaleEventDetailFragment f = new ResaleEventDetailFragment();
        Bundle b = new Bundle();
        b.putString("eventId", eventId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) eventId = getArguments().getString("eventId", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resale_event_detail, container, false);

        View header = view.findViewById(R.id.layoutResaleDetailHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top + dp(8), v.getPaddingRight(), dp(14));
                return insets;
            });
        }

        view.findViewById(R.id.btnResaleDetailBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        imvBanner = view.findViewById(R.id.imvResaleDetailBanner);
        txtTitle = view.findViewById(R.id.txtResaleDetailTitle);
        txtDate = view.findViewById(R.id.txtResaleDetailDate);
        txtLocation = view.findViewById(R.id.txtResaleDetailLocation);
        rvListings = view.findViewById(R.id.rvResaleListings);
        txtEmpty = view.findViewById(R.id.txtResaleDetailEmpty);

        adapter = new ResaleTicketListAdapter(requireContext(), listings, this::onBuyClick);
        rvListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvListings.setAdapter(adapter);

        loadListings();
        return view;
    }

    private void loadListings() {
        repo.getAllPendingTransfers(new TicketTransferRepository.OnTransfersLoadedListener() {
            @Override
            public void onSuccess(List<TicketTransfer> transfers) {
                if (!isAdded()) return;
                listings.clear();
                boolean headerSet = false;
                for (TicketTransfer t : transfers) {
                    String eid = t.getEventId() != null ? t.getEventId() : t.getTransferId();
                    if (eventId.equals(eid)) {
                        listings.add(t);
                        if (!headerSet) {
                            bindEventHeader(t);
                            headerSet = true;
                        }
                    }
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

    private void bindEventHeader(TicketTransfer t) {
        txtTitle.setText(t.getEventTitle());
        txtDate.setText(t.getEventDate());
        txtLocation.setText(t.getEventLocation());
        if (t.getEventImageUrl() != null && !t.getEventImageUrl().isEmpty()) {
            Glide.with(this).load(t.getEventImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imvBanner);
        }
    }

    private void updateEmpty() {
        txtEmpty.setVisibility(listings.isEmpty() ? View.VISIBLE : View.GONE);
        rvListings.setVisibility(listings.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onBuyClick(TicketTransfer t) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để mua vé", Toast.LENGTH_SHORT).show();
            return;
        }
        // Không cho tự mua vé của chính mình
        if (user.getUid().equals(t.getSenderId())) {
            Toast.makeText(requireContext(), "Bạn không thể mua lại vé do chính mình đăng bán", Toast.LENGTH_SHORT).show();
            return;
        }

        String typeName = "Vé bán lại - " + (t.getEventTitle() != null ? t.getEventTitle() : "");
        FillAttendeeInfoFragment frag = FillAttendeeInfoFragment.newInstanceResale(
                t.getEventId(), typeName, t.getPrice(), t.getEventImageUrl(), t.getTransferId());

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, frag)
                .addToBackStack("resale_buy")
                .commit();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
