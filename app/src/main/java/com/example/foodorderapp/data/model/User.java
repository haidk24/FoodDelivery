package com.example.foodorderapp.data.model;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String phone ;
    private String role;       // customer | restaurant | shipper | admin
    private String status;     // active | banned
    private int orderCount;
    private Object createdAt;

    public User() {}

    public User(String uid, String displayName, String email, String phone, String role, String status, int orderCount) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.orderCount = orderCount;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    private long totalSpent;
    private String restaurantId;
    private String restaurantName;
    private int dishCount;

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    private String banReason;
    private String bannedAt;
    private String bannedBy;
    private boolean phoneVerified = true;
    private String lastOrderTime;
    private String joinTimeText;

    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = totalSpent; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public int getDishCount() { return dishCount; }
    public void setDishCount(int dishCount) { this.dishCount = dishCount; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public String getBannedAt() { return bannedAt; }
    public void setBannedAt(String bannedAt) { this.bannedAt = bannedAt; }

    public String getBannedBy() { return bannedBy; }
    public void setBannedBy(String bannedBy) { this.bannedBy = bannedBy; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getLastOrderTime() { return lastOrderTime; }
    public void setLastOrderTime(String lastOrderTime) { this.lastOrderTime = lastOrderTime; }

    public String getJoinTimeText() { return joinTimeText; }
    public void setJoinTimeText(String joinTimeText) { this.joinTimeText = joinTimeText; }

    public boolean isBanned() {
        return "banned".equals(status);
    }
}
