package com.example.foodorderapp.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OrderNormalizationTest {
    @Test public void normalizesLegacyOrderStatuses() {
        assertEquals(Order.STATUS_PENDING, Order.normalizeStatus("Chờ xác nhận"));
        assertEquals(Order.STATUS_PREPARING, Order.normalizeStatus("Đang chuẩn bị"));
        assertEquals(Order.STATUS_SHIPPING, Order.normalizeStatus("Đang giao hàng"));
        assertEquals(Order.STATUS_COMPLETED, Order.normalizeStatus("Đã giao thành công"));
        assertEquals(Order.STATUS_CANCELLED, Order.normalizeStatus("Đã hủy"));
    }

    @Test public void normalizesCashPaymentToCod() {
        assertEquals(Order.PAYMENT_COD, Order.normalizePaymentMethod("Tiền mặt"));
        assertEquals(Order.PAYMENT_COD, Order.normalizePaymentMethod("cash"));
        assertEquals("MOMO", Order.normalizePaymentMethod("MOMO"));
    }
}
