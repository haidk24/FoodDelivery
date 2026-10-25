package com.example.foodorderapp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.foodorderapp.R;
import com.example.foodorderapp.ui.restaurant.RestaurantMenuFragment;
import com.example.foodorderapp.ui.restaurant.RestaurantOrdersFragment;
import com.example.foodorderapp.ui.restaurant.RestaurantReportFragment;
import com.example.foodorderapp.utils.RestaurantOrderBadge;
import com.example.foodorderapp.utils.RestaurantOrderNotifier;
import com.example.foodorderapp.utils.OrderNotificationPanel;
import com.example.foodorderapp.utils.RestaurantSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RestaurantMainActivity extends AppCompatActivity {
    public static final String EXTRA_TAB = "EXTRA_RESTAURANT_TAB";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1101;

    private BottomNavigationView navigation;
    private TextView restaurantName;
    private TextView restaurantStatus;
    private TextView restaurantSubtitle;
    private Fragment menuFragment;
    private Fragment ordersFragment;
    private Fragment reportFragment;
    private Fragment activeFragment;
    private FirebaseFirestore db;
    private ListenerRegistration restaurantRegistration;
    private ListenerRegistration notificationRegistration;
    private RestaurantOrderBadge orderBadge;
    private RestaurantOrderNotifier orderNotifier;
    private String restaurantId = "";
    private String displayName = "";
    private int selectedTabId = R.id.nav_menu;
    private TextView notificationBadge;
    private final List<com.example.foodorderapp.data.model.Order> notifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_main);
        db = FirebaseFirestore.getInstance();
        bindChrome();
        restoreSessionImmediately();
        initFragments(savedInstanceState);
        setupNavigation();
        setupChromeActions();
        requestNotificationPermission();
        resolveRestaurant();

        int initialTab = savedInstanceState == null
                ? getIntent().getIntExtra(EXTRA_TAB, R.id.nav_menu)
                : savedInstanceState.getInt(EXTRA_TAB, R.id.nav_menu);
        navigation.setSelectedItemId(initialTab);
    }

    private void bindChrome() {
        navigation = findViewById(R.id.bottomNavigationView);
        restaurantName = findViewById(R.id.tv_restaurant_name);
        restaurantStatus = findViewById(R.id.tv_restaurant_status);
        restaurantSubtitle = findViewById(R.id.tv_restaurant_subtitle);
        orderBadge = new RestaurantOrderBadge(navigation);
        orderNotifier = new RestaurantOrderNotifier(this);
        notificationBadge = findViewById(R.id.view_notification_dot);
    }

    private void restoreSessionImmediately() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user == null ? "" : user.getUid();
        restaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");
        displayName = getIntent().getStringExtra("EXTRA_RESTAURANT_NAME");
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            restaurantId = RestaurantSession.getRestaurantId(this, uid);
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = RestaurantSession.getRestaurantName(this, uid);
        }
        String status = RestaurantSession.getRestaurantStatus(this, uid);
        restaurantName.setText(displayName == null ? "" : displayName);
        if (!status.isEmpty()) restaurantStatus.setText("active".equals(status) ? "Mở cửa" : "Tạm đóng");
    }

    private void initFragments(Bundle state) {
        if (state == null) {
            menuFragment = RestaurantMenuFragment.newInstance(restaurantId);
            ordersFragment = RestaurantOrdersFragment.newInstance(restaurantId);
            reportFragment = RestaurantReportFragment.newInstance(restaurantId);
            activeFragment = menuFragment;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.restaurant_content_frame, reportFragment, "RESTAURANT_REPORT").hide(reportFragment)
                    .add(R.id.restaurant_content_frame, ordersFragment, "RESTAURANT_ORDERS").hide(ordersFragment)
                    .add(R.id.restaurant_content_frame, menuFragment, "RESTAURANT_MENU")
                    .commit();
        } else {
            menuFragment = getSupportFragmentManager().findFragmentByTag("RESTAURANT_MENU");
            ordersFragment = getSupportFragmentManager().findFragmentByTag("RESTAURANT_ORDERS");
            reportFragment = getSupportFragmentManager().findFragmentByTag("RESTAURANT_REPORT");
            selectedTabId = state.getInt(EXTRA_TAB, R.id.nav_menu);
            activeFragment = selectedTabId == R.id.nav_order ? ordersFragment
                    : selectedTabId == R.id.nav_report ? reportFragment : menuFragment;
        }
    }

    private void setupNavigation() {
        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            selectedTabId = id;
            if (id == R.id.nav_menu) {
                switchTab(menuFragment, "Quản lý thực đơn");
            } else if (id == R.id.nav_order) {
                switchTab(ordersFragment, "Quản lý đơn hàng");
            } else if (id == R.id.nav_report) {
                switchTab(reportFragment, "Báo cáo & Doanh thu");
            }
            return true;
        });
    }

    private void switchTab(Fragment target, String subtitle) {
        restaurantSubtitle.setText(subtitle);
        if (target == null || target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    public void selectTab(int itemId) {
        navigation.setSelectedItemId(itemId);
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getIntExtra(EXTRA_TAB, R.id.nav_menu) == R.id.nav_order) {
            selectTab(R.id.nav_order);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_TAB, selectedTabId);
        super.onSaveInstanceState(outState);
    }

    private void setupChromeActions() {
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            requestNotificationPermission();
            OrderNotificationPanel.show(this, notifications, true);
        });
        findViewById(R.id.btn_profile).setOnClickListener(v -> showProfile());
        findViewById(R.id.layout_status_pill).setOnClickListener(v -> toggleStatus());
    }

    private void showProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null && user.getEmail() != null ? user.getEmail() : "Chưa có email";
        new AlertDialog.Builder(this)
                .setTitle("Tài khoản Chủ quán")
                .setMessage("Nhà hàng: " + restaurantName.getText() + "\nEmail: " + email)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    RestaurantSession.clear(this);
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void toggleStatus() {
        if (restaurantId.isEmpty()) return;
        boolean isOpen = "Mở cửa".contentEquals(restaurantStatus.getText());
        db.collection("restaurants").document(restaurantId)
                .update("status", isOpen ? "closed" : "active")
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không cập nhật được trạng thái quán", Toast.LENGTH_SHORT).show());
    }

    private void resolveRestaurant() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (!restaurantId.isEmpty()) {
            listenRestaurant(user.getUid(), restaurantId);
            return;
        }
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(document -> {
            String mapped = document.getString("restaurantId");
            if (mapped != null && !mapped.trim().isEmpty()) {
                listenRestaurant(user.getUid(), mapped.trim());
            } else {
                db.collection("restaurants").whereEqualTo("ownerId", user.getUid()).limit(1).get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.isEmpty()) {
                                listenRestaurant(user.getUid(), snapshot.getDocuments().get(0).getId());
                            }
                        });
            }
        });
    }

    private void listenRestaurant(String uid, String id) {
        restaurantId = id;
        orderBadge.listen(id);
        orderNotifier.start(id);
        listenNotifications(id);
        notifyFragments(id);
        if (restaurantRegistration != null) restaurantRegistration.remove();
        restaurantRegistration = db.collection("restaurants").document(id).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists()) return;
            String name = doc.getString("name");
            String status = doc.getString("status");
            displayName = name == null ? "" : name.trim();
            restaurantName.setText(displayName);
            restaurantStatus.setText("active".equals(status) ? "Mở cửa" : "Tạm đóng");
            RestaurantSession.saveRestaurant(this, uid, id, displayName, status);
        });
    }

    private void listenNotifications(String id) {
        if (notificationRegistration != null) notificationRegistration.remove();
        notificationRegistration = db.collection("restaurants").document(id).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    notifications.clear();
                    int unread = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        com.example.foodorderapp.data.model.Order order = doc.toObject(com.example.foodorderapp.data.model.Order.class);
                        if (order == null) continue;
                        order.setOrderId(doc.getId());
                        notifications.add(order);
                        if (com.example.foodorderapp.data.model.Order.STATUS_PENDING.equals(
                                com.example.foodorderapp.data.model.Order.normalizeStatus(order.getStatus()))) unread++;
                    }
                    Collections.sort(notifications, (a, b) -> compareNotificationDates(a.getCreatedAt(), b.getCreatedAt()));
                    updateNotificationBadge(unread);
                });
    }

    private int compareNotificationDates(Date first, Date second) {
        if (first != null && second != null) return second.compareTo(first);
        if (first != null) return -1;
        return second != null ? 1 : 0;
    }

    private void updateNotificationBadge(int unread) {
        if (notificationBadge == null) return;
        notificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        notificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Toast.makeText(this, granted ? "Đã bật thông báo đơn hàng" :
                "Thông báo đang tắt. Bạn có thể bật lại trong Cài đặt ứng dụng.",
                Toast.LENGTH_LONG).show();
    }

    private void notifyFragments(String id) {
        if (menuFragment instanceof RestaurantMenuFragment) {
            ((RestaurantMenuFragment) menuFragment).setRestaurantId(id);
        }
        if (ordersFragment instanceof RestaurantOrdersFragment) {
            ((RestaurantOrdersFragment) ordersFragment).setRestaurantId(id);
        }
        if (reportFragment instanceof RestaurantReportFragment) {
            ((RestaurantReportFragment) reportFragment).setRestaurantId(id);
        }
    }

    @Override
    public void onBackPressed() {
        if (activeFragment != menuFragment) {
            selectTab(R.id.nav_menu);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (restaurantRegistration != null) restaurantRegistration.remove();
        if (notificationRegistration != null) notificationRegistration.remove();
        if (orderBadge != null) orderBadge.stop();
        if (orderNotifier != null) orderNotifier.stop();
        super.onDestroy();
    }
}
