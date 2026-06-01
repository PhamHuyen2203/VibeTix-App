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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.vibetix.Adapters.BannerAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.vibetix.Adapters.CategoryAdapter;
import com.example.vibetix.Adapters.DestinationAdapter;
import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Adapters.FeaturedEventAdapter;
import com.example.vibetix.Adapters.FeaturedStarAdapter;
import com.example.vibetix.Adapters.ResaleEventAdapter;
import com.example.vibetix.Adapters.TrendingEventAdapter;
import com.example.vibetix.Models.Category;
import com.example.vibetix.Models.Destination;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.FeaturedStar;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

import java.util.ArrayList;

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
        loadRealData();
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

        // Promo banner click
        imvPromoBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), getString(R.string.str_promo_discount), Toast.LENGTH_SHORT).show();
            }
        });

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

    private void loadRealData() {
        loadStaticData();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());

        db.collection(FirebaseCollections.EVENTS)
                .whereIn("status", java.util.Arrays.asList("approved", "ongoing", "APPROVED", "ONGOING"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    danhSachBanner.clear();
                    danhSachFeatured.clear();
                    danhSachTrending.clear();
                    danhSachLiveMusic.clear();
                    danhSachStageArts.clear();
                    danhSachWorkshop.clear();
                    danhSachTour.clear();
                    danhSachSports.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                if (event.getId() == null) event.setId(doc.getId());

                                // Skip expired events
                                String endTime = event.getEndTime();
                                if (endTime != null && endTime.compareTo(nowStr) < 0) {
                                    continue;
                                }

                                if (event.isFeatured() || event.getBannerUrl() != null) {
                                    if (event.isFeatured()) danhSachFeatured.add(event);
                                    danhSachBanner.add(event);
                                }

                                danhSachTrending.add(event);

                                String catId = event.getCategoryId();
                                if (catId != null) {
                                    switch (catId) {
                                        case "c1": danhSachLiveMusic.add(event); break;
                                        case "c2": danhSachStageArts.add(event); break;
                                        case "c3": danhSachWorkshop.add(event); break;
                                        case "c4": danhSachTour.add(event); break;
                                        case "c5": danhSachSports.add(event); break;
                                    }
                                }
                            }
                        }

                        // Sort trending by interest count
                        danhSachTrending.sort((e1, e2) -> Integer.compare(e2.getInterestCount(), e1.getInterestCount()));
                    }

                    setupAdapters();
                    setupBannerDots();
                    startBannerAutoScroll();
                })
                .addOnFailureListener(e -> {
                    setupAdapters();
                });
    }

    private void loadStaticData() {
        // Mock Categories
        danhSachCategory.add(new Category("c1", getString(R.string.str_live_music), "live-music", R.drawable.ic_cat_music, R.color.clr_cat_music));
        danhSachCategory.add(new Category("c2", getString(R.string.str_stage_arts), "stage-arts", R.drawable.ic_cat_arts, R.color.clr_cat_arts));
        danhSachCategory.add(new Category("c3", getString(R.string.str_workshop), "workshop", R.drawable.ic_cat_workshop, R.color.clr_cat_workshop));
        danhSachCategory.add(new Category("c4", getString(R.string.str_tour_experience), "tour", R.drawable.ic_cat_tour, R.color.clr_cat_tour));
        danhSachCategory.add(new Category("c5", getString(R.string.str_sports), "sports", R.drawable.ic_cat_sports, R.color.clr_cat_sports));
        danhSachCategory.add(new Category("c6", getString(R.string.str_cat_festival), "festival", R.drawable.ic_cat_festival, R.color.clr_cat_festival));

        // Mock Featured Stars
        FeaturedStar s1 = new FeaturedStar("s1", "Sơn Tùng M-TP", null); s1.setLocalImageResId(R.drawable.star_son_tung); danhSachStar.add(s1);
        FeaturedStar s2 = new FeaturedStar("s2", "Hà Anh Tuấn", null); s2.setLocalImageResId(R.drawable.star_ha_anh_tuan); danhSachStar.add(s2);
        FeaturedStar s3 = new FeaturedStar("s3", "Mỹ Tâm", null); s3.setLocalImageResId(R.drawable.star_my_tam); danhSachStar.add(s3);
        FeaturedStar s4 = new FeaturedStar("s4", "Hoàng Dũng", null); s4.setLocalImageResId(R.drawable.star_hoang_dung); danhSachStar.add(s4);
        FeaturedStar s5 = new FeaturedStar("s5", "Tăng Duy Tân", null); s5.setLocalImageResId(R.drawable.star_tang_duy_tan); danhSachStar.add(s5);

        // Mock Resale Tickets
        Event rs1 = new Event("rs1", "BẰNG KIỀU - CÒN MƯA NGANG QUA", null, "19/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 420000);
        rs1.setLocalImageResId(R.drawable.event_featured_bang_kieu); rs1.setVenueCity("TP.Hồ Chí Minh"); rs1.setInterestCount(4200); rs1.setSoldOut(true); danhSachResale.add(rs1);
        Event rs2 = new Event("rs2", "The Story Concert", null, "27/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 580000);
        rs2.setLocalImageResId(R.drawable.event_featured_the_story); rs2.setVenueCity("TP.Hồ Chí Minh"); rs2.setInterestCount(6500); rs2.setSoldOut(true); danhSachResale.add(rs2);
        
        // Mock Destinations
        Destination d1 = new Destination("d1", "TP.Hồ Chí Minh", null, 120); d1.setLocalImageResId(R.drawable.destination_hcmc); danhSachDestination.add(d1);
        Destination d2 = new Destination("d2", "Hà Nội", null, 89); d2.setLocalImageResId(R.drawable.destination_hanoi); danhSachDestination.add(d2);
        Destination d3 = new Destination("d3", "Đà Nẵng", null, 45); d3.setLocalImageResId(R.drawable.destination_da_nang); danhSachDestination.add(d3);
        Destination d4 = new Destination("d4", "Hội An", null, 32); d4.setLocalImageResId(R.drawable.destination_hoi_an); danhSachDestination.add(d4);
        Destination d5 = new Destination("d5", "Đà Lạt", null, 28); d5.setLocalImageResId(R.drawable.destination_da_lat); danhSachDestination.add(d5);
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

        // Destinations
        destinationAdapter = new DestinationAdapter(requireContext(), danhSachDestination,
                dest -> Toast.makeText(requireContext(), dest.getName(), Toast.LENGTH_SHORT).show());
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
        if (getActivity() != null && event.getId() != null) {
            android.content.Intent intent = new android.content.Intent(
                    getActivity(),
                    com.example.vibetix.Activities.User.EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            startActivity(intent);
        }
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
}
