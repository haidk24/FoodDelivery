package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

/** Data boundary for restaurant moderation performed by an admin. */
public class AdminRestaurantRepository {
    public interface RestaurantsListener { void onRestaurants(List<Restaurant> restaurants); void onError(Exception error); }
    public interface Completion { void onComplete(Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration observe(RestaurantsListener listener) {
        return db.collection("restaurants").addSnapshotListener((snapshot, error) -> {
            if (error != null) { listener.onError(error); return; }
            List<Restaurant> restaurants = new ArrayList<>();
            if (snapshot != null) for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                Restaurant restaurant = document.toObject(Restaurant.class);
                if (restaurant != null) { restaurant.restaurantId = document.getId(); restaurants.add(restaurant); }
            }
            listener.onRestaurants(restaurants);
        });
    }
    public void approve(String restaurantId, Completion completion) {
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener(restaurant -> {
            if (!restaurant.exists()) { completion.onComplete(new IllegalArgumentException("Không tìm thấy hồ sơ nhà hàng")); return; }
            String ownerId = restaurant.getString("ownerId");
            if (ownerId == null || ownerId.trim().isEmpty()) { completion.onComplete(new IllegalArgumentException("Hồ sơ chưa có ownerId hợp lệ")); return; }
            WriteBatch batch = db.batch();
            batch.update(restaurant.getReference(), "status", "active");
            batch.update(db.collection("users").document(ownerId), "role", "restaurant");
            batch.commit().addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
        }).addOnFailureListener(completion::onComplete);
    }
    public void setStatus(String restaurantId, String status, Completion completion) {
        db.collection("restaurants").document(restaurantId).update("status", status)
                .addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
    }
    public void get(String restaurantId, java.util.function.BiConsumer<Restaurant, Exception> callback) {
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener(document -> {
            Restaurant restaurant = document.toObject(Restaurant.class);
            if (restaurant != null) restaurant.restaurantId = document.getId();
            callback.accept(restaurant, null);
        }).addOnFailureListener(error -> callback.accept(null, error));
    }
}
