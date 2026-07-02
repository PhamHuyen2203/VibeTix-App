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
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Firebase.HomepageLoader;
import com.example.vibetix.Repositories.TicketTransferRepository;
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
    private final TicketTransferRepository ticketTransferRepository = new TicketTransferRepository();

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

    /** Ẩn ProgressBar và hiện RecyclerView tương ứng khi data arrive. */
    private void hidePb(int pbId) {
        View v = getView();
        if (v == null) return;
        View pb = v.findViewById(pbId);
        if (pb != null) pb.setVisibility(View.GONE);
    }

    /** Hiện RecyclerView (chuyển từ GONE → VISIBLE) khi có data. */
    private void showRv(RecyclerView rv) {
        if (rv != null) rv.setVisibility(View.VISIBLE);
    }

    /** Ẩn TẤT CẢ progress bar — dùng khi query fail/hang để tránh spinner kẹt mãi. */
    private void hideAllSectionPbs() {
        hidePb(R.id.pbBanner);
        hidePb(R.id.pbFeaturedEvents);
        hidePb(R.id.pbTrendingEvents);
        hidePb(R.id.pbLiveMusic);
        hidePb(R.id.pbStageArts);
        hidePb(R.id.pbWorkshop);
        hidePb(R.id.pbTourExperience);
        hidePb(R.id.pbSports);
        hidePb(R.id.pbHomeInitial);
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

        // Resale banner click → mở trang Vé bán lại (marketplace)
        layoutResaleBanner.setOnClickListener(v -> openResaleMarketplace());

        // "Xem thêm ›" link inside the resale panel → mở trang Vé bán lại
        View txtResaleSeeMore = view.findViewById(R.id.txtResaleSeeMore);
        if (txtResaleSeeMore != null) {
            txtResaleSeeMore.setOnClickListener(v -> openResaleMarketplace());
        }

        // Bell (notification) click → show notification popup
        View imgBell = view.findViewById(R.id.imgBtnNotification);
        if (imgBell != null) {
            imgBell.setOnClickListener(v ->
                    NotificationPopupHelper.show(requireContext(), v));
        }

        // Promo banner click → show popup chi tiết
        imvPromoBanner.setOnClickListener(v -> showPromoDialog());

        // See more clicks — chuyển sang trang Events với category tương ứng
        setSeeMoreClick(headerFeaturedEvents, "all");
        setSeeMoreClick(headerTrendingEvents, "all");
        setSeeMoreClick(headerLiveMusic, "music");
        setSeeMoreClick(headerStageArts, "arts");
        setSeeMoreClick(headerWorkshop, "workshop");
        setSeeMoreClick(headerTourExperience, "tour");
        setSeeMoreClick(headerSports, "sports");
        setSeeMoreClick(headerDestinations, "all");

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

    private void setSeeMoreClick(View headerView, final String categoryKey) {
        if (headerView == null) return;
        TextView txtSeeMore = headerView.findViewById(R.id.txtSeeMore);
        if (txtSeeMore != null) {
            txtSeeMore.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity())
                            .openEventsFragmentWithFilter(categoryKey, null);
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

        // Stars + Resale + Destinations → load/derive từ Firebase (không hardcode)

        setupAdapters();
        setupBannerDots();
        startBannerAutoScroll();
        loadFeaturedStars();      // Nghệ sĩ nổi bật từ collection stars
        fetchLiveResaleTickets(); // Vé bán lại từ ticket_transfers (pending)
        loadEventsFromFirestore();
    }

    /** Hiện spinner cho các section trước khi bắt đầu tải dữ liệu. */
    private void showAllSectionPbs() {
        View v = getView();
        if (v == null) return;
        int[] ids = { R.id.pbBanner, R.id.pbFeaturedEvents, R.id.pbTrendingEvents,
                R.id.pbLiveMusic, R.id.pbStageArts, R.id.pbWorkshop,
                R.id.pbTourExperience, R.id.pbSports };
        for (int id : ids) {
            View pb = v.findViewById(id);
            if (pb != null) pb.setVisibility(View.VISIBLE);
        }
    }

    // ── Firestore: load category cache trước, rồi load từng section ──────────
    private void loadEventsFromFirestore() {
        showAllSectionPbs();
        // Bước 1: Load categories + venues song song
        FirestoreHelper.loadCategoryCache(() -> {
            // Bước 2: Sau khi category cache sẵn, load venues + events
            FirestoreHelper.loadEvents(new FirestoreHelper.OnEventsLoaded() {
                @Override public void onSuccess(java.util.List<Event> allEvents) {
                    if (!isAdded()) return;
                    buildDestinationsFromEvents(allEvents);
                    loadBannerSection();
                    loadFeaturedSection();
                    loadTrendingSection();
                    // Dùng getCatId() để lấy UUID thật từ cache
                    loadCategorySection(HomepageLoader.getCatId("music"),    danhSachLiveMusic,   liveMusicAdapter,      R.id.pbLiveMusic,      rvLiveMusic);
                    loadCategorySection(HomepageLoader.getCatId("arts"),     danhSachStageArts,   stageArtsAdapter,      R.id.pbStageArts,      rvStageArts);
                    loadCategorySection(HomepageLoader.getCatId("workshop"), danhSachWorkshop,    workshopAdapter,       R.id.pbWorkshop,       rvWorkshop);
                    loadCategorySection(HomepageLoader.getCatId("tour"),     danhSachTour,        tourExperienceAdapter, R.id.pbTourExperience, rvTourExperience);
                    loadCategorySection(HomepageLoader.getCatId("sports"),   danhSachSports,      sportsAdapter,         R.id.pbSports,         rvSports);
                }
                @Override public void onFailure(Exception e) {
                    if (isAdded()) hideAllSectionPbs();
                }
            });
        });
        // Safety: nếu mạng treo > 10s → ẩn hết spinner để UI không bị kẹt
        bannerHandler.postDelayed(() -> { if (isAdded()) hideAllSectionPbs(); }, 10000);
    }

    /** Banner: admin-controlled qua app_config/homepage, fallback is_featured */
    private void loadBannerSection() {
        HomepageLoader.loadBanners(events -> {
            if (!isAdded()) return;
            hidePb(R.id.pbBanner);
            if (events.isEmpty()) return;
            danhSachBanner.clear();
            danhSachBanner.addAll(events);
            showRv(null); // banner dùng ViewPager2, luôn visible
            if (vpBanner != null) vpBanner.setVisibility(View.VISIBLE);
            if (bannerAdapter != null) bannerAdapter.notifyDataSetChanged();
            setupBannerDots();
        });
    }

    /** Featured: is_featured=true, sắp diễn ra */
    private void loadFeaturedSection() {
        HomepageLoader.loadFeatured(events -> {
            if (!isAdded()) return;
            hidePb(R.id.pbFeaturedEvents);
            if (events.isEmpty()) return;
            danhSachFeatured.clear();
            danhSachFeatured.addAll(events);
            showRv(rvFeaturedEvents);
            if (featuredEventsAdapter != null) featuredEventsAdapter.notifyDataSetChanged();
        });
    }

    /** Trending: xếp hạng theo vé bán (order_items), fallback interest_count */
    private void loadTrendingSection() {
        HomepageLoader.loadTrending(events -> {
            if (!isAdded()) return;
            hidePb(R.id.pbTrendingEvents);
            if (events.isEmpty()) return;
            danhSachTrending.clear();
            danhSachTrending.addAll(events);
            showRv(rvTrendingEvents);
            if (trendingEventsAdapter != null) trendingEventsAdapter.notifyDataSetChanged();
        });
    }

    /** Category: nếu Firestore có data → hiện RV; nếu trống → giấu section */
    private void loadCategorySection(String categoryId,
                                     java.util.ArrayList<Event> list,
                                     com.example.vibetix.Adapters.EventAdapter adapter,
                                     int pbId,
                                     RecyclerView rv) {
        HomepageLoader.loadByCategory(categoryId, events -> {
            if (!isAdded()) return;
            hidePb(pbId);
            if (events.isEmpty()) return;
            list.clear();
            list.addAll(events);
            showRv(rv);
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
                event -> onEventClick(event));
        vpBanner.setAdapter(bannerAdapter);

        // Featured Stars
        featuredStarAdapter = new FeaturedStarAdapter(requireContext(), danhSachStar,
                star -> {
                    if (getActivity() != null) {
                        StarDetailFragment frag = StarDetailFragment.newInstance(star.getId(), star.getName());
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frameContainerMain, frag)
                                .addToBackStack("star_detail")
                                .commit();
                    }
                });
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

        // Resale Tickets — bấm card → mở chi tiết vé bán lại của sự kiện đó
        resaleEventsAdapter = new ResaleEventAdapter(requireContext(), danhSachResale,
                event -> openResaleEventDetail(event.getId()));
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

        // Tắt nested scrolling cho tất cả RV trong NestedScrollView
        // → wrap_content đo đúng height khi data arrive async
        RecyclerView[] rvs = { rvFeaturedStars, rvFeaturedEvents, rvTrendingEvents,
                rvResaleEvents, rvLiveMusic, rvStageArts, rvWorkshop,
                rvTourExperience, rvSports, rvDestinations };
        for (RecyclerView rv : rvs) {
            if (rv != null) rv.setNestedScrollingEnabled(false);
        }
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

    // ── Nghệ sĩ nổi bật — load từ collection stars (Firebase) ─────────────────
    private void loadFeaturedStars() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(com.example.vibetix.Firebase.FirebaseCollections.STARS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || querySnapshot.isEmpty()) return;
                    java.util.ArrayList<com.example.vibetix.Models.Star> allStars = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        com.example.vibetix.Models.Star star = doc.toObject(com.example.vibetix.Models.Star.class);
                        if (star.getStarId() == null) star.setStarId(doc.getId());
                        if (star.isActive()) allStars.add(star);
                    }
                    // Ưu tiên star có ảnh, rồi sort follower_count giảm dần
                    java.util.Collections.sort(allStars, (a, b) -> {
                        boolean aImg = a.getAvatarUrl() != null && !a.getAvatarUrl().isEmpty();
                        boolean bImg = b.getAvatarUrl() != null && !b.getAvatarUrl().isEmpty();
                        if (aImg != bImg) return aImg ? -1 : 1;
                        return Integer.compare(b.getFollowerCount(), a.getFollowerCount());
                    });
                    danhSachStar.clear();
                    int limit = Math.min(allStars.size(), 15);
                    for (int i = 0; i < limit; i++) {
                        com.example.vibetix.Models.Star s = allStars.get(i);
                        danhSachStar.add(new FeaturedStar(s.getStarId(), s.getStageName(), s.getAvatarUrl()));
                    }
                    if (featuredStarAdapter != null) featuredStarAdapter.notifyDataSetChanged();
                });
    }

    // ── Điểm đến — derive từ events thật: đếm số sự kiện theo thành phố ────────
    private void buildDestinationsFromEvents(java.util.List<Event> allEvents) {
        if (allEvents == null) return;
        java.util.LinkedHashMap<String, Integer> cityCount = new java.util.LinkedHashMap<>();
        for (Event e : allEvents) {
            String city = e.getVenueCity();
            if (city == null || city.isEmpty() || "Việt Nam".equals(city)) continue;
            cityCount.merge(city, 1, Integer::sum);
        }
        java.util.List<java.util.Map.Entry<String, Integer>> entries = new java.util.ArrayList<>(cityCount.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        danhSachDestination.clear();
        int idx = 0;
        for (java.util.Map.Entry<String, Integer> en : entries) {
            Destination d = new Destination("city_" + (idx++), en.getKey(), null, en.getValue());
            d.setLocalImageResId(cityToDrawable(en.getKey()));
            danhSachDestination.add(d);
        }
        if (destinationAdapter != null) destinationAdapter.notifyDataSetChanged();
        if (headerDestinations != null)
            headerDestinations.setVisibility(danhSachDestination.isEmpty() ? View.GONE : View.VISIBLE);
        if (rvDestinations != null)
            rvDestinations.setVisibility(danhSachDestination.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int cityToDrawable(String city) {
        if (city == null) return R.drawable.destination_hcmc;
        if (city.contains("Hồ Chí Minh") || city.contains("HCM")) return R.drawable.destination_hcmc;
        if (city.contains("Hà Nội"))  return R.drawable.destination_hanoi;
        if (city.contains("Đà Nẵng")) return R.drawable.destination_da_nang;
        if (city.contains("Hội An"))  return R.drawable.destination_hoi_an;
        if (city.contains("Đà Lạt"))  return R.drawable.destination_da_lat;
        return R.drawable.destination_hcmc;
    }

    // ── Navigation tới marketplace bán lại ───────────────────────────────────
    private void openResaleMarketplace() {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, new ResaleMarketplaceFragment())
                .addToBackStack("resale_marketplace")
                .commit();
    }

    private void openResaleEventDetail(String eventId) {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, ResaleEventDetailFragment.newInstance(eventId))
                .addToBackStack("resale_event_detail")
                .commit();
    }

    private void fetchLiveResaleTickets() {
        ticketTransferRepository.getAllPendingTransfers(new TicketTransferRepository.OnTransfersLoadedListener() {
            @Override
            public void onSuccess(List<TicketTransfer> transfers) {
                if (!isAdded()) return;
                danhSachResale.clear();
                if (transfers != null) {
                    for (TicketTransfer t : transfers) {
                        Event ev = new Event(
                                t.getEventId() != null ? t.getEventId() : t.getTransferId(),
                                t.getEventTitle(),
                                t.getEventImageUrl(),
                                t.getEventDate(),
                                t.getEventLocation(),
                                "resale",
                                t.getPrice());
                        ev.setSoldOut(true);
                        ev.setImageUrl(t.getEventImageUrl());
                        danhSachResale.add(ev);
                    }
                }
                if (resaleEventsAdapter != null) resaleEventsAdapter.notifyDataSetChanged();
                if (layoutResaleBanner != null) {
                    layoutResaleBanner.setVisibility(danhSachResale.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                if (layoutResaleBanner != null) layoutResaleBanner.setVisibility(View.GONE);
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

    // ── Mở Events Fragment với bộ lọc thành phố đã chọn sẵn ──────────────────
    private void openSearchWithCity(String displayName) {
        String cityKeyword;
        if (displayName.contains("Hồ Chí Minh"))   cityKeyword = "Hồ Chí Minh";
        else if (displayName.contains("Hà Nội"))    cityKeyword = "Hà Nội";
        else if (displayName.contains("Đà Nẵng"))   cityKeyword = "Đà Nẵng";
        else if (displayName.contains("Hội An"))    cityKeyword = "Hội An";
        else if (displayName.contains("Đà Lạt"))    cityKeyword = "Đà Lạt";
        else                                         cityKeyword = displayName;

        if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
            ((com.example.vibetix.Activities.User.UserMainActivity) getActivity())
                    .openEventsFragmentWithFilter(null, cityKeyword);
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
