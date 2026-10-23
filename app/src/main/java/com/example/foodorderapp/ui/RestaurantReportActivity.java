package com.example.foodorderapp.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RestaurantReportActivity extends AppCompatActivity {

    private String currentRestaurantId;
    private FirebaseFirestore db;

    // Top Bar
    private TextView tvRestaurantName;
    private View layoutStatusPill;
    private TextView tvRestaurantStatus;
    private View btnNotifications;
    private View btnProfile;

    // Time Tabs
    private TextView btnTabToday, btnTab7Days, btnTab30Days;
    private View btnDatePicker;
    private int selectedRangeDays = 7; // 1 = today, 7 = 7 days, 30 = 30 days

    // Hero Card
    private TextView tvHeroTitle;
    private TextView tvHeroRevenue;
    private TextView tvHeroSubtext;

    // 2x2 Metric Cards
    private TextView tvStatTotalOrders;
    private TextView tvStatRevenue;
    private TextView tvStatAvgOrder;
    private TextView tvStatSuccessCount;
    private TextView tvStatSuccessPct;
    private TextView tvStatCancelledCount;
    private TextView tvStatCancelledPct;

    // Best Sellers
    private ImageView imgBestSeller1, imgBestSeller2, imgBestSeller3;
    private View btnViewAllBestSellers;

    // POS Sync
    private TextView tvPosSyncTime;
    private ImageView btnPosRefresh;

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;

    private final SimpleDateFormat syncTimeFormat = new SimpleDateFormat("HH:mm 'hôm nay'", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_report);

        db = FirebaseFirestore.getInstance();
        currentRestaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");

        initViews();
        setupTopBar();
        setupTimeTabs();
        setupBestSellers();
        setupPosSync();
        setupBottomNavigation();

        resolveRestaurantDetails();
        applyRangeStats(7);
    }

    private void initViews() {
        tvRestaurantName       = findViewById(R.id.tv_restaurant_name);
        layoutStatusPill       = findViewById(R.id.layout_status_pill);
        tvRestaurantStatus     = findViewById(R.id.tv_restaurant_status);
        btnNotifications       = findViewById(R.id.btn_notifications);
        btnProfile             = findViewById(R.id.btn_profile);

        btnTabToday            = findViewById(R.id.btn_tab_today);
        btnTab7Days            = findViewById(R.id.btn_tab_7days);
        btnTab30Days           = findViewById(R.id.btn_tab_30days);
        btnDatePicker          = findViewById(R.id.btn_date_picker);

        tvHeroTitle            = findViewById(R.id.tv_hero_title);
        tvHeroRevenue          = findViewById(R.id.tv_hero_revenue);
        tvHeroSubtext          = findViewById(R.id.tv_hero_subtext);

        tvStatTotalOrders      = findViewById(R.id.tv_stat_total_orders);
        tvStatRevenue          = findViewById(R.id.tv_stat_revenue);
        tvStatAvgOrder         = findViewById(R.id.tv_stat_avg_order);
        tvStatSuccessCount     = findViewById(R.id.tv_stat_success_count);
        tvStatSuccessPct       = findViewById(R.id.tv_stat_success_pct);
        tvStatCancelledCount   = findViewById(R.id.tv_stat_cancelled_count);
        tvStatCancelledPct     = findViewById(R.id.tv_stat_cancelled_pct);

        imgBestSeller1         = findViewById(R.id.img_best_seller_1);
        imgBestSeller2         = findViewById(R.id.img_best_seller_2);
        imgBestSeller3         = findViewById(R.id.img_best_seller_3);
        btnViewAllBestSellers  = findViewById(R.id.btn_view_all_best_sellers);

        tvPosSyncTime          = findViewById(R.id.tv_pos_sync_time);
        btnPosRefresh          = findViewById(R.id.btn_pos_refresh);

        bottomNavigationView   = findViewById(R.id.bottomNavigationView);
    }

    private void setupTopBar() {
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "Báo cáo doanh thu đã được chốt phiên!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showProfileDialog());
        }

        if (layoutStatusPill != null) {
            layoutStatusPill.setOnClickListener(v -> toggleRestaurantOpenStatus());
        }
    }

    private void toggleRestaurantOpenStatus() {
        if (tvRestaurantStatus == null) return;
        boolean isOpen = "Mở cửa".equalsIgnoreCase(tvRestaurantStatus.getText().toString());
        String newStatus = isOpen ? "Tạm đóng" : "Mở cửa";
        tvRestaurantStatus.setText(newStatus);
        Toast.makeText(this, "Trạng thái quán: " + newStatus, Toast.LENGTH_SHORT).show();

        if (currentRestaurantId != null && !currentRestaurantId.isEmpty()) {
            db.collection("restaurants").document(currentRestaurantId)
                    .update("status", isOpen ? "closed" : "active");
        }
    }

    private void showProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null ? user.getEmail() : "chuban@gmail.com";

        new AlertDialog.Builder(this)
                .setTitle("Tài khoản Chủ quán")
                .setMessage("Nhà hàng: " + tvRestaurantName.getText() + "\nEmail: " + email)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(RestaurantReportActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupTimeTabs() {
        if (btnTabToday != null) {
            btnTabToday.setOnClickListener(v -> applyRangeStats(1));
        }
        if (btnTab7Days != null) {
            btnTab7Days.setOnClickListener(v -> applyRangeStats(7));
        }
        if (btnTab30Days != null) {
            btnTab30Days.setOnClickListener(v -> applyRangeStats(30));
        }

        if (btnDatePicker != null) {
            btnDatePicker.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    Toast.makeText(this, "Xem thống kê ngày " + dayOfMonth + "/" + (month + 1) + "/" + year, Toast.LENGTH_SHORT).show();
                    applyRangeStats(1);
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            });
        }
    }

    private void applyRangeStats(int days) {
        selectedRangeDays = days;
        resetTimeTab(btnTabToday, "Hôm nay");
        resetTimeTab(btnTab7Days, "7 ngày");
        resetTimeTab(btnTab30Days, "30 ngày");

        if (days == 1) {
            setActiveTimeTab(btnTabToday, "✓ Hôm nay");
            tvHeroTitle.setText("DOANH THU HOÀN TẤT (HÔM NAY)");
            tvHeroRevenue.setText("1.250.000 đ");
            tvHeroSubtext.setText("✔ Tổng tiền đơn giao thành công (COD) • 18 đơn");

            tvStatTotalOrders.setText("20 đơn");
            tvStatRevenue.setText("1.25M đ");
            tvStatAvgOrder.setText("TB: 69.400đ/đơn");
            tvStatSuccessCount.setText("18");
            tvStatSuccessPct.setText("90.0%");
            tvStatCancelledCount.setText("2");
            tvStatCancelledPct.setText("10.0%");

        } else if (days == 7) {
            setActiveTimeTab(btnTab7Days, "✓ 7 ngày");
            tvHeroTitle.setText("DOANH THU HOÀN TẤT (7 NGÀY)");
            tvHeroRevenue.setText("8.450.000 đ");
            tvHeroSubtext.setText("✔ Tổng tiền đơn giao thành công (COD) • 112 đơn");

            tvStatTotalOrders.setText("115 đơn");
            tvStatRevenue.setText("8.45M đ");
            tvStatAvgOrder.setText("TB: 73.400đ/đơn");
            tvStatSuccessCount.setText("112");
            tvStatSuccessPct.setText("97.4%");
            tvStatCancelledCount.setText("3");
            tvStatCancelledPct.setText("2.6%");

        } else {
            setActiveTimeTab(btnTab30Days, "✓ 30 ngày");
            tvHeroTitle.setText("DOANH THU HOÀN TẤT (30 NGÀY)");
            tvHeroRevenue.setText("36.800.000 đ");
            tvHeroSubtext.setText("✔ Tổng tiền đơn giao thành công (COD) • 480 đơn");

            tvStatTotalOrders.setText("510 đơn");
            tvStatRevenue.setText("36.8M đ");
            tvStatAvgOrder.setText("TB: 75.100đ/đơn");
            tvStatSuccessCount.setText("480");
            tvStatSuccessPct.setText("94.1%");
            tvStatCancelledCount.setText("30");
            tvStatCancelledPct.setText("5.9%");
        }
    }

    private void setActiveTimeTab(TextView tab, String text) {
        if (tab == null) return;
        tab.setText(text);
        tab.setBackgroundResource(R.drawable.bg_chip_category_selected);
        tab.setTextColor(0xFFFFFFFF);
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void resetTimeTab(TextView tab, String text) {
        if (tab == null) return;
        tab.setText(text);
        tab.setBackgroundResource(R.drawable.bg_chip_category_unselected);
        tab.setTextColor(0xFF334155);
        tab.setTypeface(null, Typeface.NORMAL);
    }

    private void setupBestSellers() {
        if (imgBestSeller1 != null) {
            Glide.with(this)
                    .load("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&auto=format&fit=crop&q=80")
                    .centerCrop()
                    .into(imgBestSeller1);
        }
        if (imgBestSeller2 != null) {
            Glide.with(this)
                    .load("https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=80")
                    .centerCrop()
                    .into(imgBestSeller2);
        }
        if (imgBestSeller3 != null) {
            Glide.with(this)
                    .load("https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=500&auto=format&fit=crop&q=80")
                    .centerCrop()
                    .into(imgBestSeller3);
        }

        if (btnViewAllBestSellers != null) {
            btnViewAllBestSellers.setOnClickListener(v -> {
                Intent intent = new Intent(this, MenuActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
            });
        }
    }

    private void setupPosSync() {
        if (btnPosRefresh != null) {
            btnPosRefresh.setOnClickListener(v -> {
                String nowStr = "Cập nhật lúc " + syncTimeFormat.format(new Date());
                if (tvPosSyncTime != null) tvPosSyncTime.setText(nowStr);
                Toast.makeText(this, "Đã đồng bộ dữ liệu POS thành công!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(R.id.nav_report);

        try {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_order);
            badge.setNumber(2);
            badge.setVisible(true);
        } catch (Exception ignored) {}

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_report) {
                return true;
            } else if (id == R.id.nav_menu) {
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_order) {
                Intent intent = new Intent(getApplicationContext(), OrderDashboardActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void resolveRestaurantDetails() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) return;

        String uid = authUser.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String mappedRestaurantId = userDoc.getString("restaurantId");
            if (mappedRestaurantId != null && !mappedRestaurantId.trim().isEmpty()) {
                currentRestaurantId = mappedRestaurantId.trim();
                loadRestaurantName();
                return;
            }

            db.collection("restaurants").whereEqualTo("ownerId", uid).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            currentRestaurantId = snap.getDocuments().get(0).getId();
                            loadRestaurantName();
                        }
                    });
        });
    }

    private void loadRestaurantName() {
        if (currentRestaurantId != null && !currentRestaurantId.isEmpty()) {
            db.collection("restaurants").document(currentRestaurantId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.trim().isEmpty() && tvRestaurantName != null) {
                                tvRestaurantName.setText(name);
                            }
                        }
                    });
        }
    }
}
