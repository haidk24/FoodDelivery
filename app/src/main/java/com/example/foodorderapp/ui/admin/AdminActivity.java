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
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.foodorderapp.R;
import com.example.foodorderapp.ui.SignInActivity;
import com.example.foodorderapp.viewModel.AdminViewModel;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class AdminActivity extends AppCompatActivity {

    private AdminViewModel viewModel;
    private BottomNavigationView bottomNav;
    private NavController navController;

    private TextView tvAdminSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tvAdminSubtitle = findViewById(R.id.tv_admin_subtitle);
        setupTopBarActions();

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

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
                Integer pendingCount = viewModel.getPendingRestaurantCount().getValue();
                if (pendingCount != null && pendingCount > 0) {
                    Toast.makeText(this, "Có " + pendingCount + " hồ sơ nhà hàng cần bạn phê duyệt!", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.navigate(R.id.nav_restaurants);
                    }
                } else {
                    Toast.makeText(this, "Không có thông báo mới.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnProfile = findViewById(R.id.btn_admin_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showAdminProfileDialog());
        }
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
        viewModel.getCurrentUserRole().observe(this, role -> {
            if (role != null && !role.equals("admin") && !getIntent().getBooleanExtra("preview_mode", false)) {
                Toast.makeText(this, "Bạn không có quyền truy cập khu vực Quản trị!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void observeBadge() {
        viewModel.getPendingRestaurantCount().observe(this, count -> {
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
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
