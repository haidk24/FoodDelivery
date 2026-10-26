package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Firestore operations for admin-managed user accounts. */
public class AdminUserRepository {
    public interface Completion { void onComplete(Exception error); }
    public interface UserCallback { void onResult(User user, Exception error); }
    public interface UsersCallback { void onResult(List<User> users, Exception error); }
    public interface OrdersCallback { void onResult(List<Order> orders, Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadUsers(String roleFilter, UsersCallback callback) {
        Query query = db.collection("users");
        if (roleFilter != null && !roleFilter.trim().isEmpty() && !"all".equals(roleFilter)) {
            query = query.whereEqualTo("role", roleFilter);
        }
        query.get().addOnSuccessListener(snapshot -> {
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                User user = document.toObject(User.class);
                if (user == null) continue;
                user.setUid(document.getId());
                user.setOrderCount(0);
                user.setTotalSpent(0L);
                users.add(user);
            }
            Collections.sort(users, (left, right) ->
                    Long.compare(createdAtMillis(right.getCreatedAt()), createdAtMillis(left.getCreatedAt())));
            attachSummaries(users, callback);
        }).addOnFailureListener(error -> callback.onResult(Collections.emptyList(), error));
    }

    private void attachSummaries(List<User> users, UsersCallback callback) {
        Map<String, User> usersById = new HashMap<>();
        for (User user : users) {
            if (user.getUid() != null) usersById.put(user.getUid(), user);
        }
        AtomicInteger remaining = new AtomicInteger(2);
        AtomicReference<Exception> firstError = new AtomicReference<>();
        Runnable complete = () -> {
            if (remaining.decrementAndGet() == 0) callback.onResult(users, firstError.get());
        };

        db.collection("restaurants").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                User owner = usersById.get(document.getString("ownerId"));
                if (owner == null) continue;
                owner.setRestaurantId(document.getId());
                owner.setRestaurantName(document.getString("name"));
            }
            complete.run();
        }).addOnFailureListener(error -> {
            firstError.compareAndSet(null, error);
            complete.run();
        });

        db.collection("orders").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                User customer = usersById.get(document.getString("customerId"));
                if (customer == null) continue;
                customer.setOrderCount(customer.getOrderCount() + 1);
                if (!Order.STATUS_COMPLETED.equals(Order.normalizeStatus(document.getString("status")))) continue;
                Object amount = document.get("totalAmount");
                if (amount instanceof Number) {
                    customer.setTotalSpent(customer.getTotalSpent() + ((Number) amount).longValue());
                }
            }
            complete.run();
        }).addOnFailureListener(error -> {
            firstError.compareAndSet(null, error);
            complete.run();
        });
    }

    private long createdAtMillis(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Date) return ((Date) value).getTime();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    public void createProfile(User user, Completion completion) {
        if (user == null || user.getUid() == null || user.getUid().trim().isEmpty()) {
            completion.onComplete(new IllegalArgumentException("Không xác định được tài khoản người dùng"));
            return;
        }
        db.collection("users").document(user.getUid()).set(user)
                .addOnSuccessListener(unused -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }

    public void getOrders(String customerId, OrdersCallback callback) {
        db.collection("orders").whereEqualTo("customerId", customerId).get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Order order = document.toObject(Order.class);
                        if (order == null) continue;
                        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
                            order.setOrderId(document.getId());
                        }
                        orders.add(order);
                    }
                    callback.onResult(orders, null);
                })
                .addOnFailureListener(error -> callback.onResult(Collections.emptyList(), error));
    }

    public void setStatus(String uid, String status, Completion completion) {
        db.collection("users").document(uid).update("status", status)
                .addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
    }
    public void update(User user, Completion completion) {
        if (user == null || user.getUid() == null || user.getUid().trim().isEmpty()) {
            completion.onComplete(new IllegalArgumentException("Không xác định được tài khoản người dùng"));
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", user.getDisplayName());
        updates.put("phone", user.getPhone());
        updates.put("vehicleDescription", user.getVehicleDescription());
        updates.put("role", user.getRole());
        db.collection("users").document(user.getUid()).update(updates)
                .addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
    }
    public void getDetail(String uid, UserCallback callback) {
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            User user = document.toObject(User.class);
            if (user == null) { callback.onResult(null, new IllegalArgumentException("Không tìm thấy tài khoản người dùng")); return; }
            user.setUid(document.getId());
            db.collection("orders").whereEqualTo("customerId", uid).get().addOnSuccessListener(orders -> {
                long totalSpent = 0;
                for (com.google.firebase.firestore.DocumentSnapshot order : orders.getDocuments()) {
                    if (!Order.STATUS_COMPLETED.equals(Order.normalizeStatus(order.getString("status")))) continue;
                    Object amount = order.get("totalAmount");
                    if (amount instanceof Number) totalSpent += ((Number) amount).longValue();
                }
                user.setOrderCount(orders.size());
                user.setTotalSpent(totalSpent);
                callback.onResult(user, null);
            }).addOnFailureListener(error -> callback.onResult(user, error));
        }).addOnFailureListener(error -> callback.onResult(null, error));
    }
}
