package com.example.foodorderapp.data.dataSource.firebase;

import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.FoodCategory;
import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FoodFirebaseDataSource {
    public interface FoodsListener { void onFoods(List<Food> foods); void onError(Exception error); }
    public interface Completion { void onComplete(Exception error); }
    public interface RestaurantIdCallback { void onResult(String restaurantId, Exception error); }
    public interface RestaurantNameCallback { void onResult(String name, Exception error); }
    public interface RestaurantCallback { void onResult(Restaurant restaurant, Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration observeRestaurantFoods(String restaurantId, FoodsListener listener) {
        return db.collection("restaurants").document(restaurantId).collection("foods")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<Food> result = new ArrayList<>();
                    if (snapshot != null) for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Food food = doc.toObject(Food.class);
                        if (food == null) continue;
                        food.setFoodId(doc.getId());
                        String raw = doc.getString("category");
                        String normalized = FoodCategory.normalize(raw);
                        food.setCategory(normalized);
                        if (!normalized.equals(raw)) doc.getReference().update("category", normalized);
                        result.add(food);
                    }
                    listener.onFoods(result);
                });
    }
    public void setAvailable(String restaurantId, String foodId, boolean available, Completion completion) {
        db.collection("restaurants").document(restaurantId).collection("foods").document(foodId)
                .update("isAvailable", available).addOnSuccessListener(v -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }
    public void delete(String restaurantId, String foodId, Completion completion) {
        db.collection("restaurants").document(restaurantId).collection("foods").document(foodId)
                .delete().addOnSuccessListener(v -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }
    public void resolveRestaurantId(String userId, RestaurantIdCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onResult("", new IllegalArgumentException("Không xác định được tài khoản"));
            return;
        }
        db.collection("users").document(userId).get().addOnSuccessListener(user -> {
            String restaurantId = user.getString("restaurantId");
            if (restaurantId != null && !restaurantId.trim().isEmpty()) {
                callback.onResult(restaurantId.trim(), null);
                return;
            }
            db.collection("restaurants").whereEqualTo("ownerId", userId).limit(1).get()
                    .addOnSuccessListener(restaurants -> {
                        if (restaurants.isEmpty()) callback.onResult("", new IllegalStateException("Không tìm thấy nhà hàng của tài khoản này"));
                        else callback.onResult(restaurants.getDocuments().get(0).getId(), null);
                    }).addOnFailureListener(error -> callback.onResult("", error));
        }).addOnFailureListener(error -> callback.onResult("", error));
    }
    public void create(String restaurantId, Food food, Completion completion) {
        com.google.firebase.firestore.DocumentReference reference = db.collection("restaurants").document(restaurantId).collection("foods").document();
        food.setFoodId(reference.getId());
        food.setRestaurantId(restaurantId);
        reference.set(food).addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
    }
    public void update(String restaurantId, String foodId, Map<String, Object> updates, Completion completion) {
        db.collection("restaurants").document(restaurantId).collection("foods").document(foodId)
                .update(updates).addOnSuccessListener(v -> completion.onComplete(null)).addOnFailureListener(completion::onComplete);
    }
    public void getRestaurantName(String restaurantId, RestaurantNameCallback callback) {
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener(document -> {
            String name = document.getString("name");
            callback.onResult(name == null ? "" : name.trim(), null);
        }).addOnFailureListener(error -> callback.onResult("", error));
    }
    public void getRestaurant(String restaurantId, RestaurantCallback callback) {
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener(document -> {
            Restaurant restaurant = document.toObject(Restaurant.class);
            if (restaurant != null) restaurant.setRestaurantId(document.getId());
            callback.onResult(restaurant, restaurant == null
                    ? new IllegalArgumentException("Không tìm thấy nhà hàng") : null);
        }).addOnFailureListener(error -> callback.onResult(null, error));
    }
}
