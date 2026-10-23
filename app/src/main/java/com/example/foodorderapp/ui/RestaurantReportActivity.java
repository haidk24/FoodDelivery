package com.example.foodorderapp.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.utils.RestaurantOrderBadge;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantReportActivity extends AppCompatActivity {
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private final List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;
    private ListenerRegistration ordersRegistration;
    private ListenerRegistration restaurantRegistration;
    private RestaurantOrderBadge orderBadge;
    private BottomNavigationView navigation;
    private TextView restaurantName, restaurantStatus, todayTab, weekTab, monthTab;
    private LinearLayout chart;
    private int selectedDays = 7;
    private Calendar selectedDate;
    private boolean reportLoaded;

    private static class Seller {
        String name;
        int quantity;
        double revenue;
        double price;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_restaurant_report);
        db = FirebaseFirestore.getInstance();
        restaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");
        restaurantName = findViewById(R.id.tv_restaurant_name);
        restaurantStatus = findViewById(R.id.tv_restaurant_status);
        todayTab = findViewById(R.id.btn_tab_today);
        weekTab = findViewById(R.id.btn_tab_7days);
        monthTab = findViewById(R.id.btn_tab_30days);
        chart = findViewById(R.id.layout_revenue_chart);
        chart.removeAllViews();
        navigation = findViewById(R.id.bottomNavigationView);
        orderBadge = new RestaurantOrderBadge(navigation);
        setupNavigation();
        setupFilters();
        setupTopBar();
        findViewById(R.id.btn_view_all_best_sellers).setOnClickListener(v -> open(MenuActivity.class));
        renderReport();
        resolveRestaurant();
    }

    private void setupTopBar() {
        findViewById(R.id.btn_notifications).setOnClickListener(v -> open(OrderDashboardActivity.class));
        findViewById(R.id.btn_profile).setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String email = user != null && user.getEmail() != null ? user.getEmail() : "";
            new AlertDialog.Builder(this).setTitle("Tài khoản Chủ quán")
                .setMessage("Nhà hàng: " + restaurantName.getText() + "\nEmail: " + email)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).setNegativeButton("Đóng", null).show();
        });
        findViewById(R.id.layout_status_pill).setOnClickListener(v -> {
            if (restaurantId == null || restaurantId.isEmpty()) return;
            boolean open = "Mở cửa".contentEquals(restaurantStatus.getText());
            db.collection("restaurants").document(restaurantId)
                .update("status", open ? "closed" : "active")
                .addOnFailureListener(e -> Toast.makeText(this, "Không cập nhật được trạng thái quán", Toast.LENGTH_SHORT).show());
        });
    }

    private void setupFilters() {
        todayTab.setOnClickListener(v -> selectDays(1));
        weekTab.setOnClickListener(v -> selectDays(7));
        monthTab.setOnClickListener(v -> selectDays(30));
        findViewById(R.id.btn_date_picker).setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
                selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, day);
                selectedDays = 1;
                updateTabs();
                renderReport();
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
        updateTabs();
    }

    private void selectDays(int days) {
        selectedDate = null;
        selectedDays = days;
        updateTabs();
        renderReport();
    }

    private void updateTabs() {
        styleTab(todayTab, selectedDate == null && selectedDays == 1, "Hôm nay");
        styleTab(weekTab, selectedDate == null && selectedDays == 7, "7 ngày");
        styleTab(monthTab, selectedDate == null && selectedDays == 30, "30 ngày");
    }

    private void styleTab(TextView tab, boolean active, String label) {
        tab.setText((active ? "✓ " : "") + label);
        tab.setBackgroundResource(active ? R.drawable.bg_chip_category_selected : R.drawable.bg_chip_category_unselected);
        tab.setTextColor(active ? 0xFFFFFFFF : 0xFF334155);
        tab.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setupNavigation() {
        navigation.setSelectedItemId(R.id.nav_report);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_report) return true;
            if (item.getItemId() == R.id.nav_menu) open(MenuActivity.class);
            if (item.getItemId() == R.id.nav_order) open(OrderDashboardActivity.class);
            return true;
        });
    }

    private void open(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.putExtra("EXTRA_RESTAURANT_ID", restaurantId);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void resolveRestaurant() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showError("Vui lòng đăng nhập để xem báo cáo");
            return;
        }
        String uid = user.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String mapped = doc.getString("restaurantId");
            if (mapped != null && !mapped.trim().isEmpty()) {
                listenRestaurant(mapped.trim());
            } else {
                db.collection("restaurants").whereEqualTo("ownerId", uid).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) showError("Không tìm thấy nhà hàng");
                        else listenRestaurant(snap.getDocuments().get(0).getId());
                    }).addOnFailureListener(e -> showError("Không tải được nhà hàng"));
            }
        }).addOnFailureListener(e -> showError("Không tải được tài khoản"));
    }

    private void listenRestaurant(String id) {
        restaurantId = id;
        orderBadge.listen(id);
        if (restaurantRegistration != null) restaurantRegistration.remove();
        restaurantRegistration = db.collection("restaurants").document(id).addSnapshotListener((doc, error) -> {
            if (error == null && doc != null && doc.exists()) {
                String name = doc.getString("name");
                restaurantName.setText(name == null ? "" : name);
                restaurantStatus.setText("active".equals(doc.getString("status")) ? "Mở cửa" : "Tạm đóng");
            }
        });
        if (ordersRegistration != null) ordersRegistration.remove();
        ordersRegistration = db.collection("restaurants").document(id).collection("orders")
            .addSnapshotListener((snapshot, error) -> {
                if (error != null || snapshot == null) {
                    showError("Không tải được báo cáo từ Firebase");
                    return;
                }
                orders.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    Order order = doc.toObject(Order.class);
                    if (order != null) orders.add(order);
                }
                reportLoaded = true;
                renderReport();
            });
    }

    private void showError(String message) {
        orders.clear();
        reportLoaded = false;
        renderReport();
        setText(R.id.tv_chart_peak_detail, "Không tải được dữ liệu");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showUnavailableMetrics() {
        int[] unavailable = {R.id.tv_hero_revenue, R.id.tv_hero_subtext,
            R.id.tv_stat_total_orders, R.id.tv_stat_revenue, R.id.tv_stat_avg_order,
            R.id.tv_stat_success_count, R.id.tv_stat_success_pct,
            R.id.tv_stat_cancelled_count, R.id.tv_stat_cancelled_pct,
            R.id.tv_payment_cod_pct, R.id.tv_payment_cod_amount,
            R.id.tv_payment_cod_count, R.id.tv_payment_other_amount,
            R.id.tv_payment_other_count};
        for (int id : unavailable) setText(id, "—");
        chart.removeAllViews();
        setText(R.id.tv_chart_peak, "—");
        setText(R.id.tv_chart_peak_detail, "Đang chờ dữ liệu Firebase");
        findViewById(R.id.layout_best_seller_1).setVisibility(View.GONE);
        findViewById(R.id.layout_best_seller_2).setVisibility(View.GONE);
        findViewById(R.id.layout_best_seller_3).setVisibility(View.GONE);
    }

    private void renderReport() {
        if (!reportLoaded) {
            showUnavailableMetrics();
            return;
        }
        Calendar start = selectedDate == null ? Calendar.getInstance() : (Calendar) selectedDate.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        if (selectedDate == null) start.add(Calendar.DAY_OF_YEAR, 1 - selectedDays);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, selectedDays);
        Date from = start.getTime(), until = end.getTime();
        String period = selectedDate != null ? dayFormat.format(from)
            : selectedDays == 1 ? "HÔM NAY" : selectedDays + " NGÀY";

        int total = 0, completed = 0, cancelled = 0, codCount = 0, otherCount = 0;
        double revenue = 0, codRevenue = 0, otherRevenue = 0;
        Map<String, Double> byDay = new LinkedHashMap<>();
        Map<String, Integer> completedByDay = new LinkedHashMap<>();
        Map<String, Seller> sellers = new HashMap<>();
        Calendar cursor = (Calendar) start.clone();
        for (int i = 0; i < selectedDays; i++) {
            String key = dayFormat.format(cursor.getTime());
            byDay.put(key, 0d);
            completedByDay.put(key, 0);
            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }
        for (Order order : orders) {
            Date created = order.getCreatedAt();
            if (created == null || created.before(from) || !created.before(until)) continue;
            total++;
            String status = Order.normalizeStatus(order.getStatus());
            if (Order.STATUS_CANCELLED.equals(status)) cancelled++;
            if (!Order.STATUS_COMPLETED.equals(status)) continue;
            completed++;
            double amount = Math.max(0, order.getTotalAmount());
            revenue += amount;
            String key = dayFormat.format(created);
            byDay.put(key, byDay.get(key) + amount);
            completedByDay.put(key, completedByDay.get(key) + 1);
            if (order.getPaymentMethod() != null
                    && order.getPaymentMethod().toUpperCase(Locale.ROOT).contains("COD")) {
                codCount++; codRevenue += amount;
            } else {
                otherCount++; otherRevenue += amount;
            }
            if (order.getItems() == null) continue;
            for (CartItem item : order.getItems()) {
                if (item == null || item.getQuantity() <= 0) continue;
                String itemKey = item.getFoodId() != null && !item.getFoodId().isEmpty()
                    ? item.getFoodId() : item.getName();
                if (itemKey == null || itemKey.isEmpty()) continue;
                Seller seller = sellers.get(itemKey);
                if (seller == null) {
                    seller = new Seller();
                    seller.name = item.getName() == null ? "Món ăn" : item.getName();
                    sellers.put(itemKey, seller);
                }
                seller.quantity += item.getQuantity();
                seller.revenue += item.getPrice() * item.getQuantity();
                seller.price = item.getPrice();
            }
        }
        setText(R.id.tv_hero_title, "DOANH THU HOÀN TẤT (" + period + ")");
        setText(R.id.tv_hero_revenue, formatMoney(revenue));
        setText(R.id.tv_hero_subtext, "Tổng tiền đơn hoàn tất • " + completed + " đơn");
        setText(R.id.tv_stat_total_orders, total + " đơn");
        setText(R.id.tv_stat_revenue, formatMoney(revenue));
        setText(R.id.tv_stat_avg_order, "TB: " + formatMoney(completed == 0 ? 0 : revenue / completed) + "/đơn");
        setText(R.id.tv_stat_success_count, String.valueOf(completed));
        setText(R.id.tv_stat_success_pct, percent(completed, total));
        setText(R.id.tv_stat_cancelled_count, String.valueOf(cancelled));
        setText(R.id.tv_stat_cancelled_pct, percent(cancelled, total));
        setText(R.id.tv_payment_cod_pct, percent(codCount, completed));
        setText(R.id.tv_payment_cod_amount, formatMoney(codRevenue));
        setText(R.id.tv_payment_cod_count, codCount + " đơn hoàn tất");
        setText(R.id.tv_payment_other_amount, formatMoney(otherRevenue));
        setText(R.id.tv_payment_other_count, otherCount + " đơn hoàn tất");
        renderChart(byDay, completedByDay);
        renderSellers(sellers);
    }

    private void renderChart(Map<String, Double> daily, Map<String, Integer> counts) {
        chart.removeAllViews();
        double peak = 0;
        String peakDay = "";
        int completed = 0;
        for (Map.Entry<String, Double> entry : daily.entrySet()) {
            if (entry.getValue() > peak) { peak = entry.getValue(); peakDay = entry.getKey(); }
            completed += counts.get(entry.getKey());
        }
        setText(R.id.tv_chart_period, selectedDate != null ? "Ngày " + dayFormat.format(selectedDate.getTime())
            : selectedDays + " ngày gần nhất");
        setText(R.id.tv_chart_peak, "Đỉnh: " + formatMoney(peak));
        setText(R.id.tv_chart_peak_detail, peak == 0 ? "Chưa có doanh thu"
            : "● Cao điểm: " + peakDay + " (" + formatMoney(peak) + ")");
        setText(R.id.tv_chart_completed, completed + " đơn hoàn tất");
        int width = dp(selectedDays == 30 ? 5 : 22);
        for (Map.Entry<String, Double> entry : daily.entrySet()) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1));
            View bar = new View(this);
            int height = peak == 0 ? 2 : Math.max(2, (int) (100 * entry.getValue() / peak));
            bar.setLayoutParams(new LinearLayout.LayoutParams(width, dp(height)));
            bar.setBackgroundResource(entry.getKey().equals(peakDay) && peak > 0
                ? R.drawable.bg_bar_highlight_t7 : R.drawable.bg_bar_peach);
            column.addView(bar);
            TextView label = new TextView(this);
            label.setText(selectedDays == 30 ? entry.getKey().substring(0, 2) : entry.getKey());
            label.setTextSize(selectedDays == 30 ? 8 : 10);
            label.setTextColor(0xFF64748B);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            column.addView(label);
            chart.addView(column);
        }
    }

    private void renderSellers(Map<String, Seller> map) {
        List<Seller> ranked = new ArrayList<>(map.values());
        ranked.sort((a, b) -> Integer.compare(b.quantity, a.quantity));
        int[] rows = {R.id.layout_best_seller_1, R.id.layout_best_seller_2, R.id.layout_best_seller_3};
        int[] names = {R.id.tv_best_seller_name_1, R.id.tv_best_seller_name_2, R.id.tv_best_seller_name_3};
        int[] quantities = {R.id.tv_best_seller_quantity_1, R.id.tv_best_seller_quantity_2, R.id.tv_best_seller_quantity_3};
        int[] amounts = {R.id.tv_best_seller_amount_1, R.id.tv_best_seller_amount_2, R.id.tv_best_seller_amount_3};
        int[] prices = {R.id.tv_best_seller_price_1, R.id.tv_best_seller_price_2, R.id.tv_best_seller_price_3};
        int[] images = {R.id.img_best_seller_1, R.id.img_best_seller_2, R.id.img_best_seller_3};
        for (int i = 0; i < rows.length; i++) {
            findViewById(rows[i]).setVisibility(i < ranked.size() ? View.VISIBLE : View.GONE);
            if (i >= ranked.size()) continue;
            Seller seller = ranked.get(i);
            setText(names[i], seller.name);
            setText(quantities[i], seller.quantity + " phần đã bán");
            setText(amounts[i], formatMoney(seller.revenue));
            setText(prices[i], formatMoney(seller.price) + "/phần");
            findViewById(images[i]).setVisibility(View.GONE);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String formatMoney(double value) { return money.format(Math.round(value)) + " đ"; }
    private String percent(int n, int d) {
        return String.format(Locale.getDefault(), "%.1f%%", d == 0 ? 0d : n * 100d / d);
    }
    private void setText(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) view.setText(text);
    }
    @Override protected void onDestroy() {
        if (ordersRegistration != null) ordersRegistration.remove();
        if (restaurantRegistration != null) restaurantRegistration.remove();
        if (orderBadge != null) orderBadge.stop();
        super.onDestroy();
    }
}
