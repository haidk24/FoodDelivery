package com.example.foodorderapp.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.ui.adapter.RestaurantOrderAdapter;
import com.example.foodorderapp.utils.RestaurantOrderBadge;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderDashboardActivity extends AppCompatActivity {

    private String currentRestaurantId;
    private FirebaseFirestore db;

    // Top Bar
    private TextView tvRestaurantName;
    private View layoutStatusPill;
    private TextView tvRestaurantStatus;
    private View btnNotifications;
    private View btnProfile;

    // Search & Filter Buttons
    private EditText etSearchOrder;
    private ImageView btnClearSearch;
    private View btnFilterTune;

    // Status Chips
    private TextView chipAll, chipPending, chipPreparing, chipDelivering, chipCompleted;
    private String selectedStatus = "Tất cả";
    private String currentSearchQuery = "";

    // Alert Banner
    private TextView tvUrgentOrdersCount;

    // RecyclerView & Empty
    private RecyclerView rvOrders;
    private LinearLayout layoutEmptyOrders;
    private ProgressBar progressBar;
    private RestaurantOrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;
    private RestaurantOrderBadge orderBadge;
    private ListenerRegistration ordersRegistration;
    private ListenerRegistration restaurantRegistration;
    private int selectedDaysFilter = 0;
    private String selectedPaymentFilter = "Tất cả";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_dashboard);

        db = FirebaseFirestore.getInstance();
        currentRestaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");

        initViews();
        setupTopBar();
        setupSearchAndFilters();
        setupRecyclerView();
        setupBottomNavigation();

        resolveRestaurantAndListenOrders();
    }

    private void initViews() {
        tvRestaurantName       = findViewById(R.id.tv_restaurant_name);
        layoutStatusPill       = findViewById(R.id.layout_status_pill);
        tvRestaurantStatus     = findViewById(R.id.tv_restaurant_status);
        btnNotifications       = findViewById(R.id.btn_notifications);
        btnProfile             = findViewById(R.id.btn_profile);

        etSearchOrder          = findViewById(R.id.et_search_order);
        btnClearSearch         = findViewById(R.id.btn_clear_search);
        btnFilterTune          = findViewById(R.id.btn_filter_tune);

        chipAll                = findViewById(R.id.chip_all);
        chipPending            = findViewById(R.id.chip_pending);
        chipPreparing          = findViewById(R.id.chip_preparing);
        chipDelivering         = findViewById(R.id.chip_delivering);
        chipCompleted          = findViewById(R.id.chip_completed);

        tvUrgentOrdersCount    = findViewById(R.id.tv_urgent_orders_count);

        rvOrders               = findViewById(R.id.rvOrders);
        layoutEmptyOrders      = findViewById(R.id.layout_empty_orders);
        progressBar            = findViewById(R.id.progress_bar);

        bottomNavigationView   = findViewById(R.id.bottomNavigationView);
    }

    private void setupTopBar() {
        if (btnNotifications != null) btnNotifications.setOnClickListener(v -> {
            selectedStatus = "Chờ xác nhận";
            updateStatusChipStyles();
            if (orderAdapter != null) orderAdapter.filter(currentSearchQuery, selectedStatus);
        });

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
        if (currentRestaurantId != null && !currentRestaurantId.isEmpty()) {
            db.collection("restaurants").document(currentRestaurantId)
                    .update("status", isOpen ? "closed" : "active")
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Trạng thái quán: " + newStatus, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Không cập nhật được trạng thái quán", Toast.LENGTH_SHORT).show());
        }
    }

    private void showProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null && user.getEmail() != null ? user.getEmail() : "Chưa có email";

        new AlertDialog.Builder(this)
                .setTitle("Tài khoản Chủ quán")
                .setMessage("Nhà hàng: " + tvRestaurantName.getText() + "\nEmail: " + email)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(OrderDashboardActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupSearchAndFilters() {
        if (etSearchOrder != null) {
            etSearchOrder.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString();
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (orderAdapter != null) {
                        orderAdapter.filter(currentSearchQuery, selectedStatus);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchOrder != null) etSearchOrder.setText("");
            });
        }

        if (btnFilterTune != null) btnFilterTune.setOnClickListener(v -> showAdvancedFilters());

        // Chip click listeners
        setupStatusChip(chipAll, "Tất cả");
        setupStatusChip(chipPending, "Chờ xác nhận");
        setupStatusChip(chipPreparing, "Đang chuẩn bị");
        setupStatusChip(chipDelivering, "Đang giao hàng");
        setupStatusChip(chipCompleted, "Đã hoàn thành");
    }

    private void setupStatusChip(TextView chip, String status) {
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            selectedStatus = status;
            updateStatusChipStyles();
            if (orderAdapter != null) {
                orderAdapter.filter(currentSearchQuery, selectedStatus);
            }
        });
    }

    private void showAdvancedFilters() {
        String[] ranges = {"Tất cả thời gian", "Hôm nay", "7 ngày", "30 ngày"};
        int selected = selectedDaysFilter == 1 ? 1 : selectedDaysFilter == 7 ? 2
                : selectedDaysFilter == 30 ? 3 : 0;
        new AlertDialog.Builder(this).setTitle("Lọc theo thời gian")
                .setSingleChoiceItems(ranges, selected, (dialog, which) -> {
                    selectedDaysFilter = new int[]{0, 1, 7, 30}[which];
                    dialog.dismiss();
                    orderAdapter.setAdvancedFilters(selectedDaysFilter, selectedPaymentFilter);
                    orderAdapter.filter(currentSearchQuery, selectedStatus);
                    String[] payments = {"Tất cả", "COD", "Khác COD"};
                    int paymentSelected = "COD".equals(selectedPaymentFilter) ? 1
                            : "Khác COD".equals(selectedPaymentFilter) ? 2 : 0;
                    new AlertDialog.Builder(this).setTitle("Phương thức thanh toán")
                            .setSingleChoiceItems(payments, paymentSelected, (paymentDialog, paymentIndex) -> {
                                selectedPaymentFilter = payments[paymentIndex];
                                paymentDialog.dismiss();
                                orderAdapter.setAdvancedFilters(selectedDaysFilter, selectedPaymentFilter);
                                orderAdapter.filter(currentSearchQuery, selectedStatus);
                            }).setNegativeButton("Đóng", null).show();
                }).setNegativeButton("Đóng", null).show();
    }

    private void updateStatusChipStyles() {
        resetChip(chipAll);
        resetChip(chipPending);
        resetChip(chipPreparing);
        resetChip(chipDelivering);
        resetChip(chipCompleted);

        if ("Tất cả".equalsIgnoreCase(selectedStatus)) {
            setActiveChip(chipAll);
        } else if ("Chờ xác nhận".equalsIgnoreCase(selectedStatus)) {
            setActiveChip(chipPending);
        } else if ("Đang chuẩn bị".equalsIgnoreCase(selectedStatus)) {
            setActiveChip(chipPreparing);
        } else if ("Đang giao hàng".equalsIgnoreCase(selectedStatus)) {
            setActiveChip(chipDelivering);
        } else if ("Đã hoàn thành".equalsIgnoreCase(selectedStatus)) {
            setActiveChip(chipCompleted);
        }
    }

    private void setActiveChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_category_selected);
        chip.setTextColor(0xFFFFFFFF);
        chip.setTypeface(null, Typeface.BOLD);
    }

    private void resetChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_category_unselected);
        chip.setTextColor(0xFF334155);
        chip.setTypeface(null, Typeface.NORMAL);
    }

    private void setupRecyclerView() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new RestaurantOrderAdapter(this, orderList);

        orderAdapter.setSummaryListener((total, pending, preparing, delivering, completed) -> {
            if (chipAll != null) chipAll.setText("Tất cả (" + total + ")");
            if (chipPending != null) chipPending.setText("Chờ xác nhận " + pending);
            if (chipPreparing != null) chipPreparing.setText("Đang chuẩn bị (" + preparing + ")");
            if (chipDelivering != null) chipDelivering.setText("Đang giao hàng (" + delivering + ")");
            if (chipCompleted != null) chipCompleted.setText("Đã hoàn thành (" + completed + ")");

            if (tvUrgentOrdersCount != null) {
                tvUrgentOrdersCount.setText(pending + " đơn mới");
            }

            if (layoutEmptyOrders != null) {
                layoutEmptyOrders.setVisibility(orderAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        rvOrders.setAdapter(orderAdapter);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(R.id.nav_order);

        orderBadge = new RestaurantOrderBadge(bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_order) {
                return true;
            } else if (id == R.id.nav_menu) {
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_report) {
                Intent intent = new Intent(getApplicationContext(), RestaurantReportActivity.class);
                intent.putExtra("EXTRA_RESTAURANT_ID", currentRestaurantId);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void resolveRestaurantAndListenOrders() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            showOrdersUnavailable("Vui lòng đăng nhập để xem đơn hàng");
            return;
        }

        String uid = authUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    String mappedRestaurantId = document.getString("restaurantId");
                    if (mappedRestaurantId != null && !mappedRestaurantId.trim().isEmpty()) {
                        currentRestaurantId = mappedRestaurantId.trim();
                        loadRestaurantDetailsAndOrders();
                        return;
                    }

                    db.collection("restaurants").whereEqualTo("ownerId", uid).limit(1).get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.isEmpty()) {
                                    showOrdersUnavailable("Không tìm thấy nhà hàng của tài khoản này");
                                    return;
                                }
                                currentRestaurantId = snapshot.getDocuments().get(0).getId();
                                loadRestaurantDetailsAndOrders();
                            })
                            .addOnFailureListener(e -> showOrdersUnavailable("Không tải được nhà hàng"));
                })
                .addOnFailureListener(e -> showOrdersUnavailable("Không tải được tài khoản"));
    }

    private void loadRestaurantDetailsAndOrders() {
        if (orderBadge != null) orderBadge.listen(currentRestaurantId);
        if (currentRestaurantId != null && !currentRestaurantId.isEmpty()) {
        if (restaurantRegistration != null) restaurantRegistration.remove();
        restaurantRegistration = db.collection("restaurants").document(currentRestaurantId)
                    .addSnapshotListener((doc, error) -> {
                        if (error != null) return;
                        if (doc != null && doc.exists()) {
                            if (tvRestaurantStatus != null) tvRestaurantStatus.setText(
                                    "active".equals(doc.getString("status")) ? "Mở cửa" : "Tạm đóng");
                            String name = doc.getString("name");
                            if (name != null && !name.trim().isEmpty() && tvRestaurantName != null) {
                                tvRestaurantName.setText(name);
                            }
                        }
                    });
        }

        listenOrdersFromFirestore();
    }

    private void listenOrdersFromFirestore() {
        if (currentRestaurantId == null || currentRestaurantId.isEmpty()) {
            showOrdersUnavailable("Không tìm thấy nhà hàng");
            return;
        }
        if (ordersRegistration != null) ordersRegistration.remove();
        ordersRegistration = db.collection("restaurants").document(currentRestaurantId)
                .collection("orders").addSnapshotListener((value, error) -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (error != null) {
                showOrdersUnavailable("Không tải được đơn hàng: " + error.getMessage());
                return;
            }

            if (value != null) {
                ((TextView) findViewById(R.id.tv_empty_orders_title)).setText("Chưa có đơn hàng nào");
                ((TextView) findViewById(R.id.tv_empty_orders_subtitle)).setText("Các đơn hàng mới sẽ xuất hiện tại đây");
                orderList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    order.setOrderId(doc.getId());
                    if (order.getRestaurantId() == null || order.getRestaurantId().isEmpty()) {
                        order.setRestaurantId(currentRestaurantId);
                    }
                    orderList.add(order);
                }

                // Sort newest orders first
                java.util.Collections.sort(orderList, (o1, o2) -> {
                    Date d1 = o1.getCreatedAt();
                    Date d2 = o2.getCreatedAt();
                    if (d1 != null && d2 != null) return d2.compareTo(d1);
                    if (d1 != null) return -1;
                    if (d2 != null) return 1;
                    return 0;
                });

                orderAdapter.updateData(orderList);
                orderAdapter.filter(currentSearchQuery, selectedStatus);
            }
        });
    }

    private void showOrdersUnavailable(String message) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        orderList.clear();
        if (orderAdapter != null) orderAdapter.updateData(orderList);
        ((TextView) findViewById(R.id.tv_empty_orders_title)).setText("Không tải được đơn hàng");
        ((TextView) findViewById(R.id.tv_empty_orders_subtitle)).setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (ordersRegistration != null) ordersRegistration.remove();
        if (restaurantRegistration != null) restaurantRegistration.remove();
        if (orderBadge != null) orderBadge.stop();
        super.onDestroy();
    }

}
