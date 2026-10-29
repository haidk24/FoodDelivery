package com.example.foodorderapp.ui.customer.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface CustomerActionListener {
        void onTrackOrder(Order order);
        void onReviewOrder(Order order);
        void onViewOrderDetails(Order order);
        void onReorder(Order order);
    }

    private final Context context;
    private List<Order> orderList;
    private CustomerActionListener customerActionListener;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList != null ? orderList : new ArrayList<>();
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
        holder.itemView.setOnClickListener(v -> dispatchDetails(order));
    }

    private void loadRestaurantInfo(OrderViewHolder holder, Order order) {
        String name = order.getRestaurantName();
        String address = order.getRestaurantAddress();
        holder.tvRestaurantName.setText(TextUtils.isEmpty(name) ? "Chưa có thông tin nhà hàng" : name);
        holder.tvRestaurantAddress.setText(TextUtils.isEmpty(address) ? "Chưa lưu địa chỉ nhà hàng" : address);
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

            String imageUrl = items.get(0).getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(context).load(imageUrl).placeholder(R.drawable.banh)
                        .error(R.drawable.banh).into(holder.imgOrderFoodThumbnail);
            } else holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh);
        } else {
            holder.tvOrderDishesSummary.setText("Đơn hàng món ăn");
            holder.tvOrderDishesCount.setText("Món ăn đang chuẩn bị");
            holder.imgOrderFoodThumbnail.setImageResource(R.drawable.banh);
        }
    }

    private void bindStatusAndActions(OrderViewHolder holder, Order order) {
        String status = Order.normalizeStatus(order.getStatus());

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
            holder.btnCallShipper.setVisibility(View.GONE);
            holder.tvShipperName.setText(TextUtils.isEmpty(order.getShipperName())
                    ? "Tài xế đang giao đơn" : order.getShipperName());
            holder.tvShipperEta.setText(TextUtils.isEmpty(order.getShipperVehicle())
                    ? "Đơn đã được bàn giao cho tài xế" : order.getShipperVehicle());
            if (!TextUtils.isEmpty(order.getDeliveryAddressText())) {
                holder.tvDeliveryDest.setText("Đang tới: " + order.getDeliveryAddressText());
            }

            holder.layoutShipperDispatch.setOnClickListener(v -> {
                if (customerActionListener != null) {
                    customerActionListener.onTrackOrder(order);
                } else {
                    dispatchTracking(order);
                }
            });

            // Action Buttons: Chi tiết + Theo dõi đơn hàng
            holder.btnSecondaryAction.setVisibility(View.VISIBLE);
            holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivSecondaryIcon.setImageResource(R.drawable.ic_receipt);
            holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvSecondaryText.setText("Chi tiết");
            holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnSecondaryAction.setOnClickListener(v -> dispatchDetails(order));

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
                    dispatchTracking(order);
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
                holder.tvOrderRatingBadge.setText("★ Đã đánh giá");
                holder.layoutReviewDelight.setVisibility(View.GONE);
                holder.tvPaymentLabel.setText("Đã thanh toán");

                // Actions: Xem nhận xét (link/btn) + Đặt lại đơn
                holder.btnSecondaryAction.setVisibility(View.VISIBLE);
                holder.btnSecondaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
                holder.ivSecondaryIcon.setImageResource(R.drawable.ic_rate_review);
                holder.ivSecondaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextSecondary)));
                holder.tvSecondaryText.setText("Xem nhận xét");
                holder.tvSecondaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
                holder.btnSecondaryAction.setOnClickListener(v -> dispatchReview(order));

                holder.btnPrimaryAction.setVisibility(View.VISIBLE);
                holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
                holder.ivPrimaryIcon.setImageResource(R.drawable.ic_replay);
                holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
                holder.tvPrimaryText.setText("Đặt lại đơn");
                holder.tvPrimaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
                holder.btnPrimaryAction.setOnClickListener(v -> dispatchReorder(order));

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
                holder.btnSecondaryAction.setOnClickListener(v -> dispatchReorder(order));

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
                        dispatchReview(order);
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
            holder.btnSecondaryAction.setOnClickListener(v -> dispatchDetails(order));

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
                    dispatchTracking(order);
                }
            });

        } else if (Order.STATUS_PREPARING.equals(status) || Order.STATUS_CONFIRMED.equals(status)) {
            // ĐÃ ĐƯỢC NHÀ HÀNG XÁC NHẬN
            holder.indicatorActiveShipping.setVisibility(View.GONE);
            holder.tvOrderStatus.setText("Đã xác nhận");
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
            holder.btnSecondaryAction.setOnClickListener(v -> dispatchDetails(order));

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
                    dispatchTracking(order);
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
            holder.btnSecondaryAction.setOnClickListener(v -> dispatchDetails(order));

            holder.btnPrimaryAction.setVisibility(View.VISIBLE);
            holder.btnPrimaryAction.setBackgroundResource(R.drawable.bg_btn_order_action_light);
            holder.ivPrimaryIcon.setImageResource(R.drawable.ic_replay);
            holder.ivPrimaryIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorTextPrimary)));
            holder.tvPrimaryText.setText("Đặt lại đơn");
            holder.tvPrimaryText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
            holder.btnPrimaryAction.setOnClickListener(v -> dispatchReorder(order));

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
            holder.btnSecondaryAction.setOnClickListener(v -> dispatchDetails(order));

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
                    dispatchTracking(order);
                }
            });
        }
    }

    private void dispatchDetails(Order order) { if (customerActionListener != null) customerActionListener.onViewOrderDetails(order); }
    private void dispatchTracking(Order order) { if (customerActionListener != null) customerActionListener.onTrackOrder(order); }
    private void dispatchReview(Order order) { if (customerActionListener != null) customerActionListener.onReviewOrder(order); }
    private void dispatchReorder(Order order) { if (customerActionListener != null) customerActionListener.onReorder(order); }

    public void setCustomerActionListener(CustomerActionListener customerActionListener) {
        this.customerActionListener = customerActionListener;
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        View indicatorActiveShipping;
        TextView tvOrderId, tvOrderTime, tvRestaurantName, tvRestaurantAddress;
        TextView tvOrderStatus, tvOrderRatingBadge;
        TextView tvCustomerInfo;
        ImageView imgOrderFoodThumbnail;
        TextView tvOrderDishesSummary, tvOrderDishesCount, tvPaymentLabel, tvTotalAmount;
        View layoutShipperDispatch;
        TextView tvShipperName, tvShipperEta, tvDeliveryDest, btnCallShipper;
        ProgressBar progressDelivery;

        View layoutReviewDelight;

        View layoutCustomerActions;
        View btnUpdateStatus;
        View btnSecondaryAction, btnPrimaryAction;
        ImageView ivSecondaryIcon, ivPrimaryIcon;
        TextView tvSecondaryText, tvPrimaryText;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            indicatorActiveShipping = itemView.findViewById(R.id.indicator_active_shipping);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantAddress = itemView.findViewById(R.id.tvRestaurantAddress);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderRatingBadge = itemView.findViewById(R.id.tvOrderRatingBadge);
            tvCustomerInfo = itemView.findViewById(R.id.tvCustomerInfo);
            imgOrderFoodThumbnail = itemView.findViewById(R.id.imgOrderFoodThumbnail);
            tvOrderDishesSummary = itemView.findViewById(R.id.tvOrderDishesSummary);
            tvOrderDishesCount = itemView.findViewById(R.id.tvOrderDishesCount);
            tvPaymentLabel = itemView.findViewById(R.id.tvPaymentLabel);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            layoutShipperDispatch = itemView.findViewById(R.id.layout_shipper_dispatch);
            tvShipperName = itemView.findViewById(R.id.tv_shipper_name);
            tvShipperEta = itemView.findViewById(R.id.tv_shipper_eta);
            progressDelivery = itemView.findViewById(R.id.progress_delivery);
            tvDeliveryDest = itemView.findViewById(R.id.tv_delivery_dest);
            btnCallShipper = itemView.findViewById(R.id.btn_call_shipper);

            layoutReviewDelight = itemView.findViewById(R.id.layout_review_delight);

            layoutCustomerActions = itemView.findViewById(R.id.layoutCustomerActions);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            btnSecondaryAction = itemView.findViewById(R.id.btnSecondaryAction);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
            ivSecondaryIcon = itemView.findViewById(R.id.ivSecondaryIcon);
            ivPrimaryIcon = itemView.findViewById(R.id.ivPrimaryIcon);
            tvSecondaryText = itemView.findViewById(R.id.tvSecondaryText);
            tvPrimaryText = itemView.findViewById(R.id.tvPrimaryText);

        }
    }
}
