package com.example.foodorderapp.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.database.FoodDatabase;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.ui.MainActivity;
import com.example.foodorderapp.ui.OrderItemsActivity;
import com.example.foodorderapp.ui.OrderTrackingActivity;
import com.example.foodorderapp.ui.ReviewActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface CustomerActionListener {
        void onTrackOrder(Order order);
        void onReviewOrder(Order order);
    }

    private final Context context;
    private List<Order> orderList;
    private final FirebaseFirestore db;
    private final String restaurantId;
    private final boolean restaurantMode;
    private CustomerActionListener customerActionListener;

    private static final java.util.Map<String, String[]> restaurantCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, String> foodImageCache = new java.util.concurrent.ConcurrentHashMap<>();

    public OrderAdapter(Context context, List<Order> orderList, String restaurantId) {
        this.context = context;
        this.orderList = orderList != null ? orderList : new ArrayList<>();
        this.restaurantId = restaurantId == null ? "" : restaurantId;
        this.restaurantMode = true;
        this.db = FirebaseFirestore.getInstance();
        setHasStableIds(true);
    }

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList != null ? orderList : new ArrayList<>();
        this.restaurantId = "";
        this.restaurantMode = false;
        this.db = FirebaseFirestore.getInstance();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (orderList != null && position < orderList.size()) {
            Order order = orderList.get(position);
            if (order != null && order.getOrderId() != null) {
                return (long) order.getOrderId().hashCode();
            }
        }
        return position;
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        // Order ID and Date
        String shortId = order.getOrderId();
        if (shortId != null && shortId.length() > 10) {
            shortId = shortId.substring(0, 10);
        }
        holder.tvOrderId.setText("#" + (shortId != null ? shortId : ""));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault());
        if (order.getCreatedAt() != null) {
            holder.tvOrderTime.setText(sdf.format(order.getCreatedAt()));
        } else {
            holder.tvOrderTime.setText("--:--");
        }

        // Restaurant Name and Address
        loadRestaurantInfo(holder, order);

        // Dishes Preview & Thumbnail
        bindDishesPreview(holder, order);

        // Total Amount
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", order.getTotalAmount()));

        // Status-specific UI & Action Buttons
        bindStatusAndActions(holder, order);

        // Item View Click opens Order Detail
        holder.itemView.setOnClickListener(v -> openOrderItemsScreen(order));
    }

    private void loadRestaurantInfo(OrderViewHolder holder, Order order) {
        String resId = order.getRestaurantId();
        if (!TextUtils.isEmpty(resId) && restaurantCache.containsKey(resId)) {
            String[] cached = restaurantCache.get(resId);
            if (cached != null) {
                holder.tvRestaurantName.setText(cached[0]);
                holder.tvRestaurantAddress.setText(cached[1]);
                return;
            }
        }

        // Generic fallback while loading, avoids flashing wrong restaurant name
        holder.tvRestaurantName.setText("Quán ăn");
        holder.tvRestaurantAddress.setText("Hoàn Kiếm, Hà Nội");

        if (!TextUtils.isEmpty(resId)) {
            db.collection("restaurants").document(resId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Restaurant r = doc.toObject(Restaurant.class);
                            String name = r != null && !TextUtils.isEmpty(r.getName()) ? r.getName() : doc.getString("name");
                            String n = !TextUtils.isEmpty(name) ? name : "Quán ăn";
                            String a = r != null ? r.getAddressText() : "Chưa cập nhật";
                            restaurantCache.put(resId, new String[]{n, a});
                            holder.tvRestaurantName.setText(n);
                            holder.tvRestaurantAddress.setText(a);
                        }
                    })
                    .addOnFailureListener(e -> {});
        }
    }

    private void bindDishesPreview(OrderViewHolder holder, Order order) {
        List<CartItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int totalCount = 0;
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                if (item != null) {
                    if (i < 2) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(item.getQuantity()).append("x ").append(item.getName());
                    }
                    totalCount += item.getQuantity();
                }
            }
            if (items.size() > 2) {
                sb.append("...");
            }
            holder.tvOrderDishesSummary.setText(sb.toString());
            holder.tvOrderDishesCount.setText(String.format(Locale.getDefault(), "%d món • Chuẩn bị nóng sốt", totalCount));

            // Load dish image from food collection if foodId exists
            String firstFoodId = items.get(0).getFoodId();
            if (!TextUtils.isEmpty(firstFoodId)) {
                if (foodImageCache.containsKey(firstFoodId)) {
                    Glide.with(context)
                            .load(foodImageCache.get(firstFoodId))
                            .placeholder(R.drawable.banh)
                            .error(R.drawable.banh)
                            .into(holder.imgOrderFoodThumbnail);
                } else {
                    db.collection("foods").document(firstFoodId).get()
                            .addOnSuccessListener(doc -> {
                                if (doc != null && doc.exists()) {
                                    String imgUrl = doc.getString("imageUrl");
                                    if (!TextUtils.isEmpty(imgUrl)) {
                                        foodImageCache.put(firstFoodId, imgUrl);
                                        Glide.with(context).load(imgUrl)
                                                .placeholder(R.drawable.banh)
                                                .error(R.drawable.banh)
                                                .into(holder.imgOrderFoodThumbnail);
                                        return;
                                    }
                                }
                                holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh);
                            })
                            .addOnFailureListener(e -> holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh));
                }
            } else {
                holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh);
            }
        } else {
            holder.tvOrderDishesSummary.setText("Đơn hàng món ăn");
            holder.tvOrderDishesCount.setText("Món ăn đang chuẩn bị");
            holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh);
        }
    }

    private void bindStatusAndActions(OrderViewHolder holder, Order order) {
        String status = Order.normalizeStatus(order.getStatus());

        if (restaurantMode) {
            // Restaurant Mode UI
            holder.tvCustomerInfo.setVisibility(View.VISIBLE);
            holder.tvCustomerInfo.setText(buildCustomerInfo(order));
            holder.layoutCustomerActions.setVisibility(View.GONE);
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderRatingBadge.setVisibility(View.GONE);

            bindRestaurantStatusUI(holder, status, order);
            return;
        }

        // Customer Mode UI
        holder.tvCustomerInfo.setVisibility(View.GONE);
        holder.btnUpdateStatus.setVisibility(View.GONE);
        holder.layoutCustomerActions.setVisibility(View.VISIBLE);

        if (Order.STATUS_SHIPPING.equals(status)) {
            // ĐANG GIAO HÀNG
            holder.indicatorActiveShipping.setVisibility(View.VISIBLE);
            holder.tvOrderStatus.setText("● Đang giao hàng");
            holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_shipping);
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTertiary));
            holder.tvOrderRatingBadge.setVisibility(View.GONE);
            holder.tvPaymentLabel.setText("Thu hộ COD");

            // Dispatch shipper box
            holder.layoutShipperDispatch.setVisibility(View.VISIBLE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(order.getCustomerAddress())) {
                holder.tvDeliveryDest.setText("Đang tới: " + order.getCustomerAddress());
            }

            holder.layoutShipperDispatch.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    openOrderTrackingScreen(order);
                }
            });

            holder.btnCallShipper.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0987654321"));
                context.startActivity(intent);
            });

            // Action Buttons: Chi tiết + Theo dõi đơn hàng
            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> openOrderItemsScreen(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_track);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_near_me);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvPrimaryText.setText("Theo dõi đơn hàng");
            holder.tvPrimaryText.setTextColor(Color.WHITE);
            holder.btnPrimaryAction.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    openOrderTrackingScreen(order);
                }
            });

        } else if (Order.STATUS_COMPLETED.equals(status)) {
            // ĐÃ GIAO / ĐÃ HOÀN TẤT
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);

            if (order.isReviewed()) {
                // Đã đánh giá
                holder.tvOrderStatus.setText("✔ Đã hoàn tất");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_completed);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
                holder.tvOrderRatingBadge.setVisibility(View.VISIBLE);
                holder.tvOrderRatingBadge.setText("★ Đã đánh giá 5.0");
                holder.layoutReviewDelight.setVisibility(View.GONE);
                holder.tvPaymentLabel.setText("Đã thanh toán");

                // Actions: Xem nhận xét (link/btn) + Đặt lại đơn
                holder.btnSecondaryAction.setVisibility(View.VISIBLE);
                holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
                holder.ivSecondaryIcon.setImageResource(R.drawable.ic_rate_review);
                holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextSecondary)));
                holder.tvSecondaryText.setText("Xem nhận xét");
                holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
                holder.btnSecondaryAction.setOnClickListener(v -> openReviewScreen(order));

                holder.btnPrimaryAction.setVisibility(View.VISIBLE);
                holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
                holder.ivPrimaryIcon.setImageResource(R.drawable.ic_replay);
                holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
                holder.tvPrimaryText.setText("Đặt lại đơn");
                holder.tvPrimaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
                holder.btnPrimaryAction.setOnClickListener(v -> reorderItems(order));

            } else {
                // Chưa đánh giá
                holder.tvOrderStatus.setText("✔ Đã giao thành công");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_shipping);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTertiary));
                holder.tvOrderRatingBadge.setVisibility(View.GONE);
                holder.layoutReviewDelight.setVisibility(View.VISIBLE);
                holder.tvPaymentLabel.setText("Tổng thanh toán");

                // Actions: Đặt lại đơn + Đánh giá ngay
                holder.btnSecondaryAction.setVisibility(View.VISIBLE);
                holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
                holder.ivSecondaryIcon.setImageResource(R.drawable.ic_replay);
                holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
                holder.tvSecondaryText.setText("Đặt lại đơn");
                holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
                holder.btnSecondaryAction.setOnClickListener(v -> reorderItems(order));

                holder.btnPrimaryAction.setVisibility(View.VISIBLE);
                holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_review);
                holder.ivPrimaryIcon.setImageResource(R.drawable.ic_star);
                holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                holder.tvPrimaryText.setText("Đánh giá ngay");
                holder.tvPrimaryText.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.btnPrimaryAction.setOnClickListener(v -> {
                    if (customerActionListener != null) {
                        customerActionListener.onReviewOrder(order);
                    } else {
                        openReviewScreen(order);
                    }
                });
            }

        } else if (Order.STATUS_PENDING.equals(status)) {
            // CHỜ XÁC NHẬN
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderStatus.setText("Chờ xác nhận");
            holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_pending);
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSecondary));
            holder.tvOrderRatingBadge.setVisibility(View.GONE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            holder.tvPaymentLabel.setText("Thu hộ COD");

            // Actions: Chi tiết + Theo dõi đơn
            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> openOrderItemsScreen(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_track);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_near_me);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvPrimaryText.setText("Theo dõi đơn hàng");
            holder.tvPrimaryText.setTextColor(Color.WHITE);
            holder.btnPrimaryAction.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    openOrderTrackingScreen(order);
                }
            });

        } else if (Order.STATUS_PREPARING.equals(status) || Order.STATUS_CONFIRMED.equals(status)) {
            // ĐANG CHUẨN BỊ MÓN
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderStatus.setText("Đang chuẩn bị");
            holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_pending);
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            holder.tvOrderRatingBadge.setVisibility(View.GONE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            holder.tvPaymentLabel.setText("Thu hộ COD");

            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> openOrderItemsScreen(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_track);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_near_me);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvPrimaryText.setText("Theo dõi đơn hàng");
            holder.tvPrimaryText.setTextColor(Color.WHITE);
            holder.btnPrimaryAction.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    openOrderTrackingScreen(order);
                }
            });

        } else if (Order.STATUS_CANCELLED.equals(status)) {
            // ĐÃ HỦY
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderStatus.setText("Đã hủy");
            holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_cancelled);
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.tvOrderRatingBadge.setVisibility(View.GONE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            holder.tvPaymentLabel.setText("Tổng tiền");

            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> openOrderItemsScreen(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_replay);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvPrimaryText.setText("Đặt lại đơn");
            holder.tvPrimaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnPrimaryAction.setOnClickListener(v -> reorderItems(order));

        } else {
            // MẶC ĐỊNH CHO TRẠNG THÁI KHÁC (KHÔNG TỰ ĐỘNG ĐỂ "ĐÃ HỦY")
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderStatus.setText("Đang xử lý");
            holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_pending);
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSecondary));
            holder.tvOrderRatingBadge.setVisibility(View.GONE);
            holder.layoutShipperDispatch.setVisibility(View.GONE);
            holder.layoutReviewDelight.setVisibility(View.GONE);
            holder.tvPaymentLabel.setText("Thu hộ COD");

            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> openOrderItemsScreen(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_track);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_near_me);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvPrimaryText.setText("Theo dõi đơn hàng");
            holder.tvPrimaryText.setTextColor(Color.WHITE);
            holder.btnPrimaryAction.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    openOrderTrackingScreen(order);
                }
            });
        }
    }

    private void reorderItems(Order order) {
        List<CartItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            Toast.makeText(context, "Không có món ăn để đặt lại", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : items) {
            Food food = new Food();
            food.setFoodId(item.getFoodId());
            food.setName(item.getName());
            food.setPrice(item.getPrice());
            food.setNewPrice(item.getPrice());
            food.setCount(item.getQuantity());
            food.setRestaurantId(order.getRestaurantId());
            FoodDatabase.getInstance(context).foodDao().insertFood(food);
        }

        Toast.makeText(context, "Đã thêm các món vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
        if (context instanceof MainActivity) {
            BottomNavigationView bottomNav = ((MainActivity) context).findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_cart);
            }
        }
    }

    private void bindRestaurantStatusUI(OrderViewHolder holder, String status, Order order) {
        String norm = Order.normalizeStatus(status);
        switch (norm) {
            case Order.STATUS_PENDING:
                holder.tvOrderStatus.setText("ĐANG CHỜ XÁC NHẬN");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_pending);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSecondary));
                holder.btnUpdateStatus.setText("XÁC NHẬN ĐƠN");
                holder.btnUpdateStatus.setEnabled(true);
                break;
            case Order.STATUS_PREPARING:
                holder.tvOrderStatus.setText("ĐANG CHUẨN BỊ MÓN");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_pending);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.btnUpdateStatus.setText("GIAO CHO SHIPPER");
                holder.btnUpdateStatus.setEnabled(true);
                break;
            case Order.STATUS_SHIPPING:
                holder.tvOrderStatus.setText("ĐANG GIAO HÀNG");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_shipping);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTertiary));
                holder.btnUpdateStatus.setText("HOÀN TẤT ĐƠN");
                holder.btnUpdateStatus.setEnabled(true);
                break;
            case Order.STATUS_COMPLETED:
                holder.tvOrderStatus.setText("ĐÃ GIAO THÀNH CÔNG");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_shipping);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTertiary));
                holder.btnUpdateStatus.setText("ĐÃ HOÀN TẤT");
                holder.btnUpdateStatus.setEnabled(false);
                break;
            case Order.STATUS_CANCELLED:
                holder.tvOrderStatus.setText("ĐÃ HỦY ĐƠN");
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_cancelled);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
                holder.btnUpdateStatus.setEnabled(false);
                break;
            default:
                holder.tvOrderStatus.setText(status.toUpperCase(Locale.ROOT));
                holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_status_completed);
                holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.colorTextMuted));
                holder.btnUpdateStatus.setEnabled(false);
                break;
        }

        holder.btnUpdateStatus.setOnClickListener(v -> {
            String resId = order.getRestaurantId() != null && !order.getRestaurantId().isEmpty()
                    ? order.getRestaurantId() : restaurantId;
            if (resId == null || resId.isEmpty()) {
                Toast.makeText(context, "Không tìm thấy nhà hàng hiện tại", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentStatus = order.getStatus() == null ? Order.STATUS_PENDING : order.getStatus();
            String nextStatus = resolveNextStatus(currentStatus);
            if (nextStatus == null) return;

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", nextStatus);

            Date now = new Date();
            if (Order.STATUS_PREPARING.equals(nextStatus)) updates.put("confirmedAt", now);
            else if (Order.STATUS_SHIPPING.equals(nextStatus)) updates.put("shippingAt", now);
            else if (Order.STATUS_COMPLETED.equals(nextStatus)) updates.put("deliveredAt", now);

            WriteBatch batch = db.batch();
            batch.update(db.collection("restaurants").document(resId)
                    .collection("orders").document(order.getOrderId()), updates);
            batch.update(db.collection("orders").document(order.getOrderId()), updates);

            batch.commit()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã cập nhật trạng thái đơn", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private String resolveNextStatus(String currentStatus) {
        String norm = Order.normalizeStatus(currentStatus);
        if (Order.STATUS_PENDING.equals(norm)) return Order.STATUS_PREPARING;
        if (Order.STATUS_PREPARING.equals(norm)) return Order.STATUS_SHIPPING;
        if (Order.STATUS_SHIPPING.equals(norm)) return Order.STATUS_COMPLETED;
        return null;
    }

    private boolean canReview(Order order) {
        String status = Order.normalizeStatus(order.getStatus());
        boolean completed = Order.STATUS_COMPLETED.equals(status);
        return completed && !order.isReviewed();
    }

    private void openOrderItemsScreen(Order order) {
        ArrayList<String> itemLines = new ArrayList<>();
        List<CartItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            itemLines.add("Không có dữ liệu món ăn trong đơn này.");
        } else {
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                if (item == null) continue;
                String itemName = item.getName() == null || item.getName().trim().isEmpty() ? "Món ăn" : item.getName().trim();
                int quantity = Math.max(item.getQuantity(), 0);
                itemLines.add((i + 1) + ". " + itemName + "  x" + quantity);
            }
        }

        Intent intent = new Intent(context, OrderItemsActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());
        intent.putExtra("EXTRA_TOTAL", order.getTotalAmount());
        intent.putExtra("EXTRA_STATUS", order.getStatus());
        intent.putExtra("EXTRA_CUSTOMER_NAME", order.getCustomerName());
        intent.putExtra("EXTRA_CUSTOMER_PHONE", order.getCustomerPhone());
        intent.putExtra("EXTRA_CUSTOMER_ADDRESS", order.getCustomerAddress());
        intent.putExtra("EXTRA_RESTAURANT_ID", order.getRestaurantId());
        intent.putStringArrayListExtra("EXTRA_ITEM_LINES", itemLines);
        context.startActivity(intent);
    }

    public void openOrderTrackingScreen(Order order) {
        Intent intent = new Intent(context, OrderTrackingActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());
        intent.putExtra("EXTRA_TOTAL", order.getTotalAmount());
        intent.putExtra("EXTRA_STATUS", order.getStatus());
        intent.putExtra("EXTRA_CUSTOMER_NAME", order.getCustomerName());
        intent.putExtra("EXTRA_CUSTOMER_PHONE", order.getCustomerPhone());
        intent.putExtra("EXTRA_CUSTOMER_ADDRESS", order.getCustomerAddress());
        intent.putExtra("EXTRA_RESTAURANT_ID", order.getRestaurantId());
        context.startActivity(intent);
    }

    public void openReviewScreen(Order order) {
        if (!canReview(order)) {
            Toast.makeText(context, "Chỉ có thể đánh giá sau khi đơn đã hoàn tất", Toast.LENGTH_SHORT).show();
            return;
        }
        String customerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : order.getCustomerId();
        Intent intent = new Intent(context, ReviewActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        intent.putExtra("customerId", customerId);
        intent.putExtra("restaurantId", order.getRestaurantId());
        intent.putExtra("foodName", buildReviewTitle(order));
        context.startActivity(intent);
    }

    private String buildReviewTitle(Order order) {
        List<CartItem> items = order.getItems();
        if (items == null || items.isEmpty() || items.get(0) == null) {
            return "Đơn hàng #" + safeText(order.getOrderId(), "");
        }
        String firstName = safeText(items.get(0).getName(), "Món ăn");
        if (items.size() == 1) return firstName;
        return firstName + " và " + (items.size() - 1) + " món khác";
    }

    public void setCustomerActionListener(CustomerActionListener customerActionListener) {
        this.customerActionListener = customerActionListener;
    }

    private String buildCustomerInfo(Order order) {
        String name = safeText(order.getCustomerName(), "Chưa cập nhật");
        String phone = safeText(order.getCustomerPhone(), "Chưa cập nhật");
        String address = safeText(order.getCustomerAddress(), "Chưa cập nhật");
        return "Khách: " + name + " - " + phone + "\nĐịa chỉ: " + address;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        View indicatorActiveShipping;
        TextView tvOrderId, tvOrderTime, tvRestaurantName, tvRestaurantAddress;
        TextView tvOrderStatus, tvOrderRatingBadge;
        ImageView imgOrderFoodThumbnail;
        TextView tvOrderDishesSummary, tvOrderDishesCount, tvPaymentLabel, tvTotalAmount;
        TextView tvCustomerInfo;

        View layoutShipperDispatch;
        TextView tvShipperName, tvShipperEta, tvDeliveryDest, btnCallShipper;
        ProgressBar progressDelivery;

        View layoutReviewDelight;

        View layoutCustomerActions;
        View btnSecondaryAction, btnPrimaryAction;
        ImageView ivSecondaryIcon, ivPrimaryIcon;
        TextView tvSecondaryText, tvPrimaryText;

        Button btnUpdateStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            indicatorActiveShipping = itemView.findViewById(R.id.indicator_active_shipping);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantAddress = itemView.findViewById(R.id.tvRestaurantAddress);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderRatingBadge = itemView.findViewById(R.id.tvOrderRatingBadge);
            imgOrderFoodThumbnail = itemView.findViewById(R.id.imgOrderFoodThumbnail);
            tvOrderDishesSummary = itemView.findViewById(R.id.tvOrderDishesSummary);
            tvOrderDishesCount = itemView.findViewById(R.id.tvOrderDishesCount);
            tvPaymentLabel = itemView.findViewById(R.id.tvPaymentLabel);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvCustomerInfo = itemView.findViewById(R.id.tvCustomerInfo);

            layoutShipperDispatch = itemView.findViewById(R.id.layout_shipper_dispatch);
            tvShipperName = itemView.findViewById(R.id.tv_shipper_name);
            tvShipperEta = itemView.findViewById(R.id.tv_shipper_eta);
            progressDelivery = itemView.findViewById(R.id.progress_delivery);
            tvDeliveryDest = itemView.findViewById(R.id.tv_delivery_dest);
            btnCallShipper = itemView.findViewById(R.id.btn_call_shipper);

            layoutReviewDelight = itemView.findViewById(R.id.layout_review_delight);

            layoutCustomerActions = itemView.findViewById(R.id.layoutCustomerActions);
            btnSecondaryAction = itemView.findViewById(R.id.btnSecondaryAction);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
            ivSecondaryIcon = itemView.findViewById(R.id.ivSecondaryIcon);
            ivPrimaryIcon = itemView.findViewById(R.id.ivPrimaryIcon);
            tvSecondaryText = itemView.findViewById(R.id.tvSecondaryText);
            tvPrimaryText = itemView.findViewById(R.id.tvPrimaryText);

            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}