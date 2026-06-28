package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Utils.NotificationPopupHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.vibetix.Adapters.BannerAdapter;
import com.example.vibetix.Adapters.CategoryAdapter;
import com.example.vibetix.Adapters.DestinationAdapter;
import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Adapters.FeaturedEventAdapter;
import com.example.vibetix.Adapters.FeaturedStarAdapter;
import com.example.vibetix.Adapters.ResaleEventAdapter;
import com.example.vibetix.Adapters.TrendingEventAdapter;
import com.example.vibetix.Fragments.User.EventDetailFragment;
import com.example.vibetix.Models.Category;
import com.example.vibetix.Models.Destination;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.FeaturedStar;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Firebase.HomepageLoader;
import com.example.vibetix.Repositories.TicketRepository;
import com.example.vibetix.Utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // UI Views
    LinearLayout layoutSearchBar;
    LinearLayout layoutBannerDots;
    LinearLayout layoutResaleBanner;
    LinearLayout rowHeaderDefault;
    LinearLayout rowSearchExpanded;
    ImageView imvPromoBanner;

    RecyclerView rvCategories;
    ViewPager2 vpBanner;
    RecyclerView rvFeaturedStars;
    RecyclerView rvFeaturedEvents;
    RecyclerView rvTrendingEvents;
    RecyclerView rvResaleEvents;
    RecyclerView rvLiveMusic;
    RecyclerView rvStageArts;
    RecyclerView rvWorkshop;
    RecyclerView rvTourExperience;
    RecyclerView rvSports;
    RecyclerView rvDestinations;

    // Section header views
    View headerFeaturedEvents;
    View headerTrendingEvents;
    View headerLiveMusic;
    View headerStageArts;
    View headerWorkshop;
    View headerTourExperience;
    View headerSports;
    View headerDestinations;

    // Adapters
    CategoryAdapter categoryAdapter;
    BannerAdapter bannerAdapter;
    FeaturedStarAdapter featuredStarAdapter;
    FeaturedEventAdapter featuredEventsAdapter;
    TrendingEventAdapter trendingEventsAdapter;
    ResaleEventAdapter resaleEventsAdapter;
    EventAdapter liveMusicAdapter;
    EventAdapter stageArtsAdapter;
    EventAdapter workshopAdapter;
    EventAdapter tourExperienceAdapter;
    EventAdapter sportsAdapter;
    DestinationAdapter destinationAdapter;

    // Data
    ArrayList<Category> danhSachCategory = new ArrayList<>();
    ArrayList<Event> danhSachBanner = new ArrayList<>();
    ArrayList<FeaturedStar> danhSachStar = new ArrayList<>();
    ArrayList<Event> danhSachFeatured = new ArrayList<>();
    ArrayList<Event> danhSachTrending = new ArrayList<>();
    ArrayList<Event> danhSachResale = new ArrayList<>();
    ArrayList<Event> danhSachLiveMusic = new ArrayList<>();
    ArrayList<Event> danhSachStageArts = new ArrayList<>();
    ArrayList<Event> danhSachWorkshop = new ArrayList<>();
    ArrayList<Event> danhSachTour = new ArrayList<>();
    ArrayList<Event> danhSachSports = new ArrayList<>();
    ArrayList<Destination> danhSachDestination = new ArrayList<>();

    // Repositories
    private final TicketRepository ticketRepository = new TicketRepository();

    // Banner auto-scroll
    Handler bannerHandler = new Handler(Looper.getMainLooper());
    Runnable bannerRunnable;
    private static final long BANNER_DELAY_MS = 3000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);
        addViews(view);
        addEvents(view);
        loadMockData();  // Setup adapters + stars/destinations + banner dots
        return view;
    }

    private void addViews(View view) {
        // Apply ONLY the status-bar top inset to the header background container.
        // We do this in code (not fitsSystemWindows="true" on the XML LinearLayout)
        // because fitsSystemWindows on a LinearLayout applies ALL insets including
        // paddingBottom = gesture-bar height, which creates an ugly blue gap below
        // the header row.  Here we only want paddingTop = status-bar height.
        View headerHome = view.findViewById(R.id.headerHome);
        if (headerHome != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerHome, (v, windowInsets) -> {
                int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, topInset, 0, 0);
                return windowInsets; // pass insets through so other views also receive them
            });
        }

        layoutSearchBar = view.findViewById(R.id.layoutSearchBar);
        rowHeaderDefault = view.findViewById(R.id.rowHeaderDefault);
        rowSearchExpanded = view.findViewById(R.id.rowSearchExpanded);
        layoutBannerDots = view.findViewById(R.id.layoutBannerDots);
        layoutResaleBanner = view.findViewById(R.id.layoutResaleBanner);
        imvPromoBanner   = view.findViewById(R.id.imvPromoBanner);

        // rvCategories section removed from layout to match Figma; null-check in setupAdapters() handles this
        vpBanner = view.findViewById(R.id.vpBanner);
        rvFeaturedStars = view.findViewById(R.id.rvFeaturedStars);
        rvFeaturedEvents = view.findViewById(R.id.rvFeaturedEvents);
        rvTrendingEvents = view.findViewById(R.id.rvTrendingEvents);
        rvResaleEvents = view.findViewById(R.id.rvResaleEvents);
        rvLiveMusic = view.findViewById(R.id.rvLiveMusic);
        rvStageArts = view.findViewById(R.id.rvStageArts);
        rvWorkshop = view.findViewById(R.id.rvWorkshop);
        rvTourExperience = view.findViewById(R.id.rvTourExperience);
        rvSports = view.findViewById(R.id.rvSports);
        rvDestinations = view.findViewById(R.id.rvDestinations);

        headerFeaturedEvents = view.findViewById(R.id.headerFeaturedEvents);
        headerTrendingEvents = view.findViewById(R.id.headerTrendingEvents);
        headerLiveMusic = view.findViewById(R.id.headerLiveMusic);
        headerStageArts = view.findViewById(R.id.headerStageArts);
        headerWorkshop = view.findViewById(R.id.headerWorkshop);
        headerTourExperience = view.findViewById(R.id.headerTourExperience);
        headerSports = view.findViewById(R.id.headerSports);
        headerDestinations = view.findViewById(R.id.headerDestinations);

        // Set section header titles
        setSectionTitle(headerFeaturedEvents, getString(R.string.str_featured_events));
        setSectionTitle(headerTrendingEvents, getString(R.string.str_trending_events));
        setSectionTitle(headerLiveMusic, getString(R.string.str_live_music));
        setSectionTitle(headerStageArts, getString(R.string.str_stage_arts));
        setSectionTitle(headerWorkshop, getString(R.string.str_workshop));
        setSectionTitle(headerTourExperience, getString(R.string.str_tour_experience));
        setSectionTitle(headerSports, getString(R.string.str_sports));
        setSectionTitle(headerDestinations, getString(R.string.str_popular_destinations));
    }

    private void setSectionTitle(View headerView, String title) {
        if (headerView != null) {
            TextView txtTitle = headerView.findViewById(R.id.txtSectionTitle);
            if (txtTitle != null) txtTitle.setText(title);
        }
    }

    private void addEvents(View view) {
        // Search icon → expand search bar; ✕ button → collapse back to default
        layoutSearchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSearchFragment();
                }
            }
        });
        View btnSearchBack = view.findViewById(R.id.btnSearchBack);
        if (btnSearchBack != null) {
            btnSearchBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (rowSearchExpanded != null) rowSearchExpanded.setVisibility(View.GONE);
                    if (rowHeaderDefault != null) rowHeaderDefault.setVisibility(View.VISIBLE);
                }
            });
        }

        // Resale banner click (background tap)
        layoutResaleBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), getString(R.string.str_resale_ticket), Toast.LENGTH_SHORT).show();
            }
        });

        // "Xem thêm ›" link inside the resale panel
        View txtResaleSeeMore = view.findViewById(R.id.txtResaleSeeMore);
        if (txtResaleSeeMore != null) {
            txtResaleSeeMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(requireContext(), getString(R.string.str_resale_ticket), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Bell (notification) click → show notification popup
        View imgBell = view.findViewById(R.id.imgBtnNotification);
        if (imgBell != null) {
            imgBell.setOnClickListener(v ->
                    NotificationPopupHelper.show(requireContext(), v));
        }

        // Promo banner click → show popup chi tiết
        imvPromoBanner.setOnClickListener(v -> showPromoDialog());

        // See more clicks
        setSeeMoreClick(headerFeaturedEvents, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerTrendingEvents, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerLiveMusic, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerStageArts, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerWorkshop, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerTourExperience, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerSports, Constants.EVENT_STATUS_PUBLISHED);
        setSeeMoreClick(headerDestinations, Constants.EVENT_STATUS_PUBLISHED);

        // Banner auto-scroll
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (danhSachBanner.isEmpty()) return;
                int nextItem = (vpBanner.getCurrentItem() + 1) % danhSachBanner.size();
                vpBanner.setCurrentItem(nextItem, true);
                bannerHandler.postDelayed(this, BANNER_DELAY_MS);
            }
        };

        vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBannerDots(position);
            }
        });
    }

    private void setSeeMoreClick(View headerView, final String category) {
        if (headerView == null) return;
        TextView txtSeeMore = headerView.findViewById(R.id.txtSeeMore);
        if (txtSeeMore != null) {
            txtSeeMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: navigate to SearchFragment với category filter
                    Toast.makeText(requireContext(), getString(R.string.str_see_more), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadMockData() {
        // Mock Categories (from ERD: CATEGORY entity — id, name, slug, iconResId, iconBgColorResId)
        danhSachCategory.add(new Category("c1", getString(R.string.str_live_music), "live-music", R.drawable.ic_cat_music, R.color.clr_cat_music));
        danhSachCategory.add(new Category("c2", getString(R.string.str_stage_arts), "stage-arts", R.drawable.ic_cat_arts, R.color.clr_cat_arts));
        danhSachCategory.add(new Category("c3", getString(R.string.str_workshop), "workshop", R.drawable.ic_cat_workshop, R.color.clr_cat_workshop));
        danhSachCategory.add(new Category("c4", getString(R.string.str_tour_experience), "tour", R.drawable.ic_cat_tour, R.color.clr_cat_tour));
        danhSachCategory.add(new Category("c5", getString(R.string.str_sports), "sports", R.drawable.ic_cat_sports, R.color.clr_cat_sports));
        danhSachCategory.add(new Category("c6", getString(R.string.str_cat_festival), "festival", R.drawable.ic_cat_festival, R.color.clr_cat_festival));

        // Banner, Featured, Trending → load từ Firebase (không hardcode)
        // Mock Featured Stars
        FeaturedStar s1 = new FeaturedStar("s1", "Sơn Tùng M-TP", null);
        s1.setLocalImageResId(R.drawable.star_son_tung);
        danhSachStar.add(s1);

        FeaturedStar s2 = new FeaturedStar("s2", "Hà Anh Tuấn", null);
        s2.setLocalImageResId(R.drawable.star_ha_anh_tuan);
        danhSachStar.add(s2);

        FeaturedStar s3 = new FeaturedStar("s3", "Mỹ Tâm", null);
        s3.setLocalImageResId(R.drawable.star_my_tam);
        danhSachStar.add(s3);

        FeaturedStar s4 = new FeaturedStar("s4", "Hoàng Dũng", null);
        s4.setLocalImageResId(R.drawable.star_hoang_dung);
        danhSachStar.add(s4);

        FeaturedStar s5 = new FeaturedStar("s5", "Tăng Duy Tân", null);
        s5.setLocalImageResId(R.drawable.star_tang_duy_tan);
        danhSachStar.add(s5);

        FeaturedStar s6 = new FeaturedStar("s6", "Bảo Thy", null);
        s6.setLocalImageResId(R.drawable.star_bao_thy);
        danhSachStar.add(s6);

        FeaturedStar s7 = new FeaturedStar("s7", "Đen Vâu", null);
        s7.setLocalImageResId(R.drawable.star_den_vau);
        danhSachStar.add(s7);

        FeaturedStar s8 = new FeaturedStar("s8", "Soobin Hoàng Sơn", null);
        s8.setLocalImageResId(R.drawable.star_soobin_hoang_son);
        danhSachStar.add(s8);

        FeaturedStar s9 = new FeaturedStar("s9", "Noo Phước Thịnh", null);
        s9.setLocalImageResId(R.drawable.star_noo_phuoc_thinh);
        danhSachStar.add(s9);

        // ── Events sections (Featured/Trending/Category) load từ Firebase ──────
        // Không hardcode mock events → sections trống ban đầu, Firebase fill vào
        // (xem loadEventsFromFirestore() bên dưới)

        // ── VÉ BÁN LẠI (Mock) ─────────────────────────────────────────────────
        // Firebase chưa có data: tickets collection trống, orders không có status "reselling"
        // Giữ mock để UI hiển thị. Khi teammate bổ sung data thật → TicketRepository.getAllResellingTickets() sẽ tự load
        Event rs1 = new Event("rs1", "NON SONG Live Show", null, "05/06/2026", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 420000);
        rs1.setLocalImageResId(R.drawable.event_live_non_song); rs1.setVenueCity("TP.Hồ Chí Minh"); rs1.setSoldOut(true);
        danhSachResale.add(rs1);

        Event rs2 = new Event("rs2", "Private Show in Fantasy", null, "16/05/2026", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 650000);
        rs2.setLocalImageResId(R.drawable.event_arts_private_fantasy); rs2.setVenueCity("Hà Nội"); rs2.setSoldOut(true);
        danhSachResale.add(rs2);

        Event rs3 = new Event("rs3", "FORESTIVAL CHIẾN BINH BÌNH MINH", null, "20/07/2026", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 900000);
        rs3.setLocalImageResId(R.drawable.event_trending_rising_fest); rs3.setVenueCity("TP.Hồ Chí Minh"); rs3.setSoldOut(true);
        danhSachResale.add(rs3);

        // Destinations
        Destination d1 = new Destination("d1", "TP.Hồ Chí Minh", null, 120);
        d1.setLocalImageResId(R.drawable.destination_hcmc);
        danhSachDestination.add(d1);

        Destination d2 = new Destination("d2", "Hà Nội", null, 89);
        d2.setLocalImageResId(R.drawable.destination_hanoi);
        danhSachDestination.add(d2);

        Destination d3 = new Destination("d3", "Đà Nẵng", null, 45);
        d3.setLocalImageResId(R.drawable.destination_da_nang);
        danhSachDestination.add(d3);

        Destination d4 = new Destination("d4", "Hội An", null, 32);
        d4.setLocalImageResId(R.drawable.destination_hoi_an);
        danhSachDestination.add(d4);

        Destination d5 = new Destination("d5", "Đà Lạt", null, 28);
        d5.setLocalImageResId(R.drawable.destination_da_lat);
        danhSachDestination.add(d5);
        // Huế đã bị xóa — không có đủ data Firestore

        setupAdapters();
        setupBannerDots();
        startBannerAutoScroll();
        fetchLiveResaleTickets();
        loadEventsFromFirestore(); // Fetch Firestore sau khi mock đã hiển thị
    }

    // ── Firestore: load category cache trước, rồi load từng section ──────────
    private void loadEventsFromFirestore() {
        // Bước 1: Load categories + venues song song
        FirestoreHelper.loadCategoryCache(() -> {
            // Bước 2: Sau khi category cache sẵn, load venues + events
            FirestoreHelper.loadEvents(new FirestoreHelper.OnEventsLoaded() {
                @Override public void onSuccess(java.util.List<Event> ignored) {
                    if (!isAdded()) return;
                    loadBannerSection();
                    loadFeaturedSection();
                    loadTrendingSection();
                    // Dùng getCatId() để lấy UUID thật từ cache
                    loadCategorySection(HomepageLoader.getCatId("music"),    danhSachLiveMusic,   liveMusicAdapter);
                    loadCategorySection(HomepageLoader.getCatId("arts"),     danhSachStageArts,   stageArtsAdapter);
                    loadCategorySection(HomepageLoader.getCatId("workshop"), danhSachWorkshop,    workshopAdapter);
                    loadCategorySection(HomepageLoader.getCatId("tour"),     danhSachTour,        tourExperienceAdapter);
                    loadCategorySection(HomepageLoader.getCatId("sports"),   danhSachSports,      sportsAdapter);
                }
                @Override public void onFailure(Exception e) {}
            });
        });
    }

    /** Banner: admin-controlled qua app_config/homepage, fallback is_featured */
    private void loadBannerSection() {
        HomepageLoader.loadBanners(events -> {
            if (!isAdded() || events.isEmpty()) return;
            danhSachBanner.clear();
            danhSachBanner.addAll(events);
            if (bannerAdapter != null) bannerAdapter.notifyDataSetChanged();
            setupBannerDots();
        });
    }

    /** Featured: is_featured=true, sắp diễn ra */
    private void loadFeaturedSection() {
        HomepageLoader.loadFeatured(events -> {
            if (!isAdded() || events.isEmpty()) return;
            danhSachFeatured.clear();
            danhSachFeatured.addAll(events);
            if (featuredEventsAdapter != null) featuredEventsAdapter.notifyDataSetChanged();
        });
    }

    /** Trending: xếp hạng theo vé bán (order_items), fallback interest_count */
    private void loadTrendingSection() {
        HomepageLoader.loadTrending(events -> {
            if (!isAdded() || events.isEmpty()) return;
            danhSachTrending.clear();
            danhSachTrending.addAll(events);
            if (trendingEventsAdapter != null) trendingEventsAdapter.notifyDataSetChanged();
        });
    }

    /** Category: nếu Firestore có data → replace mock; nếu trống → giữ mock */
    private void loadCategorySection(String categoryId,
                                     java.util.ArrayList<Event> list,
                                     com.example.vibetix.Adapters.EventAdapter adapter) {
        HomepageLoader.loadByCategory(categoryId, events -> {
            if (!isAdded() || events.isEmpty()) return; // giữ mock
            list.clear();
            list.addAll(events);
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void setupAdapters() {
        // Categories — rvCategories may be absent from layout (section hidden to match Figma)
        categoryAdapter = new CategoryAdapter(requireContext(), danhSachCategory,
                category -> Toast.makeText(requireContext(), category.getName(), Toast.LENGTH_SHORT).show());
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvCategories.setAdapter(categoryAdapter);
        }

        // Banner
        bannerAdapter = new BannerAdapter(requireContext(), danhSachBanner,
                event -> Toast.makeText(requireContext(), event.getTitle(), Toast.LENGTH_SHORT).show());
        vpBanner.setAdapter(bannerAdapter);

        // Featured Stars
        featuredStarAdapter = new FeaturedStarAdapter(requireContext(), danhSachStar,
                star -> Toast.makeText(requireContext(), star.getName(), Toast.LENGTH_SHORT).show());
        rvFeaturedStars.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedStars.setAdapter(featuredStarAdapter);

        // Featured Events — portrait poster only (no text), uses FeaturedEventAdapter
        featuredEventsAdapter = new FeaturedEventAdapter(requireContext(), danhSachFeatured,
                event -> onEventClick(event));
        rvFeaturedEvents.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedEvents.setAdapter(featuredEventsAdapter);

        // Trending Events — landscape image + rank number overlay, uses TrendingEventAdapter
        trendingEventsAdapter = new TrendingEventAdapter(requireContext(), danhSachTrending,
                event -> onEventClick(event));
        rvTrendingEvents.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingEvents.setAdapter(trendingEventsAdapter);

        // Resale Tickets — uses dedicated ResaleEventAdapter (item_resale_card layout)
        resaleEventsAdapter = new ResaleEventAdapter(requireContext(), danhSachResale,
                event -> onEventClick(event));
        rvResaleEvents.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvResaleEvents.setAdapter(resaleEventsAdapter);

        // Live Music
        liveMusicAdapter = new EventAdapter(requireContext(), danhSachLiveMusic,
                event -> onEventClick(event));
        rvLiveMusic.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvLiveMusic.setAdapter(liveMusicAdapter);

        // Stage & Arts
        stageArtsAdapter = new EventAdapter(requireContext(), danhSachStageArts,
                event -> onEventClick(event));
        rvStageArts.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStageArts.setAdapter(stageArtsAdapter);

        // Workshop
        workshopAdapter = new EventAdapter(requireContext(), danhSachWorkshop,
                event -> onEventClick(event));
        rvWorkshop.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvWorkshop.setAdapter(workshopAdapter);

        // Tour & Experience
        tourExperienceAdapter = new EventAdapter(requireContext(), danhSachTour,
                event -> onEventClick(event));
        rvTourExperience.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTourExperience.setAdapter(tourExperienceAdapter);

        // Sports
        sportsAdapter = new EventAdapter(requireContext(), danhSachSports,
                event -> onEventClick(event));
        rvSports.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSports.setAdapter(sportsAdapter);

        // Destinations — click → mở Search với bộ lọc thành phố tương ứng
        destinationAdapter = new DestinationAdapter(requireContext(), danhSachDestination,
                dest -> openSearchWithCity(dest.getName()));
        rvDestinations.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDestinations.setAdapter(destinationAdapter);
    }

    private void setupBannerDots() {
        layoutBannerDots.removeAllViews();
        for (int i = 0; i < danhSachBanner.size(); i++) {
            View dot = new View(requireContext());
            int size = 8;
            int margin = 4;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0
                    ? android.R.drawable.presence_online
                    : android.R.drawable.presence_invisible);
            layoutBannerDots.addView(dot);
        }
    }

    private void updateBannerDots(int activePosition) {
        for (int i = 0; i < layoutBannerDots.getChildCount(); i++) {
            View dot = layoutBannerDots.getChildAt(i);
            dot.setBackgroundResource(i == activePosition
                    ? android.R.drawable.presence_online
                    : android.R.drawable.presence_invisible);
        }
    }

    private void startBannerAutoScroll() {
        bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY_MS);
    }

    private void stopBannerAutoScroll() {
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    private void onEventClick(Event event) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, EventDetailFragment.newInstance(event.getId()))
                    .addToBackStack("home")
                    .commit();
        }
    }

    private void fetchLiveResaleTickets() {
        ticketRepository.getAllResellingTickets(new TicketRepository.OnTicketsLoadedListener() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                if (tickets != null && !tickets.isEmpty()) {
                    ArrayList<Event> liveResale = new ArrayList<>();
                    for (Ticket t : tickets) {
                        Event ev = new Event(t.getEventId(), t.getEventTitle(), null, t.getEventDate(), t.getEventLocation(), "resale", (int) t.getResalePrice());
                        ev.setSoldOut(true);
                        
                        int coverResId = R.drawable.event_live_non_song;
                        if (!"b1".equals(t.getEventId()) && !"e1".equals(t.getEventId()) && !"rs1".equals(t.getEventId())) {
                            coverResId = R.drawable.event_arts_private_fantasy;
                        }
                        ev.setLocalImageResId(coverResId);
                        liveResale.add(ev);
                    }
                    danhSachResale.addAll(0, liveResale);
                    if (resaleEventsAdapter != null) {
                        resaleEventsAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Fail silently
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBannerAutoScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBannerAutoScroll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopBannerAutoScroll();
    }

    // ── Mở Search Fragment với bộ lọc thành phố đã chọn sẵn ──────────────────
    private void openSearchWithCity(String displayName) {
        // Map tên hiển thị → keyword dùng để filter (khớp với venue_city trong Firestore)
        String cityKeyword;
        if (displayName.contains("Hồ Chí Minh"))   cityKeyword = "Hồ Chí Minh";
        else if (displayName.contains("Hà Nội"))    cityKeyword = "Hà Nội";
        else if (displayName.contains("Đà Nẵng"))   cityKeyword = "Đà Nẵng";
        else if (displayName.contains("Hội An"))    cityKeyword = "Hội An";
        else if (displayName.contains("Đà Lạt"))    cityKeyword = "Đà Lạt";
        else                                         cityKeyword = displayName;

        // Truyền keyword qua Bundle sang SearchFragment
        android.os.Bundle args = new android.os.Bundle();
        args.putString("preset_city", cityKeyword);

        SearchFragment searchFragment = new SearchFragment();
        searchFragment.setArguments(args);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainerMain, searchFragment)
                .addToBackStack("search_city")
                .commit();
        }
    }

    // ── Promo dialog ───────────────────────────────────────────────────────────
    private void showPromoDialog() {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_banner_promo_dialog, null);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Đóng
        View btnClose = dialogView.findViewById(R.id.btnPromoDialogClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Nhận ngay → mở trang Sự kiện
        View btnGo = dialogView.findViewById(R.id.btnPromoDialogGo);
        if (btnGo != null) {
            btnGo.setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openEventsFragment();
                }
            });
        }

        dialog.show();
    }
}
