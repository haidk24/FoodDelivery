package com.example.foodorderapp.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.ui.restaurant.RestaurantMainActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Creates device notifications from the restaurant's live Firestore order listener.
 * It deliberately ignores the first snapshot, which contains orders that existed before
 * the restaurant app started listening.
 */
public final class RestaurantOrderNotifier {
    private static final String CHANNEL_ID = "restaurant_order_updates";
    private static final String CHANNEL_NAME = "Đơn hàng mới";

    private final Context context;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private ListenerRegistration registration;
    private boolean initialSnapshotReceived;

    public RestaurantOrderNotifier(Context context) {
        this.context = context.getApplicationContext();
        ensureChannel(context);
    }

    public void start(String restaurantId) {
        stop();
        if (restaurantId == null || restaurantId.trim().isEmpty()) return;
        initialSnapshotReceived = false;
        registration = FirebaseFirestore.getInstance().collection("restaurants")
                .document(restaurantId.trim()).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    if (!initialSnapshotReceived) {
                        initialSnapshotReceived = true;
                        return;
                    }
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;
                        Order order = change.getDocument().toObject(Order.class);
                        order.setOrderId(change.getDocument().getId());
                        if (Order.STATUS_PENDING.equals(Order.normalizeStatus(order.getStatus()))) {
                            show(order, restaurantId.trim());
                        }
                    }
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void show(Order order, String restaurantId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String orderId = order.getOrderId() == null ? "" : order.getOrderId();
        String customerName = order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()
                ? "Khách hàng" : order.getCustomerName().trim();
        String body = customerName + " vừa đặt đơn "
                + currencyFormat.format(order.getTotalAmount()) + " đ";
        Intent intent = new Intent(context, RestaurantMainActivity.class)
                .putExtra(RestaurantMainActivity.EXTRA_TAB, R.id.nav_order)
                .putExtra("EXTRA_RESTAURANT_ID", restaurantId)
                .putExtra("EXTRA_ORDER_ID", orderId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, orderId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Bạn có đơn hàng mới")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Thông báo khi có đơn mới trong lúc ứng dụng nhà hàng đang chạy");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}
