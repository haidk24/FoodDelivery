package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads real, active shipper profiles that can be assigned by a restaurant. */
public class ShipperRepository {
    public interface Callback { void onResult(List<User> shippers, Exception error); }

    private final FirebaseFirestore firestore;

    public ShipperRepository() { this(FirebaseFirestore.getInstance()); }
    ShipperRepository(FirebaseFirestore firestore) { this.firestore = firestore; }

    public void loadActiveShippers(Callback callback) {
        firestore.collection("users").whereEqualTo("role", "shipper").get()
                .addOnSuccessListener(snapshot -> {
                    List<User> shippers = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        User shipper = document.toObject(User.class);
                        if (shipper == null) continue;
                        shipper.setUid(document.getId());
                        if (shipper.getStatus().isEmpty() || "active".equalsIgnoreCase(shipper.getStatus())) {
                            shippers.add(shipper);
                        }
                    }
                    Collections.sort(shippers, (left, right) -> left.getDisplayName()
                            .compareToIgnoreCase(right.getDisplayName()));
                    callback.onResult(shippers, null);
                }).addOnFailureListener(error -> callback.onResult(Collections.emptyList(), error));
    }
}
