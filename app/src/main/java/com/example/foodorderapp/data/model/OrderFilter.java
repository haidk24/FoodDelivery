package com.example.foodorderapp.data.model;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Pure filter logic shared by restaurant order UI and unit tests. */
public final class OrderFilter {
    private OrderFilter() { }

    public static boolean matches(Order order, String query, String statusFilter, int daysFilter,
                                  String paymentFilter, long nowMillis) {
        return order != null
                && matchesText(order, query)
                && matchesStatus(order, statusFilter)
                && matchesDate(order.getCreatedAt(), daysFilter, nowMillis)
                && matchesPayment(order, paymentFilter);
    }

    public static boolean matchesSummaryScope(Order order, String query, int daysFilter,
                                              String paymentFilter, long nowMillis) {
        return order != null && matchesText(order, query)
                && matchesDate(order.getCreatedAt(), daysFilter, nowMillis)
                && matchesPayment(order, paymentFilter);
    }

    public static boolean matchesDate(Date createdAt, int daysFilter, long nowMillis) {
        if (daysFilter <= 0) return true;
        if (createdAt == null) return false;
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(nowMillis);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        end.add(Calendar.DAY_OF_YEAR, 1);
        Calendar start = (Calendar) end.clone();
        start.add(Calendar.DAY_OF_YEAR, -daysFilter);
        long value = createdAt.getTime();
        return value >= start.getTimeInMillis() && value < end.getTimeInMillis();
    }

    private static boolean matchesText(Order order, String query) {
        String rawQuery = clean(query).toLowerCase(Locale.ROOT);
        if (rawQuery.isEmpty()) return true;
        String normalizedQuery = stripAccents(rawQuery);
        String id = clean(order.getOrderId()).toLowerCase(Locale.ROOT);
        String name = clean(order.getCustomerName()).toLowerCase(Locale.ROOT);
        return id.contains(rawQuery) || name.contains(rawQuery)
                || stripAccents(name).contains(normalizedQuery);
    }

    private static boolean matchesStatus(Order order, String statusFilter) {
        String filter = clean(statusFilter);
        if (filter.isEmpty() || "Tất cả".equalsIgnoreCase(filter)) return true;
        String status = Order.normalizeStatus(order.getStatus());
        if ("Chờ xác nhận".equalsIgnoreCase(filter)) return Order.STATUS_PENDING.equals(status);
        if ("Đang chuẩn bị".equalsIgnoreCase(filter)) return Order.STATUS_PREPARING.equals(status);
        if ("Đang giao hàng".equalsIgnoreCase(filter)) return Order.STATUS_SHIPPING.equals(status);
        if ("Đã hoàn thành".equalsIgnoreCase(filter)) return Order.STATUS_COMPLETED.equals(status);
        return false;
    }

    private static boolean matchesPayment(Order order, String paymentFilter) {
        String filter = clean(paymentFilter);
        if (filter.isEmpty() || "Tất cả".equalsIgnoreCase(filter)) return true;
        if ("COD".equalsIgnoreCase(filter)) return order.isCodPayment();
        if ("Khác COD".equalsIgnoreCase(filter)) return !order.isCodPayment();
        return false;
    }

    private static String stripAccents(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
