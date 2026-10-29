package com.example.foodorderapp.ui.customer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.feature.orders.OrdersViewModel;
import com.example.foodorderapp.feature.restaurant.info.RestaurantInfoViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvOrderCodeBadge;
    private TextView tvMapRestaurantLabel;
    private TextView tvTrackingEta;

    private View layoutShipperMarker, cardShipperInfo;
    private TextView tvShipperName, tvShipperBike;
    private MaterialButton btnCallShipper;

    // Stepper views
    private ImageView ivStep1Dot, ivStep2Dot, ivStep3Dot, ivStep4Dot;
    private View lineStep1To2, lineStep2To3, lineStep3To4;
    private TextView tvStep1Title, tvStep1Time;
    private TextView tvStep2Title, tvStep2Time;
    private TextView tvStep3Title, tvStep3Time;
    private TextView tvStep4Title, tvStep4Time;

    // Quick Summary
    private TextView tvSummaryRestaurant, tvSummaryDishes, tvSummaryTotal;
    private MaterialButton btnViewOrderDetails;

    private OrdersViewModel ordersViewModel;
    private RestaurantInfoViewModel restaurantViewModel;
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        ordersViewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        restaurantViewModel = new ViewModelProvider(this).get(RestaurantInfoViewModel.class);
        initViews();
        observeRestaurant();

        orderId = getIntent().getStringExtra("EXTRA_ORDER_ID");
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvOrderCodeBadge.setText("#" + (orderId.length() > 8 ? orderId.substring(0, 8) : orderId));

        // Prepopulate from Intent
        double total = getIntent().getDoubleExtra("EXTRA_TOTAL", 0);
        String restaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");

        if (total > 0) {
            tvSummaryTotal.setText(formatCurrency(total));
        }
        if (!TextUtils.isEmpty(restaurantId)) {
            loadRestaurantInfo(restaurantId);
        }

        btnBack.setOnClickListener(v -> onBackPressed());

        btnCallShipper.setOnClickListener(v -> callAssignedShipper());

        btnViewOrderDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("EXTRA_ORDER_ID", orderId);
            if (currentOrder != null) {
                intent.putExtra("EXTRA_TOTAL", currentOrder.getTotalAmount());
                intent.putExtra("EXTRA_STATUS", currentOrder.getStatus());
                intent.putExtra("EXTRA_CUSTOMER_NAME", currentOrder.getCustomerName());
                intent.putExtra("EXTRA_CUSTOMER_PHONE", currentOrder.getCustomerPhone());
                intent.putExtra("EXTRA_CUSTOMER_ADDRESS", currentOrder.getDeliveryAddressText());
                intent.putExtra("EXTRA_RESTAURANT_ID", currentOrder.getRestaurantId());
            }
            startActivity(intent);
        });

        listenOrderRealtime();
    }

    private void observeRestaurant() {
        restaurantViewModel.getRestaurant().observe(this, restaurant -> {
            if (restaurant == null || TextUtils.isEmpty(restaurant.getName())) return;
            tvSummaryRestaurant.setText(restaurant.getName());
            tvMapRestaurantLabel.setText(restaurant.getName());
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvOrderCodeBadge = findViewById(R.id.tv_order_code_badge);
        tvMapRestaurantLabel = findViewById(R.id.tv_map_restaurant_label);
        tvTrackingEta = findViewById(R.id.tv_tracking_eta);

        layoutShipperMarker = findViewById(R.id.layout_shipper_marker);
        cardShipperInfo = findViewById(R.id.card_shipper_info);
        tvShipperName = findViewById(R.id.tv_shipper_name);
        tvShipperBike = findViewById(R.id.tv_shipper_bike);
        btnCallShipper = findViewById(R.id.btn_call_shipper);

        ivStep1Dot = findViewById(R.id.iv_step1_dot);
        ivStep2Dot = findViewById(R.id.iv_step2_dot);
        ivStep3Dot = findViewById(R.id.iv_step3_dot);
        ivStep4Dot = findViewById(R.id.iv_step4_dot);

        lineStep1To2 = findViewById(R.id.line_step1_to_2);
        lineStep2To3 = findViewById(R.id.line_step2_to_3);
        lineStep3To4 = findViewById(R.id.line_step3_to_4);

        tvStep1Title = findViewById(R.id.tv_step1_title);
        tvStep1Time = findViewById(R.id.tv_step1_time);
        tvStep2Title = findViewById(R.id.tv_step2_title);
        tvStep2Time = findViewById(R.id.tv_step2_time);
        tvStep3Title = findViewById(R.id.tv_step3_title);
        tvStep3Time = findViewById(R.id.tv_step3_time);
        tvStep4Title = findViewById(R.id.tv_step4_title);
        tvStep4Time = findViewById(R.id.tv_step4_time);

        tvSummaryRestaurant = findViewById(R.id.tv_summary_restaurant);
        tvSummaryDishes = findViewById(R.id.tv_summary_dishes);
        tvSummaryTotal = findViewById(R.id.tv_summary_total);
        btnViewOrderDetails = findViewById(R.id.btn_view_order_details);
    }

    private void listenOrderRealtime() {
        ordersViewModel.getOrders().observe(this, orders -> {
            if (orders == null || orders.isEmpty()) return;
            currentOrder = orders.get(0);
            populateTracking(currentOrder);
        });
        ordersViewModel.observeOrder(orderId);
    }

    private void populateTracking(Order order) {
        tvOrderCodeBadge.setText("#" + (order.getOrderId() != null && order.getOrderId().length() > 8
                ? order.getOrderId().substring(0, 8) : order.getOrderId()));

        if (!TextUtils.isEmpty(order.getRestaurantId())) {
            loadRestaurantInfo(order.getRestaurantId());
        }

        // Quick Dishes preview
        List<CartItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                if (item != null) {
                    if (i > 0) sb.append(", ");
                    sb.append(item.getQuantity()).append("x ").append(item.getName());
                }
            }
            tvSummaryDishes.setText(sb.toString());
        }

        tvSummaryTotal.setText(formatCurrency(order.getTotalAmount()));
        bindShipper(order);
        updateStepperStages(order);
    }

    private void bindShipper(Order order) {
        String status = Order.normalizeStatus(order.getStatus());
        boolean shipping = Order.STATUS_SHIPPING.equals(status);
        boolean completed = Order.STATUS_COMPLETED.equals(status);
        boolean assigned = order.hasAssignedShipper();

        layoutShipperMarker.setVisibility(shipping ? View.VISIBLE : View.GONE);
        cardShipperInfo.setVisibility(shipping || (completed && assigned) ? View.VISIBLE : View.GONE);
        if (cardShipperInfo.getVisibility() != View.VISIBLE) return;

        String name = order.getShipperName();
        String vehicle = order.getShipperVehicle();
        String phone = order.getShipperPhone();
        tvShipperName.setText(!TextUtils.isEmpty(name) ? name
                : "Tài xế đang giao đơn");
        tvShipperBike.setText(!TextUtils.isEmpty(vehicle) ? vehicle
                : assigned ? "Chưa cập nhật phương tiện" : "Đơn đã được bàn giao cho tài xế");
        boolean canCall = !TextUtils.isEmpty(phone);
        btnCallShipper.setVisibility(canCall ? View.VISIBLE : View.GONE);
        btnCallShipper.setEnabled(canCall);
    }

    private void callAssignedShipper() {
        if (currentOrder == null || TextUtils.isEmpty(currentOrder.getShipperPhone())) {
            Toast.makeText(this, "Tài xế chưa có số điện thoại liên hệ", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentOrder.getShipperPhone())));
    }

    private void updateStepperStages(Order order) {
        String status = Order.normalizeStatus(order.getStatus());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String createdTime = order.getCreatedAt() != null ? timeFormat.format(order.getCreatedAt()) : "--:--";
        String confirmedTime = order.getConfirmedAt() != null ? timeFormat.format(order.getConfirmedAt()) : "--:--";
        String shippingTime = order.getShippingAt() != null ? timeFormat.format(order.getShippingAt()) : "--:--";
        String deliveredTime = order.getDeliveredAt() != null ? timeFormat.format(order.getDeliveredAt()) : "--:--";

        int colorTertiary = ContextCompat.getColor(this, R.color.colorTertiary);
        int colorPrimary = ContextCompat.getColor(this, R.color.colorPrimary);
        int colorInactive = ContextCompat.getColor(this, R.color.colorOutlineVariant);
        int colorTextPrimary = ContextCompat.getColor(this, R.color.colorTextPrimary);
        int colorTextSecondary = ContextCompat.getColor(this, R.color.colorTextSecondary);

        // Reset every view because the same screen receives realtime status changes.
        lineStep1To2.setBackgroundColor(colorInactive);
        lineStep2To3.setBackgroundColor(colorInactive);
        lineStep3To4.setBackgroundColor(colorInactive);
        ivStep1Dot.setImageTintList(ColorStateList.valueOf(colorPrimary));
        ivStep2Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivStep3Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivStep4Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
        tvStep1Title.setText("Đã đặt đơn hàng");
        tvStep2Title.setText("Nhà hàng xác nhận");
        tvStep3Title.setText("Đang giao đến bạn");
        tvStep4Title.setText("Giao hàng thành công");
        tvStep1Title.setTextColor(colorPrimary);
        tvStep2Title.setTextColor(colorTextSecondary);
        tvStep3Title.setTextColor(colorTextSecondary);
        tvStep4Title.setTextColor(colorTextSecondary);
        tvStep1Time.setText(createdTime);
        tvStep2Time.setText("Chờ nhà hàng xác nhận");
        tvStep3Time.setText("Chờ bàn giao tài xế");
        tvStep4Time.setText("Chờ hoàn tất");

        switch (status) {
            case Order.STATUS_PREPARING:
            case Order.STATUS_CONFIRMED:
                // Step 1 done, Step 2 active
                ivStep1Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep1Title.setTextColor(colorTextPrimary);
                tvStep1Time.setText(createdTime + " • Quán đã xác nhận đơn hàng");
                lineStep1To2.setBackgroundColor(colorTertiary);
                ivStep2Dot.setImageTintList(ColorStateList.valueOf(colorPrimary));
                tvStep2Title.setText("Quán đang chế biến món ăn");
                tvStep2Title.setTextColor(colorPrimary);
                tvStep2Time.setText(confirmedTime + " • Đang nấu và chuẩn bị đóng gói nóng sốt");

                lineStep2To3.setBackgroundColor(colorInactive);
                ivStep3Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep3Title.setTextColor(colorTextSecondary);
                tvStep3Time.setText("Chờ bàn giao tài xế");

                lineStep3To4.setBackgroundColor(colorInactive);
                ivStep4Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep4Title.setTextColor(colorTextSecondary);
                tvStep4Time.setText("Chờ hoàn tất");

                tvTrackingEta.setText("Đơn hàng đã được xác nhận");
                break;

            case Order.STATUS_SHIPPING:
                // Step 1 done, Step 2 done, Step 3 active (Shipping)
                ivStep1Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep1Title.setTextColor(colorTextPrimary);
                lineStep1To2.setBackgroundColor(colorTertiary);
                ivStep2Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep2Title.setText("Tài xế đã nhận món từ quán");
                tvStep2Title.setTextColor(colorTextPrimary);
                tvStep2Time.setText(shippingTime + " • Đã kiểm tra món ăn đầy đủ");

                lineStep2To3.setBackgroundColor(colorTertiary);
                ivStep3Dot.setImageTintList(ColorStateList.valueOf(colorPrimary));
                tvStep3Title.setText("Đang giao đến bạn");
                tvStep3Title.setTextColor(colorPrimary);
                String dest = TextUtils.isEmpty(order.getDeliveryAddressText())
                        ? "địa chỉ của bạn" : order.getDeliveryAddressText();
                tvStep3Time.setText("Tài xế đang di chuyển tới " + dest);

                lineStep3To4.setBackgroundColor(colorInactive);
                ivStep4Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep4Title.setTextColor(colorTextSecondary);
                tvStep4Time.setText("Chờ nhận hàng");

                tvTrackingEta.setText("Đơn hàng đang được giao đến bạn");
                break;

            case Order.STATUS_COMPLETED:
                // All steps done
                ivStep1Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep1Title.setTextColor(colorTextPrimary);
                lineStep1To2.setBackgroundColor(colorTertiary);
                ivStep2Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep2Title.setTextColor(colorTextPrimary);
                tvStep2Time.setText(confirmedTime + " • Quán đã hoàn tất món");

                lineStep2To3.setBackgroundColor(colorTertiary);
                ivStep3Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep3Title.setTextColor(colorTextPrimary);
                tvStep3Time.setText(shippingTime + " • Đã vận chuyển an toàn");

                lineStep3To4.setBackgroundColor(colorTertiary);
                ivStep4Dot.setImageTintList(ColorStateList.valueOf(colorTertiary));
                tvStep4Title.setText("Giao hàng thành công");
                tvStep4Title.setTextColor(colorTertiary);
                tvStep4Time.setText(deliveredTime + " • Đơn hàng đã hoàn tất");

                tvTrackingEta.setText("✔ Đã giao hàng thành công!");
                break;

            case Order.STATUS_CANCELLED:
                lineStep1To2.setBackgroundColor(colorInactive);
                lineStep2To3.setBackgroundColor(colorInactive);
                lineStep3To4.setBackgroundColor(colorInactive);
                ivStep1Dot.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorError)));
                tvStep1Title.setText("Đơn hàng đã hủy");
                tvStep1Title.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                tvStep1Time.setText("Đơn hàng đã bị hủy bỏ");
                tvTrackingEta.setText("Đơn hàng đã bị hủy");
                break;

            case Order.STATUS_PENDING:
            default:
                // Step 1 in progress
                tvStep1Time.setText(createdTime + " • Đang chờ quán xác nhận");
                lineStep1To2.setBackgroundColor(colorInactive);
                ivStep2Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep2Title.setTextColor(colorTextSecondary);
                tvStep2Time.setText("Chờ quán tiếp nhận");

                lineStep2To3.setBackgroundColor(colorInactive);
                ivStep3Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep3Title.setTextColor(colorTextSecondary);
                tvStep3Time.setText("Chờ tài xế nhận đơn");

                lineStep3To4.setBackgroundColor(colorInactive);
                ivStep4Dot.setImageTintList(ColorStateList.valueOf(colorInactive));
                tvStep4Title.setTextColor(colorTextSecondary);
                tvStep4Time.setText("Chờ hoàn tất");

                tvTrackingEta.setText("Đang chờ quán xác nhận đơn hàng");
                break;
        }
    }

    private void loadRestaurantInfo(String resId) {
        restaurantViewModel.load(resId);
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

}
