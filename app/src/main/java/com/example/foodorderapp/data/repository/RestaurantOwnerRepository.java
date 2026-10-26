package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Owner-facing restaurant profile and restaurant-order listeners. */
public class RestaurantOwnerRepository {
    public interface RestaurantIdCallback { void onResult(String restaurantId, Exception error); }
    public interface RestaurantListener { void onRestaurant(Restaurant restaurant); void onError(Exception error); }
    public interface OrdersListener { void onOrders(List<Order> orders); void onError(Exception error); }
    public interface Completion { void onComplete(Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void resolveRestaurantId(String ownerId, RestaurantIdCallback callback) {
        db.collection("users").document(ownerId).get().addOnSuccessListener(user -> {
            String restaurantId = user.getString("restaurantId");
            if (restaurantId != null && !restaurantId.trim().isEmpty()) {
                callback.onResult(restaurantId.trim(), null);
                return;
            }
            db.collection("restaurants").whereEqualTo("ownerId", ownerId).limit(1).get()
                    .addOnSuccessListener(restaurants -> {
                        if (restaurants.isEmpty()) callback.onResult("", new IllegalStateException("Không tìm thấy nhà hàng"));
                        else callback.onResult(restaurants.getDocuments().get(0).getId(), null);
                    })
                    .addOnFailureListener(error -> callback.onResult("", error));
        }).addOnFailureListener(error -> callback.onResult("", error));
    }

    public ListenerRegistration observeRestaurant(String restaurantId, RestaurantListener listener) {
        return db.collection("restaurants").document(restaurantId).addSnapshotListener((document, error) -> {
            if (error != null) { listener.onError(error); return; }
            Restaurant restaurant = document == null ? null : document.toObject(Restaurant.class);
            if (restaurant == null) { listener.onError(new IllegalStateException("Không tìm thấy nhà hàng")); return; }
            restaurant.setRestaurantId(document.getId());
            listener.onRestaurant(restaurant);
        });
    }

    public ListenerRegistration observeOrders(String restaurantId, OrdersListener listener) {
        return db.collection("restaurants").document(restaurantId).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<Order> orders = new ArrayList<>();
                    if (snapshot != null) for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                        Order order = document.toObject(Order.class);
                        if (order == null) continue;
                        order.setOrderId(document.getId());
                        orders.add(order);
                    }
                    listener.onOrders(orders);
                });
    }

    public void updateStatus(String restaurantId, String status, Completion completion) {
        db.collection("restaurants").document(restaurantId).update("status", status)
                .addOnSuccessListener(unused -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }
}
