package com.example.foodorderapp.feature.restaurant.registration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.repository.RestaurantRegistrationRepository;

import java.util.Map;

public class RestaurantRegistrationViewModel extends ViewModel {
    private final RestaurantRegistrationRepository repository = new RestaurantRegistrationRepository();
    private final MutableLiveData<Restaurant> existingRestaurant = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> submitted = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public void loadExisting(String userId) {
        loading.setValue(true);
        repository.findByOwner(userId, (restaurant, failure) -> {
            existingRestaurant.postValue(restaurant);
            if (failure != null) error.postValue(failure.getMessage());
            loading.postValue(false);
        });
    }

    public void submit(String ownerId, String name, String phone, Map<String, Object> address, boolean hasLicence) {
        loading.setValue(true);
        repository.submit(ownerId, name, phone, address, hasLicence, failure -> {
            if (failure == null) submitted.postValue(true);
            else error.postValue(failure.getMessage());
            loading.postValue(false);
        });
    }

    public LiveData<Restaurant> getExistingRestaurant() { return existingRestaurant; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSubmitted() { return submitted; }
    public LiveData<String> getError() { return error; }
}
