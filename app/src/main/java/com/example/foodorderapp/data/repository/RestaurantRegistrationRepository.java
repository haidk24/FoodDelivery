package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/** Data boundary for a customer applying to open a restaurant. */
public class RestaurantRegistrationRepository {
    public interface RestaurantCallback { void onResult(Restaurant restaurant, Exception error); }
    public interface Completion { void onComplete(Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void findByOwner(String userId, RestaurantCallback callback) {
        db.collection("restaurants").whereEqualTo("ownerId", userId).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) { callback.onResult(null, null); return; }
                    com.google.firebase.firestore.DocumentSnapshot document = snapshot.getDocuments().get(0);
                    Restaurant restaurant = document.toObject(Restaurant.class);
                    if (restaurant != null) restaurant.setRestaurantId(document.getId());
                    callback.onResult(restaurant, null);
                })
                .addOnFailureListener(error -> callback.onResult(null, error));
    }

    public void submit(String ownerId, String name, String phone, Map<String, Object> address,
                       boolean hasLicence, Completion completion) {
        Restaurant restaurant = new Restaurant();
        restaurant.ownerId = ownerId;
        restaurant.name = name;
        restaurant.phone = phone;
        restaurant.address = address;
        restaurant.hasLicence = hasLicence;
        restaurant.status = "pending";
        restaurant.rating = 0;
        restaurant.createdAt = FieldValue.serverTimestamp();
        db.collection("restaurants").add(restaurant)
                .addOnSuccessListener(reference -> completion.onComplete(null))
                .addOnFailureListener(completion::onComplete);
    }
}
