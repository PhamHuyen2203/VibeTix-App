package com.example.vibetix.Fragments.User;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StarDetailFragment extends Fragment {

    private static final String TAG = "StarDetailFragment";
    private static final String ARG_STAR_ID = "star_id";
    private static final String ARG_STAR_NAME = "star_name";

    private String starId = "";
    private String starName = "";

    private ImageView btnStarBack;
    private ImageView imvStarCover;
    private ImageView imvStarAvatar;
    private ImageView imvStarVerified;
    private TextView txtStarStageName;
    private TextView txtStarSubtitle;
    private TextView txtStarFollowers;
    private TextView txtStarBio;
    private Button btnFollowStar;
    private ProgressBar pbStarEventsLoading;
    private TextView txtStarEventsEmpty;
    private RecyclerView rvStarEvents;

    private EventAdapter eventAdapter;
    private final ArrayList<Event> starEventsList = new ArrayList<>();
    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private boolean isFollowing = false;

    public static StarDetailFragment newInstance(String starId, String starName) {
        StarDetailFragment fragment = new StarDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STAR_ID, starId);
        args.putString(ARG_STAR_NAME, starName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            starId = getArguments().getString(ARG_STAR_ID, "");
            starName = getArguments().getString(ARG_STAR_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_star_detail, container, false);
        bindViews(view);
        applyInsets(view);
        setupRecyclerView();
        setupClickListeners();
        loadStarProfile();
        loadStarEvents();
        return view;
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutStarHeader);
        if (header != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                if (top <= 0) {
                    int resId = v.getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) top = v.getResources().getDimensionPixelSize(resId);
                }
                if (top <= 0) top = (int) (28 * v.getResources().getDisplayMetrics().density);
                v.setPadding(0, top, 0, 0);
                return insets;
            });
            androidx.core.view.ViewCompat.requestApplyInsets(header);
        }
    }

    private void bindViews(View view) {
        btnStarBack = view.findViewById(R.id.btnStarBack);
        imvStarCover = view.findViewById(R.id.imvStarCover);
        imvStarAvatar = view.findViewById(R.id.imvStarAvatar);
        imvStarVerified = view.findViewById(R.id.imvStarVerified);
        txtStarStageName = view.findViewById(R.id.txtStarStageName);
        txtStarSubtitle = view.findViewById(R.id.txtStarSubtitle);
        txtStarFollowers = view.findViewById(R.id.txtStarFollowers);
        txtStarBio = view.findViewById(R.id.txtStarBio);
        btnFollowStar = view.findViewById(R.id.btnFollowStar);
        pbStarEventsLoading = view.findViewById(R.id.pbStarEventsLoading);
        txtStarEventsEmpty = view.findViewById(R.id.txtStarEventsEmpty);
        rvStarEvents = view.findViewById(R.id.rvStarEvents);

        if (!starName.isEmpty()) {
            txtStarStageName.setText(starName);
        }
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(requireContext(), starEventsList,
                event -> openEventDetail(event.getId()),
                R.layout.item_event_card_grid);
        rvStarEvents.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        rvStarEvents.setAdapter(eventAdapter);
    }

    private void setupClickListeners() {
        btnStarBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnFollowStar.setOnClickListener(v -> toggleFollow());
    }

    private void loadStarProfile() {
        if (starId.isEmpty()) return;

        // Try searching in "stars" collection first
        FirebaseFirestore.getInstance().collection("stars").document(starId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        populateStarFromDocument(doc);
                    } else {
                        // Try searching in "organizers" collection if not found in "stars"
                        fetchFromOrganizersCollection();
                    }
                })
                .addOnFailureListener(e -> fetchFromOrganizersCollection());
    }

    private void fetchFromOrganizersCollection() {
        isOrganizerProfile = true;
        FirebaseFirestore.getInstance().collection("organizers").document(starId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;
                    String brandName = doc.getString("brand_name");
                    if (brandName == null) brandName = doc.getString("name");
                    if (brandName != null && !brandName.isEmpty()) {
                        txtStarStageName.setText(brandName);
                    }
                    String logoUrl = doc.getString("logo_url");
                    if (imvStarAvatar != null) {
                        com.example.vibetix.Utils.ImageUtils.loadCircle(
                                this, logoUrl, imvStarAvatar, R.drawable.img_mascot_wave);
                    }
                    String desc = doc.getString("description");
                    if (desc != null && !desc.isEmpty()) {
                        txtStarBio.setText(desc);
                    }
                    txtStarSubtitle.setText(getString(R.string.str_official_organizer_label));
                    // Đếm follower thực từ user_star_follows
                    loadOrganizerFollowerCount();
                    checkFollowStatus();
                });
    }

    private void loadOrganizerFollowerCount() {
        FirebaseFirestore.getInstance()
                .collection(com.example.vibetix.Firebase.FirebaseCollections.USER_STAR_FOLLOWS)
                .whereEqualTo("star_id", starId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    currentFollowerCount = snap != null ? snap.size() : 0;
                    if (txtStarFollowers != null) {
                        txtStarFollowers.setText(getString(R.string.str_follower_count_label, formatter.format(currentFollowerCount)));
                    }
                });
    }

    private void populateStarFromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
        String stageName = doc.getString("stage_name");
        String realName = doc.getString("real_name");
        if (stageName != null && !stageName.isEmpty()) {
            txtStarStageName.setText(stageName);
        }
        if (realName != null && !realName.isEmpty()) {
            txtStarSubtitle.setText(realName);
        } else {
            txtStarSubtitle.setText("Nghệ sĩ biểu diễn");
        }

        Boolean verified = doc.getBoolean("is_verified");
        imvStarVerified.setVisibility(Boolean.TRUE.equals(verified) ? View.VISIBLE : View.GONE);

        Long followers = doc.getLong("follower_count");
        currentFollowerCount = followers != null ? followers.intValue() : 0;
        txtStarFollowers.setText(getString(R.string.str_follower_count_label, formatter.format(currentFollowerCount)));
        checkFollowStatus();

        String bio = doc.getString("bio");
        if (bio != null && !bio.isEmpty()) {
            txtStarBio.setText(bio);
        }

        String avatarUrl = doc.getString("avatar_url");
        if (avatarUrl != null && !avatarUrl.isEmpty() && imvStarAvatar != null) {
            Glide.with(requireContext()).load(avatarUrl).circleCrop().into(imvStarAvatar);
        } else if (imvStarAvatar != null) {
            Glide.with(requireContext()).load(R.drawable.img_mascot_wave).circleCrop().into(imvStarAvatar);
        }

        String coverUrl = doc.getString("cover_url");
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(requireContext()).load(coverUrl).into(imvStarCover);
        }
    }

    /**
     * Truy vấn collection event_stars nơi star_id == starId để lấy ra các event_id tương ứng
     */
    private void loadStarEvents() {
        if (starId.isEmpty()) {
            showEventsEmpty();
            return;
        }

        pbStarEventsLoading.setVisibility(View.VISIBLE);
        rvStarEvents.setVisibility(View.GONE);
        txtStarEventsEmpty.setVisibility(View.GONE);

        // 1. Tìm các document trong event_stars theo star_id
        FirebaseFirestore.getInstance().collection("event_stars")
                .whereEqualTo("star_id", starId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<String> eventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String eid = doc.getString("event_id");
                        if (eid != null && !eid.isEmpty() && !eventIds.contains(eid)) {
                            eventIds.add(eid);
                        }
                    }

                    if (eventIds.isEmpty()) {
                        // Thử tìm theo organizer_id trong events collection
                        fetchEventsByOrganizerId();
                    } else {
                        // 2. Tải chi tiết các sự kiện từ list eventIds
                        fetchEventsByIds(eventIds);
                    }
                })
                .addOnFailureListener(e -> fetchEventsByOrganizerId());
    }

    private void fetchEventsByOrganizerId() {
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("organizer_id", starId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    starEventsList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Event ev = FirestoreHelper.docToEvent(doc);
                        if (ev != null) starEventsList.add(ev);
                    }
                    updateEventsUI();
                })
                .addOnFailureListener(e -> showEventsEmpty());
    }

    private void fetchEventsByIds(List<String> eventIds) {
        List<String> subList = eventIds.subList(0, Math.min(eventIds.size(), 20));
        FirebaseFirestore.getInstance().collection("events")
                .whereIn(FieldPath.documentId(), subList)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    starEventsList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Event ev = FirestoreHelper.docToEvent(doc);
                        if (ev != null) starEventsList.add(ev);
                    }
                    updateEventsUI();
                })
                .addOnFailureListener(e -> showEventsEmpty());
    }

    private void updateEventsUI() {
        pbStarEventsLoading.setVisibility(View.GONE);
        if (starEventsList.isEmpty()) {
            showEventsEmpty();
        } else {
            txtStarEventsEmpty.setVisibility(View.GONE);
            rvStarEvents.setVisibility(View.VISIBLE);
            eventAdapter.notifyDataSetChanged();
        }
    }

    private void showEventsEmpty() {
        pbStarEventsLoading.setVisibility(View.GONE);
        rvStarEvents.setVisibility(View.GONE);
        txtStarEventsEmpty.setVisibility(View.VISIBLE);
    }

    private void openEventDetail(String eventId) {
        if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
            ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSubFragment(EventDetailFragment.newInstance(eventId));
        }
    }

    // ── Follow/Unfollow logic ─────────────────────────────────────────────────

    private int currentFollowerCount = 0;
    private boolean isOrganizerProfile = false;

    /** Kiểm tra user đã follow star/organizer này chưa (gọi sau khi load profile) */
    private void checkFollowStatus() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || starId.isEmpty()) return;

        String docId = user.getUid() + "_" + starId;
        FirebaseFirestore.getInstance()
                .collection(com.example.vibetix.Firebase.FirebaseCollections.USER_STAR_FOLLOWS)
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    isFollowing = doc.exists();
                    updateFollowUI();
                });
    }

    /** Toggle follow/unfollow + cập nhật Firebase */
    private void toggleFollow() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_login_to_follow), Toast.LENGTH_SHORT).show();
            return;
        }

        isFollowing = !isFollowing;
        updateFollowUI();

        String docId = user.getUid() + "_" + starId;
        com.google.firebase.firestore.DocumentReference followDoc = FirebaseFirestore.getInstance()
                .collection(com.example.vibetix.Firebase.FirebaseCollections.USER_STAR_FOLLOWS)
                .document(docId);

        if (isFollowing) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("user_id", user.getUid());
            data.put("star_id", starId);
            data.put("followed_at", com.google.firebase.Timestamp.now());
            followDoc.set(data);
            currentFollowerCount++;

            // Chỉ increment follower_count trên stars collection (không phải organizer)
            if (!isOrganizerProfile) {
                FirebaseFirestore.getInstance().collection("stars").document(starId)
                        .update("follower_count", com.google.firebase.firestore.FieldValue.increment(1));
            }

            Toast.makeText(requireContext(), getString(R.string.str_toast_followed), Toast.LENGTH_SHORT).show();
        } else {
            followDoc.delete();
            currentFollowerCount = Math.max(0, currentFollowerCount - 1);

            if (!isOrganizerProfile) {
                FirebaseFirestore.getInstance().collection("stars").document(starId)
                        .update("follower_count", com.google.firebase.firestore.FieldValue.increment(-1));
            }
        }

        // Cập nhật UI số follow
        if (txtStarFollowers != null) {
            txtStarFollowers.setText(getString(R.string.str_follower_count_label, formatter.format(currentFollowerCount)));
        }
    }

    /** Cập nhật UI nút follow */
    private void updateFollowUI() {
        if (btnFollowStar == null) return;
        if (isFollowing) {
            btnFollowStar.setText(getString(R.string.str_btn_following));
            btnFollowStar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF808E92));
        } else {
            btnFollowStar.setText(getString(R.string.str_btn_follow));
            btnFollowStar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF22C55E));
        }
    }
}
