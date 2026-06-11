package com.example.vibetix.Fragments.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Organizer.CreateEditEventActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Fragments.Organizer.MyEventsListFragment;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * OrganizerHubFragment — Tab "Tổ chức" trong UserMainActivity.
 *
 * State A (chưa có event): Màn hình onboarding — khuyến khích tạo event đầu tiên.
 * State B (đã có event/role): Hiển thị MyEventsListFragment để chọn sự kiện.
 *
 * Tránh flash khi onResume bằng cờ isStateLoaded — chỉ check lại khi Fragment bị recreate.
 */
public class OrganizerHubFragment extends Fragment {

    // State A views
    private View layoutStateA;
    private android.widget.ProgressBar pbLoadingHub;

    // State B views
    private View layoutStateB;

    private FirebaseFirestore db;
    private String currentUserId;

    /** Cờ tránh query lại Firestore mỗi lần onResume — reset khi Fragment bị destroy. */
    private boolean isStateLoaded = false;
    private boolean cachedHasAccess = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        SessionManager sessionManager = new SessionManager(requireContext());
        if (sessionManager.getUserDetails() != null) {
            currentUserId = sessionManager.getUserDetails().getUserId();
        } else {
            com.google.firebase.auth.FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            currentUserId = fbUser != null ? fbUser.getUid() : null;
        }

        bindViews(view);
        checkOrganizerStatus(false);
    }

    private void bindViews(View v) {
        layoutStateA = v.findViewById(R.id.layoutBecomeOrganizer);
        layoutStateB = v.findViewById(R.id.layoutOrganizerDashboard);
        pbLoadingHub = v.findViewById(R.id.pbLoadingHub);

        // State A button
        View btnFirstEvent = v.findViewById(R.id.btnCreateFirstEvent);
        if (btnFirstEvent != null) {
            btnFirstEvent.setOnClickListener(x -> openCreateEvent());
        }
    }

    /**
     * @param forceRefresh nếu true → bỏ qua cache và query lại Firestore.
     */
    private void checkOrganizerStatus(boolean forceRefresh) {
        if (currentUserId == null) {
            showState(false);
            return;
        }

        // Dùng cache nếu đã load và không force
        if (isStateLoaded && !forceRefresh) {
            showState(cachedHasAccess);
            return;
        }

        // Hiện loading
        if (pbLoadingHub != null) pbLoadingHub.setVisibility(View.VISIBLE);
        if (layoutStateA != null) layoutStateA.setVisibility(View.GONE);
        if (layoutStateB != null) layoutStateB.setVisibility(View.GONE);

        db.collection(FirebaseCollections.EVENT_STAFF)
                .whereEqualTo("user_id", currentUserId)
                .limit(1) // Chỉ cần biết có hay không — lấy 1 record là đủ
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    cachedHasAccess = (snapshot != null && !snapshot.isEmpty());
                    isStateLoaded = true;
                    showState(cachedHasAccess);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Fallback: show onboarding
                    isStateLoaded = true;
                    cachedHasAccess = false;
                    showState(false);
                });
    }

    private void showState(boolean hasAccess) {
        if (!isAdded()) return;

        if (pbLoadingHub != null) pbLoadingHub.setVisibility(View.GONE);

        if (hasAccess) {
            if (layoutStateA != null) layoutStateA.setVisibility(View.GONE);
            if (layoutStateB != null) layoutStateB.setVisibility(View.VISIBLE);

            // Load MyEventsListFragment chỉ khi chưa load (tránh recreate)
            if (getChildFragmentManager().findFragmentById(R.id.frameOrganizerHubContent) == null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.frameOrganizerHubContent, new MyEventsListFragment())
                        .commitAllowingStateLoss();
            }
        } else {
            if (layoutStateA != null) layoutStateA.setVisibility(View.VISIBLE);
            if (layoutStateB != null) layoutStateB.setVisibility(View.GONE);
        }
    }

    private void openCreateEvent() {
        startActivity(new Intent(getActivity(), CreateEditEventActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chỉ force refresh nếu chưa có cache — tránh flash
        if (!isStateLoaded) {
            checkOrganizerStatus(false);
        }
        // Nếu đã load State A và user vừa tạo sự kiện thành công → refresh
        else if (!cachedHasAccess) {
            checkOrganizerStatus(true);
        }
        // State B đang hiển thị — không cần query lại
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isStateLoaded = false; // Reset để load lại khi Fragment được recreate
    }
}
