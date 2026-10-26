package com.example.foodorderapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Room-only snapshot of a food selected for the local cart. */
@Entity(tableName = "cart_items")
public class CartFoodEntity {
    @PrimaryKey @NonNull
    public String cartKey = "";
    public String foodId = "";
    public String restaurantId = "";
    public String name = "";
    public String description = "";
    public String imageUrl = "";
    public double price;
    public double oldPrice;
    public double newPrice;
    public int discountPercent;
    public int quantity;
}
