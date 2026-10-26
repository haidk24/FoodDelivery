package com.example.foodorderapp.feature.restaurant.owner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.repository.RestaurantOwnerRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RestaurantOwnerViewModel extends ViewModel {
    private final RestaurantOwnerRepository repository = new RestaurantOwnerRepository();
    private final MutableLiveData<String> restaurantId = new MutableLiveData<>("");
    private final MutableLiveData<Restaurant> restaurant = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration restaurantRegistration;
    private ListenerRegistration orderRegistration;

    public void resolveAndObserve(String ownerId, String knownRestaurantId) {
        if (knownRestaurantId != null && !knownRestaurantId.trim().isEmpty()) {
            observe(knownRestaurantId.trim());
            return;
        }
        repository.resolveRestaurantId(ownerId, (id, failure) -> {
            if (failure != null) { postError(failure); return; }
            observe(id);
        });
    }

    private void observe(String id) {
        if (id == null || id.trim().isEmpty()) return;
        if (id.equals(restaurantId.getValue()) && restaurantRegistration != null) return;
        stopListeners();
        restaurantId.postValue(id);
        restaurantRegistration = repository.observeRestaurant(id, new RestaurantOwnerRepository.RestaurantListener() {
            @Override public void onRestaurant(Restaurant value) { restaurant.postValue(value); }
            @Override public void onError(Exception failure) { postError(failure); }
        });
        orderRegistration = repository.observeOrders(id, new RestaurantOwnerRepository.OrdersListener() {
            @Override public void onOrders(List<Order> orders) {
                Collections.sort(orders, (left, right) -> compareDates(left.getCreatedAt(), right.getCreatedAt()));
                notifications.postValue(orders);
            }
            @Override public void onError(Exception failure) { postError(failure); }
        });
    }

    public void toggleStatus() {
        Restaurant value = restaurant.getValue();
        String id = restaurantId.getValue();
        if (value == null || id == null || id.isEmpty()) return;
        String next = "active".equals(value.getStatus()) ? "closed" : "active";
        repository.updateStatus(id, next, failure -> { if (failure != null) postError(failure); });
    }

    private int compareDates(Date first, Date second) {
        if (first != null && second != null) return second.compareTo(first);
        if (first != null) return -1;
        return second != null ? 1 : 0;
    }
    private void postError(Exception failure) { error.postValue(failure.getMessage()); }
    private void stopListeners() {
        if (restaurantRegistration != null) restaurantRegistration.remove();
        if (orderRegistration != null) orderRegistration.remove();
        restaurantRegistration = null;
        orderRegistration = null;
    }
    @Override protected void onCleared() { stopListeners(); }

    public LiveData<String> getRestaurantId() { return restaurantId; }
    public LiveData<Restaurant> getRestaurant() { return restaurant; }
    public LiveData<List<Order>> getNotifications() { return notifications; }
    public LiveData<String> getError() { return error; }
}
