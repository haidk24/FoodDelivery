package com.example.foodorderapp.feature.customer.fooddetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.repository.CartRepository;
import com.example.foodorderapp.data.repository.RestaurantRepository;

public class FoodDetailViewModel extends AndroidViewModel {
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final CartRepository cartRepository;
    private final MutableLiveData<Restaurant> restaurant = new MutableLiveData<>();
    private final MutableLiveData<String> restaurantError = new MutableLiveData<>();

    public FoodDetailViewModel(@NonNull Application application) {
        super(application);
        cartRepository = new CartRepository(application);
    }

    public void loadRestaurant(String restaurantId) {
        restaurantRepository.get(restaurantId, (value, error) -> {
            if (error != null) restaurantError.postValue(error.getMessage());
            else restaurant.postValue(value);
        });
    }

    public void addToCart(Food food, int quantity) {
        cartRepository.addOrIncrement(food, quantity);
    }

    public LiveData<Restaurant> getRestaurant() { return restaurant; }
    public LiveData<String> getRestaurantError() { return restaurantError; }
}
