package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Order;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Repositories.OrderRepository;
import com.example.vibetix.Utils.Constants;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class SimulatedPaymentFragment extends Fragment {

    private String eventId = "b1";
    private String ticketTypeName = "Vé Tiêu chuẩn";
    private int quantity = 1;
    private long ticketPrice = 400000;
    private long totalPrice = 400000;
    private boolean isResale = false;   // mua lại vé
    private String transferId = "";     // transfer cần đánh dấu accepted khi mua lại
    private final com.example.vibetix.Repositories.TicketTransferRepository ticketTransferRepository =
            new com.example.vibetix.Repositories.TicketTransferRepository();

    // Attendee details
    private String name, email, phone, notes;
    private String eventImageUrl = "";

    // Billing variables
    private long discount = 0;
    private String appliedDiscountId = null;
    private long finalTotal = 0;

    // Timer
    private CountDownTimer countDownTimer;
    private static final long COUNTDOWN_DURATION_MS = 15 * 60 * 1000; // 15 mins

    // Repositories
    private final EventRepository eventRepository = new EventRepository();
    private final OrderRepository orderRepository = new OrderRepository();

    // Views
    private ImageView btnPaymentBack;
    private ImageView imvPaymentEventThumb;
    private TextView txtPaymentEventTitle;
    private TextView txtPaymentEventDate;
    private TextView txtPaymentEventLocation;
    private TextView txtPaymentCountdown;
    private TextView txtPaymentReceiverEmail;
    private RadioGroup rgPaymentMethods;
    private TextView txtPaymentSummaryName;
    private TextView txtPaymentSummaryPrice;
    private TextView txtPaymentSummaryFee;
    private TextView txtPaymentSummaryDiscount;
    private TextView txtPaymentSummaryTotal;
    private CheckBox cbPaymentAgree;
    private TextView txtPaymentBottomTotal;
    private Button btnPaymentPay;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static SimulatedPaymentFragment newInstance(String eventId, String ticketTypeName, int quantity,
                                                       long ticketPrice, long totalPrice, String name, String email,
                                                       String phone, String notes, String eventImageUrl) {
        SimulatedPaymentFragment fragment = new SimulatedPaymentFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("ticketTypeName", ticketTypeName);
        args.putInt("quantity", quantity);
        args.putLong("ticketPrice", ticketPrice);
        args.putLong("totalPrice", totalPrice);
        args.putString("name", name);
        args.putString("email", email);
        args.putString("phone", phone);
        args.putString("notes", notes);
        args.putString("eventImageUrl", eventImageUrl != null ? eventImageUrl : "");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "b1");
            ticketTypeName = getArguments().getString("ticketTypeName", "Vé Tiêu chuẩn");
            quantity = getArguments().getInt("quantity", 1);
            ticketPrice = getArguments().getLong("ticketPrice", 400000);
            totalPrice = getArguments().getLong("totalPrice", 400000);
            name          = getArguments().getString("name", "");
            email         = getArguments().getString("email", "");
            phone         = getArguments().getString("phone", "");
            notes         = getArguments().getString("notes", "");
            eventImageUrl = getArguments().getString("eventImageUrl", "");
            isResale      = getArguments().getBoolean("isResale", false);
            transferId    = getArguments().getString("transferId", "");
        }
        finalTotal = totalPrice - discount;
        if (finalTotal < 0) finalTotal = 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simulated_payment, container, false);
        bindViews(view);
        applyInsets(view);
        loadEventDetails();
        setupCountdown();
        setupClickListeners(view);
        updateBillingSummary();
        return view;
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutPaymentHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (top <= 0) {
                    int resId = v.getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) top = v.getResources().getDimensionPixelSize(resId);
                }
                if (top <= 0) top = (int) (28 * v.getResources().getDisplayMetrics().density);
                v.setPadding(0, top, 0, 0);
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }
    }

    private void bindViews(View view) {
        btnPaymentBack = view.findViewById(R.id.btnPaymentBack);
        imvPaymentEventThumb = view.findViewById(R.id.imvPaymentEventThumb);
        txtPaymentEventTitle = view.findViewById(R.id.txtPaymentEventTitle);
        txtPaymentEventDate = view.findViewById(R.id.txtPaymentEventDate);
        txtPaymentEventLocation = view.findViewById(R.id.txtPaymentEventLocation);
        txtPaymentCountdown = view.findViewById(R.id.txtPaymentCountdown);
        txtPaymentReceiverEmail = view.findViewById(R.id.txtPaymentReceiverEmail);
        rgPaymentMethods = view.findViewById(R.id.rgPaymentMethods);
        txtPaymentSummaryName = view.findViewById(R.id.txtPaymentSummaryName);
        txtPaymentSummaryPrice = view.findViewById(R.id.txtPaymentSummaryPrice);
        txtPaymentSummaryFee = view.findViewById(R.id.txtPaymentSummaryFee);
        txtPaymentSummaryDiscount = view.findViewById(R.id.txtPaymentSummaryDiscount);
        txtPaymentSummaryTotal = view.findViewById(R.id.txtPaymentSummaryTotal);
        cbPaymentAgree = view.findViewById(R.id.cbPaymentAgree);
        txtPaymentBottomTotal = view.findViewById(R.id.txtPaymentBottomTotal);
        btnPaymentPay = view.findViewById(R.id.btnPaymentPay);

        // Click "Thông tin nhận vé" → popup thông tin người dùng
        if (txtPaymentReceiverEmail != null) {
            View receiverParent = (View) txtPaymentReceiverEmail.getParent();
            if (receiverParent != null) receiverParent = (View) receiverParent.getParent();
            if (receiverParent != null) receiverParent = (View) receiverParent.getParent();
            View receiverClick = receiverParent != null ? receiverParent : txtPaymentReceiverEmail;
            receiverClick.setOnClickListener(v -> {
                String info = "Họ tên: " + (name != null ? name : "—")
                        + "\nEmail: " + (email != null ? email : "—")
                        + "\nĐiện thoại: " + (phone != null ? phone : "—");
                if (notes != null && !notes.isEmpty()) info += "\nGhi chú: " + notes;
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Thông tin người nhận vé")
                        .setMessage(info)
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }

        // Voucher click
        View voucherCard = view.findViewById(R.id.txtPaymentAppliedVoucher);
        if (voucherCard != null) {
            View voucherParent = (View) voucherCard.getParent();
            if (voucherParent != null) voucherParent = (View) voucherParent.getParent();
            View clickTarget = voucherParent != null ? voucherParent : voucherCard;
            clickTarget.setOnClickListener(v -> showVoucherDialog());
        }
    }

    /** Hiện dialog chọn voucher — filter: active, chưa hết hạn, chưa hết lượt, đúng scope, đúng min_order */
    private void showVoucherDialog() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(com.example.vibetix.Firebase.FirebaseCollections.DISCOUNTS)
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded() || snap.isEmpty()) {
                        Toast.makeText(requireContext(), "Không có mã giảm giá khả dụng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> codes = new java.util.ArrayList<>();
                    java.util.List<String> descriptions = new java.util.ArrayList<>();
                    java.util.List<String> discountIds = new java.util.ArrayList<>();
                    java.util.List<Long> values = new java.util.ArrayList<>();
                    java.util.List<String> types = new java.util.ArrayList<>();
                    java.util.List<Long> maxDiscounts = new java.util.ArrayList<>();

                    long now = System.currentTimeMillis();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        String code = doc.getString("code");
                        String type = doc.getString("type");
                        Long value = doc.getLong("value");
                        Long maxDisc = doc.getLong("max_discount");
                        Long minOrder = doc.getLong("min_order_value");
                        Long usageLimit = doc.getLong("usage_limit");
                        Long usedCount = doc.getLong("used_count");

                        if (code == null || value == null) continue;

                        // Hết hạn?
                        com.google.firebase.Timestamp expiry = doc.getTimestamp("expiry_date");
                        if (expiry != null && expiry.toDate().getTime() < now) continue;

                        // Hết lượt dùng?
                        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) continue;

                        // Min order value?
                        if (minOrder != null && totalPrice < minOrder) continue;

                        // Scope: global cho tất cả, event chỉ cho event cụ thể
                        String scope = doc.getString("scope");
                        String discEventId = doc.getString("event_id");
                        if ("event".equals(scope) && discEventId != null && !discEventId.equals(eventId)) continue;

                        codes.add(code);
                        discountIds.add(doc.getString("discount_id") != null ? doc.getString("discount_id") : doc.getId());
                        values.add(value);
                        types.add(type != null ? type : "fixed");
                        maxDiscounts.add(maxDisc != null ? maxDisc : Long.MAX_VALUE);

                        // Mô tả rõ ràng
                        String desc;
                        if ("percentage".equals(type)) {
                            desc = "Giảm " + value + "%";
                            if (maxDisc != null && maxDisc < Long.MAX_VALUE) {
                                desc += " (tối đa " + formatter.format(maxDisc) + "d)";
                            }
                        } else {
                            desc = "Giảm " + formatter.format(value) + "d";
                        }
                        if ("event".equals(scope)) desc += " - Cho sự kiện này";
                        else desc += " - Toàn sàn";
                        descriptions.add(desc);
                    }

                    if (codes.isEmpty()) {
                        Toast.makeText(requireContext(), "Không có mã phù hợp cho đơn này", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build custom dialog với từng voucher card
                    android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
                    container.setOrientation(android.widget.LinearLayout.VERTICAL);
                    container.setPadding(dp(16), dp(8), dp(16), dp(8));

                    final androidx.appcompat.app.AlertDialog.Builder builder =
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                    builder.setTitle("Chọn mã giảm giá");

                    for (int i = 0; i < codes.size(); i++) {
                        final int idx = i;
                        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
                        row.setOrientation(android.widget.LinearLayout.VERTICAL);
                        row.setPadding(dp(16), dp(14), dp(16), dp(14));
                        row.setBackgroundResource(R.drawable.bg_search_bar);
                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = dp(8);
                        row.setLayoutParams(lp);

                        // Dòng 1: Mô tả (nội dung chính)
                        TextView txtDesc = new TextView(requireContext());
                        txtDesc.setText(descriptions.get(i));
                        txtDesc.setTextSize(14);
                        txtDesc.setTextColor(getResources().getColor(R.color.clr_text_black));
                        txtDesc.setTypeface(null, android.graphics.Typeface.BOLD);
                        row.addView(txtDesc);

                        // Dòng 2: Mã code
                        TextView txtCode = new TextView(requireContext());
                        txtCode.setText("Mã: " + codes.get(i));
                        txtCode.setTextSize(12);
                        txtCode.setTextColor(getResources().getColor(R.color.clr_text_secondary));
                        txtCode.setPadding(0, dp(4), 0, 0);
                        row.addView(txtCode);

                        // Nút "Áp dụng"
                        TextView btnApply = new TextView(requireContext());
                        btnApply.setText("Áp dụng");
                        btnApply.setTextSize(13);
                        btnApply.setTextColor(getResources().getColor(R.color.clr_primary_blue));
                        btnApply.setTypeface(null, android.graphics.Typeface.BOLD);
                        btnApply.setPadding(0, dp(8), 0, 0);
                        row.addView(btnApply);

                        container.addView(row);
                    }

                    android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
                    scroll.addView(container);

                    builder.setView(scroll);
                    builder.setNegativeButton("Đóng", null);
                    final androidx.appcompat.app.AlertDialog dialog = builder.create();
                    dialog.show();

                    // Wire click sau khi dialog show (để có thể dismiss)
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final int idx = i;
                        container.getChildAt(i).setOnClickListener(v -> {
                            applyVoucher(idx, codes, discountIds, values, types, maxDiscounts);
                            dialog.dismiss();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi tải voucher", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyVoucher(int idx, java.util.List<String> codes, java.util.List<String> discountIds,
                              java.util.List<Long> values, java.util.List<String> types, java.util.List<Long> maxDiscounts) {
        long val = values.get(idx);
        String type = types.get(idx);
        long maxDisc = maxDiscounts.get(idx);

        if ("percentage".equals(type)) {
            discount = totalPrice * val / 100;
            if (maxDisc < Long.MAX_VALUE && discount > maxDisc) discount = maxDisc;
        } else {
            discount = val;
        }
        if (discount > totalPrice) discount = totalPrice;

        appliedDiscountId = discountIds.get(idx);
        finalTotal = totalPrice - discount;
        if (finalTotal < 0) finalTotal = 0;

        updateBillingSummary();
        TextView txtVoucher = requireView().findViewById(R.id.txtPaymentAppliedVoucher);
        if (txtVoucher != null) {
            txtVoucher.setText(codes.get(idx) + " (-" + formatter.format(discount) + "d)");
            txtVoucher.setTextColor(getResources().getColor(R.color.clr_success));
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private void loadEventDetails() {
        // Show image immediately from passed-in URL while async fetch runs
        if (!eventImageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(eventImageUrl)
                    .placeholder(R.drawable.event_live_non_song)
                    .into(imvPaymentEventThumb);
        }
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                
                txtPaymentEventTitle.setText(event.getTitle());
                txtPaymentEventDate.setText(event.getDate());
                txtPaymentEventLocation.setText(event.getLocation());
                
                String img = event.getImageUrl();
                if (img != null && !img.isEmpty()) {
                    eventImageUrl = img;
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(img)
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvPaymentEventThumb);
                } else if (!eventImageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(eventImageUrl)
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvPaymentEventThumb);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.str_error_generic), Toast.LENGTH_SHORT).show();
                }
            }
        });

        txtPaymentReceiverEmail.setText(email);
    }

    private void setupCountdown() {
        countDownTimer = new CountDownTimer(COUNTDOWN_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String timeStr = String.format(Locale.getDefault(), "%02d : %02d", minutes, seconds);
                txtPaymentCountdown.setText(getString(R.string.str_payment_timer_prefix) + timeStr);
            }

            @Override
            public void onFinish() {
                txtPaymentCountdown.setText(getString(R.string.str_payment_timer_expired));
                btnPaymentPay.setEnabled(false);
                Toast.makeText(requireContext(), getString(R.string.str_payment_expired_toast), Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void updateBillingSummary() {
        txtPaymentSummaryName.setText(ticketTypeName + " x" + quantity);
        txtPaymentSummaryPrice.setText(formatter.format(totalPrice) + " đ");
        // Ẩn phí nền tảng
        if (txtPaymentSummaryFee != null) {
            View feeRow = (View) txtPaymentSummaryFee.getParent();
            if (feeRow != null) feeRow.setVisibility(View.GONE);
        }
        txtPaymentSummaryDiscount.setText("- " + formatter.format(discount) + " đ");
        txtPaymentSummaryTotal.setText(formatter.format(finalTotal) + " đ");
        txtPaymentBottomTotal.setText(formatter.format(finalTotal) + " đ");
    }

    private void setupClickListeners(View root) {
        btnPaymentBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Step indicator click → quay lại bước trước
        View step2 = root.findViewById(R.id.layoutPaymentStep2);
        if (step2 != null) {
            step2.setOnClickListener(v -> {
                // "Thông tin" → quay lại màn điền thông tin
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }
        View step1 = root.findViewById(R.id.layoutPaymentStep1);
        if (step1 != null) {
            step1.setOnClickListener(v -> {
                // "Chọn vé" → quay về màn chọn vé (pop tới entry fill_info, gồm cả nó)
                getParentFragmentManager().popBackStack("fill_info",
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            });
        }

        btnPaymentPay.setOnClickListener(v -> {
            if (!cbPaymentAgree.isChecked()) {
                Toast.makeText(requireContext(), getString(R.string.str_agree_vibetix_rules), Toast.LENGTH_SHORT).show();
                return;
            }

            btnPaymentPay.setEnabled(false);
            Toast.makeText(requireContext(), getString(R.string.str_processing_payment), Toast.LENGTH_SHORT).show();

            processSimulatedPayment();
        });
    }

    private void processSimulatedPayment() {
        String orderId = generateOrderId();
        String paymentMethod = getSelectedPaymentMethodName();
        long now = System.currentTimeMillis();

        com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String userId = fbUser != null ? fbUser.getUid() : "";
        String userEmail = (fbUser != null && fbUser.getEmail() != null) ? fbUser.getEmail() : email;

        // 1. Lưu Order (user_id = Auth UID, khớp schema seed data)
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setOrderDate(new com.google.firebase.Timestamp(new Date(now)));
        order.setTotalAmount(finalTotal);
        order.setPaymentMethod(paymentMethod);
        order.setStatusStr(Constants.ORDER_STATUS_PAID);

        orderRepository.saveOrder(order, new OrderRepository.OnOrderActionListener() {
            @Override
            public void onSuccess(String savedOrderId) {
                if (!isAdded()) return;
                // Lưu order_discount nếu có áp dụng voucher
                if (appliedDiscountId != null && discount > 0) {
                    String odId = java.util.UUID.randomUUID().toString();
                    java.util.Map<String, Object> od = new java.util.HashMap<>();
                    od.put("order_id", orderId);
                    od.put("discount_id", appliedDiscountId);
                    od.put("applied_amount", discount);
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("order_discounts").document(odId).set(od);
                }
                saveOrderItemsAndTickets(orderId, paymentMethod, now, userId, userEmail);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                btnPaymentPay.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.str_payment_error) + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveOrderItemsAndTickets(String orderId, String paymentMethod, long orderTime,
                                          String userId, String userEmail) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        String eventTitleStr = txtPaymentEventTitle.getText().toString();
        String eventDateStr = txtPaymentEventDate.getText().toString();
        String eventLocationStr = txtPaymentEventLocation.getText().toString();

        // Parse ticketItemsJson (chi tiết từng loại vé)
        String itemsJson = getArguments() != null ? getArguments().getString("ticketItemsJson", "") : "";
        java.util.List<String[]> ticketItems = new java.util.ArrayList<>(); // [typeId, name, qty, price]

        if (!itemsJson.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(itemsJson);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    ticketItems.add(new String[]{
                            obj.getString("typeId"),
                            obj.getString("name"),
                            String.valueOf(obj.getInt("qty")),
                            String.valueOf(obj.getLong("price"))
                    });
                }
            } catch (org.json.JSONException ignored) {}
        }

        // Fallback: nếu không có JSON chi tiết → tạo 1 item gộp (backward compat)
        if (ticketItems.isEmpty()) {
            ticketItems.add(new String[]{"", ticketTypeName, String.valueOf(quantity), String.valueOf(ticketPrice)});
        }

        // Tạo order_items + user_tickets cho TỪNG loại vé
        int totalTickets = 0;
        for (String[] ti : ticketItems) totalTickets += Integer.parseInt(ti[2]);

        int[] savedCount = {0};
        int finalTotalTickets = totalTickets;
        for (String[] ti : ticketItems) {
            String typeId = ti[0];
            String typeName = ti[1];
            int qty = Integer.parseInt(ti[2]);
            long price = Long.parseLong(ti[3]);

            // 1 order_item per ticket type
            String orderItemId = java.util.UUID.randomUUID().toString();
            java.util.Map<String, Object> orderItem = new java.util.HashMap<>();
            orderItem.put("order_item_id", orderItemId);
            orderItem.put("order_id", orderId);
            orderItem.put("event_id", eventId);
            orderItem.put("ticket_type_id", typeId);
            orderItem.put("price_per_ticket", price);
            orderItem.put("quantity", qty);

            db.collection(com.example.vibetix.Firebase.FirebaseCollections.ORDER_ITEMS)
                    .document(orderItemId).set(orderItem);

            // user_tickets cho mỗi vé trong loại này
            for (int j = 0; j < qty; j++) {
                String userTicketId = java.util.UUID.randomUUID().toString();
                java.util.Map<String, Object> userTicket = new java.util.HashMap<>();
                userTicket.put("user_ticket_id", userTicketId);
                userTicket.put("order_item_id", orderItemId);
                userTicket.put("event_id", eventId);
                userTicket.put("user_id", userId);
                userTicket.put("ticket_code", java.util.UUID.randomUUID().toString());
                userTicket.put("display_code", generateRandomAlphanumeric(8).toUpperCase());
                userTicket.put("status", "valid");
                userTicket.put("is_used", false);
                userTicket.put("checked_in_at", null);
                userTicket.put("issued_at", com.google.firebase.Timestamp.now());

                db.collection(com.example.vibetix.Firebase.FirebaseCollections.USER_TICKETS)
                        .document(userTicketId).set(userTicket)
                        .addOnSuccessListener(aVoid -> {
                            if (!isAdded()) return;
                            savedCount[0]++;
                            if (savedCount[0] == finalTotalTickets) {
                                navigateToSuccessScreen(orderId, paymentMethod, orderTime);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), getString(R.string.str_save_ticket_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void navigateToSuccessScreen(String orderId, String paymentMethod, long orderTime) {
        if (countDownTimer != null) countDownTimer.cancel();

        // Nếu là MUA LẠI vé → đánh dấu transfer accepted (chuyển quyền cho người mua)
        if (isResale && transferId != null && !transferId.isEmpty()) {
            com.google.firebase.auth.FirebaseUser fb = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            String buyerId = fb != null ? fb.getUid() : "";
            ticketTransferRepository.acceptTransfer(transferId, buyerId,
                    new com.example.vibetix.Repositories.TicketTransferRepository.OnTransferActionListener() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(Exception e) {}
                    });
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(orderTime));

        PaymentSuccessFragment successFrag = PaymentSuccessFragment.newInstance(
                orderId,
                paymentMethod,
                ticketTypeName,
                quantity,
                finalTotal,
                formattedDate,
                email,
                eventId
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, successFrag)
                .commit();
    }

    private String generateOrderId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String getSelectedPaymentMethodName() {
        int checkedId = rgPaymentMethods.getCheckedRadioButtonId();
        if (checkedId == R.id.rbVietQR) return getString(R.string.str_vietqr);
        if (checkedId == R.id.rbZaloPay) return getString(R.string.str_zalopay);
        if (checkedId == R.id.rbShopeePay) return getString(R.string.str_shopeepay);
        if (checkedId == R.id.rbCreditCard) return getString(R.string.str_creditcard);
        return getString(R.string.str_mobilebanking);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
