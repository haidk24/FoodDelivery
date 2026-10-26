package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.FoodCategory;
import com.example.foodorderapp.data.model.Restaurant;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Loads the customer home catalog from active restaurants and their food subcollections. */
public class HomeRepository {
    public interface Callback { void onResult(HomeData data, Exception error); }

    public static class HomeData {
        public final List<Restaurant> restaurants;
        public final List<Food> foods;

        public HomeData(List<Restaurant> restaurants, List<Food> foods) {
            this.restaurants = restaurants;
            this.foods = foods;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void load(Callback callback) {
        db.collection("restaurants").whereEqualTo("status", "active").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onResult(new HomeData(new ArrayList<>(), new ArrayList<>()), null);
                        return;
                    }
                    List<Restaurant> restaurants = new ArrayList<>();
                    List<Food> foods = new ArrayList<>();
                    AtomicInteger remaining = new AtomicInteger(snapshot.size());
                    AtomicReference<Exception> firstError = new AtomicReference<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Restaurant restaurant = document.toObject(Restaurant.class);
                        if (restaurant == null) restaurant = new Restaurant();
                        restaurant.setRestaurantId(document.getId());
                        Restaurant target = restaurant;
                        document.getReference().collection("foods").get()
                                .addOnSuccessListener(foodSnapshot -> {
                                    List<Food> restaurantFoods = new ArrayList<>();
                                    for (DocumentSnapshot foodDocument : foodSnapshot.getDocuments()) {
                                        Food food = foodDocument.toObject(Food.class);
                                        if (food == null || !food.isAvailable()) continue;
                                        food.setFoodId(foodDocument.getId());
                                        food.setRestaurantId(document.getId());
                                        food.setCategory(FoodCategory.normalize(food.getCategory()));
                                        restaurantFoods.add(food);
                                        foods.add(food);
                                    }
                                    if (!restaurantFoods.isEmpty()) {
                                        target.setFoods(restaurantFoods);
                                        restaurants.add(target);
                                    }
                                    completeOne(remaining, restaurants, foods, firstError.get(), callback);
                                })
                                .addOnFailureListener(error -> {
                                    firstError.compareAndSet(null, error);
                                    completeOne(remaining, restaurants, foods, firstError.get(), callback);
                                });
                    }
                })
                .addOnFailureListener(error -> callback.onResult(null, error));
    }

    private void completeOne(AtomicInteger remaining, List<Restaurant> restaurants,
                             List<Food> foods, Exception error, Callback callback) {
        if (remaining.decrementAndGet() != 0) return;
        Collections.sort(restaurants, (left, right) -> {
            int rating = Double.compare(right.getRating(), left.getRating());
            return rating != 0 ? rating : left.getName().compareToIgnoreCase(right.getName());
        });
        Collections.sort(foods, (left, right) -> {
            int discount = Integer.compare(right.getDiscountPercent(), left.getDiscountPercent());
            return discount != 0 ? discount : left.getName().compareToIgnoreCase(right.getName());
        });
        callback.onResult(new HomeData(restaurants, foods), error);
    }
}
