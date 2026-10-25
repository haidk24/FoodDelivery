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
import com.example.foodorderapp.viewModel.AdminViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AdminViewModel viewModel;

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

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        bindViews(view);
        hideRecentOrderCards();
        setupLiveTime();
        setupClickListeners(view);
        observeData();
        loadRecentOrders();

        viewModel.observeRestaurants();
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
                    viewModel.approveRestaurant(currentPendingRestaurant.restaurantId);
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
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAdminStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) renderStats(stats);
        });

        // Lắng nghe danh sách nhà hàng chờ duyệt để hiển thị card Alert
        viewModel.getPendingRestaurantList().observe(getViewLifecycleOwner(), pendingList -> {
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
        viewModel.getPendingRestaurantCount().observe(getViewLifecycleOwner(), count -> {
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
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), ok -> {
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

    private void loadRecentOrders() {
        FirebaseFirestore.getInstance().collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        hideRecentOrderCards();
                        return;
                    }
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (cardRecentOrder1 != null) cardRecentOrder1.setVisibility(View.GONE);
                    if (cardRecentOrder2 != null) cardRecentOrder2.setVisibility(View.GONE);
                    if (!docs.isEmpty()) {
                        if (cardRecentOrder1 != null) cardRecentOrder1.setVisibility(View.VISIBLE);
                        bindOrderView(docs.get(0), tvOrder1Code, tvOrder1Time, tvOrder1Desc, tvOrder1Amount, tvOrder1Status);
                    }
                    if (docs.size() > 1) {
                        if (cardRecentOrder2 != null) cardRecentOrder2.setVisibility(View.VISIBLE);
                        bindOrderView(docs.get(1), tvOrder2Code, tvOrder2Time, tvOrder2Desc, tvOrder2Amount, tvOrder2Status);
                    }
                })
                .addOnFailureListener(e -> {
                    hideRecentOrderCards();
                });
    }

    private void hideRecentOrderCards() {
        if (cardRecentOrder1 != null) cardRecentOrder1.setVisibility(View.GONE);
        if (cardRecentOrder2 != null) cardRecentOrder2.setVisibility(View.GONE);
    }

    private void bindOrderView(DocumentSnapshot doc, TextView tvCode, TextView tvTime, TextView tvDesc, TextView tvAmount, TextView tvStatus) {
        if (doc == null) return;
        String orderId = doc.getString("orderId");
        if (orderId == null || orderId.isEmpty()) orderId = doc.getId();
        if (tvCode != null) {
            String shortId = orderId.length() > 8 ? orderId.substring(0, 8) : orderId;
            tvCode.setText("Đơn #" + shortId);
        }

        if (tvTime != null) {
            Object createdAt = doc.get("createdAt");
            if (createdAt instanceof Timestamp) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(((Timestamp) createdAt).toDate().getTime());
                tvTime.setText(relativeTime);
            } else tvTime.setText("Chưa có thời gian");
        }

        if (tvDesc != null) {
            Order order = doc.toObject(Order.class);
            String address = order == null ? "" : order.getDeliveryAddressText();
            if (address.isEmpty()) {
                Object legacyAddress = doc.get("address");
                address = legacyAddress == null ? "" : String.valueOf(legacyAddress);
            }
            if (address == null || address.isEmpty()) address = "Chưa cập nhật địa chỉ";
            String paymentMethod = doc.getString("paymentMethod");
            if (paymentMethod == null || paymentMethod.isEmpty()) paymentMethod = "Chưa cập nhật thanh toán";
            String customerName = doc.getString("customerName");
            tvDesc.setText((customerName == null || customerName.isEmpty() ? "Khách hàng" : customerName)
                    + " • " + address + " • " + paymentMethod);
        }

        if (tvAmount != null) {
            Object rawTotal = doc.get("totalAmount");
            if (rawTotal instanceof Number) tvAmount.setText(formatCurrency(((Number) rawTotal).longValue()));
            else tvAmount.setText("—");
        }

        if (tvStatus != null) {
            String status = doc.getString("status");
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
