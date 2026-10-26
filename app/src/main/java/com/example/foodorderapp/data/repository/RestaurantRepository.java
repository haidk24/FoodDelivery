package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.FirebaseFirestore;

/** Shared Firestore boundary for customer-facing restaurant data. */
public class RestaurantRepository {
    public interface RestaurantCallback {
        void onResult(Restaurant restaurant, Exception error);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void get(String restaurantId, RestaurantCallback callback) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            callback.onResult(null, new IllegalArgumentException("Không xác định được nhà hàng"));
            return;
        }
        db.collection("restaurants").document(restaurantId.trim()).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onResult(null, new IllegalArgumentException("Không tìm thấy nhà hàng"));
                        return;
                    }
                    Restaurant restaurant = document.toObject(Restaurant.class);
                    if (restaurant != null) restaurant.setRestaurantId(document.getId());
                    callback.onResult(restaurant, restaurant == null
                            ? new IllegalStateException("Dữ liệu nhà hàng không hợp lệ") : null);
                })
                .addOnFailureListener(error -> callback.onResult(null, error));
    }
}
