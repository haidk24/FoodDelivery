package com.example.foodorderapp.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.feature.customer.cart.CartViewModel;
import com.example.foodorderapp.feature.orders.viewmodel.OrdersViewModel;
import com.example.foodorderapp.ui.MainActivity;
import com.example.foodorderapp.ui.OrderTrackingActivity;
import com.example.foodorderapp.ui.OrderItemsActivity;
import com.example.foodorderapp.ui.ReviewActivity;
import com.example.foodorderapp.ui.adapter.OrderAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyOrderFragment extends Fragment {

    private RecyclerView rvOrders;
    private View layoutEmptyOrders;
    private MaterialButton btnExploreDishes;

    private View btnSearchOrder;
    private View layoutSearchBox;
    private EditText etSearchOrder;

    // Filter Tabs
    private View tabAll, tabShipping, tabPending, tabDelivered, tabCancelled;
    private TextView tvTabAllTitle, tvTabShippingTitle, tvTabPendingTitle, tvTabDeliveredTitle, tvTabCancelledTitle;
    private TextView tvBadgeAll, tvBadgeShipping, tvBadgePending, tvBadgeDelivered, tvBadgeCancelled;

    private View cardCustomerSupport;
    private View btnContactSupport;

    private OrderAdapter orderAdapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();

    private String currentFilter = "all";
    private String currentSearchKeyword = "";

    private OrdersViewModel ordersViewModel;
    private CartViewModel cartViewModel;

    public MyOrderFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        ordersViewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        setupTabs();
        setupSearch();
        setupSupportAction();

        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter(requireContext(), filteredOrders);
        orderAdapter.setCustomerActionListener(new OrderAdapter.CustomerActionListener() {
            @Override
            public void onTrackOrder(Order order) {
                openOrderTracking(order);
            }

            @Override
            public void onReviewOrder(Order order) {
                openReview(order);
            }

            @Override
            public void onViewOrderDetails(Order order) {
                openOrderDetails(order);
            }

            @Override
            public void onReorder(Order order) {
                reorder(order);
            }
        });
        rvOrders.setAdapter(orderAdapter);

        loadMyOrders();
    }

    private void initViews(View view) {
        rvOrders = view.findViewById(R.id.rv_orders);
        layoutEmptyOrders = view.findViewById(R.id.layout_empty_orders);
        btnExploreDishes = view.findViewById(R.id.btn_explore_dishes);

        btnSearchOrder = view.findViewById(R.id.btn_search_order);
        layoutSearchBox = view.findViewById(R.id.layout_search_box);
        etSearchOrder = view.findViewById(R.id.et_search_order);

        tabAll = view.findViewById(R.id.tab_all);
        tabShipping = view.findViewById(R.id.tab_shipping);
        tabPending = view.findViewById(R.id.tab_pending);
        tabDelivered = view.findViewById(R.id.tab_delivered);
        tabCancelled = view.findViewById(R.id.tab_cancelled);

        tvTabAllTitle = view.findViewById(R.id.tv_tab_all_title);
        tvTabShippingTitle = view.findViewById(R.id.tv_tab_shipping_title);
        tvTabPendingTitle = view.findViewById(R.id.tv_tab_pending_title);
        tvTabDeliveredTitle = view.findViewById(R.id.tv_tab_delivered_title);
        tvTabCancelledTitle = view.findViewById(R.id.tv_tab_cancelled_title);

        tvBadgeAll = view.findViewById(R.id.tv_badge_all);
        tvBadgeShipping = view.findViewById(R.id.tv_badge_shipping);
        tvBadgePending = view.findViewById(R.id.tv_badge_pending);
        tvBadgeDelivered = view.findViewById(R.id.tv_badge_delivered);
        tvBadgeCancelled = view.findViewById(R.id.tv_badge_cancelled);

        cardCustomerSupport = view.findViewById(R.id.card_customer_support);
        btnContactSupport = view.findViewById(R.id.btn_contact_support);

        View cardUserAvatar = view.findViewById(R.id.card_user_avatar);
        if (cardUserAvatar != null) {
            cardUserAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openProfile();
                }
            });
        }

        btnExploreDishes.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            }
        });
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> selectFilter("all"));
        tabShipping.setOnClickListener(v -> selectFilter("shipping"));
        tabPending.setOnClickListener(v -> selectFilter("pending"));
        tabDelivered.setOnClickListener(v -> selectFilter("delivered"));
        tabCancelled.setOnClickListener(v -> selectFilter("cancelled"));
    }

    private void selectFilter(String filterKey) {
        currentFilter = filterKey;
        updateTabStyles();
        applyFilterAndSearch();
    }

    private void updateTabStyles() {
        if (getContext() == null) return;

        // Reset all tabs to inactive
        setTabInactive(tabAll, tvTabAllTitle, tvBadgeAll);
        setTabInactive(tabShipping, tvTabShippingTitle, tvBadgeShipping);
        setTabInactive(tabPending, tvTabPendingTitle, tvBadgePending);
        setTabInactive(tabDelivered, tvTabDeliveredTitle, tvBadgeDelivered);
        setTabInactive(tabCancelled, tvTabCancelledTitle, tvBadgeCancelled);

        // Highlight active tab
        switch (currentFilter) {
            case "all":
                setTabActive(tabAll, tvTabAllTitle, tvBadgeAll);
                break;
            case "shipping":
                setTabActive(tabShipping, tvTabShippingTitle, tvBadgeShipping);
                break;
            case "pending":
                setTabActive(tabPending, tvTabPendingTitle, tvBadgePending);
                break;
            case "delivered":
                setTabActive(tabDelivered, tvTabDeliveredTitle, tvBadgeDelivered);
                break;
            case "cancelled":
                setTabActive(tabCancelled, tvTabCancelledTitle, tvBadgeCancelled);
                break;
        }
    }

    private void setTabActive(View tab, TextView title, TextView badge) {
        tab.setBackgroundResource(R.drawable.bg_filter_chip_active);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        badge.setBackgroundResource(R.drawable.bg_badge_filter_active);
        badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void setTabInactive(View tab, TextView title, TextView badge) {
        tab.setBackgroundResource(R.drawable.bg_filter_chip_inactive);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
        badge.setBackgroundResource(R.drawable.bg_badge_filter_inactive);
        badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
    }

    private void setupSearch() {
        btnSearchOrder.setOnClickListener(v -> {
            if (layoutSearchBox.getVisibility() == View.VISIBLE) {
                layoutSearchBox.setVisibility(View.GONE);
                etSearchOrder.setText("");
                currentSearchKeyword = "";
                applyFilterAndSearch();
            } else {
                layoutSearchBox.setVisibility(View.VISIBLE);
                etSearchOrder.requestFocus();
            }
        });

        etSearchOrder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSupportAction() {
        View.OnClickListener supportAction = v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Trung tâm CSKH 24/7 🎧")
                    .setMessage("Đội ngũ chăm sóc khách hàng Food Order Pro luôn sẵn sàng hỗ trợ bạn.\n\nHotline: 1900 1234 (Miễn phí)")
                    .setPositiveButton("Gọi tổng đài", (dialog, which) -> {
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setData(Uri.parse("tel:19001234"));
                        startActivity(callIntent);
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
        };

        cardCustomerSupport.setOnClickListener(supportAction);
        btnContactSupport.setOnClickListener(supportAction);
    }

    private void loadMyOrders() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showEmptyState(true);
            return;
        }
        ordersViewModel.getOrders().observe(getViewLifecycleOwner(), newOrders -> {
            if (Boolean.TRUE.equals(ordersViewModel.getLoading().getValue())) return;
            renderOrders(newOrders);
        });
        ordersViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && allOrders.isEmpty()) showEmptyState(true);
        });
        ordersViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.FALSE.equals(loading)) renderOrders(ordersViewModel.getOrders().getValue());
        });
        ordersViewModel.observeCustomer(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private void renderOrders(List<Order> value) {
        allOrders.clear();
        if (value != null) allOrders.addAll(value);
        updateFilterCounts();
        applyFilterAndSearch();
    }

    private void updateFilterCounts() {
        int countAll = allOrders.size();
        int countShipping = 0;
        int countPending = 0;
        int countDelivered = 0;
        int countCancelled = 0;

        for (Order o : allOrders) {
            String s = Order.normalizeStatus(o.getStatus());
            if (Order.STATUS_SHIPPING.equals(s)) {
                countShipping++;
            } else if (Order.STATUS_PENDING.equals(s) || Order.STATUS_PREPARING.equals(s) || Order.STATUS_CONFIRMED.equals(s)) {
                countPending++;
            } else if (Order.STATUS_COMPLETED.equals(s)) {
                countDelivered++;
            } else if (Order.STATUS_CANCELLED.equals(s)) {
                countCancelled++;
            }
        }

        tvBadgeAll.setText(String.valueOf(countAll));
        tvBadgeShipping.setText(String.valueOf(countShipping));
        tvBadgePending.setText(String.valueOf(countPending));
        tvBadgeDelivered.setText(String.valueOf(countDelivered));
        tvBadgeCancelled.setText(String.valueOf(countCancelled));
    }

    private void applyFilterAndSearch() {
        filteredOrders.clear();

        for (Order order : allOrders) {
            String s = Order.normalizeStatus(order.getStatus());

            // 1. Check category filter
            boolean matchesCategory = false;
            switch (currentFilter) {
                case "all":
                    matchesCategory = true;
                    break;
                case "shipping":
                    matchesCategory = Order.STATUS_SHIPPING.equals(s);
                    break;
                case "pending":
                    matchesCategory = Order.STATUS_PENDING.equals(s) || Order.STATUS_PREPARING.equals(s) || Order.STATUS_CONFIRMED.equals(s);
                    break;
                case "delivered":
                    matchesCategory = Order.STATUS_COMPLETED.equals(s);
                    break;
                case "cancelled":
                    matchesCategory = Order.STATUS_CANCELLED.equals(s);
                    break;
            }

            if (!matchesCategory) continue;

            // 2. Check keyword search (Order ID or item name)
            if (!TextUtils.isEmpty(currentSearchKeyword)) {
                boolean matchesKeyword = false;
                if (order.getOrderId() != null && order.getOrderId().toLowerCase(Locale.ROOT).contains(currentSearchKeyword)) {
                    matchesKeyword = true;
                } else if (order.getItems() != null) {
                    for (CartItem item : order.getItems()) {
                        if (item != null && item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(currentSearchKeyword)) {
                            matchesKeyword = true;
                            break;
                        }
                    }
                }
                if (!matchesKeyword) continue;
            }

            filteredOrders.add(order);
        }

        orderAdapter.notifyDataSetChanged();
        showEmptyState(filteredOrders.isEmpty());
    }

    private void openReview(Order order) {
        if (order == null || !Order.STATUS_COMPLETED.equals(Order.normalizeStatus(order.getStatus()))) return;
        String customerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : order.getCustomerId();
        List<CartItem> items = order.getItems();
        String title = items == null || items.isEmpty() || items.get(0) == null
                ? "Đơn hàng #" + (order.getOrderId() == null ? "" : order.getOrderId())
                : items.get(0).getName() + (items.size() > 1 ? " và " + (items.size() - 1) + " món khác" : "");
        Intent intent = new Intent(requireContext(), ReviewActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        intent.putExtra("customerId", customerId);
        intent.putExtra("restaurantId", order.getRestaurantId());
        intent.putExtra("foodName", title);
        startActivity(intent);
    }

    private void openOrderTracking(Order order) {
        Intent intent = new Intent(requireContext(), OrderTrackingActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());
        intent.putExtra("EXTRA_TOTAL", order.getTotalAmount());
        intent.putExtra("EXTRA_STATUS", order.getStatus());
        intent.putExtra("EXTRA_CUSTOMER_NAME", order.getCustomerName());
        intent.putExtra("EXTRA_CUSTOMER_PHONE", order.getCustomerPhone());
        intent.putExtra("EXTRA_CUSTOMER_ADDRESS", order.getDeliveryAddressText());
        intent.putExtra("EXTRA_RESTAURANT_ID", order.getRestaurantId());
        startActivity(intent);
    }

    private void openOrderDetails(Order order) {
        if (order == null || TextUtils.isEmpty(order.getOrderId())) return;
        Intent intent = new Intent(requireContext(), OrderItemsActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());
        intent.putExtra("EXTRA_TOTAL", order.getTotalAmount());
        intent.putExtra("EXTRA_STATUS", order.getStatus());
        intent.putExtra("EXTRA_CUSTOMER_NAME", order.getCustomerName());
        intent.putExtra("EXTRA_CUSTOMER_PHONE", order.getCustomerPhone());
        intent.putExtra("EXTRA_CUSTOMER_ADDRESS", order.getDeliveryAddressText());
        intent.putExtra("EXTRA_RESTAURANT_ID", order.getRestaurantId());
        startActivity(intent);
    }

    private void reorder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) return;
        for (CartItem item : order.getItems()) {
            if (item == null) continue;
            Food food = new Food();
            food.setFoodId(item.getFoodId());
            food.setName(item.getName());
            food.setPrice(item.getPrice());
            food.setNewPrice(item.getPrice());
            food.setImageUrl(item.getImageUrl());
            food.setRestaurantId(order.getRestaurantId());
            cartViewModel.addOrIncrement(food, item.getQuantity());
        }
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).updateCartBadge();
        BottomNavigationView navigation = requireActivity().findViewById(R.id.bottom_navigation);
        if (navigation != null) navigation.setSelectedItemId(R.id.nav_cart);
    }

    private void showEmptyState(boolean isEmpty) {
        layoutEmptyOrders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
