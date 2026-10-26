package com.example.foodorderapp.data.model;

import java.io.Serializable;

public class Food implements Serializable {
    private String foodId = "";
    private String name;
    private String description;
    private double price;
    private double oldPrice;
    private double newPrice;
    private int discountPercent;
    private String imageUrl;
    private String category;
    private String restaurantId;
    @com.google.firebase.firestore.PropertyName("isAvailable")
    private boolean isAvailable;
    private int orderCount;
    private int count;

    // 1. BẮT BUỘC: Hàm khởi tạo rỗng cho Firestore
    public Food() {}

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId == null ? "" : foodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return getNewPrice();
    }

    public void setPrice(double price) {
        this.price = price;
        if (newPrice <= 0) {
            this.newPrice = price;
        }
        if (oldPrice <= 0) {
            this.oldPrice = price;
        }
    }

    public double getOldPrice() {
        if (oldPrice > 0) {
            return oldPrice;
        }
        return getNewPrice();
    }

    public void setOldPrice(double oldPrice) {
        this.oldPrice = oldPrice;
    }

    public double getNewPrice() {
        if (newPrice > 0) {
            return newPrice;
        }
        if (price > 0) {
            return price;
        }
        return oldPrice;
    }

    public void setNewPrice(double newPrice) {
        this.newPrice = newPrice;
        this.price = newPrice;
    }

    public int getDiscountPercent() {
        if (discountPercent > 0) {
            return discountPercent;
        }

        double currentOldPrice = getOldPrice();
        double currentNewPrice = getNewPrice();
        if (currentOldPrice > currentNewPrice && currentOldPrice > 0) {
            return (int) Math.round(((currentOldPrice - currentNewPrice) / currentOldPrice) * 100);
        }
        return 0;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = Math.max(discountPercent, 0);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return FoodCategory.normalize(category);
    }

    public void setCategory(String category) {
        this.category = FoodCategory.normalize(category);
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }


    @com.google.firebase.firestore.PropertyName("isAvailable")
    public boolean isAvailable() {
        return isAvailable;
    }

    @com.google.firebase.firestore.PropertyName("isAvailable")
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(count, 0);
    }

    public Food(String foodId, String name, String description, double price, String imageUrl, String category, boolean isAvailable, int orderCount) {
        this(foodId, name, description, price, imageUrl, category, isAvailable, orderCount, price, price, 0, null);
    }

    public Food(String foodId, String name, String description, double price, String imageUrl,
                String category, boolean isAvailable, int orderCount,
                double oldPrice, double newPrice, int discountPercent) {
        this(foodId, name, description, price, imageUrl, category, isAvailable, orderCount, oldPrice, newPrice, discountPercent, null);
    }

    public Food(String foodId, String name, String description, double price, String imageUrl,
                String category, boolean isAvailable, int orderCount,
                double oldPrice, double newPrice, int discountPercent, String restaurantId) {
        this.foodId = foodId == null ? "" : foodId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.discountPercent = Math.max(discountPercent, 0);
        this.imageUrl = imageUrl;
        this.category = FoodCategory.normalize(category);
        this.restaurantId = restaurantId;
        this.isAvailable = isAvailable;
        this.orderCount = orderCount;
        this.count = 0;
    }

    // TODO: Đặt chuột ở đây, bấm Alt + Insert -> Getter and Setter -> Bấm Ctrl + A -> Enter nhé!
}
