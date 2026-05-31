package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.OrganizerProfile;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

/**
 * OrganizerRegisterFragment — dùng khi navigate đến màn hình đăng ký BTC riêng.
 * (OrganizerProfileFragment dùng layout này làm dialog view trong AlertDialog.)
 */
public class OrganizerRegisterFragment extends Fragment {

    private EditText edtBrandName, edtDescription, edtWebsite;

    private SharedPreferences profilePrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);

        bindViews(view);
        loadExistingData();
    }

    private void bindViews(View v) {
        edtBrandName   = v.findViewById(R.id.edtBrandName);
        edtDescription = v.findViewById(R.id.edtDescription);
        edtWebsite     = v.findViewById(R.id.edtWebsite);
    }

    private void loadExistingData() {
        String json = profilePrefs.getString("org_profile_json", null);
        if (json == null) return;
        try {
            OrganizerProfile org = OrganizerProfile.fromJson(new org.json.JSONObject(json));
            if (org.getBrandName()   != null) edtBrandName.setText(org.getBrandName());
            if (org.getDescription() != null) edtDescription.setText(org.getDescription());
            if (org.getWebsiteUrl()  != null) edtWebsite.setText(org.getWebsiteUrl());
        } catch (Exception ignored) {}
    }

    /** Gọi từ bên ngoài (nếu dùng như full-screen fragment) để submit dữ liệu */
    public void submitRegistration() {
        String brandName   = edtBrandName   != null ? edtBrandName.getText().toString().trim()   : "";
        String description = edtDescription != null ? edtDescription.getText().toString().trim() : "";
        String website     = edtWebsite     != null ? edtWebsite.getText().toString().trim()     : "";

        if (TextUtils.isEmpty(brandName)) {
            if (edtBrandName != null) edtBrandName.setError("Vui lòng nhập tên ban tổ chức");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            if (edtDescription != null) edtDescription.setError("Vui lòng nhập mô tả");
            return;
        }

        OrganizerProfile org = new OrganizerProfile(brandName, description, website, Constants.ORG_STATUS_PENDING);
        profilePrefs.edit()
                .putString("org_profile_json", org.toJson().toString())
                .apply();

        Toast.makeText(requireContext(),
                "Đã gửi đăng ký. Chờ kết quả trong 1–3 ngày làm việc.",
                Toast.LENGTH_LONG).show();

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
