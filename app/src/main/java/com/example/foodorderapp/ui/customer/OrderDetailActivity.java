package com.example.foodorderapp.ui.customer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.feature.orders.OrdersViewModel;
import com.example.foodorderapp.feature.restaurant.info.RestaurantInfoViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvOrderCodeBadge;

    private FrameLayout iconStatusBg;
    private ImageView ivStatusIcon;
    private TextView tvOrderStatus, tvOrderTime, tvStatusPill;

    private TextView tvRestaurantName, tvRestaurantAddress;
    private View btnCallRestaurant;

    private TextView tvItemsCount;
    private LinearLayout layoutDishesContainer;
    private LinearLayout layoutNoteBox;
    private TextView tvOrderNote;

    private TextView tvCustomerName, tvCustomerPhone, tvCustomerAddress;

    private TextView tvSubtotal, tvShippingFee, tvDiscount, tvOrderTotal, tvPaymentMethod;
    private MaterialButton btnCancelOrder, btnPrimaryAction;

    private OrdersViewModel ordersViewModel;
    private RestaurantInfoViewModel restaurantViewModel;
    private String orderId;
    private Order currentOrder;
    private String restaurantPhone = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

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

        // Pre-fill from Intent while waiting for Firestore
        String initialStatus = getIntent().getStringExtra("EXTRA_STATUS");
        double initialTotal = getIntent().getDoubleExtra("EXTRA_TOTAL", 0);
        String customerName = getIntent().getStringExtra("EXTRA_CUSTOMER_NAME");
        String customerPhone = getIntent().getStringExtra("EXTRA_CUSTOMER_PHONE");
        String customerAddress = getIntent().getStringExtra("EXTRA_CUSTOMER_ADDRESS");
        String restaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");

        if (initialStatus != null) {
            bindStatusUI(initialStatus, null);
        }
        if (customerName != null) tvCustomerName.setText(customerName);
        if (customerPhone != null) tvCustomerPhone.setText(customerPhone);
        if (customerAddress != null) tvCustomerAddress.setText(customerAddress);
        if (initialTotal > 0) tvOrderTotal.setText(formatCurrency(initialTotal));

        if (!TextUtils.isEmpty(restaurantId)) {
            loadRestaurantInfo(restaurantId);
        }

        btnBack.setOnClickListener(v -> onBackPressed());

        btnCallRestaurant.setOnClickListener(v -> {
            if (TextUtils.isEmpty(restaurantPhone)) {
                Toast.makeText(this, "Nhà hàng chưa cập nhật số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + restaurantPhone));
                startActivity(callIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Số điện thoại quán: " + restaurantPhone, Toast.LENGTH_SHORT).show();
            }
        });

        listenOrderRealtime();
    }

    private void observeRestaurant() {
        restaurantViewModel.getRestaurant().observe(this, restaurant -> {
            if (restaurant == null) return;
            if (!TextUtils.isEmpty(restaurant.getName())) tvRestaurantName.setText(restaurant.getName());
            String address = restaurant.getAddressText();
            if (!TextUtils.isEmpty(address) && !"Chua cap nhat".equals(address)) tvRestaurantAddress.setText(address);
            restaurantPhone = restaurant.getPhone();
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvOrderCodeBadge = findViewById(R.id.tv_order_code_badge);

        iconStatusBg = findViewById(R.id.icon_status_bg);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvOrderStatus = findViewById(R.id.tv_order_status);
        tvOrderTime = findViewById(R.id.tv_order_time);
        tvStatusPill = findViewById(R.id.tv_status_pill);

        tvRestaurantName = findViewById(R.id.tv_restaurant_name);
        tvRestaurantAddress = findViewById(R.id.tv_restaurant_address);
        btnCallRestaurant = findViewById(R.id.btn_call_restaurant);

        tvItemsCount = findViewById(R.id.tv_items_count);
        layoutDishesContainer = findViewById(R.id.layout_dishes_container);
        layoutNoteBox = findViewById(R.id.layout_note_box);
        tvOrderNote = findViewById(R.id.tv_order_note);

        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvCustomerAddress = findViewById(R.id.tv_customer_address);

        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvOrderTotal = findViewById(R.id.tv_order_total);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);

        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnPrimaryAction = findViewById(R.id.btn_primary_action);
    }

    private void listenOrderRealtime() {
        ordersViewModel.getOrders().observe(this, orders -> {
            if (orders == null || orders.isEmpty()) return;
            currentOrder = orders.get(0);
            populateOrderDetails(currentOrder);
        });
        ordersViewModel.observeOrder(orderId);
    }

    private void populateOrderDetails(Order order) {
        tvOrderCodeBadge.setText("#" + (order.getOrderId() != null && order.getOrderId().length() > 8 
                ? order.getOrderId().substring(0, 8) : order.getOrderId()));

        bindStatusUI(order.getStatus(), order.getCreatedAt());

        if (!TextUtils.isEmpty(order.getRestaurantId())) {
            loadRestaurantInfo(order.getRestaurantId());
        }

        // Dishes
        renderDishes(order.getItems());

        // Note
        if (!TextUtils.isEmpty(order.getNote())) {
            layoutNoteBox.setVisibility(View.VISIBLE);
            tvOrderNote.setText(order.getNote());
        } else {
            layoutNoteBox.setVisibility(View.GONE);
        }

        // Customer
        tvCustomerName.setText(safeText(order.getCustomerName(), "Khách hàng"));
        tvCustomerPhone.setText(safeText(order.getCustomerPhone(), "Chưa có SĐT"));
        tvCustomerAddress.setText(safeText(order.getDeliveryAddressText(), "Chưa có địa chỉ"));

        // Payment
        double subtotal = 0;
        if (order.getItems() != null) {
            for (CartItem it : order.getItems()) {
                if (it != null) {
                    subtotal += it.getPrice() * it.getQuantity();
                }
            }
        }
        double shippingFee = 15000;
        double total = order.getTotalAmount();
        double discount = Math.max(0, (subtotal + shippingFee) - total);

        tvSubtotal.setText(formatCurrency(subtotal > 0 ? subtotal : total));
        tvShippingFee.setText(formatCurrency(shippingFee));
        tvDiscount.setText(discount > 0 ? "-" + formatCurrency(discount) : "0 đ");
        tvOrderTotal.setText(formatCurrency(total));

        String paymentMethod = order.getPaymentMethod();
        if (TextUtils.isEmpty(paymentMethod) || "COD".equalsIgnoreCase(paymentMethod)) {
            tvPaymentMethod.setText("Thanh toán khi nhận hàng (COD)");
        } else {
            tvPaymentMethod.setText(paymentMethod);
        }

        setupActionButtons(order);
    }

    private void renderDishes(List<CartItem> items) {
        layoutDishesContainer.removeAllViews();
        if (items == null || items.isEmpty()) {
            tvItemsCount.setText("0 món");
            return;
        }

        int totalCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (CartItem item : items) {
            if (item == null) continue;
            totalCount += item.getQuantity();

            View dishView = inflater.inflate(R.layout.item_order_detail_dish, layoutDishesContainer, false);
            ImageView ivDish = dishView.findViewById(R.id.iv_dish_image);
            TextView tvName = dishView.findViewById(R.id.tv_dish_name);
            TextView tvUnit = dishView.findViewById(R.id.tv_dish_unit_info);
            TextView tvTotal = dishView.findViewById(R.id.tv_dish_total_price);

            tvName.setText(item.getName());
            tvUnit.setText(item.getQuantity() + "x • " + formatCurrency(item.getPrice()) + " / phần");
            tvTotal.setText(formatCurrency(item.getPrice() * item.getQuantity()));

            if (!TextUtils.isEmpty(item.getImageUrl())) {
                Glide.with(this).load(item.getImageUrl()).placeholder(R.drawable.banh)
                        .error(R.drawable.banh).into(ivDish);
            } else ivDish.setImageResource(R.drawable.banh);

            layoutDishesContainer.addView(dishView);
        }

        tvItemsCount.setText(totalCount + " món");
    }

    private void bindStatusUI(String status, Date createdAt) {
        String safe = Order.normalizeStatus(status);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault());
        String timeStr = createdAt != null ? sdf.format(createdAt) : "Thời gian thực";

        switch (safe) {
            case Order.STATUS_PREPARING:
            case Order.STATUS_CONFIRMED:
                ivStatusIcon.setImageResource(R.drawable.ic_restaurant);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
                iconStatusBg.setBackgroundResource(R.drawable.bg_pill_status_pending);
                tvOrderStatus.setText("Đang chuẩn bị món");
                tvOrderTime.setText(timeStr + " • Quán đang chế biến");
                tvStatusPill.setText("Đang nấu");
                tvStatusPill.setBackgroundResource(R.drawable.bg_pill_status_pending);
                tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                break;

            case Order.STATUS_SHIPPING:
                ivStatusIcon.setImageResource(R.drawable.ic_moped);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTertiary)));
                iconStatusBg.setBackgroundResource(R.drawable.bg_pill_status_shipping);
                tvOrderStatus.setText("Đang giao hàng");
                tvOrderTime.setText(timeStr + " • Tài xế đang trên đường");
                tvStatusPill.setText("● Đang giao");
                tvStatusPill.setBackgroundResource(R.drawable.bg_pill_status_shipping);
                tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.colorTertiary));
                break;

            case Order.STATUS_COMPLETED:
                ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTertiary)));
                iconStatusBg.setBackgroundResource(R.drawable.bg_pill_status_completed);
                tvOrderStatus.setText("Giao hàng thành công");
                tvOrderTime.setText(timeStr + " • Cảm ơn bạn đã đặt món");
                tvStatusPill.setText("✔ Hoàn tất");
                tvStatusPill.setBackgroundResource(R.drawable.bg_pill_status_completed);
                tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.colorTertiary));
                break;

            case Order.STATUS_CANCELLED:
                ivStatusIcon.setImageResource(R.drawable.ic_delete_outline);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorError)));
                iconStatusBg.setBackgroundResource(R.drawable.bg_pill_status_cancelled);
                tvOrderStatus.setText("Đơn hàng đã hủy");
                tvOrderTime.setText(timeStr + " • Đã hủy bỏ");
                tvStatusPill.setText("Đã hủy");
                tvStatusPill.setBackgroundResource(R.drawable.bg_pill_status_cancelled);
                tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                break;

            case Order.STATUS_PENDING:
            default:
                ivStatusIcon.setImageResource(R.drawable.ic_schedule);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondary)));
                iconStatusBg.setBackgroundResource(R.drawable.bg_pill_status_pending);
                tvOrderStatus.setText("Chờ xác nhận");
                tvOrderTime.setText(timeStr + " • Quán đang nhận đơn");
                tvStatusPill.setText("Đang xử lý");
                tvStatusPill.setBackgroundResource(R.drawable.bg_pill_status_pending);
                tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.colorSecondary));
                break;
        }
    }

    private void loadRestaurantInfo(String resId) {
        restaurantViewModel.load(resId);
    }

    private void setupActionButtons(Order order) {
        String safe = Order.normalizeStatus(order.getStatus());

        if (Order.STATUS_PENDING.equals(safe)) {
            btnCancelOrder.setVisibility(View.VISIBLE);
            btnCancelOrder.setOnClickListener(v -> confirmCancelOrder(order));

            btnPrimaryAction.setText("Theo dõi đơn hàng");
            btnPrimaryAction.setOnClickListener(v -> openTrackingActivity(order));
        } else if (Order.STATUS_PREPARING.equals(safe) || Order.STATUS_SHIPPING.equals(safe)) {
            btnCancelOrder.setVisibility(View.GONE);
            btnPrimaryAction.setText("Theo dõi đơn hàng");
            btnPrimaryAction.setOnClickListener(v -> openTrackingActivity(order));
        } else if (Order.STATUS_COMPLETED.equals(safe) || Order.STATUS_CANCELLED.equals(safe)) {
            btnCancelOrder.setVisibility(View.GONE);
            btnPrimaryAction.setText("Đặt lại đơn");
            btnPrimaryAction.setOnClickListener(v -> reorderItems(order));
        } else {
            btnCancelOrder.setVisibility(View.GONE);
            btnPrimaryAction.setText("Theo dõi đơn hàng");
            btnPrimaryAction.setOnClickListener(v -> openTrackingActivity(order));
        }
    }

    private void openTrackingActivity(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());
        intent.putExtra("EXTRA_TOTAL", order.getTotalAmount());
        intent.putExtra("EXTRA_STATUS", order.getStatus());
        intent.putExtra("EXTRA_CUSTOMER_NAME", order.getCustomerName());
        intent.putExtra("EXTRA_CUSTOMER_PHONE", order.getCustomerPhone());
        intent.putExtra("EXTRA_CUSTOMER_ADDRESS", order.getDeliveryAddressText());
        intent.putExtra("EXTRA_RESTAURANT_ID", order.getRestaurantId());
        startActivity(intent);
    }

    private void confirmCancelOrder(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + (order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8) : order.getOrderId()) + " không?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    ordersViewModel.updateStatus(order, Order.STATUS_CANCELLED, error -> {
                        if (error == null) Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        else Toast.makeText(this, "Hủy đơn thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void reorderItems(Order order) {
        List<CartItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "Không có món ăn để đặt lại", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            com.example.foodorderapp.data.repository.CartRepository cartRepository =
                    new com.example.foodorderapp.data.repository.CartRepository(this);
            for (CartItem item : items) {
                if (item == null) continue;
                Food food = new Food();
                food.setFoodId(item.getFoodId());
                food.setName(item.getName());
                food.setPrice(item.getPrice());
                food.setNewPrice(item.getPrice());
                food.setImageUrl(item.getImageUrl());
                food.setRestaurantId(order.getRestaurantId());
                cartRepository.addOrIncrement(food, item.getQuantity());
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã thêm các món vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value.trim();
    }

}
