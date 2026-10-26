package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.dataSource.firebase.OrderFirebaseDataSource;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.User;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class OrderRepository {
    private final OrderFirebaseDataSource firebase;
    public OrderRepository() { this(new OrderFirebaseDataSource()); }
    public OrderRepository(OrderFirebaseDataSource firebase) { this.firebase = firebase; }
    public ListenerRegistration observeRestaurantOrders(String id, OrderFirebaseDataSource.OrdersListener listener) {
        return firebase.observeRestaurantOrders(id, listener);
    }
    public ListenerRegistration observeCustomerOrders(String id, OrderFirebaseDataSource.OrdersListener listener) {
        return firebase.observeCustomerOrders(id, listener);
    }
    public ListenerRegistration observeOrder(String id, OrderFirebaseDataSource.OrdersListener listener) {
        return firebase.observeOrder(id, listener);
    }
    public void createOrder(Order order, OrderFirebaseDataSource.Completion completion) {
        firebase.createOrder(order, completion);
    }
    public void updateStatus(Order order, String status, OrderFirebaseDataSource.Completion completion) {
        firebase.updateStatus(order, status, completion);
    }
    public void startDelivery(Order order, User shipper, OrderFirebaseDataSource.Completion completion) {
        firebase.startDelivery(order, shipper, completion);
    }
    public void getRecentOrders(OrderFirebaseDataSource.RecentOrdersCallback callback) {
        firebase.getRecentOrders(callback);
    }
}
