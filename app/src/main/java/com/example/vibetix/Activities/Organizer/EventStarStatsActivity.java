package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.EventStar;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.Star;
import com.example.vibetix.Models.UserStarFollow;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ActivityEventStarStatsBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventStarStatsActivity extends AppCompatActivity {

    private ActivityEventStarStatsBinding binding;
    private FirebaseFirestore db;
    private String eventId;

    private Set<String> buyerSet = new HashSet<>();
    private List<StarStatItem> statItems = new ArrayList<>();
    private StarStatsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventStarStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getStringExtra("EXTRA_EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không có sự kiện được chọn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvStats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StarStatsAdapter();
        binding.rvStats.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.rvStats.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        statItems.clear();
        buyerSet.clear();

        // G2 fix: orders không có event_id → phải query order_items trước để lấy order_id
        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(itemSnap -> {
                    Set<String> orderIds = new HashSet<>();
                    for (DocumentSnapshot doc : itemSnap) {
                        String oid = doc.getString("order_id");
                        if (oid != null) orderIds.add(oid);
                    }
                    if (orderIds.isEmpty()) {
                        fetchEventStars();
                        return;
                    }
                    // Chunk by 10 for whereIn
                    List<String> orderIdList = new ArrayList<>(orderIds);
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (int i = 0; i < orderIdList.size(); i += 10) {
                        List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                        tasks.add(db.collection(FirebaseCollections.ORDERS)
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .get());
                    }
                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        for (Object r : results) {
                            QuerySnapshot snap = (QuerySnapshot) r;
                            for (DocumentSnapshot doc : snap) {
                                String status = doc.getString("status");
                                boolean isPaid = status != null &&
                                        (status.equalsIgnoreCase("paid") || status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("confirmed"));
                                if (isPaid) {
                                    String uid = doc.getString("user_id");
                                    if (uid != null) buyerSet.add(uid);
                                }
                            }
                        }
                        fetchEventStars();
                    }).addOnFailureListener(e -> fetchEventStars());
                })
                .addOnFailureListener(e -> showFailureAlert(e));
    }

    private void fetchEventStars() {
        db.collection(FirebaseCollections.EVENT_STARS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> starIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        EventStar es = doc.toObject(EventStar.class);
                        if (es != null && es.getStarId() != null) {
                            starIds.add(es.getStarId());
                        }
                    }

                    if (starIds.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    fetchStarDetails(starIds);
                })
                .addOnFailureListener(this::showFailureAlert);
    }

    private void fetchStarDetails(List<String> starIds) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : starIds) {
            tasks.add(db.collection(FirebaseCollections.STARS).document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            for (Object res : results) {
                DocumentSnapshot doc = (DocumentSnapshot) res;
                if (doc.exists()) {
                    Star star = doc.toObject(Star.class);
                    if (star != null) {
                        star.setStarId(doc.getId());
                        StarStatItem item = new StarStatItem();
                        item.star = star;
                        statItems.add(item);
                    }
                }
            }

            fetchFollowersForStars();
        }).addOnFailureListener(this::showFailureAlert);
    }

    private void fetchFollowersForStars() {
        if (statItems.isEmpty()) {
            binding.pbLoading.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (StarStatItem item : statItems) {
            tasks.add(db.collection(FirebaseCollections.USER_STAR_FOLLOWS)
                    .whereEqualTo("star_id", item.star.getStarId())
                    .get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            for (int i = 0; i < results.size(); i++) {
                QuerySnapshot snap = (QuerySnapshot) results.get(i);
                StarStatItem item = statItems.get(i);
                
                Set<String> followerSet = new HashSet<>();
                for (DocumentSnapshot doc : snap) {
                    UserStarFollow follow = doc.toObject(UserStarFollow.class);
                    if (follow != null && follow.getUserId() != null) {
                        followerSet.add(follow.getUserId());
                    }
                }
                
                // Intersection
                Set<String> convertedUsers = new HashSet<>(followerSet);
                convertedUsers.retainAll(buyerSet);
                
                // G1 fix: dùng số đếm thực từ query user_star_follows thay vì cached field
                item.totalFollowers = followerSet.size();
                item.convertedFollowers = convertedUsers.size();
            }

            binding.pbLoading.setVisibility(View.GONE);
            if (statItems.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.rvStats.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(this::showFailureAlert);
    }

    private void showFailureAlert(Exception e) {
        binding.pbLoading.setVisibility(View.GONE);
        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // --- Inner Classes ---

    private static class StarStatItem {
        Star star;
        int totalFollowers;
        int convertedFollowers;
        
        double getConversionRate() {
            if (totalFollowers == 0) return 0;
            return ((double) convertedFollowers / totalFollowers) * 100.0;
        }
    }

    private class StarStatsAdapter extends RecyclerView.Adapter<StarStatsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_star_stat, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StarStatItem item = statItems.get(position);

            holder.tvName.setText(item.star.getStageName() != null ? item.star.getStageName() : "Unknown");
            holder.tvTotalFollowers.setText("Tổng Followers: " + item.totalFollowers);
            holder.tvFollowerBuyers.setText(String.valueOf(item.convertedFollowers));
            
            double rate = item.getConversionRate();
            holder.tvConversionRate.setText(String.format("%.1f%%", rate));

            if (item.star.getAvatarUrl() != null && !item.star.getAvatarUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.star.getAvatarUrl())
                        .placeholder(R.drawable.ic_organizer_placeholder)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_organizer_placeholder);
            }
        }

        @Override
        public int getItemCount() {
            return statItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView ivAvatar;
            TextView tvName, tvTotalFollowers, tvFollowerBuyers, tvConversionRate;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                tvTotalFollowers = itemView.findViewById(R.id.tvTotalFollowers);
                tvFollowerBuyers = itemView.findViewById(R.id.tvFollowerBuyers);
                tvConversionRate = itemView.findViewById(R.id.tvConversionRate);
            }
        }
    }
}
