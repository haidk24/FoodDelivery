package com.example.foodorderapp.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Order {
    private String orderId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String restaurantId;
    private String shipperId;
    private List<CartItem> items; // Kiểu array[map]
    private double totalAmount;
    private String status;
    private String note;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PREPARING = "preparing";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_SHIPPING = "shipping";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    public static String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return STATUS_PENDING;
        }
        String s = status.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.contains("hủy") || s.contains("huy") || s.contains("cancel")) {
            return STATUS_CANCELLED;
        }
        if (s.contains("hoàn") || s.contains("hoan") || s.contains("deliver") || s.contains("complet") || s.contains("thành công") || s.contains("thanh cong")) {
            return STATUS_COMPLETED;
        }
        if (s.contains("giao") || s.contains("ship")) {
            return STATUS_SHIPPING;
        }
        if (s.contains("chuẩn bị") || s.contains("chuan bi") || s.contains("nấu") || s.contains("nau") || s.contains("prepar") || s.contains("confirm")) {
            return STATUS_PREPARING;
        }
        if (s.contains("chờ") || s.contains("cho") || s.contains("pend")) {
            return STATUS_PENDING;
        }
        return s;
    }

    public String getNormalizedStatus() {
        return normalizeStatus(this.status);
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getShipperId() {
        return shipperId;
    }

    public void setShipperId(String shipperId) {
        this.shipperId = shipperId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress instanceof Address ? (Address) deliveryAddress : null;
    }

    public void setDeliveryAddress(Object deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    private String paymentMethod;
    private boolean reviewed;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    private Object deliveryAddress;

    @ServerTimestamp
    private Date createdAt;
    private Date confirmedAt;
    private Date shippingAt;
    private Date deliveredAt;

    public Order() {}

    public Date getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Date confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Date getShippingAt() {
        return shippingAt;
    }

    public void setShippingAt(Date shippingAt) {
        this.shippingAt = shippingAt;
    }

    public Date getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Date deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
