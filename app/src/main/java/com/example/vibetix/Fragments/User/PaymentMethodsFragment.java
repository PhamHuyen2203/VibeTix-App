package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.PaymentMethod;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethodsFragment extends Fragment {

    private ImageView btnBack;
    private RecyclerView rvPaymentMethods;
    private LinearLayout layoutEmpty;
    private TextView btnAddNewPayment;  // TextView để tránh Material override background

    // Bottom sheet views
    private LinearLayout bottomSheetAddPayment;
    private View scrimOverlay;
    private ImageView btnCloseSheet;
    private LinearLayout optionMomo, optionZalopay, optionVisa, optionAtm;
    private EditText edtAccountNumber;
    private Button btnConfirmAddPayment;

    private String selectedType = null;
    private PaymentMethodAdapter adapter;
    private List<PaymentMethod> methods = new ArrayList<>();

    private SharedPreferences profilePrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_methods, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);

        bindViews(view);
        applyInsets(view);
        loadMethods();
        setupRecyclerView();
        setupListeners();
    }

    private void bindViews(View v) {
        btnBack          = v.findViewById(R.id.btnBack);
        rvPaymentMethods = v.findViewById(R.id.rvPaymentMethods);
        layoutEmpty      = v.findViewById(R.id.layoutEmpty);
        btnAddNewPayment = v.findViewById(R.id.btnAddNewPayment);

        bottomSheetAddPayment = v.findViewById(R.id.bottomSheetAddPayment);
        scrimOverlay          = v.findViewById(R.id.scrimOverlay);
        btnCloseSheet         = v.findViewById(R.id.btnCloseSheet);
        optionMomo            = v.findViewById(R.id.optionMomo);
        optionZalopay         = v.findViewById(R.id.optionZalopay);
        optionVisa            = v.findViewById(R.id.optionVisa);
        optionAtm             = v.findViewById(R.id.optionAtm);
        edtAccountNumber      = v.findViewById(R.id.edtAccountNumber);
        btnConfirmAddPayment  = v.findViewById(R.id.btnConfirmAddPayment);
    }

    private void loadMethods() {
        methods.clear();
        String raw = profilePrefs.getString(Constants.KEY_PAYMENT_METHODS, null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    PaymentMethod pm = PaymentMethod.fromJson(arr.getJSONObject(i));
                    if (pm != null) methods.add(pm);
                }
            } catch (Exception ignored) {}
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (methods.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvPaymentMethods.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvPaymentMethods.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        adapter = new PaymentMethodAdapter(methods, new PaymentMethodAdapter.Listener() {
            @Override
            public void onSetDefault(int position) {
                for (int i = 0; i < methods.size(); i++) {
                    methods.get(i).setDefault(i == position);
                }
                saveMethods();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDelete(int position) {
                if (!isAdded()) return;
                String name = methods.get(position).getDisplayName();
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa phương thức thanh toán")
                        .setMessage("Bạn có chắc muốn xóa \"" + name + "\" không?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            methods.remove(position);
                            saveMethods();
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, methods.size());
                            updateEmptyState();
                            Toast.makeText(requireContext(), "Đã xóa " + name, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvPaymentMethods.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPaymentMethods.setAdapter(adapter);
    }

    // ── Status-bar inset ──────────────────────────────────────────────────────
    private void applyInsets(View root) {
        View header = root.findViewById(R.id.layoutPaymentHeader);
        if (header != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, top, 0, 0);
                return insets;
            });
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnAddNewPayment.setOnClickListener(v -> showBottomSheet());

        scrimOverlay.setOnClickListener(v -> hideBottomSheet());
        btnCloseSheet.setOnClickListener(v -> hideBottomSheet());

        optionMomo.setOnClickListener(v    -> selectType(Constants.PAYMENT_MOMO,    optionMomo));
        optionZalopay.setOnClickListener(v -> selectType(Constants.PAYMENT_ZALOPAY, optionZalopay));
        optionVisa.setOnClickListener(v    -> selectType(Constants.PAYMENT_VISA,    optionVisa));
        optionAtm.setOnClickListener(v     -> selectType(Constants.PAYMENT_ATM,     optionAtm));

        btnConfirmAddPayment.setOnClickListener(v -> confirmAddPayment());
    }

    private void selectType(String type, LinearLayout option) {
        selectedType = type;
        // Reset all
        resetOptionBackground(optionMomo);
        resetOptionBackground(optionZalopay);
        resetOptionBackground(optionVisa);
        resetOptionBackground(optionAtm);
        // Highlight selected
        option.setBackgroundResource(R.drawable.bg_payment_item_default);
    }

    private void resetOptionBackground(LinearLayout option) {
        option.setBackgroundResource(R.drawable.bg_payment_item);
    }

    private void showBottomSheet() {
        selectedType = null;
        edtAccountNumber.setText("");
        resetOptionBackground(optionMomo);
        resetOptionBackground(optionZalopay);
        resetOptionBackground(optionVisa);
        resetOptionBackground(optionAtm);
        scrimOverlay.setVisibility(View.VISIBLE);
        bottomSheetAddPayment.setVisibility(View.VISIBLE);
    }

    private void hideBottomSheet() {
        scrimOverlay.setVisibility(View.GONE);
        bottomSheetAddPayment.setVisibility(View.GONE);
    }

    private void confirmAddPayment() {
        if (selectedType == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn loại phương thức", Toast.LENGTH_SHORT).show();
            return;
        }

        String account = edtAccountNumber.getText().toString().trim();
        String displayName = getDisplayName(selectedType);

        PaymentMethod pm = new PaymentMethod(selectedType, displayName,
                account.isEmpty() ? "" : account);

        // First method becomes default
        if (methods.isEmpty()) pm.setDefault(true);

        methods.add(pm);
        saveMethods();
        adapter.notifyItemInserted(methods.size() - 1);
        updateEmptyState();
        hideBottomSheet();

        Toast.makeText(requireContext(), "Đã thêm " + displayName, Toast.LENGTH_SHORT).show();
    }

    private String getDisplayName(String type) {
        switch (type) {
            case Constants.PAYMENT_MOMO:    return "MoMo";
            case Constants.PAYMENT_ZALOPAY: return "ZaloPay";
            case Constants.PAYMENT_VNPAY:   return "VNPay";
            case Constants.PAYMENT_VISA:    return "Thẻ Visa";
            case Constants.PAYMENT_ATM:     return "Thẻ ATM";
            default: return type;
        }
    }

    private void saveMethods() {
        // Cache local
        JSONArray arr = new JSONArray();
        for (PaymentMethod pm : methods) arr.put(pm.toJson());
        profilePrefs.edit().putString(Constants.KEY_PAYMENT_METHODS, arr.toString()).apply();

        // Sync lên Firestore users/{uid}.payment_methods
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        java.util.List<Map<String, Object>> pmList = new ArrayList<>();
        for (PaymentMethod pm : methods) {
            Map<String, Object> m = new HashMap<>();
            m.put("id",           pm.getId());
            m.put("type",         pm.getType());
            m.put("display_name", pm.getDisplayName());
            m.put("account",      pm.getAccount());
            m.put("is_default",   pm.isDefault());
            pmList.add(m);
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .update("payment_methods", pmList);
    }

    // ---- Inner Adapter ----

    public static class PaymentMethodAdapter
            extends RecyclerView.Adapter<PaymentMethodAdapter.VH> {

        public interface Listener {
            void onSetDefault(int position);
            void onDelete(int position);
        }

        private final List<PaymentMethod> items;
        private final Listener listener;

        public PaymentMethodAdapter(List<PaymentMethod> items, Listener listener) {
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_payment_method, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            PaymentMethod pm = items.get(position);

            h.txtPaymentIcon.setText(getInitial(pm.getType()));
            h.txtPaymentIcon.setBackgroundColor(getColor(pm.getType()));
            h.txtPaymentName.setText(pm.getDisplayName());
            h.txtPaymentAccount.setText(pm.getAccount());

            h.txtDefaultBadge.setVisibility(pm.isDefault() ? View.VISIBLE : View.GONE);
            h.rbSetDefault.setChecked(pm.isDefault());

            // Update background
            h.itemView.setBackgroundResource(pm.isDefault()
                    ? R.drawable.bg_payment_item_default
                    : R.drawable.bg_payment_item);

            h.rbSetDefault.setOnClickListener(v -> listener.onSetDefault(h.getAdapterPosition()));
            h.btnDeletePayment.setOnClickListener(v -> listener.onDelete(h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String getInitial(String type) {
            if (type == null) return "?";
            switch (type) {
                case Constants.PAYMENT_MOMO:    return "M";
                case Constants.PAYMENT_ZALOPAY: return "Z";
                case Constants.PAYMENT_VNPAY:   return "V";
                case Constants.PAYMENT_VISA:    return "Vi";
                case Constants.PAYMENT_ATM:     return "A";
                default: return "?";
            }
        }

        private int getColor(String type) {
            if (type == null) return 0xFF808E92;
            switch (type) {
                case Constants.PAYMENT_MOMO:    return 0xFFA50064;
                case Constants.PAYMENT_ZALOPAY: return 0xFF0068FF;
                case Constants.PAYMENT_VNPAY:   return 0xFF0050A0;
                case Constants.PAYMENT_VISA:    return 0xFF1A1F71;
                case Constants.PAYMENT_ATM:     return 0xFFE65100;
                default: return 0xFF808E92;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView  txtPaymentIcon, txtPaymentName, txtPaymentAccount, txtDefaultBadge;
            RadioButton rbSetDefault;
            ImageView   btnDeletePayment;

            VH(View v) {
                super(v);
                txtPaymentIcon    = v.findViewById(R.id.txtPaymentIcon);
                txtPaymentName    = v.findViewById(R.id.txtPaymentName);
                txtPaymentAccount = v.findViewById(R.id.txtPaymentAccount);
                txtDefaultBadge   = v.findViewById(R.id.txtDefaultBadge);
                rbSetDefault      = v.findViewById(R.id.rbSetDefault);
                btnDeletePayment  = v.findViewById(R.id.btnDeletePayment);
            }
        }
    }
}
