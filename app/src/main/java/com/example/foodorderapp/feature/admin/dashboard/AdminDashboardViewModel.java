package com.example.foodorderapp.feature.admin.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.AdminStats;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.repository.AdminDashboardRepository;
import com.example.foodorderapp.data.repository.AuthRepository;
import com.example.foodorderapp.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

/** State for the admin dashboard only. */
public class AdminDashboardViewModel extends ViewModel {
    private final AuthRepository authRepository = new AuthRepository();
    private final AdminDashboardRepository dashboardRepository = new AdminDashboardRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final MutableLiveData<AdminStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> recentOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> currentUserRole = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AdminDashboardViewModel() {
        loadCurrentUserRole();
        loadStats();
    }

    public void loadStats() {
        loading.setValue(true);
        dashboardRepository.loadStats((value, failure) -> {
            if (value != null) stats.postValue(value);
            if (failure != null) postError("tải thống kê dashboard", failure);
            loading.postValue(false);
        });
    }

    public void loadRecentOrders() {
        orderRepository.getRecentOrders((orders, failure) -> {
            recentOrders.postValue(orders == null ? new ArrayList<>() : orders);
            if (failure != null) postError("tải đơn hàng gần đây", failure);
        });
    }

    private void loadCurrentUserRole() {
        authRepository.getCurrentUserProfile()
                .addOnSuccessListener(user -> currentUserRole.setValue(user == null ? null : user.getRole()))
                .addOnFailureListener(failure -> postError("tải quyền quản trị", failure));
    }

    private void postError(String action, Exception failure) {
        error.postValue("Lỗi " + action + ": " + failure.getMessage());
    }

    public LiveData<AdminStats> getStats() { return stats; }
    public LiveData<List<Order>> getRecentOrders() { return recentOrders; }
    public LiveData<String> getCurrentUserRole() { return currentUserRole; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
