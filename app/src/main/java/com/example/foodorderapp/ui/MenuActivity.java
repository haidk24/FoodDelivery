package com.example.foodorderapp.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.ui.adapter.FoodAdapter;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuActivity extends AppCompatActivity {

    private RecyclerView rvMenu;
    private View fabAddFood;
    private FoodAdapter foodAdapter;
    private final List<Food> foodList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentRestaurantId = "";
    private String currentRestaurantName = "";

    // Top Bar views
    private TextView tvRestaurantName, tvRestaurantSubtitle, tvRestaurantStatus;
    private View layoutStatusPill, btnNotifications, btnProfile, viewNotificationDot;

    // Header & Filter views
    private TextView tvCountActive, tvCountHidden;
    private View btnFilterTune;
    private EditText etSearchFood;
    private ImageView btnClearSearch;

    // Category chips
    private TextView chipAll, chipMain, chipAppetizer, chipDrink, chipDessert;
    private String selectedCategory = "Tất cả";
    private String currentSearchQuery = "";

    // Empty state & Loading
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        db = FirebaseFirestore.getInstance();

        currentRestaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");
        if (currentRestaurantId == null) currentRestaurantId = "";
        currentRestaurantName = getIntent().getStringExtra("EXTRA_RESTAURANT_NAME");
        if (currentRestaurantName == null) currentRestaurantName = "";

        bindViews();

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = authUser != null ? authUser.getUid() : "";
        if (!currentRestaurantName.isEmpty()) {
            tvRestaurantName.setText(currentRestaurantName);
        } else {
            String cachedName = com.example.foodorderapp.utils.RestaurantSession.getRestaurantName(this, uid);
            if (!cachedName.isEmpty()) {
                currentRestaurantName = cachedName;
                tvRestaurantName.setText(cachedName);
            }
            if (currentRestaurantId.isEmpty()) {
                currentRestaurantId = com.example.foodorderapp.utils.RestaurantSession.getRestaurantId(this, uid);
            }
        }

        setupTopBar();
        setupSearchAndFilters();
        setupRecyclerView();
        setupBottomNavigation();
        setupFab();

        verifyRestaurantRoleAndLoadMenu();
    }

    private void bindViews() {
        // Top Bar
        tvRestaurantName     = findViewById(R.id.tv_restaurant_name);
        tvRestaurantSubtitle = findViewById(R.id.tv_restaurant_subtitle);
        tvRestaurantStatus   = findViewById(R.id.tv_restaurant_status);
        layoutStatusPill     = findViewById(R.id.layout_status_pill);
        btnNotifications     = findViewById(R.id.btn_notifications);
        btnProfile           = findViewById(R.id.btn_profile);
        viewNotificationDot  = findViewById(R.id.view_notification_dot);

        // Header Counters & Tune
        tvCountActive        = findViewById(R.id.tv_count_active);
        tvCountHidden        = findViewById(R.id.tv_count_hidden);
        btnFilterTune        = findViewById(R.id.btn_filter_tune);

        // Search Bar
        etSearchFood         = findViewById(R.id.et_search_food);
        btnClearSearch       = findViewById(R.id.btn_clear_search);

        // Category Chips
        chipAll              = findViewById(R.id.chip_all);
        chipMain             = findViewById(R.id.chip_main);
        chipAppetizer        = findViewById(R.id.chip_appetizer);
        chipDrink            = findViewById(R.id.chip_drink);
        chipDessert          = findViewById(R.id.chip_dessert);

        // Recycler, Loading, Empty State
        rvMenu               = findViewById(R.id.rvMenu);
        progressBar          = findViewById(R.id.progress_bar);
        layoutEmptyState     = findViewById(R.id.layout_empty_state);

        // FAB & Bottom Nav
        fabAddFood           = findViewById(R.id.fabAddFood);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupTopBar() {
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "Bạn có 2 đơn hàng mới cần xử lý!", Toast.LENGTH_SHORT).show()
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
        boolean isCurrentlyOpen = "Mở cửa".equalsIgnoreCase(tvRestaurantStatus.getText().toString());
        String newStatus = isCurrentlyOpen ? "Tạm đóng" : "Mở cửa";
        tvRestaurantStatus.setText(newStatus);
        Toast.makeText(this, "Trạng thái quán: " + newStatus, Toast.LENGTH_SHORT).show();

        if (!currentRestaurantId.isEmpty()) {
            db.collection("restaurants").document(currentRestaurantId)
                    .update("status", isCurrentlyOpen ? "closed" : "active");
        }
    }

    private void showProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null ? user.getEmail() : "chuban@gmail.com";

        new AlertDialog.Builder(this)
                .setTitle("Tài khoản Chủ quán")
                .setMessage("Nhà hàng: " + tvRestaurantName.getText() + "\nEmail: " + email)
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupSearchAndFilters() {
        if (etSearchFood != null) {
            etSearchFood.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString();
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (foodAdapter != null) {
                        foodAdapter.filter(currentSearchQuery, selectedCategory);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchFood != null) {
                    etSearchFood.setText("");
                }
            });
        }

        if (btnFilterTune != null) {
            btnFilterTune.setOnClickListener(v ->
                    Toast.makeText(this, "Bộ lọc nâng cao theo giá và trạng thái", Toast.LENGTH_SHORT).show()
            );
        }

        // Category click listeners
        setupCategoryChip(chipAll, "Tất cả");
        setupCategoryChip(chipMain, "Món chính");
        setupCategoryChip(chipAppetizer, "Khai vị");
        setupCategoryChip(chipDrink, "Đồ uống");
        setupCategoryChip(chipDessert, "Tráng miệng");
    }

    private void setupCategoryChip(TextView chip, String category) {
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            selectedCategory = category;
            updateCategoryChipStyles();
            if (foodAdapter != null) {
                foodAdapter.filter(currentSearchQuery, selectedCategory);
            }
        });
    }

    private void updateCategoryChipStyles() {
        resetChip(chipAll, "Tất cả (26)");
        resetChip(chipMain, "Món chính (14)");
        resetChip(chipAppetizer, "Khai vị (6)");
        resetChip(chipDrink, "Đồ uống (4)");
        resetChip(chipDessert, "Tráng miệng (2)");

        if ("Tất cả".equalsIgnoreCase(selectedCategory)) {
            setActiveChip(chipAll);
        } else if ("Món chính".equalsIgnoreCase(selectedCategory)) {
            setActiveChip(chipMain);
        } else if ("Khai vị".equalsIgnoreCase(selectedCategory)) {
            setActiveChip(chipAppetizer);
        } else if ("Đồ uống".equalsIgnoreCase(selectedCategory)) {
            setActiveChip(chipDrink);
        } else if ("Tráng miệng".equalsIgnoreCase(selectedCategory)) {
            setActiveChip(chipDessert);
        }
    }

    private void setActiveChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_category_selected);
        chip.setTextColor(0xFFFFFFFF);
        chip.setTypeface(null, Typeface.BOLD);
    }

    private void resetChip(TextView chip, String label) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_category_unselected);
        chip.setTextColor(0xFF334155);
        chip.setTypeface(null, Typeface.NORMAL);
    }

    private void setupRecyclerView() {
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        foodAdapter = new FoodAdapter(this, foodList, currentRestaurantId);

        foodAdapter.setCountListener((activeCount, hiddenCount, total, main, app, drink, dessert) -> {
            if (tvCountActive != null) tvCountActive.setText(activeCount + " món đang bán");
            if (tvCountHidden != null) tvCountHidden.setText(hiddenCount + " món tạm ẩn");
            if (chipAll != null) chipAll.setText("Tất cả (" + total + ")");
            if (chipMain != null) chipMain.setText("Món chính (" + main + ")");
            if (chipAppetizer != null) chipAppetizer.setText("Khai vị (" + app + ")");
            if (chipDrink != null) chipDrink.setText("Đồ uống (" + drink + ")");
            if (chipDessert != null) chipDessert.setText("Tráng miệng (" + dessert + ")");
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(foodAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        rvMenu.setAdapter(foodAdapter);
    }

    private void setupFab() {
        if (fabAddFood != null) {
            fabAddFood.setOnClickListener(v -> {
                if (currentRestaurantId == null || currentRestaurantId.isEmpty()) {
                    Toast.makeText(this, "Không tìm thấy nhà hàng hiện tại", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MenuActivity.this, AddFoodActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(R.id.nav_menu);

        try {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_order);
            badge.setNumber(2);
            badge.setVisible(true);
        } catch (Exception ignored) {}

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_menu) {
                return true;
            } else if (id == R.id.nav_order) {
                Intent intent = new Intent(getApplicationContext(), OrderDashboardActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                intent.putExtra("EXTRA_RESTAURANT_NAME", currentRestaurantName);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_report) {
                Intent intent = new Intent(getApplicationContext(), RestaurantReportActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                intent.putExtra("EXTRA_RESTAURANT_NAME", currentRestaurantName);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void verifyRestaurantRoleAndLoadMenu() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }

        String uid = authUser.getUid();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    String role = document.getString("role");
                    if (!"restaurant".equalsIgnoreCase(role) && !getIntent().getBooleanExtra("preview_mode", false)) {
                        Toast.makeText(this, "Chỉ tài khoản nhà hàng mới vào được quản lý menu", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(MenuActivity.this, MainActivity.class));
                        finish();
                        return;
                    }

                    String mappedRestaurantId = document.getString("restaurantId");
                    if (mappedRestaurantId != null && !mappedRestaurantId.trim().isEmpty()) {
                        currentRestaurantId = mappedRestaurantId.trim();
                        loadRestaurantDetailsAndMenu();
                        return;
                    }

                    // Fallback: search restaurant by ownerId
                    db.collection("restaurants")
                            .whereEqualTo("ownerId", uid)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.isEmpty()) {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "Chưa liên kết nhà hàng cho tài khoản này", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                currentRestaurantId = snapshot.getDocuments().get(0).getId();
                                persistRestaurantMapping(uid, currentRestaurantId);
                                loadRestaurantDetailsAndMenu();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
    }

    private void persistRestaurantMapping(String uid, String restaurantId) {
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("restaurantId", restaurantId);
        db.collection("users").document(uid).update(userUpdate);

        Map<String, Object> restaurantUpdate = new HashMap<>();
        restaurantUpdate.put("ownerId", uid);
        db.collection("restaurants").document(restaurantId).update(restaurantUpdate);
    }

    private void loadRestaurantDetailsAndMenu() {
        foodAdapter.setRestaurantId(currentRestaurantId);

        // Load restaurant name
        db.collection("restaurants").document(currentRestaurantId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.trim().isEmpty() && tvRestaurantName != null) {
                            currentRestaurantName = name;
                            tvRestaurantName.setText(name);
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                com.example.foodorderapp.utils.RestaurantSession.saveRestaurant(
                                        this, user.getUid(), currentRestaurantId, name, doc.getString("status"));
                            }
                        }
                    }
                });

        loadMenuFromFirebase();
    }

    private void loadMenuFromFirebase() {
        db.collection("restaurants")
                .document(currentRestaurantId)
                .collection("foods")
                .addSnapshotListener((value, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e("MENU_ERROR", "Error: " + error.getMessage());
                        return;
                    }

                    foodList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Food food = doc.toObject(Food.class);
                            food.setFoodId(doc.getId());
                            foodList.add(food);
                        }
                    }

                    // Dynamically calculate counters
                    int countActive = 0;
                    int countHidden = 0;
                    for (Food f : foodList) {
                        if (f.isAvailable()) countActive++;
                        else countHidden++;
                    }
                    if (tvCountActive != null) tvCountActive.setText(countActive + " món đang bán");
                    if (tvCountHidden != null) tvCountHidden.setText(countHidden + " món tạm ẩn");

                    if (foodList.isEmpty()) {
                        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                    }
                    foodAdapter.updateData(foodList);
                });
    }

    private void performLogout() {
        com.example.foodorderapp.utils.RestaurantSession.clear(this);
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MenuActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}