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
        loadMockData();
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
                if (getActivity() instanceof com.example.vibetix.Activities.UserMainActivity) {
                    ((com.example.vibetix.Activities.UserMainActivity) getActivity()).openSearchFragment();
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

    private void loadMockData() {
        // Mock Categories (from ERD: CATEGORY entity — id, name, slug, iconResId, iconBgColorResId)
        danhSachCategory.add(new Category("c1", getString(R.string.str_live_music), "live-music", R.drawable.ic_cat_music, R.color.clr_cat_music));
        danhSachCategory.add(new Category("c2", getString(R.string.str_stage_arts), "stage-arts", R.drawable.ic_cat_arts, R.color.clr_cat_arts));
        danhSachCategory.add(new Category("c3", getString(R.string.str_workshop), "workshop", R.drawable.ic_cat_workshop, R.color.clr_cat_workshop));
        danhSachCategory.add(new Category("c4", getString(R.string.str_tour_experience), "tour", R.drawable.ic_cat_tour, R.color.clr_cat_tour));
        danhSachCategory.add(new Category("c5", getString(R.string.str_sports), "sports", R.drawable.ic_cat_sports, R.color.clr_cat_sports));
        danhSachCategory.add(new Category("c6", getString(R.string.str_cat_festival), "festival", R.drawable.ic_cat_festival, R.color.clr_cat_festival));

        // Mock Banners (ERD: EVENT.banner_url for landscape, EVENT.status, VENUE.city)
        Event b1 = new Event("b1", "VinhVerse - Đêm nhạc thế kỷ", null, "19/05/2025", "TP.HCM", "ongoing", 500000);
        b1.setLocalImageResId(R.drawable.banner_vinh_verse);
        b1.setVenueCity("TP.Hồ Chí Minh");
        b1.setInterestCount(8200);
        b1.setFeatured(true);
        danhSachBanner.add(b1);

        Event b2 = new Event("b2", "Private Show in Fantasy", null, "25/05/2025", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 800000);
        b2.setLocalImageResId(R.drawable.banner_private_show_fantasy);
        b2.setVenueCity("Hà Nội");
        b2.setInterestCount(5400);
        b2.setFeatured(true);
        danhSachBanner.add(b2);

        Event b3 = new Event("b3", "Hòa nhạc Non Sông 2025", null, "01/06/2025", "Đà Nẵng", Constants.EVENT_STATUS_PUBLISHED, 350000);
        b3.setLocalImageResId(R.drawable.banner_non_song);
        b3.setVenueCity("Đà Nẵng");
        b3.setInterestCount(3100);
        b3.setFeatured(true);
        danhSachBanner.add(b3);

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

        // Mock Featured Events (EVENT.is_featured = true)
        // localPortraitImageResId = portrait poster for Featured section
        // localImageResId         = landscape banner (used in other sections)
        // localPortraitImageResId = ảnh poster dọc hiển thị trong mục "Sự kiện đặc biệt".
        // e1 & e2 dùng ảnh poster thật (dọc). e3 & e4 tạm dùng ảnh ngang cho đến khi có poster riêng.
        Event e1 = new Event("e1", "Mừng Ngày Hội Non Sông - Võ Hà Trâm", null, "01/05/2026", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 350000);
        e1.setLocalImageResId(R.drawable.event_live_non_song);
        e1.setLocalPortraitImageResId(R.drawable.event_live_non_song_2); // ảnh poster dọc 194×259
        e1.setVenueCity("TP.Hồ Chí Minh"); e1.setInterestCount(4200); e1.setFeatured(true);
        danhSachFeatured.add(e1);

        Event e2 = new Event("e2", "Private Show in Fantasy - Quốc Thiên", null, "16/05/2026", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 800000);
        e2.setLocalImageResId(R.drawable.event_arts_private_fantasy);
        e2.setLocalPortraitImageResId(R.drawable.banner_private_show_fantasy); // ảnh poster dọc
        e2.setVenueCity("Hà Nội"); e2.setInterestCount(5400); e2.setFeatured(true);
        danhSachFeatured.add(e2);

        Event e3 = new Event("e3", "BẰNG KIỀU - CÒN MƯA NGANG QUA", null, "19/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 350000);
        e3.setLocalImageResId(R.drawable.event_featured_bang_kieu);
        e3.setLocalPortraitImageResId(R.drawable.event_featured_bang_kieu); // tạm dùng ảnh ngang
        e3.setVenueCity("TP.Hồ Chí Minh"); e3.setInterestCount(3100); e3.setFeatured(true);
        danhSachFeatured.add(e3);

        Event e4 = new Event("e4", "Vì Lý Do Đời - Mr. Siro", null, "01/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 600000);
        e4.setLocalImageResId(R.drawable.event_featured_vi_ly_doi);
        e4.setLocalPortraitImageResId(R.drawable.event_featured_vi_ly_doi); // tạm dùng ảnh ngang
        e4.setVenueCity("TP.Hồ Chí Minh"); e4.setInterestCount(3900); e4.setFeatured(true);
        danhSachFeatured.add(e4);

        // Mock Trending Events
        Event t1 = new Event("t1", "FWS SEA 2025", null, "15/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 1200000);
        t1.setLocalImageResId(R.drawable.event_trending_fws_sea);
        t1.setVenueCity("TP.Hồ Chí Minh"); t1.setInterestCount(12500);
        danhSachTrending.add(t1);

        Event t2 = new Event("t2", "RISING FEST 2025", null, "22/06/2025", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 900000);
        t2.setLocalImageResId(R.drawable.event_trending_rising_fest);
        t2.setVenueCity("Hà Nội"); t2.setInterestCount(9800);
        danhSachTrending.add(t2);

        Event t3 = new Event("t3", "Ultra Vietnam 2025", null, "12/07/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 1500000);
        t3.setLocalImageResId(R.drawable.event_trending_rising_2);
        t3.setVenueCity("TP.Hồ Chí Minh"); t3.setInterestCount(15200);
        danhSachTrending.add(t3);

        // Mock Resale Tickets (vé bán lại — soldOut=true để hiện badge "Bán lại")
        Event rs1 = new Event("rs1", "BẰNG KIỀU - CÒN MƯA NGANG QUA", null, "19/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 420000);
        rs1.setLocalImageResId(R.drawable.event_featured_bang_kieu);
        rs1.setVenueCity("TP.Hồ Chí Minh"); rs1.setInterestCount(4200); rs1.setSoldOut(true);
        danhSachResale.add(rs1);

        Event rs2 = new Event("rs2", "The Story Concert", null, "27/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 580000);
        rs2.setLocalImageResId(R.drawable.event_featured_the_story);
        rs2.setVenueCity("TP.Hồ Chí Minh"); rs2.setInterestCount(6500); rs2.setSoldOut(true);
        danhSachResale.add(rs2);

        Event rs3 = new Event("rs3", "FWS SEA 2025", null, "15/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 1350000);
        rs3.setLocalImageResId(R.drawable.event_trending_fws_sea);
        rs3.setVenueCity("TP.Hồ Chí Minh"); rs3.setInterestCount(12500); rs3.setSoldOut(true);
        danhSachResale.add(rs3);

        Event rs4 = new Event("rs4", "Ultra Vietnam 2025", null, "12/07/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 1650000);
        rs4.setLocalImageResId(R.drawable.event_trending_rising_2);
        rs4.setVenueCity("TP.Hồ Chí Minh"); rs4.setInterestCount(15200); rs4.setSoldOut(true);
        danhSachResale.add(rs4);

        // Mock Live Music
        Event lm1 = new Event("lm1", "Đêm nhạc Trịnh Công Sơn", null, "28/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 150000);
        lm1.setLocalImageResId(R.drawable.event_live_concert_1);
        lm1.setVenueCity("TP.Hồ Chí Minh"); lm1.setInterestCount(2300);
        danhSachLiveMusic.add(lm1);

        Event lm2 = new Event("lm2", "NON SONG Live Show", null, "05/06/2025", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 250000);
        lm2.setLocalImageResId(R.drawable.event_live_non_song);
        lm2.setVenueCity("Hà Nội"); lm2.setInterestCount(4700);
        danhSachLiveMusic.add(lm2);

        Event lm3 = new Event("lm3", "Jazz Night HCM", null, "10/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 300000);
        lm3.setLocalImageResId(R.drawable.event_live_non_song_2);
        lm3.setVenueCity("TP.Hồ Chí Minh"); lm3.setInterestCount(1600);
        danhSachLiveMusic.add(lm3);

        Event lm4 = new Event("lm4", "Live Concert Vol.2", null, "18/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 400000);
        lm4.setLocalImageResId(R.drawable.event_live_concert_2);
        lm4.setVenueCity("TP.Hồ Chí Minh"); lm4.setInterestCount(2100);
        danhSachLiveMusic.add(lm4);

        // Mock Stage & Arts
        Event sa1 = new Event("sa1", "Chèo Đất Việt", null, "30/05/2025", "Hà Nội", Constants.EVENT_STATUS_PUBLISHED, 100000);
        sa1.setLocalImageResId(R.drawable.event_arts_traditional);
        sa1.setVenueCity("Hà Nội"); sa1.setInterestCount(890);
        danhSachStageArts.add(sa1);

        Event sa2 = new Event("sa2", "Opera Night 2025", null, "07/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 450000);
        sa2.setLocalImageResId(R.drawable.event_arts_concert);
        sa2.setVenueCity("TP.Hồ Chí Minh"); sa2.setInterestCount(3200);
        danhSachStageArts.add(sa2);

        Event sa3 = new Event("sa3", "Private Show in Fantasy", null, "14/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 700000);
        sa3.setLocalImageResId(R.drawable.event_arts_private_fantasy);
        sa3.setVenueCity("TP.Hồ Chí Minh"); sa3.setInterestCount(5100);
        danhSachStageArts.add(sa3);

        Event sa4 = new Event("sa4", "Quốc Thiên Live", null, "21/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 300000);
        sa4.setLocalImageResId(R.drawable.event_arts_quoc_thien);
        sa4.setVenueCity("TP.Hồ Chí Minh"); sa4.setInterestCount(1400);
        danhSachStageArts.add(sa4);

        // Mock Workshop
        Event ws1 = new Event("ws1", "GSTAR SUMMIT 2025", null, "25/05/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 500000);
        ws1.setLocalImageResId(R.drawable.event_ws_gstar_summit);
        ws1.setVenueCity("TP.Hồ Chí Minh"); ws1.setInterestCount(2800);
        danhSachWorkshop.add(ws1);

        Event ws2 = new Event("ws2", "Workshop Âm nhạc & Nghệ thuật", null, "01/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 100000);
        ws2.setLocalImageResId(R.drawable.event_ws_concert);
        ws2.setVenueCity("TP.Hồ Chí Minh"); ws2.setInterestCount(750);
        danhSachWorkshop.add(ws2);

        // Mock Tour
        Event tr1 = new Event("tr1", "Tour Di tích Văn Hóa - Vân Mộc", null, "29/05/2025", "Quảng Bình", Constants.EVENT_STATUS_PUBLISHED, 350000);
        tr1.setLocalImageResId(R.drawable.event_tour_mountain);
        tr1.setVenueCity("Quảng Bình"); tr1.setInterestCount(1200);
        danhSachTour.add(tr1);

        Event tr2 = new Event("tr2", "Trải nghiệm suối nước nóng Đà Lạt", null, "10/06/2025", "Đà Lạt", Constants.EVENT_STATUS_PUBLISHED, 200000);
        tr2.setLocalImageResId(R.drawable.event_tour_outdoor);
        tr2.setVenueCity("Đà Lạt"); tr2.setInterestCount(980);
        danhSachTour.add(tr2);

        // Mock Sports
        Event sp1 = new Event("sp1", "FWS SEA 2025 - Finals", null, "20/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 150000);
        sp1.setLocalImageResId(R.drawable.event_sports_esport);
        sp1.setVenueCity("TP.Hồ Chí Minh"); sp1.setInterestCount(7600);
        sp1.setSoldOut(true);
        danhSachSports.add(sp1);

        Event sp2 = new Event("sp2", "THE GLOBAL CHAMPIONSHIP 2025", null, "25/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 200000);
        sp2.setLocalImageResId(R.drawable.event_sports_global_champ);
        sp2.setVenueCity("TP.Hồ Chí Minh"); sp2.setInterestCount(11000);
        danhSachSports.add(sp2);

        Event sp3 = new Event("sp3", "Playoffs 2025", null, "15/06/2025", "TP.HCM", Constants.EVENT_STATUS_PUBLISHED, 100000);
        sp3.setLocalImageResId(R.drawable.event_sports_playoffs);
        sp3.setVenueCity("TP.Hồ Chí Minh"); sp3.setInterestCount(4300);
        danhSachSports.add(sp3);

        // Mock Destinations
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

        Destination d6 = new Destination("d6", "Huế", null, 20);
        danhSachDestination.add(d6);

        setupAdapters();
        setupBannerDots();
        startBannerAutoScroll();
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
        // TODO: navigate to EventDetailFragment and pass event.getId()
        Toast.makeText(requireContext(), event.getTitle(), Toast.LENGTH_SHORT).show();
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
