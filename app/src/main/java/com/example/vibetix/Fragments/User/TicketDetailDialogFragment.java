package com.example.vibetix.Fragments.User;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

import java.text.DecimalFormat;

/**
 * Dialog hiển thị chi tiết một vé (order detail) khi người dùng bấm vào vé ở "Vé của tôi".
 */
public class TicketDetailDialogFragment extends DialogFragment {

    private Ticket ticket;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public TicketDetailDialogFragment() {}

    public static TicketDetailDialogFragment newInstance(Ticket ticket) {
        TicketDetailDialogFragment f = new TicketDetailDialogFragment();
        f.ticket = ticket;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ticket_detail, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (ticket == null) { dismiss(); return; }

        ImageView imv = view.findViewById(R.id.imvDetailImage);
        TextView txtStatus = view.findViewById(R.id.txtDetailStatus);
        TextView txtTitle = view.findViewById(R.id.txtDetailTitle);
        TextView txtDate = view.findViewById(R.id.txtDetailDate);
        TextView txtLocation = view.findViewById(R.id.txtDetailLocation);
        TextView txtPrice = view.findViewById(R.id.txtDetailPrice);
        TextView txtAttendee = view.findViewById(R.id.txtDetailAttendee);
        TextView txtOrderId = view.findViewById(R.id.txtDetailOrderId);
        Button btnClose = view.findViewById(R.id.btnDetailClose);
        Button btnQr = view.findViewById(R.id.btnDetailQr);

        if (ticket.getEventImageUrl() != null && !ticket.getEventImageUrl().isEmpty()) {
            Glide.with(this).load(ticket.getEventImageUrl())
                    .placeholder(R.drawable.event_live_non_song)
                    .error(R.drawable.event_live_non_song)
                    .into(imv);
        } else {
            imv.setImageResource(R.drawable.event_live_non_song);
        }

        txtStatus.setText(statusLabel(ticket.getStatus()));
        txtTitle.setText(ticket.getEventTitle());
        txtDate.setText(ticket.getEventDate());
        txtLocation.setText(ticket.getEventLocation());

        // Chi tiết từng loại vé — parse itemBreakdown "qty:name:price|..."
        android.widget.LinearLayout listContainer = view.findViewById(R.id.layoutTicketItemsList);
        populateTicketItems(listContainer);

        // Tổng đơn hàng
        long totalPrice = ticket.getResalePrice() > 0 ? ticket.getResalePrice() : ticket.getPurchasePrice();
        txtPrice.setText(formatter.format(totalPrice) + " đ");

        // Kiểm tra discount từ order_discounts
        loadDiscount(view, totalPrice);

        txtAttendee.setText(ticket.getAttendeeName() != null && !ticket.getAttendeeName().isEmpty()
                ? ticket.getAttendeeName() : "—");
        txtOrderId.setText(ticket.getOrderId() != null && !ticket.getOrderId().isEmpty()
                ? ticket.getOrderId() : ticket.getId());

        btnClose.setOnClickListener(v -> dismiss());

        // Chỉ vé còn hiệu lực mới hiện được mã QR
        boolean canShowQr = "ACTIVE".equalsIgnoreCase(ticket.getStatus())
                || "valid".equalsIgnoreCase(ticket.getStatus());
        if (canShowQr) {
            btnQr.setVisibility(View.VISIBLE);
            btnQr.setOnClickListener(v -> {
                TicketQrDialogFragment qr = TicketQrDialogFragment.newInstance(ticket);
                qr.show(getParentFragmentManager(), "qr_dialog");
                dismiss();
            });
        } else {
            btnQr.setVisibility(View.GONE);
        }
    }

    /**
     * Parse itemBreakdown "qty:name:unitPrice|..." → rows với giá từng loại.
     * Fallback: parse ticketTypeName "1x Vé VIP, 1x Vé Thường" nếu không có breakdown.
     */
    private void populateTicketItems(android.widget.LinearLayout container) {
        if (container == null || ticket == null) return;
        float dp = getResources().getDisplayMetrics().density;

        String breakdown = ticket.getItemBreakdown();
        if (breakdown != null && !breakdown.isEmpty()) {
            // Parse "qty:name:price|..."
            for (String entry : breakdown.split("\\|")) {
                String[] parts = entry.split(":", 3);
                String qty  = parts.length > 0 ? parts[0] : "1";
                String name = parts.length > 1 ? parts[1] : "Vé";
                String priceStr = parts.length > 2 ? parts[2] : "0";
                long unitPrice = 0;
                try { unitPrice = Long.parseLong(priceStr); } catch (NumberFormatException ignored) {}

                addItemRow(container, dp, qty + "x " + name, unitPrice);
            }
        } else {
            // Fallback: parse ticketTypeName
            String typeName = ticket.getTicketTypeName();
            if (typeName == null || typeName.isEmpty()) return;
            for (String part : typeName.split(",\\s*")) {
                addItemRow(container, dp, part.trim(), 0);
            }
        }
    }

    private void addItemRow(android.widget.LinearLayout container, float dp, String label, long unitPrice) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (6 * dp);
        row.setLayoutParams(lp);

        TextView txtLabel = new TextView(requireContext());
        txtLabel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        txtLabel.setText("• " + label);
        txtLabel.setTextSize(14f);
        txtLabel.setTextColor(getResources().getColor(com.example.vibetix.R.color.clr_text_black));
        row.addView(txtLabel);

        if (unitPrice > 0) {
            TextView txtPrice = new TextView(requireContext());
            txtPrice.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            txtPrice.setText(formatter.format(unitPrice) + " đ");
            txtPrice.setTextSize(14f);
            txtPrice.setTextColor(getResources().getColor(com.example.vibetix.R.color.clr_text_secondary));
            row.addView(txtPrice);
        }

        container.addView(row);
    }

    /** Truy vấn order_discounts để hiện dòng giảm giá nếu có */
    private void loadDiscount(View view, long totalBeforeDiscount) {
        String orderId = ticket.getOrderId();
        if (orderId == null || orderId.isEmpty()) return;

        TextView txtPrice = view.findViewById(R.id.txtDetailPrice);
        android.widget.LinearLayout listContainer = view.findViewById(R.id.layoutTicketItemsList);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("order_discounts")
                .whereEqualTo("order_id", orderId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded() || snap.isEmpty()) return;
                    Long appliedAmount = snap.getDocuments().get(0).getLong("applied_amount");
                    if (appliedAmount == null || appliedAmount <= 0) return;

                    float dp = getResources().getDisplayMetrics().density;

                    // Thêm dòng giảm giá vào listContainer
                    android.widget.LinearLayout discountRow = new android.widget.LinearLayout(requireContext());
                    discountRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    discountRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = (int) (4 * dp);
                    discountRow.setLayoutParams(lp);

                    TextView lblDisc = new TextView(requireContext());
                    lblDisc.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    lblDisc.setText(getString(R.string.str_discount_code_label));
                    lblDisc.setTextSize(14f);
                    lblDisc.setTextColor(getResources().getColor(com.example.vibetix.R.color.clr_success));
                    discountRow.addView(lblDisc);

                    TextView valDisc = new TextView(requireContext());
                    valDisc.setText("-" + formatter.format(appliedAmount) + " đ");
                    valDisc.setTextSize(14f);
                    valDisc.setTextColor(getResources().getColor(com.example.vibetix.R.color.clr_success));
                    discountRow.addView(valDisc);

                    if (listContainer != null) listContainer.addView(discountRow);

                    // Cập nhật giá tổng = trước giảm - giảm
                    long finalTotal = totalBeforeDiscount - appliedAmount;
                    if (finalTotal < 0) finalTotal = 0;
                    if (txtPrice != null) txtPrice.setText(formatter.format(finalTotal) + " đ");
                });
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "ACTIVE":
            case "VALID":            return "Còn hiệu lực";
            case "RESELLING":        return "Đang bán lại";
            case "RESOLD":           return "Đã bán";
            case "RESALE_CANCELLED": return "Đã huỷ bán";
            case "USED":             return "Đã sử dụng";
            case "EXPIRED":          return "Hết hạn";
            default:                 return status;
        }
    }
}
