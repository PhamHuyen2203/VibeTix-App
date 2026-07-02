package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheet hiển thị danh sách sự kiện yêu thích và stars/organizers đang theo dõi.
 */
public class FavoritesFollowsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MODE = "mode"; // "favorites" or "following"
    private String mode = "favorites";

    private RecyclerView rvFavoriteEvents, rvFollowing;
    private TextView txtNoFavorites, txtNoFollowing;

    private final List<Event> favoriteEvents = new ArrayList<>();
    private final List<FollowItem> followingList = new ArrayList<>();
    private FavoriteEventsAdapter favAdapter;
    private FollowingAdapter followAdapter;

    public static FavoritesFollowsBottomSheet newInstance(String mode) {
        FavoritesFollowsBottomSheet sheet = new FavoritesFollowsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) mode = getArguments().getString(ARG_MODE, "favorites");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_favorites_follows, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Force chiều cao 85% màn hình
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.85);
                bottomSheet.requestLayout();
                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight((int) (screenHeight * 0.85));
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set title theo mode
        TextView txtTitle = view.findViewById(R.id.txtSheetTitle);
        if (txtTitle != null) {
            txtTitle.setText("favorites".equals(mode) ? "Yêu thích" : "Đang theo dõi");
        }

        rvFavoriteEvents = view.findViewById(R.id.rvFavoriteEvents);
        rvFollowing = view.findViewById(R.id.rvFollowing);
        txtNoFavorites = view.findViewById(R.id.txtNoFavorites);
        txtNoFollowing = view.findViewById(R.id.txtNoFollowing);

        // Tìm header labels
        View headerFav = view.findViewById(R.id.txtHeaderFavorites);
        View headerFollow = view.findViewById(R.id.txtHeaderFollowing);

        favAdapter = new FavoriteEventsAdapter();
        rvFavoriteEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavoriteEvents.setAdapter(favAdapter);

        followAdapter = new FollowingAdapter();
        rvFollowing.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFollowing.setAdapter(followAdapter);

        // Hiện chỉ section tương ứng
        if ("favorites".equals(mode)) {
            if (headerFollow != null) headerFollow.setVisibility(View.GONE);
            if (rvFollowing != null) rvFollowing.setVisibility(View.GONE);
            if (txtNoFollowing != null) txtNoFollowing.setVisibility(View.GONE);
            loadFavoriteEvents();
        } else {
            if (headerFav != null) headerFav.setVisibility(View.GONE);
            if (rvFavoriteEvents != null) rvFavoriteEvents.setVisibility(View.GONE);
            if (txtNoFavorites != null) txtNoFavorites.setVisibility(View.GONE);
            loadFollowing();
        }
    }

    private void loadFavoriteEvents() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.EVENT_INTERESTS)
                .whereEqualTo("user_id", user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<String> eventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String eid = doc.getString("event_id");
                        if (eid != null && !eventIds.contains(eid)) eventIds.add(eid);
                    }
                    if (eventIds.isEmpty()) {
                        txtNoFavorites.setVisibility(View.VISIBLE);
                        return;
                    }
                    // Fetch events in batches of 10 (Firestore whereIn limit)
                    List<String> batch = eventIds.subList(0, Math.min(eventIds.size(), 10));
                    FirebaseFirestore.getInstance().collection(FirebaseCollections.EVENTS)
                            .whereIn(FieldPath.documentId(), batch)
                            .get()
                            .addOnSuccessListener(eventSnap -> {
                                if (!isAdded()) return;
                                favoriteEvents.clear();
                                for (QueryDocumentSnapshot doc : eventSnap) {
                                    Event ev = FirestoreHelper.docToEvent(doc);
                                    if (ev != null) favoriteEvents.add(ev);
                                }
                                if (favoriteEvents.isEmpty()) {
                                    txtNoFavorites.setVisibility(View.VISIBLE);
                                } else {
                                    txtNoFavorites.setVisibility(View.GONE);
                                    favAdapter.notifyDataSetChanged();
                                }
                            });
                });
    }

    private void loadFollowing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.USER_STAR_FOLLOWS)
                .whereEqualTo("user_id", user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<String> starIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String sid = doc.getString("star_id");
                        if (sid != null && !starIds.contains(sid)) starIds.add(sid);
                    }
                    if (starIds.isEmpty()) {
                        txtNoFollowing.setVisibility(View.VISIBLE);
                        return;
                    }
                    // Load star profiles
                    final int[] pending = {starIds.size()};
                    for (String sid : starIds) {
                        FirebaseFirestore.getInstance().collection("stars").document(sid).get()
                                .addOnSuccessListener(doc -> {
                                    if (!isAdded()) return;
                                    if (doc.exists()) {
                                        String name = doc.getString("stage_name");
                                        String avatar = doc.getString("avatar_url");
                                        if (name != null) {
                                            followingList.add(new FollowItem(sid, name, avatar, "star"));
                                        }
                                    } else {
                                        // Try organizers
                                        FirebaseFirestore.getInstance().collection("organizers").document(sid).get()
                                                .addOnSuccessListener(orgDoc -> {
                                                    if (!isAdded()) return;
                                                    if (orgDoc.exists()) {
                                                        String brandName = orgDoc.getString("brand_name");
                                                        if (brandName == null) brandName = orgDoc.getString("name");
                                                        String logo = orgDoc.getString("logo_url");
                                                        if (brandName != null) {
                                                            followingList.add(new FollowItem(sid, brandName, logo, "organizer"));
                                                        }
                                                    }
                                                    pending[0]--;
                                                    if (pending[0] <= 0) updateFollowingUI();
                                                });
                                        return;
                                    }
                                    pending[0]--;
                                    if (pending[0] <= 0) updateFollowingUI();
                                })
                                .addOnFailureListener(e -> {
                                    pending[0]--;
                                    if (pending[0] <= 0 && isAdded()) updateFollowingUI();
                                });
                    }
                });
    }

    private void updateFollowingUI() {
        if (followingList.isEmpty()) {
            txtNoFollowing.setVisibility(View.VISIBLE);
        } else {
            txtNoFollowing.setVisibility(View.GONE);
            followAdapter.notifyDataSetChanged();
        }
    }

    private void navigateToEventDetail(String eventId) {
        dismiss();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, EventDetailFragment.newInstance(eventId))
                    .addToBackStack("fav_event_detail")
                    .commit();
        }
    }

    private void navigateToStarDetail(String starId, String name) {
        dismiss();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, StarDetailFragment.newInstance(starId, name))
                    .addToBackStack("fav_star_detail")
                    .commit();
        }
    }

    // ── Data class for follow items ──────────────────────────────────────────────
    private static class FollowItem {
        final String id;
        final String name;
        final String avatarUrl;
        final String type; // "star" or "organizer"

        FollowItem(String id, String name, String avatarUrl, String type) {
            this.id = id;
            this.name = name;
            this.avatarUrl = avatarUrl;
            this.type = type;
        }
    }

    // ── Adapter: Favorite Events ─────────────────────────────────────────────────
    private class FavoriteEventsAdapter extends RecyclerView.Adapter<FavoriteEventsAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int p = (int) (12 * parent.getResources().getDisplayMetrics().density);
            row.setPadding(p, p, p, p);
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event event = favoriteEvents.get(position);
            holder.bind(event);
        }

        @Override
        public int getItemCount() {
            return favoriteEvents.size();
        }

        class VH extends RecyclerView.ViewHolder {
            private final ImageView img;
            private final TextView txtTitle;
            private final TextView txtDate;

            VH(LinearLayout row) {
                super(row);
                float dp = row.getResources().getDisplayMetrics().density;

                img = new ImageView(row.getContext());
                int imgSize = (int) (48 * dp);
                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(imgSize, imgSize);
                img.setLayoutParams(imgLp);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setClipToOutline(true);
                row.addView(img);

                LinearLayout info = new LinearLayout(row.getContext());
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                infoLp.setMarginStart((int) (12 * dp));
                info.setLayoutParams(infoLp);

                txtTitle = new TextView(row.getContext());
                txtTitle.setTextColor(0xFF1C1B1B);
                txtTitle.setTextSize(14f);
                txtTitle.setMaxLines(1);
                info.addView(txtTitle);

                txtDate = new TextView(row.getContext());
                txtDate.setTextColor(0xFF808E92);
                txtDate.setTextSize(12f);
                info.addView(txtDate);

                row.addView(info);

                ImageView chevron = new ImageView(row.getContext());
                chevron.setLayoutParams(new LinearLayout.LayoutParams((int) (18 * dp), (int) (18 * dp)));
                chevron.setImageResource(R.drawable.ic_chevron_right);
                chevron.setColorFilter(0xFF808E92);
                row.addView(chevron);
            }

            void bind(Event event) {
                txtTitle.setText(event.getTitle());
                txtDate.setText(event.getDate() != null ? event.getDate() : "");

                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    Glide.with(img.getContext()).load(event.getImageUrl())
                            .centerCrop()
                            .placeholder(R.drawable.event_live_non_song)
                            .into(img);
                } else {
                    img.setImageResource(R.drawable.event_live_non_song);
                }

                itemView.setOnClickListener(v -> navigateToEventDetail(event.getId()));
            }
        }
    }

    // ── Adapter: Following Stars/Organizers ──────────────────────────────────────
    private class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int p = (int) (12 * parent.getResources().getDisplayMetrics().density);
            row.setPadding(p, p, p, p);
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FollowItem item = followingList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return followingList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            private final ImageView img;
            private final TextView txtName;
            private final TextView txtType;

            VH(LinearLayout row) {
                super(row);
                float dp = row.getResources().getDisplayMetrics().density;

                img = new ImageView(row.getContext());
                int imgSize = (int) (40 * dp);
                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(imgSize, imgSize);
                img.setLayoutParams(imgLp);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                row.addView(img);

                LinearLayout info = new LinearLayout(row.getContext());
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                infoLp.setMarginStart((int) (12 * dp));
                info.setLayoutParams(infoLp);

                txtName = new TextView(row.getContext());
                txtName.setTextColor(0xFF1C1B1B);
                txtName.setTextSize(14f);
                txtName.setMaxLines(1);
                info.addView(txtName);

                txtType = new TextView(row.getContext());
                txtType.setTextColor(0xFF808E92);
                txtType.setTextSize(11f);
                info.addView(txtType);

                row.addView(info);

                ImageView chevron = new ImageView(row.getContext());
                chevron.setLayoutParams(new LinearLayout.LayoutParams((int) (18 * dp), (int) (18 * dp)));
                chevron.setImageResource(R.drawable.ic_chevron_right);
                chevron.setColorFilter(0xFF808E92);
                row.addView(chevron);
            }

            void bind(FollowItem item) {
                txtName.setText(item.name);
                txtType.setText("star".equals(item.type) ? "Nghệ sĩ" : "Ban tổ chức");

                if (item.avatarUrl != null && !item.avatarUrl.isEmpty()) {
                    Glide.with(img.getContext()).load(item.avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.img_mascot_wave)
                            .into(img);
                } else {
                    Glide.with(img.getContext()).load(R.drawable.img_mascot_wave)
                            .circleCrop().into(img);
                }

                itemView.setOnClickListener(v -> navigateToStarDetail(item.id, item.name));
            }
        }
    }
}
