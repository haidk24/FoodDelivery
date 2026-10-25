package com.example.foodorderapp.ui.restaurant;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.ui.adapter.RestaurantOrderAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RestaurantOrdersFragment extends Fragment {
    private static final String ARG_RESTAURANT_ID = "restaurant_id";
    private final List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private RestaurantOrderAdapter adapter;
    private String restaurantId = "";
    private String selectedStatus = "Tất cả";
    private String searchQuery = "";
    private int daysFilter;
    private String paymentFilter = "Tất cả";
    private RecyclerView recycler;
    private LinearLayout empty;
    private ProgressBar progress;
    private EditText search;
    private ImageView clearSearch;
    private TextView chipAll, chipPending, chipPreparing, chipDelivering, chipCompleted, urgentCount;
    private TextView emptyTitle, emptySubtitle;

    public static RestaurantOrdersFragment newInstance(String id) {
        RestaurantOrdersFragment fragment = new RestaurantOrdersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESTAURANT_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.activity_order_dashboard, container, false);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) restaurantId = value(getArguments().getString(ARG_RESTAURANT_ID));
        prepareEmbeddedLayout(root);
        bind(root);
        setupRecycler();
        setupFilters(root);
        if (!restaurantId.isEmpty()) listenOrders();
        return root;
    }

    private void prepareEmbeddedLayout(View root) {
        root.setFitsSystemWindows(false);
        root.findViewById(R.id.restaurant_top_bar).setVisibility(View.GONE);
        root.findViewById(R.id.restaurant_top_divider).setVisibility(View.GONE);
        root.findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
        View content = root.findViewById(R.id.restaurant_screen_content);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) content.getLayoutParams();
        params.bottomMargin = 0;
        content.setLayoutParams(params);
    }

    private void bind(View root) {
        recycler = root.findViewById(R.id.rvOrders);
        empty = root.findViewById(R.id.layout_empty_orders);
        progress = root.findViewById(R.id.progress_bar);
        search = root.findViewById(R.id.et_search_order);
        clearSearch = root.findViewById(R.id.btn_clear_search);
        chipAll = root.findViewById(R.id.chip_all);
        chipPending = root.findViewById(R.id.chip_pending);
        chipPreparing = root.findViewById(R.id.chip_preparing);
        chipDelivering = root.findViewById(R.id.chip_delivering);
        chipCompleted = root.findViewById(R.id.chip_completed);
        urgentCount = root.findViewById(R.id.tv_urgent_orders_count);
        emptyTitle = root.findViewById(R.id.tv_empty_orders_title);
        emptySubtitle = root.findViewById(R.id.tv_empty_orders_subtitle);
    }

    private void setupRecycler() {
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RestaurantOrderAdapter(requireContext(), orders);
        adapter.setSummaryListener((total, pending, preparing, delivering, completed) -> {
            chipAll.setText("Tất cả (" + total + ")");
            chipPending.setText("Chờ xác nhận " + pending);
            chipPreparing.setText("Đang chuẩn bị (" + preparing + ")");
            chipDelivering.setText("Đang giao hàng (" + delivering + ")");
            chipCompleted.setText("Đã hoàn thành (" + completed + ")");
            urgentCount.setText(pending + " đơn mới");
            empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });
        recycler.setAdapter(adapter);
    }

    private void setupFilters(View root) {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                clearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.filter(searchQuery, selectedStatus);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        clearSearch.setOnClickListener(v -> search.setText(""));
        root.findViewById(R.id.btn_filter_tune).setOnClickListener(v -> showAdvancedFilters());
        setupChip(chipAll, "Tất cả");
        setupChip(chipPending, "Chờ xác nhận");
        setupChip(chipPreparing, "Đang chuẩn bị");
        setupChip(chipDelivering, "Đang giao hàng");
        setupChip(chipCompleted, "Đã hoàn thành");
    }

    private void setupChip(TextView chip, String status) {
        chip.setOnClickListener(v -> {
            selectedStatus = status;
            for (TextView item : new TextView[]{chipAll, chipPending, chipPreparing, chipDelivering, chipCompleted}) {
                item.setBackgroundResource(R.drawable.bg_chip_category_unselected);
                item.setTextColor(0xFF334155);
                item.setTypeface(null, Typeface.NORMAL);
            }
            chip.setBackgroundResource(R.drawable.bg_chip_category_selected);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(null, Typeface.BOLD);
            adapter.filter(searchQuery, selectedStatus);
        });
    }

    private void showAdvancedFilters() {
        String[] ranges = {"Tất cả thời gian", "Hôm nay", "7 ngày", "30 ngày"};
        int checked = daysFilter == 1 ? 1 : daysFilter == 7 ? 2 : daysFilter == 30 ? 3 : 0;
        new AlertDialog.Builder(requireContext()).setTitle("Lọc theo thời gian")
                .setSingleChoiceItems(ranges, checked, (dialog, which) -> {
                    daysFilter = new int[]{0, 1, 7, 30}[which];
                    dialog.dismiss();
                    showPaymentFilter();
                }).setNegativeButton("Đóng", null).show();
    }

    private void showPaymentFilter() {
        String[] methods = {"Tất cả", "COD", "Khác COD"};
        int checked = "COD".equals(paymentFilter) ? 1 : "Khác COD".equals(paymentFilter) ? 2 : 0;
        new AlertDialog.Builder(requireContext()).setTitle("Phương thức thanh toán")
                .setSingleChoiceItems(methods, checked, (dialog, which) -> {
                    paymentFilter = methods[which];
                    dialog.dismiss();
                    applyAdvancedFilters();
                }).setNegativeButton("Bỏ qua", (dialog, which) -> applyAdvancedFilters())
                .setOnCancelListener(dialog -> applyAdvancedFilters())
                .show();
    }

    private void applyAdvancedFilters() {
        adapter.setAdvancedFilters(daysFilter, paymentFilter);
        adapter.filter(searchQuery, selectedStatus);
    }

    public void setRestaurantId(String id) {
        String next = value(id);
        if (next.equals(restaurantId)) return;
        restaurantId = next;
        if (isAdded() && !next.isEmpty()) listenOrders();
    }

    private void listenOrders() {
        if (registration != null) registration.remove();
        progress.setVisibility(View.VISIBLE);
        registration = db.collection("restaurants").document(restaurantId).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    progress.setVisibility(View.GONE);
                    if (error != null || snapshot == null) {
                        showUnavailable(error == null ? "Không tải được đơn hàng" : error.getMessage());
                        return;
                    }
                    emptyTitle.setText("Chưa có đơn hàng nào");
                    emptySubtitle.setText("Các đơn hàng mới sẽ xuất hiện tại đây");
                    orders.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        if (order.getRestaurantId() == null || order.getRestaurantId().isEmpty()) {
                            order.setRestaurantId(restaurantId);
                        }
                        orders.add(order);
                    }
                    Collections.sort(orders, (first, second) -> compareDates(first.getCreatedAt(), second.getCreatedAt()));
                    adapter.updateData(orders);
                    adapter.setAdvancedFilters(daysFilter, paymentFilter);
                    adapter.filter(searchQuery, selectedStatus);
                });
    }

    private int compareDates(Date first, Date second) {
        if (first != null && second != null) return second.compareTo(first);
        if (first != null) return -1;
        return second != null ? 1 : 0;
    }

    private void showUnavailable(String message) {
        orders.clear();
        adapter.updateData(orders);
        emptyTitle.setText("Không tải được đơn hàng");
        emptySubtitle.setText(message == null ? "Vui lòng thử lại" : message);
        Toast.makeText(requireContext(), emptySubtitle.getText(), Toast.LENGTH_LONG).show();
    }

    private static String value(String text) { return text == null ? "" : text.trim(); }

    @Override public void onDestroyView() {
        if (registration != null) registration.remove();
        registration = null;
        super.onDestroyView();
    }
}
