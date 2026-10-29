package com.example.foodorderapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.foodorderapp.R;
import com.example.foodorderapp.ui.auth.SignInActivity;
import com.example.foodorderapp.feature.admin.dashboard.AdminDashboardViewModel;
import com.example.foodorderapp.feature.admin.restaurants.AdminRestaurantsViewModel;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class AdminMainActivity extends AppCompatActivity {

    private AdminDashboardViewModel dashboardViewModel;
    private AdminRestaurantsViewModel restaurantsViewModel;
    private BottomNavigationView bottomNav;
    private NavController navController;

    private TextView tvAdminSubtitle;
    private TextView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        tvAdminSubtitle = findViewById(R.id.tv_admin_subtitle);
        notificationBadge = findViewById(R.id.view_notification_dot);
        setupTopBarActions();

        // Khởi tạo ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
        restaurantsViewModel = new ViewModelProvider(this).get(AdminRestaurantsViewModel.class);
        restaurantsViewModel.observe();

        // Setup Navigation
        setupNavigation();

        // Kiểm tra quyền Admin
        checkAdminRole();

        // Observe badge nhà hàng chờ duyệt
        observeBadge();

        // Observe loading và lỗi
        observeStates();
    }

    private void setupTopBarActions() {
        View btnNotifications = findViewById(R.id.btn_admin_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                showPendingRestaurantNotifications();
            });
        }

        View btnProfile = findViewById(R.id.btn_admin_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showAdminProfileDialog());
        }
    }

    private void showPendingRestaurantNotifications() {
        java.util.List<com.example.foodorderapp.data.model.Restaurant> pending = restaurantsViewModel.getPending().getValue();
        if (pending == null || pending.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Thông báo")
                    .setMessage("Không có hồ sơ nhà hàng đang chờ phê duyệt.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }
        String[] names = new String[pending.size()];
        for (int i = 0; i < pending.size(); i++) {
            com.example.foodorderapp.data.model.Restaurant restaurant = pending.get(i);
            names[i] = (restaurant.name == null || restaurant.name.trim().isEmpty() ? "Nhà hàng mới" : restaurant.name)
                    + " — hồ sơ chờ phê duyệt";
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hồ sơ nhà hàng mới (" + pending.size() + ")")
                .setItems(names, (dialog, which) -> {
                    if (navController != null) navController.navigate(R.id.nav_restaurants);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showAdminProfileDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tài khoản Quản trị")
                .setMessage("Bạn đang đăng nhập với tư cách Administrator hệ thống.")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupNavigation() {
        bottomNav = findViewById(R.id.admin_bottom_nav);

        androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host);

        if (navHostFragment instanceof NavHostFragment) {
            navController = ((NavHostFragment) navHostFragment).getNavController();
            
            if (bottomNav != null && navController != null) {
                // Custom bottom nav handling to avoid NavigationUI startDestination popBackStack/restoreState bug
                bottomNav.setOnItemSelectedListener(item -> {
                    int itemId = item.getItemId();
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == itemId) {
                        return true;
                    }

                    if (itemId == R.id.nav_dashboard) {
                        boolean popped = navController.popBackStack(R.id.nav_dashboard, false);
                        if (!popped) {
                            navController.navigate(R.id.nav_dashboard);
                        }
                        return true;
                    }

                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_dashboard, false)
                            .build();

                    try {
                        navController.navigate(itemId, null, navOptions);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

                bottomNav.setOnItemReselectedListener(item -> {
                    // Do nothing on reselection
                });

                navController.addOnDestinationChangedListener((controller, destination, args) -> {
                    if (bottomNav != null) {
                        Menu menu = bottomNav.getMenu();
                        MenuItem menuItem = menu.findItem(destination.getId());
                        if (menuItem != null && !menuItem.isChecked()) {
                            menuItem.setChecked(true);
                        }
                    }
                    if (tvAdminSubtitle != null) {
                        tvAdminSubtitle.setText("Quản trị hệ thống");
                    }
                });
            }
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy Navigation Host", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAdminRole() {
        dashboardViewModel.getCurrentUserRole().observe(this, role -> {
            if (role != null && !role.equals("admin") && !getIntent().getBooleanExtra("preview_mode", false)) {
                Toast.makeText(this, "Bạn không có quyền truy cập khu vực Quản trị!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void observeBadge() {
        restaurantsViewModel.getPendingCount().observe(this, count -> {
            int pending = count == null ? 0 : count;
            if (notificationBadge != null) {
                notificationBadge.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
                notificationBadge.setText(pending > 99 ? "99+" : String.valueOf(pending));
            }
            if (bottomNav != null && count != null && count > 0) {
                BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_restaurants);
                badge.setNumber(count);
                badge.setVisible(true);
            } else if (bottomNav != null) {
                bottomNav.removeBadge(R.id.nav_restaurants);
            }
        });
    }

    private void observeStates() {
        dashboardViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        restaurantsViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }
}
