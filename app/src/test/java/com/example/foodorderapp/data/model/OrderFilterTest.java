package com.example.foodorderapp.data.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class OrderFilterTest {
    @Test public void filtersByAccentInsensitiveCustomerNameAndStatus() {
        Order order = order("ORD-12", "Nguyễn Văn An", "shipping", "COD", new Date());
        assertTrue(OrderFilter.matches(order, "nguyen van", "Đang giao hàng", 0, "COD", System.currentTimeMillis()));
        assertFalse(OrderFilter.matches(order, "Bình", "Đang giao hàng", 0, "COD", System.currentTimeMillis()));
    }

    @Test public void filtersByPaymentAndDateWindow() {
        long now = System.currentTimeMillis();
        Order momoToday = order("ORD-3", "An", "completed", "MOMO", new Date(now));
        Calendar old = Calendar.getInstance();
        old.setTimeInMillis(now);
        old.add(Calendar.DAY_OF_YEAR, -8);
        Order oldCod = order("ORD-4", "An", "completed", "COD", old.getTime());

        assertTrue(OrderFilter.matches(momoToday, "", "Tất cả", 7, "Khác COD", now));
        assertFalse(OrderFilter.matches(momoToday, "", "Tất cả", 7, "COD", now));
        assertFalse(OrderFilter.matches(oldCod, "", "Tất cả", 7, "COD", now));
    }

    private Order order(String id, String customer, String status, String payment, Date createdAt) {
        Order order = new Order();
        order.setOrderId(id);
        order.setCustomerName(customer);
        order.setStatus(status);
        order.setPaymentMethod(payment);
        order.setCreatedAt(createdAt);
        return order;
    }
}
