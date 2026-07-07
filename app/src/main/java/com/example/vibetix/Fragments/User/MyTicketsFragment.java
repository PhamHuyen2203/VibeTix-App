package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Adapters.FeaturedEventAdapter;
import com.example.vibetix.Adapters.TicketAdapter;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketRepository;
import com.example.vibetix.Repositories.TicketTransferRepository;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsFragment extends Fragment {

    // Tab state enums
    private enum MainTab { BOUGHT, RESELLING }
    private enum SubTabBought { UPCOMING, ENDED }
    private enum SubTabResale { PENDING, SOLD, CANCELLED, EXPIRED }

    private MainTab currentMainTab = MainTab.BOUGHT;
    private SubTabBought currentSubTabBought = SubTabBought.UPCOMING;
    private SubTabResale currentSubTabResale = SubTabResale.PENDING;

    // Data
    private String currentUserId;
    private final List<Ticket> displayTickets = new ArrayList<>();
    private TicketAdapter ticketAdapter;

    private final TicketRepository ticketRepository = new TicketRepository();
    private final TicketTransferRepository ticketTransferRepository = new TicketTransferRepository();

    // Views
    private TextView tabCategoryBought, tabCategoryReselling;

    // Sub-tab layouts
    private LinearLayout layoutSubTabsBought;
    private HorizontalScrollView layoutSubTabsResale;

    // Sub-tabs for Bought
    private LinearLayout tabSubUpcoming, tabSubEnded;
    private TextView txtSubUpcoming, txtSubEnded;
    private View indicatorSubUpcoming, indicatorSubEnded;

    // Sub-tabs for Resale (4 tabs — ACTIVE and REJECTED removed)
    private LinearLayout tabResalePending, tabResaleSold,
            tabResaleCancelled, tabResaleExpired;
    private TextView txtResalePending, txtResaleSold,
            txtResaleCancelled, txtResaleExpired;
    private View indicatorResalePending, indicatorResaleSold,
            indicatorResaleCancelled, indicatorResaleExpired;

    // Resale extra info row
    private RelativeLayout layoutResaleInfoRow;
    private TextView txtResaleEarningsLabel;

    // Main layouts
    private RecyclerView rvMyTicketsList;
    private View layoutMyTicketsEmptyState;
    private TextView txtEmptyStateTitle, txtEmptyStateSubtitle;
    private Button btnEmptyStateAction;
    private View layoutEmptyRecommendationsHeader;
    private RecyclerView rvEmptyRecommendationsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tickets, container, false);
        bindViews(view);
        applyWindowInsets(view);
        restoreUserSession();
        setupTabs();
        setupRecyclerView();
        loadTickets();
        loadSuggestions();
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) loadTickets();
    }

    private void bindViews(View view) {
        // Categories
        tabCategoryBought = view.findViewById(R.id.tabCategoryBought);
        tabCategoryReselling = view.findViewById(R.id.tabCategoryReselling);

        // Sub tab groups
        layoutSubTabsBought = view.findViewById(R.id.layoutSubTabsBought);
        layoutSubTabsResale = view.findViewById(R.id.layoutSubTabsResale);

        // Bought sub tabs
        tabSubUpcoming = view.findViewById(R.id.tabSubUpcoming);
        tabSubEnded = view.findViewById(R.id.tabSubEnded);
        txtSubUpcoming = view.findViewById(R.id.txtSubUpcoming);
        txtSubEnded = view.findViewById(R.id.txtSubEnded);
        indicatorSubUpcoming = view.findViewById(R.id.indicatorSubUpcoming);
        indicatorSubEnded = view.findViewById(R.id.indicatorSubEnded);

        // Resale sub tabs (4 — ACTIVE and REJECTED hidden)
        tabResalePending = view.findViewById(R.id.tabResalePending);
        tabResaleSold = view.findViewById(R.id.tabResaleSold);
        tabResaleCancelled = view.findViewById(R.id.tabResaleCancelled);
        tabResaleExpired = view.findViewById(R.id.tabResaleExpired);

        txtResalePending = view.findViewById(R.id.txtResalePending);
        txtResaleSold = view.findViewById(R.id.txtResaleSold);
        txtResaleCancelled = view.findViewById(R.id.txtResaleCancelled);
        txtResaleExpired = view.findViewById(R.id.txtResaleExpired);

        indicatorResalePending = view.findViewById(R.id.indicatorResalePending);
        indicatorResaleSold = view.findViewById(R.id.indicatorResaleSold);
        indicatorResaleCancelled = view.findViewById(R.id.indicatorResaleCancelled);
        indicatorResaleExpired = view.findViewById(R.id.indicatorResaleExpired);

        // Hide removed tabs
        View tabActive = view.findViewById(R.id.tabResaleActive);
        View tabRejected = view.findViewById(R.id.tabResaleRejected);
        if (tabActive != null) tabActive.setVisibility(View.GONE);
        if (tabRejected != null) tabRejected.setVisibility(View.GONE);

        // Resale extra info
        layoutResaleInfoRow = view.findViewById(R.id.layoutResaleInfoRow);
        txtResaleEarningsLabel = view.findViewById(R.id.txtResaleEarningsLabel);

        // Container views
        rvMyTicketsList = view.findViewById(R.id.rvMyTicketsList);
        layoutMyTicketsEmptyState = view.findViewById(R.id.layoutMyTicketsEmptyState);
        txtEmptyStateTitle = view.findViewById(R.id.txtEmptyStateTitle);
        txtEmptyStateSubtitle = view.findViewById(R.id.txtEmptyStateSubtitle);
        btnEmptyStateAction = view.findViewById(R.id.btnEmptyStateAction);
        layoutEmptyRecommendationsHeader = view.findViewById(R.id.layoutEmptyRecommendationsHeader);
        rvEmptyRecommendationsList = view.findViewById(R.id.rvEmptyRecommendationsList);
    }

    private void applyWindowInsets(View view) {
        View header = view.findViewById(R.id.layoutMyTicketsHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, windowInsets) -> {
                int top = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, top, 0, 0);
                return windowInsets;
            });
        }
    }

    private void restoreUserSession() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = fbUser != null ? fbUser.getUid() : "";
    }

    private void setupTabs() {
        // Main categories
        tabCategoryBought.setOnClickListener(v -> {
            selectMainTab(MainTab.BOUGHT);
            loadTickets();
        });
        tabCategoryReselling.setOnClickListener(v -> {
            selectMainTab(MainTab.RESELLING);
            loadTickets();
        });

        // Bought sub tabs
        tabSubUpcoming.setOnClickListener(v -> {
            selectSubTabBought(SubTabBought.UPCOMING);
            loadTickets();
        });
        tabSubEnded.setOnClickListener(v -> {
            selectSubTabBought(SubTabBought.ENDED);
            loadTickets();
        });

        // Resale sub tabs (4)
        tabResalePending.setOnClickListener(v -> { selectSubTabResale(SubTabResale.PENDING); loadTickets(); });
        tabResaleSold.setOnClickListener(v -> { selectSubTabResale(SubTabResale.SOLD); loadTickets(); });
        tabResaleCancelled.setOnClickListener(v -> { selectSubTabResale(SubTabResale.CANCELLED); loadTickets(); });
        tabResaleExpired.setOnClickListener(v -> { selectSubTabResale(SubTabResale.EXPIRED); loadTickets(); });

        // Empty state action
        btnEmptyStateAction.setOnClickListener(v -> {
            if (currentMainTab == MainTab.BOUGHT) {
                if (getActivity() instanceof UserMainActivity) {
                    ((UserMainActivity) getActivity()).selectTab(com.example.vibetix.R.id.tabHome);
                }
            } else if (currentMainTab == MainTab.RESELLING && currentSubTabResale == SubTabResale.PENDING) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_resell_guide), Toast.LENGTH_LONG).show();
                selectMainTab(MainTab.BOUGHT);
                selectSubTabBought(SubTabBought.UPCOMING);
                loadTickets();
            }
        });
    }

    private void selectMainTab(MainTab tab) {
        currentMainTab = tab;

        // Reset both tabs to inactive
        tabCategoryBought.setBackgroundResource(R.drawable.bg_chip_filter);
        tabCategoryBought.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_text_secondary));
        tabCategoryBought.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabCategoryReselling.setBackgroundResource(R.drawable.bg_chip_filter);
        tabCategoryReselling.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_text_secondary));
        tabCategoryReselling.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Toggle sub tab bars and info bar
        layoutSubTabsBought.setVisibility(View.GONE);
        layoutSubTabsResale.setVisibility(View.GONE);
        layoutResaleInfoRow.setVisibility(View.GONE);

        if (tab == MainTab.BOUGHT) {
            tabCategoryBought.setBackgroundResource(R.drawable.bg_tab_active_outline);
            tabCategoryBought.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_primary_blue));
            tabCategoryBought.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutSubTabsBought.setVisibility(View.VISIBLE);
        } else {
            tabCategoryReselling.setBackgroundResource(R.drawable.bg_tab_active_outline);
            tabCategoryReselling.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_primary_blue));
            tabCategoryReselling.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutSubTabsResale.setVisibility(View.VISIBLE);
            toggleResaleInfoRow();
        }
    }

    private void toggleResaleInfoRow() {
        if (currentMainTab == MainTab.RESELLING
                && currentSubTabResale != SubTabResale.PENDING) {
            layoutResaleInfoRow.setVisibility(View.VISIBLE);
        } else {
            layoutResaleInfoRow.setVisibility(View.GONE);
        }
    }

    private void selectSubTabBought(SubTabBought sub) {
        currentSubTabBought = sub;

        txtSubUpcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_text_secondary));
        txtSubUpcoming.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorSubUpcoming.setBackgroundResource(android.R.color.transparent);

        txtSubEnded.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_text_secondary));
        txtSubEnded.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorSubEnded.setBackgroundResource(android.R.color.transparent);

        if (sub == SubTabBought.UPCOMING) {
            txtSubUpcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_primary_blue));
            txtSubUpcoming.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSubUpcoming.setBackgroundResource(R.color.clr_primary_blue);
        } else {
            txtSubEnded.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_primary_blue));
            txtSubEnded.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSubEnded.setBackgroundResource(R.color.clr_primary_blue);
        }
    }

    private void selectSubTabResale(SubTabResale sub) {
        currentSubTabResale = sub;

        // Reset all 4 active tabs
        TextView[] textViews = {txtResalePending, txtResaleSold, txtResaleCancelled, txtResaleExpired};
        View[] indicators = {indicatorResalePending, indicatorResaleSold, indicatorResaleCancelled, indicatorResaleExpired};

        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_text_secondary));
            textViews[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            indicators[i].setBackgroundResource(android.R.color.transparent);
        }

        TextView activeText;
        View activeIndicator;
        switch (sub) {
            case SOLD:      activeText = txtResaleSold;      activeIndicator = indicatorResaleSold;      break;
            case CANCELLED: activeText = txtResaleCancelled; activeIndicator = indicatorResaleCancelled; break;
            case EXPIRED:   activeText = txtResaleExpired;   activeIndicator = indicatorResaleExpired;   break;
            default:        activeText = txtResalePending;   activeIndicator = indicatorResalePending;   break;
        }
        activeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.clr_primary_blue));
        activeText.setTypeface(null, android.graphics.Typeface.BOLD);
        activeIndicator.setBackgroundResource(R.color.clr_primary_blue);

        toggleResaleInfoRow();
    }

    private void setupRecyclerView() {
        ticketAdapter = new TicketAdapter(requireContext(), displayTickets, new TicketAdapter.OnTicketClickListener() {
            @Override
            public void onQrClick(Ticket ticket) {
                TicketQrDialogFragment qrDialog = TicketQrDialogFragment.newInstance(ticket);
                qrDialog.show(getChildFragmentManager(), "qr_dialog");
            }

            @Override
            public void onItemClick(Ticket ticket) {
                TicketDetailDialogFragment detail = TicketDetailDialogFragment.newInstance(ticket);
                detail.show(getChildFragmentManager(), "ticket_detail");
            }

            @Override
            public void onResellClick(Ticket ticket) {
                if ("ACTIVE".equalsIgnoreCase(ticket.getStatus()) || "valid".equalsIgnoreCase(ticket.getStatus())) {
                    PassTicketDialogFragment passDialog = PassTicketDialogFragment.newInstance(ticket, () -> loadTickets());
                    passDialog.show(getChildFragmentManager(), "pass_dialog");
                } else if ("RESELLING".equalsIgnoreCase(ticket.getStatus())) {
                    Toast.makeText(requireContext(), getString(R.string.str_toast_cancelling_resell), Toast.LENGTH_SHORT).show();
                    ticketTransferRepository.cancelTransfer(ticket.getId(), new TicketTransferRepository.OnTransferActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), getString(R.string.str_toast_resell_cancelled), Toast.LENGTH_SHORT).show();
                            loadTickets();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), getString(R.string.str_toast_cancel_resell_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if ("RESALE_CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
                    // "Đăng bán lại" for cancelled resale tickets
                    PassTicketDialogFragment passDialog = PassTicketDialogFragment.newInstance(ticket, () -> loadTickets());
                    passDialog.show(getChildFragmentManager(), "pass_dialog");
                }
            }
        });

        rvMyTicketsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyTicketsList.setAdapter(ticketAdapter);
    }

    private void loadTickets() {
        displayTickets.clear();
        ticketAdapter.notifyDataSetChanged();

        TicketRepository.OnTicketsLoadedListener boughtCallback = new TicketRepository.OnTicketsLoadedListener() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                displayTickets.clear();
                displayTickets.addAll(tickets);
                ticketAdapter.notifyDataSetChanged();
                updateEmptyStateUI();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_load_ticket_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                updateEmptyStateUI();
            }
        };

        if (currentMainTab == MainTab.BOUGHT) {
            if (currentSubTabBought == SubTabBought.UPCOMING) {
                ticketRepository.getActiveTickets(currentUserId, boughtCallback);
            } else {
                ticketRepository.getEndedTickets(currentUserId, boughtCallback);
            }
        } else if (currentMainTab == MainTab.RESELLING) {
            loadResaleTickets();
        }
    }

    private void loadResaleTickets() {
        TicketTransferRepository.OnTransfersLoadedListener transferCallback = new TicketTransferRepository.OnTransfersLoadedListener() {
            @Override
            public void onSuccess(List<TicketTransfer> transfers) {
                displayTickets.clear();
                for (TicketTransfer t : transfers) {
                    Ticket ticket = new Ticket();
                    ticket.setId(t.getTransferId());
                    ticket.setEventId(t.getEventId());
                    ticket.setEventTitle(t.getEventTitle());
                    ticket.setEventDate(t.getEventDate());
                    ticket.setEventLocation(t.getEventLocation());
                    ticket.setEventImageUrl(t.getEventImageUrl());
                    ticket.setStatus(mapTransferStatus(t.getStatus()));
                    ticket.setResalePrice(t.getPrice());
                    ticket.setPurchasePrice(t.getOriginalPrice());
                    ticket.setTicketTypeName(t.getTicketTypeName());
                    displayTickets.add(ticket);
                }
                ticketAdapter.notifyDataSetChanged();
                updateEmptyStateUI();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_load_ticket_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                updateEmptyStateUI();
            }
        };

        switch (currentSubTabResale) {
            case PENDING:
                ticketTransferRepository.getPendingTransfersBySender(currentUserId, transferCallback);
                break;
            case SOLD:
                ticketTransferRepository.getAcceptedTransfersBySender(currentUserId, new TicketTransferRepository.OnTransfersLoadedListener() {
                    @Override
                    public void onSuccess(List<TicketTransfer> transfers) {
                        transferCallback.onSuccess(transfers);
                    }
                    @Override
                    public void onFailure(Exception e) { transferCallback.onFailure(e); }
                });
                break;
            case CANCELLED:
                transferCallback_forStatus("cancelled", transferCallback);
                break;
            case EXPIRED:
                transferCallback_forStatus("expired", transferCallback);
                break;
        }
    }

    /** Query transfers by sender with a single status (cancelled / rejected / expired) */
    private void transferCallback_forStatus(String status, TicketTransferRepository.OnTransfersLoadedListener listener) {
        // getCancelledTransfersBySender queries cancelled+rejected+expired combined,
        // so we filter in-memory for the specific status
        ticketTransferRepository.getCancelledTransfersBySender(currentUserId, new TicketTransferRepository.OnTransfersLoadedListener() {
            @Override
            public void onSuccess(List<TicketTransfer> transfers) {
                List<TicketTransfer> filtered = new ArrayList<>();
                for (TicketTransfer t : transfers) {
                    if (status.equalsIgnoreCase(t.getStatus())) {
                        filtered.add(t);
                    }
                }
                listener.onSuccess(filtered);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /** Map transfer status to adapter display status */
    private String mapTransferStatus(String transferStatus) {
        if (transferStatus == null) return "RESALE_CANCELLED";
        switch (transferStatus.toLowerCase()) {
            case "pending":    return "RESELLING";
            case "accepted":   return "RESOLD";
            case "cancelled":  return "RESALE_CANCELLED";
            case "rejected":   return "RESALE_CANCELLED";
            case "expired":    return "RESALE_CANCELLED";
            default:           return "RESALE_CANCELLED";
        }
    }

    private void updateEmptyStateUI() {
        if (displayTickets.isEmpty()) {
            rvMyTicketsList.setVisibility(View.GONE);
            layoutMyTicketsEmptyState.setVisibility(View.VISIBLE);

            btnEmptyStateAction.setVisibility(View.GONE);
            layoutEmptyRecommendationsHeader.setVisibility(View.GONE);
            rvEmptyRecommendationsList.setVisibility(View.GONE);

            if (currentMainTab == MainTab.BOUGHT) {
                if (currentSubTabBought == SubTabBought.UPCOMING) {
                    txtEmptyStateTitle.setText(getString(R.string.str_empty_no_tickets_title));
                    txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_tickets_subtitle));
                    btnEmptyStateAction.setText(getString(R.string.str_buy_ticket_now));
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                    layoutEmptyRecommendationsHeader.setVisibility(View.VISIBLE);
                    rvEmptyRecommendationsList.setVisibility(View.VISIBLE);
                } else {
                    txtEmptyStateTitle.setText(getString(R.string.str_empty_no_ended_title));
                    txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_ended_subtitle));
                }
            } else if (currentMainTab == MainTab.RESELLING) {
                switch (currentSubTabResale) {
                    case PENDING:
                        txtEmptyStateTitle.setText(getString(R.string.str_empty_no_reselling_title));
                        txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_reselling_subtitle));
                        btnEmptyStateAction.setText(getString(R.string.str_resell_ticket_btn));
                        btnEmptyStateAction.setVisibility(View.VISIBLE);
                        break;
                    case SOLD:
                        txtEmptyStateTitle.setText(getString(R.string.str_empty_no_sold_title));
                        txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_sold_subtitle));
                        break;
                    case CANCELLED:
                        txtEmptyStateTitle.setText(getString(R.string.str_empty_no_cancelled_title));
                        txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_cancelled_subtitle));
                        break;
                    case EXPIRED:
                        txtEmptyStateTitle.setText(getString(R.string.str_empty_no_expired_title));
                        txtEmptyStateSubtitle.setText(getString(R.string.str_empty_no_expired_subtitle));
                        break;
                }
            }
        } else {
            rvMyTicketsList.setVisibility(View.VISIBLE);
            layoutMyTicketsEmptyState.setVisibility(View.GONE);
        }
    }

    private void loadSuggestions() {
        ArrayList<Event> suggestions = new ArrayList<>();

        Event s1 = new Event("e1", "Mừng Ngày Hội Non Sông - Võ Hà Trâm", null, "01/05/2026", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 350000);
        s1.setLocalPortraitImageResId(R.drawable.event_live_non_song_2);
        suggestions.add(s1);

        Event s2 = new Event("e2", "Private Show in Fantasy - Quốc Thiên", null, "16/05/2026", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 800000);
        s2.setLocalPortraitImageResId(R.drawable.banner_private_show_fantasy);
        suggestions.add(s2);

        FeaturedEventAdapter suggestionsAdapter = new FeaturedEventAdapter(requireContext(), suggestions, event -> {
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).openSubFragment(EventDetailFragment.newInstance(event.getId()));
            }
        });

        rvEmptyRecommendationsList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEmptyRecommendationsList.setAdapter(suggestionsAdapter);
    }
}
