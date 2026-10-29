package com.example.foodorderapp.feature.restaurant.info;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.repository.RestaurantRepository;

public class RestaurantInfoViewModel extends ViewModel {
    private final RestaurantRepository repository = new RestaurantRepository();
    private final MutableLiveData<Restaurant> restaurant = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public void load(String restaurantId) {
        repository.get(restaurantId, (value, failure) -> {
            if (failure != null) error.postValue(failure.getMessage());
            else restaurant.postValue(value);
        });
    }

    public LiveData<Restaurant> getRestaurant() { return restaurant; }
    public LiveData<String> getError() { return error; }
}
