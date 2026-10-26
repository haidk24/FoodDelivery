package com.example.foodorderapp.ui.restaurant;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.feature.orders.viewmodel.OrdersViewModel;
import com.example.foodorderapp.ui.RestaurantMainActivity;

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

public class RestaurantReportFragment extends Fragment {
    private static final String ARG_RESTAURANT_ID = "restaurant_id";
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private final List<Order> orders = new ArrayList<>();
    private OrdersViewModel ordersViewModel;
    private String restaurantId = "";
    private View root;
    private LinearLayout chart;
    private TextView todayTab, weekTab, monthTab;
    private int selectedDays = 7;
    private Calendar selectedDate;
    private boolean loaded;

    private static class Seller {
        String name;
        int quantity;
        double revenue;
        double price;
    }

    public static RestaurantReportFragment newInstance(String id) {
        RestaurantReportFragment fragment = new RestaurantReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESTAURANT_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        root = inflater.inflate(R.layout.activity_restaurant_report, container, false);
        ordersViewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        if (getArguments() != null) restaurantId = value(getArguments().getString(ARG_RESTAURANT_ID));
        prepareEmbeddedLayout();
        todayTab = root.findViewById(R.id.btn_tab_today);
        weekTab = root.findViewById(R.id.btn_tab_7days);
        monthTab = root.findViewById(R.id.btn_tab_30days);
        chart = root.findViewById(R.id.layout_revenue_chart);
        chart.removeAllViews();
        setupFilters();
        root.findViewById(R.id.btn_view_all_best_sellers).setOnClickListener(v -> {
            if (getActivity() instanceof RestaurantMainActivity) {
                ((RestaurantMainActivity) getActivity()).selectTab(R.id.nav_menu);
            }
        });
        renderReport();
        if (!restaurantId.isEmpty()) observeOrders();
        return root;
    }

    private void prepareEmbeddedLayout() {
        root.setFitsSystemWindows(false);
        root.findViewById(R.id.restaurant_top_bar).setVisibility(View.GONE);
        root.findViewById(R.id.restaurant_top_divider).setVisibility(View.GONE);
        root.findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
        View content = root.findViewById(R.id.restaurant_screen_content);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) content.getLayoutParams();
        params.bottomMargin = 0;
        content.setLayoutParams(params);
    }

    private void setupFilters() {
        todayTab.setOnClickListener(v -> selectDays(1));
        weekTab.setOnClickListener(v -> selectDays(7));
        monthTab.setOnClickListener(v -> selectDays(30));
        root.findViewById(R.id.btn_date_picker).setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
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

    public void setRestaurantId(String id) {
        String next = value(id);
        if (next.equals(restaurantId)) return;
        restaurantId = next;
        if (isAdded() && !next.isEmpty()) observeOrders();
    }

    private void observeOrders() {
        ordersViewModel.getOrders().observe(getViewLifecycleOwner(), value -> {
            if (Boolean.TRUE.equals(ordersViewModel.getLoading().getValue())) return;
            renderOrders(value);
        });
        ordersViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            loaded = false;
            renderReport();
            setText(R.id.tv_chart_peak_detail, "Không tải được dữ liệu");
        });
        ordersViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                loaded = false;
                showUnavailableMetrics();
            } else if (loading != null && ordersViewModel.getError().getValue() == null) {
                renderOrders(ordersViewModel.getOrders().getValue());
            }
        });
        ordersViewModel.observeRestaurant(restaurantId);
    }

    private void renderOrders(List<Order> value) {
        orders.clear();
        if (value != null) orders.addAll(value);
        loaded = true;
        renderReport();
    }

    private void renderReport() {
        if (root == null) return;
        if (!loaded) {
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
        Date from = start.getTime();
        Date until = end.getTime();
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
            if (order.isCodPayment()) {
                codCount++;
                codRevenue += amount;
            } else {
                otherCount++;
                otherRevenue += amount;
            }
            if (order.getItems() == null) continue;
            for (CartItem item : order.getItems()) {
                if (item == null || item.getQuantity() <= 0) continue;
                String itemKey = item.getFoodId() != null && !item.getFoodId().isEmpty() ? item.getFoodId() : item.getName();
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

    private void showUnavailableMetrics() {
        int[] ids = {R.id.tv_hero_revenue, R.id.tv_hero_subtext, R.id.tv_stat_total_orders,
                R.id.tv_stat_revenue, R.id.tv_stat_avg_order, R.id.tv_stat_success_count,
                R.id.tv_stat_success_pct, R.id.tv_stat_cancelled_count, R.id.tv_stat_cancelled_pct,
                R.id.tv_payment_cod_pct, R.id.tv_payment_cod_amount, R.id.tv_payment_cod_count,
                R.id.tv_payment_other_amount, R.id.tv_payment_other_count};
        for (int id : ids) setText(id, "—");
        chart.removeAllViews();
        setText(R.id.tv_chart_peak, "—");
        setText(R.id.tv_chart_peak_detail, "Đang chờ dữ liệu Firebase");
        root.findViewById(R.id.layout_best_seller_1).setVisibility(View.GONE);
        root.findViewById(R.id.layout_best_seller_2).setVisibility(View.GONE);
        root.findViewById(R.id.layout_best_seller_3).setVisibility(View.GONE);
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
            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1));
            View bar = new View(requireContext());
            int height = peak == 0 ? 2 : Math.max(2, (int) (100 * entry.getValue() / peak));
            bar.setLayoutParams(new LinearLayout.LayoutParams(width, dp(height)));
            bar.setBackgroundResource(entry.getKey().equals(peakDay) && peak > 0
                    ? R.drawable.bg_bar_highlight_t7 : R.drawable.bg_bar_peach);
            column.addView(bar);
            TextView label = new TextView(requireContext());
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
        ranked.sort((first, second) -> Integer.compare(second.quantity, first.quantity));
        int[] rows = {R.id.layout_best_seller_1, R.id.layout_best_seller_2, R.id.layout_best_seller_3};
        int[] names = {R.id.tv_best_seller_name_1, R.id.tv_best_seller_name_2, R.id.tv_best_seller_name_3};
        int[] quantities = {R.id.tv_best_seller_quantity_1, R.id.tv_best_seller_quantity_2, R.id.tv_best_seller_quantity_3};
        int[] amounts = {R.id.tv_best_seller_amount_1, R.id.tv_best_seller_amount_2, R.id.tv_best_seller_amount_3};
        int[] prices = {R.id.tv_best_seller_price_1, R.id.tv_best_seller_price_2, R.id.tv_best_seller_price_3};
        int[] images = {R.id.img_best_seller_1, R.id.img_best_seller_2, R.id.img_best_seller_3};
        for (int i = 0; i < rows.length; i++) {
            root.findViewById(rows[i]).setVisibility(i < ranked.size() ? View.VISIBLE : View.GONE);
            if (i >= ranked.size()) continue;
            Seller seller = ranked.get(i);
            setText(names[i], seller.name);
            setText(quantities[i], seller.quantity + " phần đã bán");
            setText(amounts[i], formatMoney(seller.revenue));
            setText(prices[i], formatMoney(seller.price) + "/phần");
            root.findViewById(images[i]).setVisibility(View.GONE);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String formatMoney(double value) { return money.format(Math.round(value)) + " đ"; }
    private String percent(int value, int total) {
        return String.format(Locale.getDefault(), "%.1f%%", total == 0 ? 0d : value * 100d / total);
    }
    private void setText(int id, String text) {
        TextView view = root.findViewById(id);
        if (view != null) view.setText(text);
    }
    private static String value(String text) { return text == null ? "" : text.trim(); }

    @Override public void onDestroyView() {
        root = null;
        super.onDestroyView();
    }
}
