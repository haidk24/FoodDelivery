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
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.ui.adapter.RestaurantOrderAdapter;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private View btnSoundAlert;
    private View btnFilterTune;

    // Status Chips
    private TextView chipAll, chipPending, chipPreparing, chipDelivering, chipCompleted;
    private String selectedStatus = "Tất cả";
    private String currentSearchQuery = "";

    // Alert Banner
    private TextView tvUrgentOrdersCount;
    private TextView tvUrgentRevenueAmount;

    // RecyclerView & Empty
    private RecyclerView rvOrders;
    private LinearLayout layoutEmptyOrders;
    private ProgressBar progressBar;
    private RestaurantOrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

    // Kitchen switch
    private SwitchCompat switchKitchenPrinter;

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;

    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private boolean isSoundAlertOn = true;

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
        setupKitchenPrinter();

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
        btnSoundAlert          = findViewById(R.id.btn_sound_alert);
        btnFilterTune          = findViewById(R.id.btn_filter_tune);

        chipAll                = findViewById(R.id.chip_all);
        chipPending            = findViewById(R.id.chip_pending);
        chipPreparing          = findViewById(R.id.chip_preparing);
        chipDelivering         = findViewById(R.id.chip_delivering);
        chipCompleted          = findViewById(R.id.chip_completed);

        tvUrgentOrdersCount    = findViewById(R.id.tv_urgent_orders_count);
        tvUrgentRevenueAmount  = findViewById(R.id.tv_urgent_revenue_amount);

        rvOrders               = findViewById(R.id.rvOrders);
        layoutEmptyOrders      = findViewById(R.id.layout_empty_orders);
        progressBar            = findViewById(R.id.progress_bar);

        switchKitchenPrinter   = findViewById(R.id.switch_kitchen_printer);
        bottomNavigationView   = findViewById(R.id.bottomNavigationView);
    }

    private void setupTopBar() {
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "Có 2 đơn hàng mới cần chuẩn bị!", Toast.LENGTH_SHORT).show()
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

        if (btnSoundAlert != null) {
            btnSoundAlert.setOnClickListener(v -> {
                isSoundAlertOn = !isSoundAlertOn;
                Toast.makeText(this, isSoundAlertOn ? "Đã bật chuông báo đơn mới 🔔" : "Đã tắt chuông báo đơn", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnFilterTune != null) {
            btnFilterTune.setOnClickListener(v ->
                    Toast.makeText(this, "Bộ lọc thời gian và phương thức thanh toán", Toast.LENGTH_SHORT).show()
            );
        }

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

        orderAdapter.setSummaryListener((total, pending, preparing, delivering, completed, urgentRevenue) -> {
            if (chipAll != null) chipAll.setText("Tất cả (" + total + ")");
            if (chipPending != null) chipPending.setText("Chờ xác nhận " + pending);
            if (chipPreparing != null) chipPreparing.setText("Đang chuẩn bị (" + preparing + ")");
            if (chipDelivering != null) chipDelivering.setText("Đang giao hàng (" + delivering + ")");
            if (chipCompleted != null) chipCompleted.setText("Đã hoàn thành (" + completed + ")");

            if (tvUrgentOrdersCount != null) {
                tvUrgentOrdersCount.setText(pending + " đơn mới");
            }
            if (tvUrgentRevenueAmount != null) {
                tvUrgentRevenueAmount.setText(currencyFormat.format(urgentRevenue) + " đ");
            }

            if (layoutEmptyOrders != null) {
                layoutEmptyOrders.setVisibility(orderAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        rvOrders.setAdapter(orderAdapter);
    }

    private void setupKitchenPrinter() {
        if (switchKitchenPrinter != null) {
            switchKitchenPrinter.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Toast.makeText(this, isChecked ? "Đã bật tự động in hóa đơn bếp" : "Đã tắt tự động in", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(R.id.nav_order);

        try {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_order);
            badge.setNumber(2);
            badge.setVisible(true);
        } catch (Exception ignored) {}

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
            setupDemoFallbackOrders();
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
                                    setupDemoFallbackOrders();
                                    return;
                                }
                                currentRestaurantId = snapshot.getDocuments().get(0).getId();
                                loadRestaurantDetailsAndOrders();
                            })
                            .addOnFailureListener(e -> setupDemoFallbackOrders());
                })
                .addOnFailureListener(e -> setupDemoFallbackOrders());
    }

    private void loadRestaurantDetailsAndOrders() {
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

        listenOrdersFromFirestore();
    }

    private void listenOrdersFromFirestore() {
        Query query;
        if (currentRestaurantId != null && !currentRestaurantId.isEmpty()) {
            query = db.collection("orders").whereEqualTo("restaurantId", currentRestaurantId);
        } else {
            query = db.collection("orders");
        }

        query.addSnapshotListener((value, error) -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (error != null) {
                return;
            }

            if (value != null) {
                orderList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    order.setOrderId(doc.getId());
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
            }
        });
    }

    private void appendDemoOrdersIfMissing(List<Order> list) {
        // Disabled: strictly real-time Firebase data, no mock data
    }

    private List<Order> createMockOrders() {
        List<Order> mocks = new ArrayList<>();

        // Order 1: Chờ xác nhận
        Order o1 = new Order();
        o1.setOrderId("SAYMDfDKIZrz691A");
        o1.setCustomerName("Thái Võ");
        o1.setCustomerPhone("0966464945");
        o1.setCustomerAddress("Số 12 Cầu Giấy, Hà Nội");
        o1.setStatus("Chờ xác nhận");
        o1.setTotalAmount(115000);
        o1.setPaymentMethod("Tiền mặt COD");
        o1.setCreatedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 15)); // 15 mins ago

        List<CartItem> items1 = new ArrayList<>();
        CartItem item1a = new CartItem();
        item1a.setName("Gỏi cuốn tôm thịt");
        item1a.setQuantity(1);
        item1a.setPrice(75000);
        items1.add(item1a);

        CartItem item1b = new CartItem();
        item1b.setName("Bún chả Hà Nội");
        item1b.setQuantity(1);
        item1b.setPrice(40000);
        items1.add(item1b);
        o1.setItems(items1);
        mocks.add(o1);

        // Order 2: Đang chuẩn bị
        Order o2 = new Order();
        o2.setOrderId("MsR1LJ1X8dJWa27B");
        o2.setCustomerName("Vũ Thanh Hải");
        o2.setCustomerPhone("0948478788");
        o2.setCustomerAddress("Tòa CT2 Nam Từ Liêm, Hà Nội");
        o2.setStatus("Đang chuẩn bị");
        o2.setTotalAmount(190000);
        o2.setPaymentMethod("Tiền mặt COD");
        o2.setCreatedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 45)); // 45 mins ago

        List<CartItem> items2 = new ArrayList<>();
        CartItem item2a = new CartItem();
        item2a.setName("Phở bò đặc biệt");
        item2a.setQuantity(2);
        item2a.setPrice(90000);
        items2.add(item2a);

        CartItem item2b = new CartItem();
        item2b.setName("Trà chanh hoa cúc");
        item2b.setQuantity(1);
        item2b.setPrice(10000);
        items2.add(item2b);
        o2.setItems(items2);
        mocks.add(o2);

        // Order 3: Đang giao hàng
        Order o3 = new Order();
        o3.setOrderId("LzAHN6zmP3z804K");
        o3.setCustomerName("Lê Hoàng Mai");
        o3.setCustomerPhone("0912345678");
        o3.setCustomerAddress("Ngõ 165 Cầu Giấy, Hà Nội");
        o3.setStatus("Đang giao hàng");
        o3.setTotalAmount(85000);
        o3.setPaymentMethod("Tiền mặt COD");
        o3.setCreatedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 95));

        List<CartItem> items3 = new ArrayList<>();
        CartItem item3a = new CartItem();
        item3a.setName("Cơm rang dưa bò giòn");
        item3a.setQuantity(1);
        item3a.setPrice(85000);
        items3.add(item3a);
        o3.setItems(items3);
        mocks.add(o3);

        // Order 4: Đã hoàn thành
        Order o4 = new Order();
        o4.setOrderId("K19Nx84JDq90A7");
        o4.setCustomerName("Trần Văn Nam");
        o4.setCustomerPhone("0988776655");
        o4.setCustomerAddress("P402 Chung Cư mini Dịch Vọng, Cầu Giấy");
        o4.setStatus("Đã hoàn thành");
        o4.setTotalAmount(140000);
        o4.setPaymentMethod("Tiền mặt COD");
        o4.setCreatedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 180));

        List<CartItem> items4 = new ArrayList<>();
        CartItem item4a = new CartItem();
        item4a.setName("Bún đậu mắm tôm thập cẩm");
        item4a.setQuantity(2);
        item4a.setPrice(70000);
        items4.add(item4a);
        o4.setItems(items4);
        mocks.add(o4);

        return mocks;
    }

    private void setupDemoFallbackOrders() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        orderList.clear();
        orderAdapter.updateData(orderList);
    }
}