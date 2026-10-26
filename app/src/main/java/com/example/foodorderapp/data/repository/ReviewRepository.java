package com.example.foodorderapp.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.foodorderapp.data.model.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class ReviewRepository {

    private static ReviewRepository instance;
    private final FirebaseFirestore db;

    private ReviewRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static ReviewRepository getInstance() {
        if (instance == null) {
            instance = new ReviewRepository();
        }
        return instance;
    }

    public void submitReview(Review review,
                             String orderId,
                             String restaurantId,
                             MutableLiveData<Boolean> isSuccess,
                             MutableLiveData<String> errorMessage,
                             MutableLiveData<Boolean> isLoading) {
        String safeRestaurantId = restaurantId == null ? "" : restaurantId.trim();
        String reviewDocId = orderId + "_" + review.getCustomerId() + "_" + safeRestaurantId;
        review.setReviewId(reviewDocId);

        DocumentReference reviewRef = db.collection("reviews").document(reviewDocId);
        DocumentReference orderRef = db.collection("orders").document(orderId);
        DocumentReference restaurantOrderRef = null;
        if (!safeRestaurantId.isEmpty()) {
            restaurantOrderRef = db.collection("restaurants").document(safeRestaurantId)
                    .collection("orders").document(orderId);
        }

        DocumentReference finalRestaurantOrderRef = restaurantOrderRef;
        db.runTransaction(transaction -> {
                    DocumentSnapshot orderSnap = transaction.get(orderRef);
                    DocumentSnapshot restaurantOrderSnap = null;
                    if (finalRestaurantOrderRef != null) {
                        restaurantOrderSnap = transaction.get(finalRestaurantOrderRef);
                    }

                    if (!orderSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "Khong tim thay don hang de danh gia",
                                FirebaseFirestoreException.Code.NOT_FOUND
                        );
                    }

                    String status = orderSnap.getString("status");
                    String normalizedStatus = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
                    boolean completed = "delivered".equals(normalizedStatus) || "completed".equals(normalizedStatus);
                    if (!completed) {
                        throw new FirebaseFirestoreException(
                                "Chi co the danh gia don da hoan tat",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        );
                    }

                    Boolean reviewed = orderSnap.getBoolean("reviewed");
                    if (Boolean.TRUE.equals(reviewed)) {
                        throw new FirebaseFirestoreException(
                                "Don hang nay da duoc danh gia",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS
                        );
                    }

                    DocumentSnapshot reviewSnap = transaction.get(reviewRef);
                    if (reviewSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "Ban da gui danh gia cho don hang nay",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS
                        );
                    }

                    Map<String, Object> reviewedUpdate = new HashMap<>();
                    reviewedUpdate.put("reviewed", true);
                    reviewedUpdate.put("reviewedAt", FieldValue.serverTimestamp());

                    transaction.set(reviewRef, review);
                    transaction.update(orderRef, reviewedUpdate);

                    if (finalRestaurantOrderRef != null && restaurantOrderSnap != null && restaurantOrderSnap.exists()) {
                        transaction.update(finalRestaurantOrderRef, reviewedUpdate);
                    }
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    isSuccess.setValue(true);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                    isLoading.setValue(false);
                });
    }
}