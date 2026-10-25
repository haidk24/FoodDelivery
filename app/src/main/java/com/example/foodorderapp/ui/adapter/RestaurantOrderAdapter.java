package com.example.foodorderapp.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Order;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RestaurantOrderAdapter extends RecyclerView.Adapter<RestaurantOrderAdapter.OrderViewHolder> {

    public interface OnOrderSummaryChangedListener {
        void onSummaryChanged(int total, int pending, int preparing, int delivering, int completed);
    }

    private final Context context;
    private final List<Order> originalList;
    private final List<Order> displayedList;
    private final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault());
    private OnOrderSummaryChangedListener summaryListener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private int daysFilter; // 0 = all dates
    private String paymentFilter = "Tất cả";
    private String summaryQuery = "";

    public RestaurantOrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.originalList = new ArrayList<>();
        if (orders != null) {
            this.originalList.addAll(orders);
        }
        this.displayedList = new ArrayList<>(this.originalList);
    }

    public void setSummaryListener(OnOrderSummaryChangedListener listener) {
        this.summaryListener = listener;
        notifySummaryChanged();
    }

    public void updateData(List<Order> newOrders) {
        if (newOrders != originalList) {
            originalList.clear();
            if (newOrders != null) {
                originalList.addAll(newOrders);
            }
        }
        filter("", "Tất cả");
    }

    public void setAdvancedFilters(int days, String payment) {
        daysFilter = days;
        paymentFilter = payment;
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.replace('đ', 'd').replace('Đ', 'd').toLowerCase();
    }

    public void filter(String query, String statusFilter) {
        displayedList.clear();
        String rawQ = query == null ? "" : query.trim().toLowerCase();
        String normQ = stripAccents(rawQ);
        summaryQuery = rawQ;
        long startTime = 0;
        long endTime = Long.MAX_VALUE;
        if (daysFilter > 0) {
            java.util.Calendar start = java.util.Calendar.getInstance();
            start.set(java.util.Calendar.HOUR_OF_DAY, 0);
            start.set(java.util.Calendar.MINUTE, 0);
            start.set(java.util.Calendar.SECOND, 0);
            start.set(java.util.Calendar.MILLISECOND, 0);
            start.add(java.util.Calendar.DAY_OF_YEAR, 1);
            endTime = start.getTimeInMillis();
            start.add(java.util.Calendar.DAY_OF_YEAR, -daysFilter);
            startTime = start.getTimeInMillis();
        }

        for (Order o : originalList) {
            String orderId = o.getOrderId() != null ? o.getOrderId().toLowerCase() : "";
            String customerName = o.getCustomerName() != null ? o.getCustomerName().toLowerCase() : "";
            String customerNameNorm = stripAccents(customerName);

            boolean matchesQuery = rawQ.isEmpty()
                    || orderId.contains(rawQ)
                    || customerName.contains(rawQ)
                    || customerNameNorm.contains(normQ);

            boolean matchesStatus = true;
            String st = Order.normalizeStatus(o.getStatus());
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("Tất cả")) {
                if (statusFilter.equalsIgnoreCase("Chờ xác nhận")) {
                    matchesStatus = Order.STATUS_PENDING.equals(st);
                } else if (statusFilter.equalsIgnoreCase("Đang chuẩn bị")) {
                    matchesStatus = Order.STATUS_PREPARING.equals(st);
                } else if (statusFilter.equalsIgnoreCase("Đang giao hàng")) {
                    matchesStatus = Order.STATUS_SHIPPING.equals(st);
                } else if (statusFilter.equalsIgnoreCase("Đã hoàn thành")) {
                    matchesStatus = Order.STATUS_COMPLETED.equals(st);
                }
            }

            boolean matchesDate = daysFilter == 0 || (o.getCreatedAt() != null
                    && o.getCreatedAt().getTime() >= startTime
                    && o.getCreatedAt().getTime() < endTime);
            boolean isCod = o.isCodPayment();
            boolean matchesPayment = "Tất cả".equals(paymentFilter)
                    || ("COD".equals(paymentFilter) == isCod);
            if (matchesQuery && matchesStatus && matchesDate && matchesPayment) {
                displayedList.add(o);
            }
        }
        notifyDataSetChanged();
        notifySummaryChanged();
    }

    private void notifySummaryChanged() {
        if (summaryListener != null) {
            int scopedTotal = 0, pending = 0, preparing = 0, delivering = 0, completed = 0;
            for (Order o : originalList) {
                if (!matchesSummaryScope(o)) continue;
                scopedTotal++;
                String st = Order.normalizeStatus(o.getStatus());
                if (Order.STATUS_PENDING.equals(st)) {
                    pending++;
                } else if (Order.STATUS_PREPARING.equals(st)) {
                    preparing++;
                } else if (Order.STATUS_SHIPPING.equals(st)) {
                    delivering++;
                } else if (Order.STATUS_COMPLETED.equals(st)) {
                    completed++;
                }
            }
            summaryListener.onSummaryChanged(scopedTotal, pending, preparing, delivering, completed);
        }
    }

    private boolean matchesSummaryScope(Order order) {
        if (daysFilter > 0) {
            java.util.Calendar start = java.util.Calendar.getInstance();
            start.set(java.util.Calendar.HOUR_OF_DAY, 0);
            start.set(java.util.Calendar.MINUTE, 0);
            start.set(java.util.Calendar.SECOND, 0);
            start.set(java.util.Calendar.MILLISECOND, 0);
            start.add(java.util.Calendar.DAY_OF_YEAR, 1);
            Date end = start.getTime();
            start.add(java.util.Calendar.DAY_OF_YEAR, -daysFilter);
            if (order.getCreatedAt() == null || order.getCreatedAt().before(start.getTime())
                    || !order.getCreatedAt().before(end)) return false;
        }
        boolean cod = order.isCodPayment();
        if (!"Tất cả".equals(paymentFilter) && ("COD".equals(paymentFilter) != cod)) return false;
        if (summaryQuery.isEmpty()) return true;
        String id = order.getOrderId() == null ? "" : order.getOrderId().toLowerCase(Locale.ROOT);
        String name = order.getCustomerName() == null ? "" : order.getCustomerName().toLowerCase(Locale.ROOT);
        return id.contains(summaryQuery) || name.contains(summaryQuery)
                || stripAccents(name).contains(stripAccents(summaryQuery));
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_restaurant, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = displayedList.get(position);
        if (order == null) return;

        // 1. Order ID & Time
        String displayId = order.getOrderId();
        if (displayId != null && displayId.length() > 14) {
            displayId = displayId.substring(0, 14) + "...";
        }
        holder.tvOrderId.setText("#" + (displayId != null ? displayId : ""));

        final String fullOrderId = order.getOrderId() != null ? order.getOrderId() : "";
        holder.btnCopyOrderId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Order ID", fullOrderId);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Đã sao chép mã đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });

        String orderTime = "Chưa có thời gian";
        if (order.getCreatedAt() != null) {
            orderTime = dateFormat.format(order.getCreatedAt());
        }
        holder.tvOrderTime.setText(orderTime);

        // 2. Status styling & Actions
        String status = Order.normalizeStatus(order.getStatus());
        holder.tvTotalPriceLabel.setText("Tổng đơn: ");
        if (Order.STATUS_PENDING.equals(status)) {
            holder.viewStatusAccentLine.setBackgroundColor(Color.parseColor("#F59E0B"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatusBadge.setText("● CHỜ XÁC NHẬN");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#D97706"));

            holder.tvCookingShipperStatus.setVisibility(View.GONE);
            holder.btnActionPrimary.setVisibility(View.VISIBLE);
            holder.btnActionPrimary.setText("✓ XÁC NHẬN ĐƠN");
            holder.btnActionPrimary.setBackgroundResource(R.drawable.bg_btn_confirm_order);
            holder.btnActionPrimary.setOnClickListener(v -> updateOrderStatus(order, Order.STATUS_PREPARING));

        } else if (Order.STATUS_PREPARING.equals(status)) {
            holder.viewStatusAccentLine.setBackgroundColor(Color.parseColor("#EA580C"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_preparing);
            holder.tvStatusBadge.setText("● ĐANG CHUẨN BỊ");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#C2410C"));

            holder.tvCookingShipperStatus.setVisibility(View.VISIBLE);
            holder.tvCookingShipperStatus.setText("🌿 Đang nấu");
            holder.tvCookingShipperStatus.setTextColor(Color.parseColor("#0D9488"));

            holder.btnActionPrimary.setVisibility(View.VISIBLE);
            holder.btnActionPrimary.setText("🛵 GIAO CHO SHIPPER");
            holder.btnActionPrimary.setBackgroundResource(R.drawable.bg_btn_handover_shipper);
            holder.btnActionPrimary.setOnClickListener(v -> updateOrderStatus(order, Order.STATUS_SHIPPING));

        } else if (Order.STATUS_SHIPPING.equals(status)) {
            holder.viewStatusAccentLine.setBackgroundColor(Color.parseColor("#16A34A"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_delivering);
            holder.tvStatusBadge.setText("🛵 ĐANG GIAO HÀNG");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#16A34A"));

            holder.tvCookingShipperStatus.setVisibility(View.VISIBLE);
            holder.tvCookingShipperStatus.setText("Đang giao hàng");
            holder.tvCookingShipperStatus.setTextColor(Color.parseColor("#334155"));

            holder.btnActionPrimary.setVisibility(View.VISIBLE);
            holder.btnActionPrimary.setText("✓ HOÀN TẤT ĐƠN");
            holder.btnActionPrimary.setBackgroundResource(R.drawable.bg_btn_finish_order);
            holder.btnActionPrimary.setOnClickListener(v -> updateOrderStatus(order, Order.STATUS_COMPLETED));

        } else if (Order.STATUS_CANCELLED.equals(status)) {
            holder.viewStatusAccentLine.setBackgroundColor(Color.parseColor("#EF4444"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed);
            holder.tvStatusBadge.setText("✕ ĐÃ HỦY ĐƠN");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#DC2626"));

            holder.tvCookingShipperStatus.setVisibility(View.GONE);
            holder.tvTotalPriceLabel.setText("Đơn đã hủy: ");
            holder.btnActionPrimary.setVisibility(View.GONE);

        } else {
            // Completed / Đã hoàn thành
            holder.viewStatusAccentLine.setBackgroundColor(Color.parseColor("#94A3B8"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed);
            holder.tvStatusBadge.setText("✓ ĐÃ HOÀN THÀNH");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#475569"));

            holder.tvCookingShipperStatus.setVisibility(View.GONE);
            holder.tvTotalPriceLabel.setText("✓ Tổng đơn: ");
            holder.btnActionPrimary.setVisibility(View.GONE);
        }

        // 3. Customer Info & Phone Call
        String customerName = order.getCustomerName() != null && !order.getCustomerName().isEmpty()
                ? order.getCustomerName() : "Khách hàng";
        String customerPhone = order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()
                ? order.getCustomerPhone() : "Chưa có số điện thoại";
        holder.tvCustomerNamePhone.setText(customerName + " • " + customerPhone);

        String deliveryAddress = order.getDeliveryAddressText();
        String customerAddress = deliveryAddress.isEmpty() ? "Chưa có địa chỉ" : deliveryAddress;
        holder.tvCustomerAddress.setText(customerAddress);

        final String finalPhone = customerPhone;
        holder.btnCallCustomer.setEnabled(order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty());
        holder.btnCallCustomer.setOnClickListener(v -> {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + finalPhone));
                context.startActivity(callIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Gọi đến: " + finalPhone, Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Food Items list
        holder.layoutOrderItems.removeAllViews();
        List<CartItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (CartItem item : items) {
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_order_row_food, holder.layoutOrderItems, false);
                TextView tvQuantity = itemView.findViewById(R.id.tv_item_quantity);
                TextView tvName = itemView.findViewById(R.id.tv_item_name);
                TextView tvPrice = itemView.findViewById(R.id.tv_item_price);

                tvQuantity.setText(item.getQuantity() + "x");
                tvName.setText(item.getName());
                tvPrice.setText(currencyFormatter.format(item.getPrice() * item.getQuantity()) + " đ");

                holder.layoutOrderItems.addView(itemView);
            }
        }

        // 5. Payment method & Total price
        String payMethod = order.getPaymentMethod();
        if (payMethod == null || payMethod.isEmpty()) {
            holder.tvPaymentMethodPill.setText("Chưa rõ phương thức");
        } else if (payMethod.toUpperCase(Locale.ROOT).contains("COD")) {
            holder.tvPaymentMethodPill.setText("Tiền mặt COD");
        } else {
            holder.tvPaymentMethodPill.setText(payMethod);
        }

        holder.tvTotalPriceValue.setText(currencyFormatter.format(order.getTotalAmount()) + " đ");
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            Toast.makeText(context, "Đơn hàng không có mã hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", newStatus);
        Date now = new Date();
        if (Order.STATUS_PREPARING.equals(newStatus)) {
            updates.put("confirmedAt", now);
        } else if (Order.STATUS_SHIPPING.equals(newStatus)) {
            updates.put("shippingAt", now);
        } else if (Order.STATUS_COMPLETED.equals(newStatus)) {
            updates.put("deliveredAt", now);
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.update(db.collection("orders").document(order.getOrderId()), updates);
        if (order.getRestaurantId() != null && !order.getRestaurantId().isEmpty()) {
            batch.update(db.collection("restaurants").document(order.getRestaurantId()).collection("orders").document(order.getOrderId()), updates);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    order.setStatus(newStatus);
                    notifyDataSetChanged();
                    notifySummaryChanged();
                    Toast.makeText(context, "Đã cập nhật: " + getStatusDisplayText(newStatus), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Không cập nhật được đơn hàng", Toast.LENGTH_SHORT).show();
                });
    }

    private String getStatusDisplayText(String status) {
        if (Order.STATUS_PREPARING.equals(status)) return "Đang chuẩn bị";
        if (Order.STATUS_SHIPPING.equals(status)) return "Đang giao hàng";
        if (Order.STATUS_COMPLETED.equals(status)) return "Đã hoàn thành";
        if (Order.STATUS_CANCELLED.equals(status)) return "Đã hủy đơn";
        return "Chờ xác nhận";
    }

    @Override
    public int getItemCount() {
        return displayedList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        View viewStatusAccentLine;
        TextView tvOrderId;
        ImageView btnCopyOrderId;
        TextView tvOrderTime;
        TextView tvStatusBadge;
        TextView tvCustomerNamePhone;
        TextView tvCustomerAddress;
        FrameLayout btnCallCustomer;
        LinearLayout layoutOrderItems;
        TextView tvPaymentMethodPill;
        TextView tvCookingShipperStatus;
        TextView tvTotalPriceLabel;
        TextView tvTotalPriceValue;
        Button btnActionPrimary;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusAccentLine   = itemView.findViewById(R.id.view_status_accent_line);
            tvOrderId              = itemView.findViewById(R.id.tv_order_id);
            btnCopyOrderId         = itemView.findViewById(R.id.btn_copy_order_id);
            tvOrderTime            = itemView.findViewById(R.id.tv_order_time);
            tvStatusBadge          = itemView.findViewById(R.id.tv_status_badge);
            tvCustomerNamePhone    = itemView.findViewById(R.id.tv_customer_name_phone);
            tvCustomerAddress      = itemView.findViewById(R.id.tv_customer_address);
            btnCallCustomer        = itemView.findViewById(R.id.btn_call_customer);
            layoutOrderItems       = itemView.findViewById(R.id.layout_order_items);
            tvPaymentMethodPill    = itemView.findViewById(R.id.tv_payment_method_pill);
            tvCookingShipperStatus = itemView.findViewById(R.id.tv_cooking_shipper_status);
            tvTotalPriceLabel      = itemView.findViewById(R.id.tv_total_price_label);
            tvTotalPriceValue      = itemView.findViewById(R.id.tv_total_price_value);
            btnActionPrimary       = itemView.findViewById(R.id.btn_action_primary);
        }
    }
}
