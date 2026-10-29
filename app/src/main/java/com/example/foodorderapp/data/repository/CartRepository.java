package com.example.foodorderapp.data.repository;

import android.content.Context;

import com.example.foodorderapp.data.local.database.CartDatabase;
import com.example.foodorderapp.data.local.entity.CartFoodEntity;
import com.example.foodorderapp.data.model.Food;

import java.util.ArrayList;
import java.util.List;

/** Local cart boundary. Room remains an implementation detail of the repository. */
public class CartRepository {
    private final CartDatabase database;

    public CartRepository(Context context) {
        database = CartDatabase.getInstance(context.getApplicationContext());
    }

    public List<Food> getItems() {
        List<CartFoodEntity> items = database.cartDao().getAll();
        List<Food> result = new ArrayList<>();
        if (items != null) for (CartFoodEntity item : items) result.add(toFood(item));
        return result;
    }

    public void update(Food food) {
        if (food == null) return;
        database.cartDao().update(toEntity(food));
    }
    public void addOrIncrement(Food food, int quantity) {
        if (food == null || food.getFoodId() == null || quantity <= 0) return;
        String key = cartKey(food);
        CartFoodEntity existing = database.cartDao().findByKey(key);
        if (existing != null) {
            existing.quantity += quantity;
            database.cartDao().update(existing);
            return;
        }
        CartFoodEntity entity = toEntity(food);
        entity.quantity = quantity;
        database.cartDao().insert(entity);
    }
    public void delete(Food food) { if (food != null) database.cartDao().deleteByKey(cartKey(food)); }
    public void clear() { database.cartDao().clear(); }

    private CartFoodEntity toEntity(Food food) {
        CartFoodEntity entity = new CartFoodEntity();
        entity.cartKey = cartKey(food);
        entity.foodId = safe(food.getFoodId());
        entity.restaurantId = safe(food.getRestaurantId());
        entity.name = safe(food.getName());
        entity.description = safe(food.getDescription());
        entity.imageUrl = safe(food.getImageUrl());
        entity.price = food.getPrice();
        entity.oldPrice = food.getOldPrice();
        entity.newPrice = food.getNewPrice();
        entity.discountPercent = food.getDiscountPercent();
        entity.quantity = food.getCount();
        return entity;
    }

    private Food toFood(CartFoodEntity entity) {
        Food food = new Food();
        food.setFoodId(entity.foodId);
        food.setRestaurantId(entity.restaurantId);
        food.setName(entity.name);
        food.setDescription(entity.description);
        food.setImageUrl(entity.imageUrl);
        food.setPrice(entity.price);
        food.setOldPrice(entity.oldPrice);
        food.setNewPrice(entity.newPrice);
        food.setDiscountPercent(entity.discountPercent);
        food.setCount(entity.quantity);
        return food;
    }

    private String cartKey(Food food) { return safe(food.getRestaurantId()) + "::" + safe(food.getFoodId()); }
    private String safe(String value) { return value == null ? "" : value; }
}
