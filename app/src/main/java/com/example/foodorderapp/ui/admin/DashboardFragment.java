package com.example.foodorderapp.ui.admin;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.AdminStats;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.feature.admin.dashboard.AdminDashboardViewModel;
import com.example.foodorderapp.feature.admin.restaurants.AdminRestaurantsViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AdminDashboardViewModel dashboardViewModel;
    private AdminRestaurantsViewModel restaurantsViewModel;

    // Loading & Live Time
    private ProgressBar progressBar;
    private TextView tvLiveTime;

    // Alert Card (Cần xử lý ngay)
    private View cardActionAlert;
    private TextView tvAlertRestaurantName;
    private TextView tvAlertDescription;
    private View btnAlertViewProfile;
    private View btnAlertQuickApprove;
    private Restaurant currentPendingRestaurant;

    // 4 KPI Metric Cards
    private TextView tvRevenue, tvRevenueSub;
    private TextView tvTotalOrders, tvOrdersSub, tvTotalOrdersTag;
    private TextView tvTotalRestaurants, tvPendingBadge, tvRestaurantsTag;
    private TextView tvTotalUsers, tvUsersSub, tvUsersTag;

    // Chart & Weekly
    private TextView tvWeeklyTotal;

    // Recent Orders
    private TextView tvViewAllOrders;
    private TextView tvOrder1Code, tvOrder1Time, tvOrder1Desc, tvOrder1Amount, tvOrder1Status;
    private TextView tvOrder2Code, tvOrder2Time, tvOrder2Desc, tvOrder2Amount, tvOrder2Status;
    private View cardRecentOrder1, cardRecentOrder2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dashboardViewModel = new ViewModelProvider(requireActivity()).get(AdminDashboardViewModel.class);
        restaurantsViewModel = new ViewModelProvider(requireActivity()).get(AdminRestaurantsViewModel.class);

        bindViews(view);
        hideRecentOrderCards();
        setupLiveTime();
        setupClickListeners(view);
        observeData();
        observeRecentOrders();
        dashboardViewModel.loadRecentOrders();

        restaurantsViewModel.observe();
    }

    private void bindViews(View v) {
        progressBar = v.findViewById(R.id.progress_bar);
        tvLiveTime = v.findViewById(R.id.tv_live_time);

        // Alert
        cardActionAlert = v.findViewById(R.id.card_action_alert);
        tvAlertRestaurantName = v.findViewById(R.id.tv_alert_restaurant_name);
        tvAlertDescription = v.findViewById(R.id.tv_alert_description);
        btnAlertViewProfile = v.findViewById(R.id.btn_alert_view_profile);
        btnAlertQuickApprove = v.findViewById(R.id.btn_alert_quick_approve);

        // KPI
        tvRevenue = v.findViewById(R.id.tv_revenue);
        tvRevenueSub = v.findViewById(R.id.tv_revenue_sub);
        tvTotalOrders = v.findViewById(R.id.tv_total_orders);
        tvOrdersSub = v.findViewById(R.id.tv_orders_sub);
        tvTotalOrdersTag = v.findViewById(R.id.tv_total_orders_tag);
        tvTotalRestaurants = v.findViewById(R.id.tv_total_restaurants);
        tvPendingBadge = v.findViewById(R.id.tv_pending_badge);
        tvRestaurantsTag = v.findViewById(R.id.tv_restaurants_tag);
        tvTotalUsers = v.findViewById(R.id.tv_total_users);
        tvUsersSub = v.findViewById(R.id.tv_users_sub);
        tvUsersTag = v.findViewById(R.id.tv_users_tag);

        // Chart
        tvWeeklyTotal = v.findViewById(R.id.tv_weekly_total);

        // Recent Orders
        tvViewAllOrders = v.findViewById(R.id.tv_view_all_orders);
        cardRecentOrder1 = v.findViewById(R.id.card_recent_order_1);
        tvOrder1Code = v.findViewById(R.id.tv_order1_code);
        tvOrder1Time = v.findViewById(R.id.tv_order1_time);
        tvOrder1Desc = v.findViewById(R.id.tv_order1_desc);
        tvOrder1Amount = v.findViewById(R.id.tv_order1_amount);
        tvOrder1Status = v.findViewById(R.id.tv_order1_status);

        cardRecentOrder2 = v.findViewById(R.id.card_recent_order_2);
        tvOrder2Code = v.findViewById(R.id.tv_order2_code);
        tvOrder2Time = v.findViewById(R.id.tv_order2_time);
        tvOrder2Desc = v.findViewById(R.id.tv_order2_desc);
        tvOrder2Amount = v.findViewById(R.id.tv_order2_amount);
        tvOrder2Status = v.findViewById(R.id.tv_order2_status);
    }

    private void setupLiveTime() {
        if (tvLiveTime != null) {
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            tvLiveTime.setText(timeStr + " • Trực tiếp");
        }
    }

    private void setupClickListeners(View rootView) {
        if (btnAlertViewProfile != null) {
            btnAlertViewProfile.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(rootView).navigate(R.id.nav_restaurants);
                } catch (Exception ignored) {}
            });
        }

        if (btnAlertQuickApprove != null) {
            btnAlertQuickApprove.setOnClickListener(v -> {
                if (currentPendingRestaurant != null && currentPendingRestaurant.restaurantId != null) {
                    restaurantsViewModel.approve(currentPendingRestaurant.restaurantId);
                    Toast.makeText(requireContext(), "Đang duyệt nhanh cho " + currentPendingRestaurant.name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không có hồ sơ nhà hàng để duyệt.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (tvViewAllOrders != null) {
            tvViewAllOrders.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(rootView).navigate(R.id.nav_report);
                } catch (Exception ignored) {}
            });
        }
    }

    private void observeData() {
        dashboardViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        dashboardViewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) renderStats(stats);
        });

        // Lắng nghe danh sách nhà hàng chờ duyệt để hiển thị card Alert
        restaurantsViewModel.getPending().observe(getViewLifecycleOwner(), pendingList -> {
            if (pendingList != null && !pendingList.isEmpty()) {
                currentPendingRestaurant = pendingList.get(0);
                if (cardActionAlert != null) cardActionAlert.setVisibility(View.VISIBLE);
                if (tvAlertRestaurantName != null) tvAlertRestaurantName.setText(currentPendingRestaurant.name != null ? currentPendingRestaurant.name : "Nhà hàng mới");
                if (tvAlertDescription != null) tvAlertDescription.setText("Hồ sơ nhà hàng đang chờ phê duyệt");
            } else {
                currentPendingRestaurant = null;
                if (cardActionAlert != null) cardActionAlert.setVisibility(View.GONE);
            }
        });

        // Badge nhà hàng chờ duyệt
        restaurantsViewModel.getPendingCount().observe(getViewLifecycleOwner(), count -> {
            int c = (count != null) ? count : 0;
            if (tvPendingBadge != null) {
                if (c > 0) {
                    tvPendingBadge.setText(c + " chờ duyệt");
                    tvPendingBadge.setTextColor(0xFFD97706);
                } else {
                    tvPendingBadge.setText("Hoạt động bình thường");
                    tvPendingBadge.setTextColor(0xFF16A34A);
                }
            }
        });

        // Thông báo lỗi
        dashboardViewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });

        restaurantsViewModel.getActionSuccess().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(requireContext(), "Thao tác thành công!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderStats(AdminStats stats) {
        if (tvTotalUsers != null) {
            long users = stats.totalUsers;
            tvTotalUsers.setText(formatNumber(users) + " người");
            if (tvUsersTag != null) tvUsersTag.setText(formatNumber(users) + " user");
            if (tvUsersSub != null) tvUsersSub.setText("Tài khoản đã đăng ký");
        }

        if (tvTotalRestaurants != null) {
            long res = stats.totalRestaurants;
            tvTotalRestaurants.setText(formatNumber(res) + " quán");
            if (tvRestaurantsTag != null) tvRestaurantsTag.setText(formatNumber(res) + " quán");
        }

        if (tvTotalOrders != null) {
            long orders = stats.totalOrdersToday;
            tvTotalOrders.setText(formatNumber(orders) + " đơn");
            if (tvTotalOrdersTag != null) tvTotalOrdersTag.setText(formatNumber(orders) + " đơn");
            if (tvOrdersSub != null) {
                tvOrdersSub.setText("Đơn phát sinh hôm nay");
            }
        }

        if (tvRevenue != null) {
            long rev = stats.revenueToday;
            tvRevenue.setText(formatRevenue(rev));
            if (tvRevenueSub != null) tvRevenueSub.setText("Từ đơn hoàn tất");
        }

        if (tvWeeklyTotal != null) {
            tvWeeklyTotal.setText("Tổng tuần: " + formatCurrency(stats.revenueThisWeek));
        }
    }

    private void observeRecentOrders() {
        dashboardViewModel.getRecentOrders().observe(getViewLifecycleOwner(), orders -> {
            hideRecentOrderCards();
            if (orders == null || orders.isEmpty()) return;
            if (cardRecentOrder1 != null) cardRecentOrder1.setVisibility(View.VISIBLE);
            bindOrderView(orders.get(0), tvOrder1Code, tvOrder1Time, tvOrder1Desc, tvOrder1Amount, tvOrder1Status);
            if (orders.size() > 1) {
                if (cardRecentOrder2 != null) cardRecentOrder2.setVisibility(View.VISIBLE);
                bindOrderView(orders.get(1), tvOrder2Code, tvOrder2Time, tvOrder2Desc, tvOrder2Amount, tvOrder2Status);
            }
        });
    }

    private void hideRecentOrderCards() {
        if (cardRecentOrder1 != null) cardRecentOrder1.setVisibility(View.GONE);
        if (cardRecentOrder2 != null) cardRecentOrder2.setVisibility(View.GONE);
    }

    private void bindOrderView(Order order, TextView tvCode, TextView tvTime, TextView tvDesc, TextView tvAmount, TextView tvStatus) {
        if (order == null) return;
        String orderId = order.getOrderId();
        if (orderId == null || orderId.isEmpty()) orderId = "—";
        if (tvCode != null) {
            String shortId = orderId.length() > 8 ? orderId.substring(0, 8) : orderId;
            tvCode.setText("Đơn #" + shortId);
        }

        if (tvTime != null) {
            Date createdAt = order.getCreatedAt();
            if (createdAt != null) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(createdAt.getTime());
                tvTime.setText(relativeTime);
            } else tvTime.setText("Chưa có thời gian");
        }

        if (tvDesc != null) {
            String address = order.getDeliveryAddressText();
            if (address == null || address.isEmpty()) address = "Chưa cập nhật địa chỉ";
            String paymentMethod = order.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.isEmpty()) paymentMethod = "Chưa cập nhật thanh toán";
            String customerName = order.getCustomerName();
            tvDesc.setText((customerName == null || customerName.isEmpty() ? "Khách hàng" : customerName)
                    + " • " + address + " • " + paymentMethod);
        }

        if (tvAmount != null) {
            tvAmount.setText(formatCurrency(Math.round(order.getTotalAmount())));
        }

        if (tvStatus != null) {
            String status = order.getStatus();
            String normalizedStatus = Order.normalizeStatus(status);
            if (Order.STATUS_COMPLETED.equals(normalizedStatus)) {
                tvStatus.setText("Đã giao");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_status_green);
                tvStatus.setTextColor(0xFF16A34A);
            } else if (Order.STATUS_PENDING.equals(normalizedStatus)) {
                tvStatus.setText("Chờ xác nhận");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_status_amber);
                tvStatus.setTextColor(0xFFD97706);
            } else if (Order.STATUS_SHIPPING.equals(normalizedStatus)) {
                tvStatus.setText("Đang giao");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_live);
                tvStatus.setTextColor(0xFF2563EB);
            } else {
                tvStatus.setText(status == null || status.isEmpty() ? "Chưa cập nhật trạng thái" : status);
                tvStatus.setBackgroundResource(R.drawable.bg_pill_status_amber);
                tvStatus.setTextColor(0xFFD97706);
            }
        }
    }

    private String formatNumber(long n) {
        return NumberFormat.getNumberInstance(Locale.US).format(n);
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " đ";
    }

    private String formatRevenue(long amount) {
        if (amount >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB đ", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000L) {
            return String.format(Locale.US, "%.1fM đ", amount / 1_000_000.0);
        } else if (amount >= 1_000L) {
            return String.format(Locale.US, "%.0fK đ", amount / 1_000.0);
        }
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " đ";
    }
}
