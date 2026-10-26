package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.dataSource.firebase.FoodFirebaseDataSource;
import com.example.foodorderapp.data.model.Food;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
import java.util.Map;

public class FoodRepository {
    private final FoodFirebaseDataSource firebase = new FoodFirebaseDataSource();
    public ListenerRegistration observeRestaurantFoods(String id, FoodFirebaseDataSource.FoodsListener listener) {
        return firebase.observeRestaurantFoods(id, listener);
    }
    public void setAvailable(String restaurantId, String foodId, boolean value, FoodFirebaseDataSource.Completion completion) {
        firebase.setAvailable(restaurantId, foodId, value, completion);
    }
    public void delete(String restaurantId, String foodId, FoodFirebaseDataSource.Completion completion) {
        firebase.delete(restaurantId, foodId, completion);
    }
    public void resolveRestaurantId(String userId, FoodFirebaseDataSource.RestaurantIdCallback callback) { firebase.resolveRestaurantId(userId, callback); }
    public void create(String restaurantId, Food food, FoodFirebaseDataSource.Completion completion) { firebase.create(restaurantId, food, completion); }
    public void update(String restaurantId, String foodId, Map<String, Object> updates, FoodFirebaseDataSource.Completion completion) { firebase.update(restaurantId, foodId, updates, completion); }
    public void getRestaurantName(String restaurantId, FoodFirebaseDataSource.RestaurantNameCallback callback) { firebase.getRestaurantName(restaurantId, callback); }
    public void getRestaurant(String restaurantId, FoodFirebaseDataSource.RestaurantCallback callback) { firebase.getRestaurant(restaurantId, callback); }
}
