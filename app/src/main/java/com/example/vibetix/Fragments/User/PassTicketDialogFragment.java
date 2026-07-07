package com.example.vibetix.Fragments.User;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketTransferRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PassTicketDialogFragment extends DialogFragment {

    private Ticket ticket;
    private OnTicketPassedListener listener;

    private final TicketTransferRepository ticketTransferRepository = new TicketTransferRepository();

    // Views
    private TextView txtPassDialogEventTitle;
    private TextView txtPassDialogOriginalPrice;
    private EditText etPassMessage;
    private Button btnPassCancel;
    private Button btnPassConfirm;
    private LinearLayout layoutTypeSelector;
    private LinearLayout containerTypeRows;

    // B2: typeName → available userTicketIds
    private final LinkedHashMap<String, List<String>> typeToIds = new LinkedHashMap<>();
    // B2: typeName → selected qty
    private final LinkedHashMap<String, Integer> selectedQty = new LinkedHashMap<>();
    // B2: typeName → price EditText (so we can read price per type on confirm)
    private final LinkedHashMap<String, EditText> priceInputs = new LinkedHashMap<>();

    public interface OnTicketPassedListener {
        void onTicketPassed();
    }

    public PassTicketDialogFragment() {}

    public static PassTicketDialogFragment newInstance(Ticket ticket, OnTicketPassedListener listener) {
        PassTicketDialogFragment f = new PassTicketDialogFragment();
        f.ticket   = ticket;
        f.listener = listener;
        return f;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_pass_ticket, container, false);
        bindViews(view);
        buildTypeMap();
        populateViews();
        setupClickListeners();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // ── Bind ────────────────────────────────────────────────────────────────────

    private void bindViews(View view) {
        txtPassDialogEventTitle    = view.findViewById(R.id.txtPassDialogEventTitle);
        txtPassDialogOriginalPrice = view.findViewById(R.id.txtPassDialogOriginalPrice);
        etPassMessage              = view.findViewById(R.id.etPassMessage);
        btnPassCancel              = view.findViewById(R.id.btnPassCancel);
        btnPassConfirm             = view.findViewById(R.id.btnPassConfirm);
        layoutTypeSelector         = view.findViewById(R.id.layoutTypeSelector);
        containerTypeRows          = view.findViewById(R.id.containerTypeRows);
    }

    // ── Build type map ───────────────────────────────────────────────────────────

    private void buildTypeMap() {
        typeToIds.clear();
        selectedQty.clear();
        priceInputs.clear();
        if (ticket == null) return;
        List<String> ids   = ticket.getIndividualTicketIds();
        List<String> names = ticket.getIndividualTicketTypeNames();
        if (ids == null || ids.isEmpty()) return;
        for (int i = 0; i < ids.size(); i++) {
            String id   = ids.get(i);
            String name = (names != null && i < names.size()) ? names.get(i) : "Vé";
            if (name == null || name.isEmpty()) name = "Vé";
            typeToIds.computeIfAbsent(name, k -> new ArrayList<>()).add(id);
        }
        for (String name : typeToIds.keySet()) selectedQty.put(name, 0);
    }

    // ── Populate views ───────────────────────────────────────────────────────────

    private final java.text.DecimalFormat priceFormatter = new java.text.DecimalFormat("#,###");

    private void populateViews() {
        if (ticket == null) return;
        txtPassDialogEventTitle.setText(ticket.getEventTitle());
        long original = ticket.getPurchasePrice();
        if (original > 0) {
            txtPassDialogOriginalPrice.setText(getString(R.string.str_original_price_label, priceFormatter.format(original)));
            txtPassDialogOriginalPrice.setVisibility(View.VISIBLE);
        } else {
            txtPassDialogOriginalPrice.setVisibility(View.GONE);
        }

        // Always show type rows (even 1 type needs a price)
        layoutTypeSelector.setVisibility(View.VISIBLE);
        // Update label based on count
        TextView lbl = (TextView) layoutTypeSelector.getChildAt(0);
        if (lbl != null) {
            lbl.setText(typeToIds.size() > 1
                    ? "Chọn loại vé & đặt giá bán *"
                    : "Đặt giá bán *");
        }
        buildTypeRows();

        // For single ticket with no selection needed, auto-set qty=1
        if (typeToIds.size() == 1) {
            String onlyType = typeToIds.keySet().iterator().next();
            if (typeToIds.get(onlyType).size() == 1) {
                selectedQty.put(onlyType, 1);
                // Refresh the qty display (done in buildTypeRows via initial count)
            }
        }
    }

    // ── Build type rows ──────────────────────────────────────────────────────────

    private void buildTypeRows() {
        containerTypeRows.removeAllViews();
        priceInputs.clear();

        for (Map.Entry<String, List<String>> entry : typeToIds.entrySet()) {
            String typeName = entry.getKey();
            int    maxQty   = entry.getValue().size();

            // Card wrapper
            LinearLayout card = new LinearLayout(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardLp);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(dp(12));
            cardBg.setColor(0xFFF9FAFB);
            cardBg.setStroke(dp(1), 0xFFE5E7EB);
            card.setBackground(cardBg);

            // ── Row 1: type name + qty control ──────────────────────────────
            LinearLayout row1 = new LinearLayout(requireContext());
            row1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            // Type name
            TextView tvName = new TextView(requireContext());
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameLp);
            tvName.setText(maxQty > 1 ? getString(R.string.str_ticket_type_max_qty, typeName, maxQty) : typeName);
            tvName.setTextSize(14f);
            tvName.setTextColor(0xFF111827);
            tvName.setTypeface(null, Typeface.BOLD);

            // Qty control
            int initQty = (typeToIds.size() == 1 && maxQty == 1) ? 1 : 0;
            selectedQty.put(typeName, initQty);

            LinearLayout qtyCtrl = new LinearLayout(requireContext());
            qtyCtrl.setLayoutParams(new LinearLayout.LayoutParams(dp(108), dp(36)));
            qtyCtrl.setOrientation(LinearLayout.HORIZONTAL);
            qtyCtrl.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable qtyBg = new GradientDrawable();
            qtyBg.setCornerRadius(dp(8));
            qtyBg.setColor(0xFFFFFFFF);
            qtyBg.setStroke(dp(1), 0xFFD1D5DB);
            qtyCtrl.setBackground(qtyBg);

            TextView btnMinus = makeQtyBtn("−");
            TextView tvCount  = makeQtyCount(String.valueOf(initQty));
            TextView btnPlus  = makeQtyBtn("+");

            qtyCtrl.addView(btnMinus);
            qtyCtrl.addView(tvCount);
            qtyCtrl.addView(btnPlus);

            updateQtyBtnColors(btnMinus, btnPlus, initQty, maxQty);

            btnMinus.setOnClickListener(v -> {
                int cur = selectedQty.getOrDefault(typeName, 0);
                if (cur > 0) {
                    cur--;
                    selectedQty.put(typeName, cur);
                    tvCount.setText(String.valueOf(cur));
                    updateQtyBtnColors(btnMinus, btnPlus, cur, maxQty);
                }
            });
            btnPlus.setOnClickListener(v -> {
                int cur = selectedQty.getOrDefault(typeName, 0);
                if (cur < maxQty) {
                    cur++;
                    selectedQty.put(typeName, cur);
                    tvCount.setText(String.valueOf(cur));
                    updateQtyBtnColors(btnMinus, btnPlus, cur, maxQty);
                }
            });

            row1.addView(tvName);
            row1.addView(qtyCtrl);
            card.addView(row1);

            // ── Row 2: price input ───────────────────────────────────────────
            LinearLayout.LayoutParams priceLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            priceLp.setMargins(0, dp(10), 0, 0);

            RelativeLayout priceRow = new RelativeLayout(requireContext());
            priceRow.setLayoutParams(priceLp);
            priceRow.setPadding(dp(12), 0, dp(12), 0);
            GradientDrawable priceBg = new GradientDrawable();
            priceBg.setCornerRadius(dp(8));
            priceBg.setColor(0xFFFFFFFF);
            priceBg.setStroke(dp(1), 0xFFD1D5DB);
            priceRow.setBackground(priceBg);

            EditText etPrice = new EditText(requireContext());
            RelativeLayout.LayoutParams etLp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            etLp.addRule(RelativeLayout.CENTER_VERTICAL);
            etLp.setMarginEnd(dp(40));
            etPrice.setLayoutParams(etLp);
            etPrice.setHint(getString(R.string.str_hint_resale_price));
            etPrice.setInputType(InputType.TYPE_CLASS_NUMBER);
            etPrice.setBackground(null);
            etPrice.setTextSize(14f);
            etPrice.setTextColor(0xFF111827);
            etPrice.setHintTextColor(0xFF9CA3AF);
            etPrice.setPadding(0, 0, 0, 0);
            etPrice.setSingleLine(true);

            TextView tvVnd = new TextView(requireContext());
            RelativeLayout.LayoutParams vndLp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            vndLp.addRule(RelativeLayout.ALIGN_PARENT_END);
            vndLp.addRule(RelativeLayout.CENTER_VERTICAL);
            tvVnd.setLayoutParams(vndLp);
            tvVnd.setText(getString(R.string.str_vnd_label));
            tvVnd.setTextSize(12f);
            tvVnd.setTextColor(0xFF9CA3AF);

            priceRow.addView(etPrice);
            priceRow.addView(tvVnd);
            card.addView(priceRow);

            priceInputs.put(typeName, etPrice);
            containerTypeRows.addView(card);
        }

        // Tip hint at bottom
        TextView tvTip = new TextView(requireContext());
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tipLp.setMargins(0, dp(4), 0, 0);
        tvTip.setLayoutParams(tipLp);
        tvTip.setText(getString(R.string.str_resale_price_tip));
        tvTip.setTextSize(12f);
        tvTip.setTextColor(0xFF2563EB);
        containerTypeRows.addView(tvTip);
    }

    // ── Click listeners ──────────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnPassCancel.setOnClickListener(v -> dismiss());

        btnPassConfirm.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_please_relogin), Toast.LENGTH_SHORT).show();
                return;
            }

            // Collect (ticketId → price) pairs to sell
            List<long[]> sales = new ArrayList<>();     // [0]=ticketIdIndex placeholder
            List<String> idsToSell   = new ArrayList<>();
            List<Long>   pricesToSell = new ArrayList<>();

            for (Map.Entry<String, Integer> e : selectedQty.entrySet()) {
                String typeName = e.getKey();
                int    qty      = e.getValue();
                if (qty <= 0) continue;

                // Validate price for this type
                EditText et = priceInputs.get(typeName);
                String priceStr = et != null ? et.getText().toString().trim().replaceAll("[^0-9]", "") : "";
                if (priceStr.isEmpty()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.str_toast_enter_price_for, typeName), Toast.LENGTH_SHORT).show();
                    return;
                }
                long price;
                try { price = Long.parseLong(priceStr); }
                catch (NumberFormatException ex) {
                    Toast.makeText(requireContext(), getString(R.string.str_toast_invalid_price_for, typeName), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (price <= 0) {
                    Toast.makeText(requireContext(), getString(R.string.str_toast_price_must_be_positive, typeName), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> available = typeToIds.get(typeName);
                if (available == null) continue;
                int count = Math.min(qty, available.size());
                for (int i = 0; i < count; i++) {
                    idsToSell.add(available.get(i));
                    pricesToSell.add(price);
                }
            }

            if (idsToSell.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_select_at_least_one), Toast.LENGTH_SHORT).show();
                return;
            }

            btnPassConfirm.setEnabled(false);
            Toast.makeText(requireContext(), getString(R.string.str_btn_processing), Toast.LENGTH_SHORT).show();

            String msg = etPassMessage != null ? etPassMessage.getText().toString().trim() : "";
            if (msg.isEmpty()) msg = "Đăng bán lại vé";
            final String finalMsg = msg;

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 30);
            Timestamp expiresAt = new Timestamp(cal.getTime());

            createTransfersBatch(currentUser.getUid(), idsToSell, pricesToSell,
                    finalMsg, expiresAt, 0);
        });
    }

    // ── Create transfers (1 per ticket with its own price) ───────────────────────

    private void createTransfersBatch(String senderId, List<String> ticketIds, List<Long> prices,
                                       String message, Timestamp expiresAt, int index) {
        if (index >= ticketIds.size()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_resell_success), Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onTicketPassed();
                dismiss();
            }
            return;
        }
        TicketTransfer transfer = new TicketTransfer();
        transfer.setTransferId(UUID.randomUUID().toString());
        transfer.setSenderId(senderId);
        transfer.setReceiverId("");
        transfer.setReceiverEmail(null);
        transfer.setUserTicketId(ticketIds.get(index));
        transfer.setStatus("pending");
        transfer.setPrice(prices.get(index));          // giá riêng từng vé
        transfer.setOriginalPrice(ticket.getPurchasePrice());
        transfer.setMessage(message);
        transfer.setEventId(ticket.getEventId());
        transfer.setEventTitle(ticket.getEventTitle());
        transfer.setEventDate(ticket.getEventDate());
        transfer.setEventLocation(ticket.getEventLocation());
        transfer.setEventImageUrl(ticket.getEventImageUrl());
        transfer.setTransferToken(UUID.randomUUID().toString());
        transfer.setCreatedAt(Timestamp.now());
        transfer.setCompletedAt(null);
        transfer.setExpiresAt(expiresAt);

        ticketTransferRepository.createTransfer(transfer, new TicketTransferRepository.OnTransferActionListener() {
            @Override public void onSuccess() {
                createTransfersBatch(senderId, ticketIds, prices, message, expiresAt, index + 1);
            }
            @Override public void onFailure(Exception e) {
                if (isAdded()) {
                    btnPassConfirm.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private TextView makeQtyBtn(String text) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.MATCH_PARENT));
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(18f);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private TextView makeQtyCount(String text) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.MATCH_PARENT));
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(15f);
        tv.setTextColor(0xFF111827);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private void updateQtyBtnColors(TextView minus, TextView plus, int cur, int max) {
        minus.setTextColor(cur > 0   ? 0xFF2563EB : 0xFFD1D5DB);
        plus.setTextColor(cur < max  ? 0xFF2563EB : 0xFFD1D5DB);
    }

    private int dp(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
