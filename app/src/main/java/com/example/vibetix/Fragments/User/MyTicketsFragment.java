package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Activities.UserMainActivity;
import com.example.vibetix.Adapters.FeaturedEventAdapter;
import com.example.vibetix.Adapters.TicketAdapter;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketRepository;
import com.example.vibetix.Utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsFragment extends Fragment {

    // Tab state enums
    private enum MainTab { BOUGHT, RESELLING, MEMBERSHIP }
    private enum SubTabBought { UPCOMING, ENDED }
    private enum SubTabResale { ACTIVE, PENDING, PAID, CANCELLED }

    private MainTab currentMainTab = MainTab.BOUGHT;
    private SubTabBought currentSubTabBought = SubTabBought.UPCOMING;
    private SubTabResale currentSubTabResale = SubTabResale.ACTIVE;

    // Data
    private String userEmail = "customer@vibetix.com";
    private final List<Ticket> displayTickets = new ArrayList<>();
    private TicketAdapter ticketAdapter;

    private final TicketRepository ticketRepository = new TicketRepository();

    // Views
    private TextView tabCategoryBought, tabCategoryReselling, tabCategoryMembership;
    
    // Sub-tab layouts
    private LinearLayout layoutSubTabsBought, layoutSubTabsResale;
    
    // Sub-tabs for Bought
    private LinearLayout tabSubUpcoming, tabSubEnded;
    private TextView txtSubUpcoming, txtSubEnded;
    private View indicatorSubUpcoming, indicatorSubEnded;

    // Sub-tabs for Resale
    private LinearLayout tabResaleActive, tabResalePending, tabResalePaid, tabResaleCancelled;
    private TextView txtResaleActive, txtResalePending, txtResalePaid, txtResaleCancelled;
    private View indicatorResaleActive, indicatorResalePending, indicatorResalePaid, indicatorResaleCancelled;

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

    private void bindViews(View view) {
        // Categories
        tabCategoryBought = view.findViewById(R.id.tabCategoryBought);
        tabCategoryReselling = view.findViewById(R.id.tabCategoryReselling);
        tabCategoryMembership = view.findViewById(R.id.tabCategoryMembership);

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

        // Resale sub tabs
        tabResaleActive = view.findViewById(R.id.tabResaleActive);
        tabResalePending = view.findViewById(R.id.tabResalePending);
        tabResalePaid = view.findViewById(R.id.tabResalePaid);
        tabResaleCancelled = view.findViewById(R.id.tabResaleCancelled);

        txtResaleActive = view.findViewById(R.id.txtResaleActive);
        txtResalePending = view.findViewById(R.id.txtResalePending);
        txtResalePaid = view.findViewById(R.id.txtResalePaid);
        txtResaleCancelled = view.findViewById(R.id.txtResaleCancelled);

        indicatorResaleActive = view.findViewById(R.id.indicatorResaleActive);
        indicatorResalePending = view.findViewById(R.id.indicatorResalePending);
        indicatorResalePaid = view.findViewById(R.id.indicatorResalePaid);
        indicatorResaleCancelled = view.findViewById(R.id.indicatorResaleCancelled);

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
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        userEmail = prefs.getString(Constants.KEY_USER_EMAIL, "customer@vibetix.com");
    }

    private void setupTabs() {
        // Main categories click listeners
        tabCategoryBought.setOnClickListener(v -> {
            selectMainTab(MainTab.BOUGHT);
            loadTickets();
        });

        tabCategoryReselling.setOnClickListener(v -> {
            selectMainTab(MainTab.RESELLING);
            loadTickets();
        });

        tabCategoryMembership.setOnClickListener(v -> {
            selectMainTab(MainTab.MEMBERSHIP);
            loadTickets();
        });

        // Sub tabs click listeners (Bought)
        tabSubUpcoming.setOnClickListener(v -> {
            selectSubTabBought(SubTabBought.UPCOMING);
            loadTickets();
        });

        tabSubEnded.setOnClickListener(v -> {
            selectSubTabBought(SubTabBought.ENDED);
            loadTickets();
        });

        // Sub tabs click listeners (Resale)
        tabResaleActive.setOnClickListener(v -> {
            selectSubTabResale(SubTabResale.ACTIVE);
            loadTickets();
        });

        tabResalePending.setOnClickListener(v -> {
            selectSubTabResale(SubTabResale.PENDING);
            loadTickets();
        });

        tabResalePaid.setOnClickListener(v -> {
            selectSubTabResale(SubTabResale.PAID);
            loadTickets();
        });

        tabResaleCancelled.setOnClickListener(v -> {
            selectSubTabResale(SubTabResale.CANCELLED);
            loadTickets();
        });

        // Empty state primary action click listener
        btnEmptyStateAction.setOnClickListener(v -> {
            if (currentMainTab == MainTab.BOUGHT) {
                // Navigate to home tab
                if (getActivity() instanceof UserMainActivity) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.frameContainerMain, new HomeFragment())
                            .commit();
                }
            } else if (currentMainTab == MainTab.RESELLING && currentSubTabResale == SubTabResale.ACTIVE) {
                // Guide the user to go to bought tickets and pass them
                Toast.makeText(requireContext(), "Chọn vé bạn đã mua ở tab 'Vé đã mua' rồi bấm 'Bán lại vé' để đăng bán!", Toast.LENGTH_LONG).show();
                selectMainTab(MainTab.BOUGHT);
                selectSubTabBought(SubTabBought.UPCOMING);
                loadTickets();
            }
        });
    }

    private void selectMainTab(MainTab tab) {
        currentMainTab = tab;

        // Reset styling for category outline buttons (Figma/Styleguide specs)
        tabCategoryBought.setBackgroundResource(R.drawable.bg_chip_filter);
        tabCategoryBought.setTextColor(getResources().getColor(R.color.clr_text_secondary));
        tabCategoryBought.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabCategoryReselling.setBackgroundResource(R.drawable.bg_chip_filter);
        tabCategoryReselling.setTextColor(getResources().getColor(R.color.clr_text_secondary));
        tabCategoryReselling.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabCategoryMembership.setBackgroundResource(R.drawable.bg_chip_filter);
        tabCategoryMembership.setTextColor(getResources().getColor(R.color.clr_text_secondary));
        tabCategoryMembership.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Toggle sub tab bars and info bar
        layoutSubTabsBought.setVisibility(View.GONE);
        layoutSubTabsResale.setVisibility(View.GONE);
        layoutResaleInfoRow.setVisibility(View.GONE);

        if (tab == MainTab.BOUGHT) {
            tabCategoryBought.setBackgroundResource(R.drawable.bg_tab_active_outline);
            tabCategoryBought.setTextColor(getResources().getColor(R.color.clr_primary_blue));
            tabCategoryBought.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutSubTabsBought.setVisibility(View.VISIBLE);
        } else if (tab == MainTab.RESELLING) {
            tabCategoryReselling.setBackgroundResource(R.drawable.bg_tab_active_outline);
            tabCategoryReselling.setTextColor(getResources().getColor(R.color.clr_primary_blue));
            tabCategoryReselling.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutSubTabsResale.setVisibility(View.VISIBLE);
            
            // Toggle resale billing totals row based on sub-tab
            toggleResaleInfoRow();
        } else {
            tabCategoryMembership.setBackgroundResource(R.drawable.bg_tab_active_outline);
            tabCategoryMembership.setTextColor(getResources().getColor(R.color.clr_primary_blue));
            tabCategoryMembership.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void toggleResaleInfoRow() {
        // Image 2 shows info row (Tổng thực nhận) is visible in pending resale sub-tab
        if (currentMainTab == MainTab.RESELLING && currentSubTabResale != SubTabResale.ACTIVE) {
            layoutResaleInfoRow.setVisibility(View.VISIBLE);
        } else {
            layoutResaleInfoRow.setVisibility(View.GONE);
        }
    }

    private void selectSubTabBought(SubTabBought sub) {
        currentSubTabBought = sub;

        txtSubUpcoming.setTextColor(getResources().getColor(R.color.clr_text_secondary));
        txtSubUpcoming.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorSubUpcoming.setBackgroundResource(android.R.color.transparent);

        txtSubEnded.setTextColor(getResources().getColor(R.color.clr_text_secondary));
        txtSubEnded.setTypeface(null, android.graphics.Typeface.NORMAL);
        indicatorSubEnded.setBackgroundResource(android.R.color.transparent);

        if (sub == SubTabBought.UPCOMING) {
            txtSubUpcoming.setTextColor(getResources().getColor(R.color.clr_primary_blue));
            txtSubUpcoming.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSubUpcoming.setBackgroundResource(R.color.clr_primary_blue);
        } else {
            txtSubEnded.setTextColor(getResources().getColor(R.color.clr_primary_blue));
            txtSubEnded.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorSubEnded.setBackgroundResource(R.color.clr_primary_blue);
        }
    }

    private void selectSubTabResale(SubTabResale sub) {
        currentSubTabResale = sub;

        // Reset all resale sub tabs
        TextView[] textViews = {txtResaleActive, txtResalePending, txtResalePaid, txtResaleCancelled};
        View[] indicators = {indicatorResaleActive, indicatorResalePending, indicatorResalePaid, indicatorResaleCancelled};

        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setTextColor(getResources().getColor(R.color.clr_text_secondary));
            textViews[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            indicators[i].setBackgroundResource(android.R.color.transparent);
        }

        // Active selection
        int activeIndex = sub.ordinal();
        textViews[activeIndex].setTextColor(getResources().getColor(R.color.clr_primary_blue));
        textViews[activeIndex].setTypeface(null, android.graphics.Typeface.BOLD);
        indicators[activeIndex].setBackgroundResource(R.color.clr_primary_blue);

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
            public void onResellClick(Ticket ticket) {
                if ("ACTIVE".equalsIgnoreCase(ticket.getStatus())) {
                    PassTicketDialogFragment passDialog = PassTicketDialogFragment.newInstance(ticket, () -> loadTickets());
                    passDialog.show(getChildFragmentManager(), "pass_dialog");
                } else if ("RESELLING".equalsIgnoreCase(ticket.getStatus())) {
                    Toast.makeText(requireContext(), "Đang hủy đăng bán...", Toast.LENGTH_SHORT).show();
                    ticketRepository.cancelResale(ticket.getId(), new TicketRepository.OnTicketActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Đã hủy đăng bán vé!", Toast.LENGTH_SHORT).show();
                            loadTickets();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Hủy đăng bán thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        rvMyTicketsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyTicketsList.setAdapter(ticketAdapter);
    }

    private void loadTickets() {
        displayTickets.clear();
        ticketAdapter.notifyDataSetChanged();

        if (currentMainTab == MainTab.MEMBERSHIP) {
            updateEmptyStateUI();
            return;
        }

        TicketRepository.OnTicketsLoadedListener callback = new TicketRepository.OnTicketsLoadedListener() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                displayTickets.clear();
                displayTickets.addAll(tickets);
                ticketAdapter.notifyDataSetChanged();
                updateEmptyStateUI();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Lỗi tải vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyStateUI();
            }
        };

        if (currentMainTab == MainTab.BOUGHT) {
            if (currentSubTabBought == SubTabBought.UPCOMING) {
                ticketRepository.getActiveTickets(userEmail, callback);
            } else {
                ticketRepository.getEndedTickets(userEmail, callback);
            }
        } else if (currentMainTab == MainTab.RESELLING) {
            // Load reselling list based on resale sub tab status
            if (currentSubTabResale == SubTabResale.ACTIVE) {
                ticketRepository.getResellingTicketsByUser(userEmail, callback);
            } else {
                // Empty states for PENDING, PAID, and CANCELLED mock listings (as modeled in user feedback)
                updateEmptyStateUI();
            }
        }
    }

    private void updateEmptyStateUI() {
        if (displayTickets.isEmpty()) {
            rvMyTicketsList.setVisibility(View.GONE);
            layoutMyTicketsEmptyState.setVisibility(View.VISIBLE);
            
            // Customize text and buttons according to Styleguide, active main tab, and sub-tab selection
            btnEmptyStateAction.setVisibility(View.GONE);
            layoutEmptyRecommendationsHeader.setVisibility(View.GONE);
            rvEmptyRecommendationsList.setVisibility(View.GONE);

            if (currentMainTab == MainTab.BOUGHT) {
                if (currentSubTabBought == SubTabBought.UPCOMING) {
                    txtEmptyStateTitle.setText("Bạn chưa có vé nào cả!");
                    txtEmptyStateSubtitle.setText("Khám phá sự kiện và đặt vé cho những trải nghiệm đáng nhớ đang chờ bạn.");
                    btnEmptyStateAction.setText("🎟  Mua vé ngay");
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                    
                    // Show suggestions on main empty screen
                    layoutEmptyRecommendationsHeader.setVisibility(View.VISIBLE);
                    rvEmptyRecommendationsList.setVisibility(View.VISIBLE);
                } else {
                    txtEmptyStateTitle.setText("Bạn chưa có vé nào đã kết thúc!");
                    txtEmptyStateSubtitle.setText("Lịch sử mua vé của bạn sẽ xuất hiện tại đây.");
                }
            } else if (currentMainTab == MainTab.RESELLING) {
                if (currentSubTabResale == SubTabResale.ACTIVE) {
                    txtEmptyStateTitle.setText("Bạn chưa có vé nào được đăng bán!");
                    txtEmptyStateSubtitle.setText("Hãy bán lại vé để người khác có cơ hội tham gia sự kiện và bạn cũng thu về dễ dàng.");
                    btnEmptyStateAction.setText("🎟  Bán lại vé");
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                } else if (currentSubTabResale == SubTabResale.PENDING) {
                    txtEmptyStateTitle.setText("Không có tin nào chờ thanh toán");
                    txtEmptyStateSubtitle.setText("Bạn chưa có vé nào đang chờ thanh toán.");
                } else if (currentSubTabResale == SubTabResale.PAID) {
                    txtEmptyStateTitle.setText("Không có tin nào đã thanh toán");
                    txtEmptyStateSubtitle.setText("Bạn chưa có vé nào đã được thanh toán thành công.");
                } else if (currentSubTabResale == SubTabResale.CANCELLED) {
                    txtEmptyStateTitle.setText("Không có tin nào đã hủy");
                    txtEmptyStateSubtitle.setText("Bạn chưa có vé nào đã hủy đăng bán.");
                }
            } else if (currentMainTab == MainTab.MEMBERSHIP) {
                txtEmptyStateTitle.setText("Không có thẻ thành viên");
                txtEmptyStateSubtitle.setText("Các chương trình thành viên VIP đang được chuẩn bị ra mắt!");
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
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, EventDetailFragment.newInstance(event.getId()))
                    .addToBackStack("home")
                    .commit();
        });

        rvEmptyRecommendationsList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEmptyRecommendationsList.setAdapter(suggestionsAdapter);
    }
}
