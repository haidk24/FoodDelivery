package com.example.foodorderapp.data.repository;


import androidx.lifecycle.MutableLiveData;

import com.example.foodorderapp.data.model.ReportStats;
import com.example.foodorderapp.data.model.Order;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ── Enum kỳ báo cáo ──────────────────────────────────────────
    public enum Period { TODAY, WEEK, MONTH }

    public static class SystemHealthStats {
        public final long activeRestaurants;
        public final long pendingRestaurants;
        public final long users;
        public SystemHealthStats(long activeRestaurants, long pendingRestaurants, long users) {
            this.activeRestaurants = activeRestaurants;
            this.pendingRestaurants = pendingRestaurants;
            this.users = users;
        }
    }
    public interface SystemHealthCallback {
        void onResult(SystemHealthStats stats, Exception error);
    }

    public void loadSystemHealth(SystemHealthCallback callback) {
        long[] values = new long[3];
        AtomicInteger remaining = new AtomicInteger(3);
        java.util.concurrent.atomic.AtomicReference<Exception> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Runnable complete = () -> {
            if (remaining.decrementAndGet() != 0) return;
            Exception error = failure.get();
            callback.onResult(error == null ? new SystemHealthStats(values[0], values[1], values[2]) : null, error);
        };
        db.collection("restaurants").whereEqualTo("status", "active").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> { values[0] = result.getCount(); complete.run(); })
                .addOnFailureListener(error -> { failure.compareAndSet(null, error); complete.run(); });
        db.collection("restaurants").whereEqualTo("status", "pending").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> { values[1] = result.getCount(); complete.run(); })
                .addOnFailureListener(error -> { failure.compareAndSet(null, error); complete.run(); });
        db.collection("users").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> { values[2] = result.getCount(); complete.run(); })
                .addOnFailureListener(error -> { failure.compareAndSet(null, error); complete.run(); });
    }

    // ── Load toàn bộ thống kê theo kỳ ────────────────────────────
    public void loadStats(
            Period period,
            MutableLiveData<ReportStats> liveData,
            MutableLiveData<Boolean>     loading,
            MutableLiveData<String>      error) {
        loadStats(period, null, liveData, loading, error);
    }

    public void loadStats(
            Period period,
            String restaurantId,
            MutableLiveData<ReportStats> liveData,
            MutableLiveData<Boolean> loading,
            MutableLiveData<String> error) {

        Date startDate = getStartDate(period);
        int  dayCount  = getDayCount(period);
        loadStatsForRange(startDate, new Date(), dayCount, restaurantId, liveData, loading, error);
    }

    public void loadStats(Date startDate, Date endDate, String restaurantId,
                          MutableLiveData<ReportStats> liveData,
                          MutableLiveData<Boolean> loading,
                          MutableLiveData<String> error) {
        long days = Math.max(1L, (endDate.getTime() - startDate.getTime()) / 86400000L + 1L);
        loadStatsForRange(startDate, endDate, (int) Math.min(days, 366L), restaurantId, liveData, loading, error);
    }

    private void loadStatsForRange(Date startDate, Date endDate, int dayCount, String restaurantId,
                                   MutableLiveData<ReportStats> liveData,
                                   MutableLiveData<Boolean> loading,
                                   MutableLiveData<String> error) {
        loading.postValue(true);

        String scopedRestaurantId = restaurantId == null ? "" : restaurantId.trim();
        boolean hasRestaurantScope = !scopedRestaurantId.isEmpty();

        Query query;
        if (hasRestaurantScope) {
            // Scoped mode: read directly from restaurant sub-collection to avoid index/rule issues.
            query = db.collection("restaurants").document(scopedRestaurantId).collection("orders")
                    .whereGreaterThanOrEqualTo("createdAt", startDate)
                    .whereLessThanOrEqualTo("createdAt", endDate);
        } else {
            // Admin/global mode.
            query = db.collection("orders").whereGreaterThanOrEqualTo("createdAt", startDate)
                    .whereLessThanOrEqualTo("createdAt", endDate);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();

                    // ── Tính toán từ danh sách orders ────────────────
                    long totalOrders     = 0;
                    long delivered       = 0;
                    long cancelled       = 0;
                    long totalRevenue    = 0;

                    // Payment method count
                    Map<String, Long> paymentCount = new HashMap<>();
                    paymentCount.put(Order.PAYMENT_COD, 0L);

                    // Revenue theo ngày — LinkedHashMap giữ thứ tự ngày
                    Map<String, Long> revenueByDay = buildEmptyDayMap(
                            startDate, dayCount);

                    // Thống kê theo restaurantId
                    Map<String, Long[]> restMap = new HashMap<>();
                    // Long[0]=totalOrders, Long[1]=totalRevenue

                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "dd/MM", Locale.getDefault());

                    for (DocumentSnapshot doc : docs) {
                        String status = doc.getString("status");
                        long amount = extractAmount(doc);
                        String method = doc.getString("paymentMethod");
                        String restId = hasRestaurantScope ? scopedRestaurantId : doc.getString("restaurantId");
                        Object createdAt = doc.get("createdAt");

                        Date orderDate = extractDate(createdAt);
                        if (orderDate == null || orderDate.before(startDate) || orderDate.after(endDate)) {
                            continue;
                        }

                        totalOrders++;

                        // Đếm theo status
                        if (Order.STATUS_COMPLETED.equals(Order.normalizeStatus(status))) {
                            delivered++;
                            totalRevenue += amount;

                            // Revenue theo ngày (chỉ tính đơn delivered)
                            String dayKey = sdf.format(orderDate);
                            if (revenueByDay.containsKey(dayKey)) {
                                revenueByDay.put(dayKey,
                                        revenueByDay.get(dayKey) + amount);
                            }

                            // Doanh thu theo nhà hàng
                            if (restId != null) {
                                Long[] stat = restMap.getOrDefault(restId, new Long[]{0L, 0L});
                                stat[0]++;
                                stat[1] += amount;
                                restMap.put(restId, stat);
                            }
                        }

                        if (Order.STATUS_CANCELLED.equals(Order.normalizeStatus(status))) cancelled++;

                        // Phương thức thanh toán
                        String normalizedMethod = Order.normalizePaymentMethod(method);
                        paymentCount.put(normalizedMethod, paymentCount.getOrDefault(normalizedMethod, 0L) + 1);
                    }

                    // ── Lấy thêm tên nhà hàng cho top list ───────────
                    final long finalDelivered    = delivered;
                    final long finalCancelled    = cancelled;
                    final long finalRevenue      = totalRevenue;
                    final long finalTotal        = totalOrders;

                    loadTopRestaurants(restMap, topList -> {
                        ReportStats stats = new ReportStats();
                        stats.totalOrders       = finalTotal;
                        stats.deliveredOrders   = finalDelivered;
                        stats.cancelledOrders   = finalCancelled;
                        stats.totalRevenue      = finalRevenue;
                        stats.paymentMethodCount = paymentCount;
                        stats.revenueByDay      = revenueByDay;
                        stats.topRestaurants    = topList;

                        liveData.postValue(stats);
                        loading.postValue(false);
                    });
                })
                .addOnFailureListener(e -> {
                    error.postValue("Lỗi tải báo cáo: " + e.getMessage());
                    loading.postValue(false);
                });
    }

    // ── Lấy tên nhà hàng cho top list ────────────────────────────
    private void loadTopRestaurants(
            Map<String, Long[]> restMap,
            OnTopLoaded callback) {

        if (restMap.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        List<ReportStats.RestaurantStat> result = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(restMap.size());

        for (Map.Entry<String, Long[]> entry : restMap.entrySet()) {
            String  restId = entry.getKey();
            Long[]  stat   = entry.getValue();

            db.collection("restaurants").document(restId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String name   = doc.getString("name");
                        Double rating = doc.getDouble("rating");
                        result.add(new ReportStats.RestaurantStat(
                                restId,
                                name != null ? name : "Không tên",
                                stat[0],
                                stat[1],
                                rating != null ? rating : 0.0
                        ));

                        // Sort theo totalOrders giảm dần khi tất cả xong
                        if (remaining.decrementAndGet() == 0) {
                            result.sort((a, b) ->
                                    Long.compare(b.totalOrders, a.totalOrders));
                            // Chỉ lấy top 5
                            List<ReportStats.RestaurantStat> top5 =
                                    result.subList(0, Math.min(5, result.size()));
                            callback.onLoaded(new ArrayList<>(top5));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            callback.onLoaded(result);
                        }
                    });
        }
    }

    interface OnTopLoaded {
        void onLoaded(List<ReportStats.RestaurantStat> list);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Date getStartDate(Period period) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        switch (period) {
            case WEEK:  cal.add(Calendar.DAY_OF_YEAR, -6); break;
            case MONTH: cal.add(Calendar.DAY_OF_YEAR, -29); break;
            default: break; // TODAY: không trừ
        }
        return cal.getTime();
    }

    private int getDayCount(Period period) {
        switch (period) {
            case WEEK:  return 7;
            case MONTH: return 30;
            default:    return 1;
        }
    }

    // Tạo map ngày rỗng để fill biểu đồ
    private Map<String, Long> buildEmptyDayMap(Date start, int dayCount) {
        Map<String, Long> map = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        for (int i = 0; i < dayCount; i++) {
            map.put(sdf.format(cal.getTime()), 0L);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return map;
    }

    private long extractAmount(DocumentSnapshot doc) {
        Object value = doc.get("totalAmount");
        if (value instanceof Number) {
            return Math.round(((Number) value).doubleValue());
        }
        return 0L;
    }

    private Date extractDate(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return null;
    }
}
