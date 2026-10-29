package com.example.foodorderapp.data.remote.firebase;

import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Firebase access for orders. Keeps both legacy order paths in sync. */
public class OrderFirebaseDataSource {
    public interface OrdersListener {
        void onOrders(List<Order> orders);
        void onError(Exception error);
    }

    public interface Completion {
        void onComplete(Exception error);
    }
    public interface RecentOrdersCallback {
        void onResult(List<Order> orders, Exception error);
    }

    private final FirebaseFirestore firestore;

    public OrderFirebaseDataSource() {
        this(FirebaseFirestore.getInstance());
    }

    OrderFirebaseDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public ListenerRegistration observeRestaurantOrders(String restaurantId, OrdersListener listener) {
        return firestore.collection("restaurants").document(restaurantId).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    if (snapshot == null) { listener.onOrders(Collections.emptyList()); return; }
                    List<Order> orders = mapOrders(snapshot);
                    for (Order order : orders) {
                        if (order.getRestaurantId() == null || order.getRestaurantId().isEmpty()) {
                            order.setRestaurantId(restaurantId);
                        }
                    }
                    sortNewestFirst(orders);
                    listener.onOrders(orders);
                });
    }

    public ListenerRegistration observeCustomerOrders(String customerId, OrdersListener listener) {
        SwitchingRegistration registration = new SwitchingRegistration();
        Query ordered = customerQuery(customerId).orderBy("createdAt", Query.Direction.DESCENDING);
        registration.set(ordered.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (isIndexOrQueryCompatibilityError(error)) {
                    registration.set(customerQuery(customerId).addSnapshotListener((fallback, fallbackError) ->
                            deliverCustomerOrders(fallback, fallbackError, listener)));
                } else listener.onError(error);
                return;
            }
            deliverCustomerOrders(snapshot, null, listener);
        }));
        return registration;
    }

    public ListenerRegistration observeOrder(String orderId, OrdersListener listener) {
        return firestore.collection("orders").document(orderId).addSnapshotListener((snapshot, error) -> {
            if (error != null) { listener.onError(error); return; }
            if (snapshot == null || !snapshot.exists()) { listener.onOrders(Collections.emptyList()); return; }
            Order order = snapshot.toObject(Order.class);
            if (order == null) { listener.onOrders(Collections.emptyList()); return; }
            order.setOrderId(snapshot.getId());
            List<Order> result = new ArrayList<>();
            result.add(order);
            listener.onOrders(result);
        });
    }

    public void getRecentOrders(RecentOrdersCallback callback) {
        firestore.collection("orders").orderBy("createdAt", Query.Direction.DESCENDING).limit(2).get()
                .addOnSuccessListener(snapshot -> callback.onResult(mapOrders(snapshot), null))
                .addOnFailureListener(error -> callback.onResult(Collections.emptyList(), error));
    }

    private Query customerQuery(String customerId) {
        return firestore.collection("orders").whereEqualTo("customerId", customerId);
    }

    private void deliverCustomerOrders(QuerySnapshot snapshot, Exception error, OrdersListener listener) {
        if (error != null) { listener.onError(error); return; }
        if (snapshot == null) { listener.onOrders(Collections.emptyList()); return; }
        List<Order> orders = mapOrders(snapshot);
        sortNewestFirst(orders);
        listener.onOrders(orders);
    }

    private List<Order> mapOrders(QuerySnapshot snapshot) {
        List<Order> orders = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Order order = document.toObject(Order.class);
            if (order != null) {
                order.setOrderId(document.getId());
                orders.add(order);
            }
        }
        return orders;
    }

    private void sortNewestFirst(List<Order> orders) {
        Collections.sort(orders, (left, right) -> {
            Date first = left.getCreatedAt();
            Date second = right.getCreatedAt();
            if (first != null && second != null) {
                int compared = second.compareTo(first);
                if (compared != 0) return compared;
            } else if (first != null) return -1;
            else if (second != null) return 1;
            String leftId = left.getOrderId() == null ? "" : left.getOrderId();
            String rightId = right.getOrderId() == null ? "" : right.getOrderId();
            return rightId.compareTo(leftId);
        });
    }

    private boolean isIndexOrQueryCompatibilityError(Exception error) {
        if (!(error instanceof FirebaseFirestoreException)) return false;
        FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) error).getCode();
        return code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                || code == FirebaseFirestoreException.Code.INVALID_ARGUMENT;
    }

    public void createOrder(Order order, Completion completion) {
        if (order == null || isBlank(order.getRestaurantId())) {
            completion.onComplete(new IllegalArgumentException("Đơn hàng thiếu mã nhà hàng hợp lệ"));
            return;
        }
        if (isBlank(order.getRestaurantName()) || isBlank(order.getRestaurantAddress())) {
            completion.onComplete(new IllegalArgumentException("Đơn hàng thiếu snapshot tên hoặc địa chỉ nhà hàng"));
            return;
        }
        String id = isBlank(order.getOrderId())
                ? firestore.collection("orders").document().getId() : order.getOrderId();
        order.setOrderId(id);
        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("orders").document(id), order);
        batch.set(firestore.collection("restaurants").document(order.getRestaurantId())
                .collection("orders").document(id), order);
        batch.commit().addOnSuccessListener(unused -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }

    public void updateStatus(Order order, String newStatus, Completion completion) {
        if (order == null || isBlank(order.getOrderId()) || isBlank(order.getRestaurantId())) {
            completion.onComplete(new IllegalArgumentException("Đơn hàng thiếu mã đơn hoặc mã nhà hàng"));
            return;
        }
        String normalized = Order.normalizeStatus(newStatus);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", normalized);
        Date now = new Date();
        if (Order.STATUS_PREPARING.equals(normalized) || Order.STATUS_CONFIRMED.equals(normalized)) {
            updates.put("confirmedAt", now);
        }
        else if (Order.STATUS_SHIPPING.equals(normalized)) updates.put("shippingAt", now);
        else if (Order.STATUS_COMPLETED.equals(normalized)) updates.put("deliveredAt", now);

        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection("orders").document(order.getOrderId()), updates);
        batch.update(firestore.collection("restaurants").document(order.getRestaurantId())
                .collection("orders").document(order.getOrderId()), updates);
        batch.commit().addOnSuccessListener(unused -> {
            order.setStatus(normalized);
            completion.onComplete(null);
        }).addOnFailureListener(completion::onComplete);
    }

    /**
     * Atomically records the selected real shipper and starts delivery in both order paths.
     * No default or fabricated shipper data is ever written.
     */
    public void startDelivery(Order order, User shipper, Completion completion) {
        if (order == null || isBlank(order.getOrderId()) || isBlank(order.getRestaurantId())) {
            completion.onComplete(new IllegalArgumentException("Đơn hàng thiếu mã đơn hoặc mã nhà hàng"));
            return;
        }
        if (shipper == null || isBlank(shipper.getUid()) || !"shipper".equalsIgnoreCase(shipper.getRole())) {
            completion.onComplete(new IllegalArgumentException("Tài khoản được chọn không phải là tài xế hợp lệ"));
            return;
        }
        if (!isBlank(shipper.getStatus()) && !"active".equalsIgnoreCase(shipper.getStatus())) {
            completion.onComplete(new IllegalArgumentException("Tài xế được chọn hiện không hoạt động"));
            return;
        }

        Date now = new Date();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Order.STATUS_SHIPPING);
        updates.put("shippingAt", now);
        updates.put("shipperId", shipper.getUid());
        updates.put("shipperName", shipper.getDisplayName());
        updates.put("shipperPhone", shipper.getPhone());
        updates.put("shipperVehicle", shipper.getVehicleDescription());
        updates.put("shipperAssignedAt", now);

        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection("orders").document(order.getOrderId()), updates);
        batch.update(firestore.collection("restaurants").document(order.getRestaurantId())
                .collection("orders").document(order.getOrderId()), updates);
        batch.commit().addOnSuccessListener(unused -> {
            order.setStatus(Order.STATUS_SHIPPING);
            order.setShippingAt(now);
            order.setShipperId(shipper.getUid());
            order.setShipperName(shipper.getDisplayName());
            order.setShipperPhone(shipper.getPhone());
            order.setShipperVehicle(shipper.getVehicleDescription());
            order.setShipperAssignedAt(now);
            completion.onComplete(null);
        }).addOnFailureListener(completion::onComplete);
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private static final class SwitchingRegistration implements ListenerRegistration {
        private ListenerRegistration current;
        private boolean removed;
        synchronized void set(ListenerRegistration next) {
            if (removed) next.remove();
            else {
                if (current != null) current.remove();
                current = next;
            }
        }
        @Override public synchronized void remove() {
            removed = true;
            if (current != null) { current.remove(); current = null; }
        }
    }
}
