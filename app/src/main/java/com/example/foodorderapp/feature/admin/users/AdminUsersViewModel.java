package com.example.foodorderapp.feature.admin.users;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.data.repository.AdminUserRepository;

import java.util.ArrayList;
import java.util.List;

/** State and actions for admin-managed user accounts. */
public class AdminUsersViewModel extends ViewModel {
    private final AdminUserRepository repository = new AdminUserRepository();
    private final MutableLiveData<List<User>> users = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<User> selectedUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private boolean isLoadingUsers;

    public void loadUsers(String roleFilter) {
        if (isLoadingUsers) return;
        isLoadingUsers = true;
        loading.setValue(true);
        users.setValue(new ArrayList<>());
        repository.loadUsers(roleFilter, (value, failure) -> {
            users.postValue(value == null ? new ArrayList<>() : value);
            if (failure != null) postError("tải danh sách người dùng", failure);
            loading.postValue(false);
            isLoadingUsers = false;
        });
    }

    public void loadDetail(String uid) {
        loading.setValue(true);
        selectedUser.setValue(null);
        repository.getDetail(uid, (user, failure) -> {
            if (user != null) selectedUser.postValue(user);
            if (failure != null) postError("tải chi tiết user", failure);
            loading.postValue(false);
        });
    }

    public void ban(String uid) { setStatus(uid, "banned", "khóa tài khoản"); }
    public void unban(String uid) { setStatus(uid, "active", "mở khóa tài khoản"); }

    private void setStatus(String uid, String status, String action) {
        loading.setValue(true);
        repository.setStatus(uid, status, failure -> {
            if (failure == null) {
                actionSuccess.postValue(true);
                loadDetail(uid);
            } else {
                postError(action, failure);
                loading.postValue(false);
            }
        });
    }

    public void update(User user) {
        if (user == null || user.getUid() == null) return;
        loading.setValue(true);
        repository.update(user, failure -> {
            if (failure == null) {
                selectedUser.postValue(user);
                actionSuccess.postValue(true);
            } else postError("cập nhật thông tin", failure);
            loading.postValue(false);
        });
    }

    public void createProfile(User user, AdminUserRepository.Completion completion) {
        repository.createProfile(user, completion);
    }

    public void loadOrders(String uid, AdminUserRepository.OrdersCallback callback) {
        repository.getOrders(uid, callback);
    }

    private void postError(String action, Exception failure) {
        error.postValue("Lỗi " + action + ": " + failure.getMessage());
    }

    public LiveData<List<User>> getUsers() { return users; }
    public LiveData<User> getSelectedUser() { return selectedUser; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }
}
