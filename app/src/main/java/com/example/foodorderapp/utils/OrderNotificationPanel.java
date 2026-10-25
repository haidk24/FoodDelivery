package com.example.foodorderapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.foodorderapp.data.model.Order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Renders live order notifications without duplicating order data in Firestore. */
public final class OrderNotificationPanel {
    private OrderNotificationPanel() { }

    public static void show(Context context, List<Order> orders, boolean restaurant) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 20);
        list.setPadding(padding, dp(context, 4), padding, dp(context, 4));
        if (orders == null || orders.isEmpty()) {
            TextView empty = text(context, "Chưa có thông báo", 15, false);
            empty.setPadding(0, dp(context, 20), 0, dp(context, 20));
            list.addView(empty);
        } else {
            SimpleDateFormat time = new SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault());
            for (Order order : orders) {
                String status = Order.normalizeStatus(order.getStatus());
                boolean pending = Order.STATUS_PENDING.equals(status);
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, dp(context, 12), 0, dp(context, 12));
                row.setAlpha(restaurant && !pending ? .45f : 1f);
                String customer = order.getCustomerName().isEmpty() ? "Khách hàng" : order.getCustomerName();
                String title = restaurant
                        ? (pending ? "Đơn hàng mới từ " + customer : "Đơn #" + shortId(order) + " đã được xử lý")
                        : "Đơn #" + shortId(order) + " • " + statusText(status);
                TextView heading = text(context, title, 15, true);
                TextView detail = text(context, restaurant
                        ? (pending ? "Chờ xác nhận • " + money(order.getTotalAmount()) : "Đã xác nhận hoặc xử lý")
                        : "Cập nhật trạng thái đơn hàng", 13, false);
                Date created = order.getCreatedAt();
                TextView date = text(context, created == null ? "" : time.format(created), 12, false);
                detail.setTextColor(Color.rgb(71, 85, 105));
                date.setTextColor(Color.rgb(100, 116, 139));
                row.addView(heading); row.addView(detail); row.addView(date);
                list.addView(row);
            }
        }
        ScrollView scroll = new ScrollView(context); scroll.addView(list);
        new AlertDialog.Builder(context).setTitle("Thông báo")
                .setView(scroll).setPositiveButton("Đóng", null).show();
    }

    private static TextView text(Context c, String value, int size, boolean bold) {
        TextView view = new TextView(c); view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(15, 23, 42)); view.setGravity(Gravity.START);
        if (bold) view.setTypeface(null, Typeface.BOLD); return view;
    }
    private static String shortId(Order order) { String id = order.getOrderId(); return id == null ? "" : id.length() > 8 ? id.substring(0, 8) : id; }
    private static String money(double amount) { return String.format(Locale.getDefault(), "%,.0f đ", amount); }
    private static String statusText(String status) { if (Order.STATUS_PENDING.equals(status)) return "Đang chờ quán xác nhận"; if (Order.STATUS_PREPARING.equals(status)) return "Quán đang chuẩn bị"; if (Order.STATUS_SHIPPING.equals(status)) return "Đang giao"; if (Order.STATUS_COMPLETED.equals(status)) return "Đã hoàn thành"; return "Đã hủy"; }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
