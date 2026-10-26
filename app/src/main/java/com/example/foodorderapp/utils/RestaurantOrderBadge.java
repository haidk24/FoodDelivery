package com.example.foodorderapp.utils;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/** Keeps the restaurant order badge in sync with pending Firestore orders. */
public final class RestaurantOrderBadge {
    private final BottomNavigationView navigationView;
    private ListenerRegistration registration;

    public RestaurantOrderBadge(BottomNavigationView navigationView) {
        this.navigationView = navigationView;
        navigationView.removeBadge(R.id.nav_order);
    }

    public void listen(String restaurantId) {
        stop();
        if (restaurantId == null || restaurantId.trim().isEmpty()) return;
        registration = FirebaseFirestore.getInstance().collection("restaurants")
                .document(restaurantId).collection("orders")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        navigationView.removeBadge(R.id.nav_order);
                        return;
                    }
                    int pending = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (Order.STATUS_PENDING.equals(Order.normalizeStatus(doc.getString("status")))) {
                            pending++;
                        }
                    }
                    if (pending == 0) {
                        navigationView.removeBadge(R.id.nav_order);
                    } else {
                        com.google.android.material.badge.BadgeDrawable badge =
                                navigationView.getOrCreateBadge(R.id.nav_order);
                        badge.setNumber(pending);
                        badge.setVisible(true);
                    }
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        navigationView.removeBadge(R.id.nav_order);
    }
}
