package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.AdminStats;
import com.example.foodorderapp.data.model.Order;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Aggregate data used only by the admin dashboard. */
public class AdminDashboardRepository {
    public interface Callback { void onResult(AdminStats stats, Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadStats(Callback callback) {
        AtomicLong users = new AtomicLong(), restaurants = new AtomicLong(), pending = new AtomicLong();
        AtomicLong ordersToday = new AtomicLong(), revenueToday = new AtomicLong(), revenueWeek = new AtomicLong();
        AtomicInteger remaining = new AtomicInteger(4);
        java.util.concurrent.atomic.AtomicReference<Exception> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Runnable complete = () -> {
            if (remaining.decrementAndGet() != 0) return;
            Exception error = failure.get();
            callback.onResult(error == null ? new AdminStats(users.get(), restaurants.get(), ordersToday.get(), revenueToday.get(), pending.get(), revenueWeek.get()) : null, error);
        };
        db.collection("users").count().get(AggregateSource.SERVER).addOnSuccessListener(r -> { users.set(r.getCount()); complete.run(); }).addOnFailureListener(e -> { failure.compareAndSet(null, e); complete.run(); });
        db.collection("restaurants").whereEqualTo("status", "active").count().get(AggregateSource.SERVER).addOnSuccessListener(r -> { restaurants.set(r.getCount()); complete.run(); }).addOnFailureListener(e -> { failure.compareAndSet(null, e); complete.run(); });
        db.collection("restaurants").whereEqualTo("status", "pending").count().get(AggregateSource.SERVER).addOnSuccessListener(r -> { pending.set(r.getCount()); complete.run(); }).addOnFailureListener(e -> { failure.compareAndSet(null, e); complete.run(); });
        Date today = startOfToday();
        db.collection("orders").whereGreaterThanOrEqualTo("createdAt", startOfWeek()).get().addOnSuccessListener(snapshot -> {
            for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                Object rawDate = document.get("createdAt");
                Date date = rawDate instanceof Timestamp ? ((Timestamp) rawDate).toDate() : rawDate instanceof Date ? (Date) rawDate : null;
                boolean isToday = date != null && !date.before(today);
                if (isToday) ordersToday.incrementAndGet();
                if (!Order.STATUS_COMPLETED.equals(Order.normalizeStatus(document.getString("status")))) continue;
                Object amount = document.get("totalAmount");
                if (amount instanceof Number) { revenueWeek.addAndGet(((Number) amount).longValue()); if (isToday) revenueToday.addAndGet(((Number) amount).longValue()); }
            }
            complete.run();
        }).addOnFailureListener(e -> { failure.compareAndSet(null, e); complete.run(); });
    }
    private Date startOfToday() { Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTime(); }
    private Date startOfWeek() { Calendar c = Calendar.getInstance(); c.setFirstDayOfWeek(Calendar.MONDAY); c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTime(); }
}
