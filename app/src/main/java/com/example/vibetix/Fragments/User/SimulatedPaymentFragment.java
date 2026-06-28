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
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Repositories.OrderRepository;
import com.example.vibetix.Repositories.TicketRepository;
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

    // Attendee details
    private String name, email, phone, company, role, notes;

    // Billing variables
    private final long platformFee = 10000;
    private final long discount = 0;
    private long finalTotal = 410000;

    // Timer
    private CountDownTimer countDownTimer;
    private static final long COUNTDOWN_DURATION_MS = 15 * 60 * 1000; // 15 mins

    // Repositories
    private final EventRepository eventRepository = new EventRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final TicketRepository ticketRepository = new TicketRepository();

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
                                                       String phone, String company, String role, String notes) {
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
        args.putString("company", company);
        args.putString("role", role);
        args.putString("notes", notes);
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
            name = getArguments().getString("name", "");
            email = getArguments().getString("email", "");
            phone = getArguments().getString("phone", "");
            company = getArguments().getString("company", "");
            role = getArguments().getString("role", "");
            notes = getArguments().getString("notes", "");
        }
        finalTotal = totalPrice + platformFee - discount;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simulated_payment, container, false);
        bindViews(view);
        loadEventDetails();
        setupCountdown();
        setupClickListeners();
        updateBillingSummary();
        return view;
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
    }

    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                
                txtPaymentEventTitle.setText(event.getTitle());
                txtPaymentEventDate.setText(event.getDate());
                txtPaymentEventLocation.setText(event.getLocation());
                
                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvPaymentEventThumb);
                } else {
                    int imageRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equalsIgnoreCase(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvPaymentEventThumb.setImageResource(imageRes);
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
        txtPaymentSummaryFee.setText(formatter.format(platformFee) + " đ");
        txtPaymentSummaryDiscount.setText("- " + formatter.format(discount) + " đ");
        txtPaymentSummaryTotal.setText(formatter.format(finalTotal) + " đ");
        txtPaymentBottomTotal.setText(formatter.format(finalTotal) + " đ");
    }

    private void setupClickListeners() {
        btnPaymentBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

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

        // Use logged in user email for order and tickets instead of the contact email
        // or at least ensure tickets are linked to the account.
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        String loggedInEmail = prefs.getString(Constants.KEY_USER_EMAIL, email);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(loggedInEmail); // Note: using email for now
        order.setOrderDate(new com.google.firebase.Timestamp(new Date(now)));
        order.setTotalAmount(finalTotal);
        order.setPaymentMethod(paymentMethod);
        order.setStatusStr(Constants.ORDER_STATUS_PAID);

        orderRepository.saveOrder(order, new OrderRepository.OnOrderActionListener() {
            @Override
            public void onSuccess(String savedOrderId) {
                if (!isAdded()) return;
                saveBoughtTickets(orderId, paymentMethod, now, loggedInEmail);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                btnPaymentPay.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.str_payment_error) + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveBoughtTickets(String orderId, String paymentMethod, long orderTime, String loggedInEmail) {
        int[] savedCount = {0};
        String eventTitleString = txtPaymentEventTitle.getText().toString();
        String eventDateString = txtPaymentEventDate.getText().toString();
        String eventLocationString = txtPaymentEventLocation.getText().toString();
        String eventImageUrl = "";

        for (int i = 0; i < quantity; i++) {
            String ticketId = "VTX-TKT-" + generateRandomAlphanumeric(10).toUpperCase();
            Ticket ticket = new Ticket(
                    ticketId,
                    orderId,
                    loggedInEmail, // Ensure tickets are saved to the logged-in user's email
                    eventId,
                    eventTitleString,
                    eventDateString,
                    eventLocationString,
                    eventImageUrl,
                    ticketTypeName,
                    ticketPrice,
                    0L,
                    "ACTIVE",
                    name
            );

            ticketRepository.saveTicket(ticket, new TicketRepository.OnTicketActionListener() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    savedCount[0]++;
                    if (savedCount[0] == quantity) {
                        navigateToSuccessScreen(orderId, paymentMethod, orderTime);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), getString(R.string.str_save_ticket_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToSuccessScreen(String orderId, String paymentMethod, long orderTime) {
        if (countDownTimer != null) countDownTimer.cancel();

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
        SimpleDateFormat sdf = new SimpleDateFormat("yyMM", Locale.getDefault());
        String datePrefix = sdf.format(new Date());
        return "VTX" + datePrefix + "-" + generateRandomAlphanumeric(6).toUpperCase();
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
