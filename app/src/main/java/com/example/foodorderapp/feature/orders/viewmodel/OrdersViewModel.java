package com.example.foodorderapp.feature.orders.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.foodorderapp.data.dataSource.firebase.OrderFirebaseDataSource;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.data.repository.OrderRepository;
import com.example.foodorderapp.data.repository.ShipperRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Collections;
import java.util.List;

public class OrdersViewModel extends ViewModel {
    private final OrderRepository repository;
    private final ShipperRepository shipperRepository;
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<List<User>> availableShippers = new MutableLiveData<>(Collections.emptyList());
    private ListenerRegistration registration;
    private String observedKey = "";

    public OrdersViewModel() { this(new OrderRepository(), new ShipperRepository()); }
    public OrdersViewModel(OrderRepository repository) { this(repository, new ShipperRepository()); }
    public OrdersViewModel(OrderRepository repository, ShipperRepository shipperRepository) {
        this.repository = repository;
        this.shipperRepository = shipperRepository;
    }
    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<User>> getAvailableShippers() { return availableShippers; }

    public void observeRestaurant(String restaurantId) {
        observe("restaurant:" + restaurantId, listener -> repository.observeRestaurantOrders(restaurantId, listener));
    }
    public void observeCustomer(String customerId) {
        observe("customer:" + customerId, listener -> repository.observeCustomerOrders(customerId, listener));
    }
    public void observeOrder(String orderId) {
        observe("order:" + orderId, listener -> repository.observeOrder(orderId, listener));
    }
    private void observe(String key, RegistrationFactory factory) {
        if (key.equals(observedKey)) return;
        clearRegistration();
        observedKey = key;
        loading.setValue(true);
        error.setValue(null);
        registration = factory.register(new OrderFirebaseDataSource.OrdersListener() {
            @Override public void onOrders(List<Order> value) { orders.postValue(value); error.postValue(null); loading.postValue(false); }
            @Override public void onError(Exception cause) { orders.postValue(Collections.emptyList()); error.postValue(cause.getMessage()); loading.postValue(false); }
        });
    }
    public void updateStatus(Order order, String status, OrderFirebaseDataSource.Completion completion) {
        repository.updateStatus(order, status, completion);
    }
    public void createOrder(Order order, OrderFirebaseDataSource.Completion completion) {
        repository.createOrder(order, completion);
    }
    public void loadAvailableShippers() {
        shipperRepository.loadActiveShippers((shippers, failure) -> {
            availableShippers.postValue(shippers == null ? Collections.emptyList() : shippers);
        });
    }
    public void startDelivery(Order order, User shipper, OrderFirebaseDataSource.Completion completion) {
        repository.startDelivery(order, shipper, completion);
    }
    private void clearRegistration() { if (registration != null) { registration.remove(); registration = null; } }
    @Override protected void onCleared() { clearRegistration(); }
    private interface RegistrationFactory { ListenerRegistration register(OrderFirebaseDataSource.OrdersListener listener); }
}
