package com.example.foodorderapp.feature.admin.restaurants;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.repository.AdminRestaurantRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Realtime restaurant approval and detail state for admin screens. */
public class AdminRestaurantsViewModel extends ViewModel {
    private final AdminRestaurantRepository repository = new AdminRestaurantRepository();
    private final MutableLiveData<List<Restaurant>> pending = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> active = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> suspended = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> pendingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Restaurant> selected = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration registration;

    public void observe() {
        if (registration != null) return;
        registration = repository.observe(new AdminRestaurantRepository.RestaurantsListener() {
            @Override public void onError(Exception failure) { postError("theo dõi nhà hàng", failure); }
            @Override public void onRestaurants(List<Restaurant> restaurants) { partition(restaurants); }
        });
    }

    private void partition(List<Restaurant> restaurants) {
        List<Restaurant> pendingList = new ArrayList<>();
        List<Restaurant> activeList = new ArrayList<>();
        List<Restaurant> suspendedList = new ArrayList<>();
        if (restaurants != null) for (Restaurant restaurant : restaurants) {
            if ("pending".equals(restaurant.status)) pendingList.add(restaurant);
            else if ("active".equals(restaurant.status)) activeList.add(restaurant);
            else if ("suspended".equals(restaurant.status)) suspendedList.add(restaurant);
        }
        Comparator<Restaurant> newestFirst = (left, right) ->
                Long.compare(createdAtMillis(right.createdAt), createdAtMillis(left.createdAt));
        Collections.sort(pendingList, newestFirst);
        Collections.sort(activeList, newestFirst);
        Collections.sort(suspendedList, newestFirst);
        pending.postValue(pendingList);
        active.postValue(activeList);
        suspended.postValue(suspendedList);
        pendingCount.postValue(pendingList.size());
        loaded.postValue(true);
    }

    public void approve(String restaurantId) { perform("duyệt nhà hàng", callback -> repository.approve(restaurantId, callback)); }
    public void suspend(String restaurantId) { perform("tạm ngưng nhà hàng", callback -> repository.setStatus(restaurantId, "suspended", callback)); }
    public void reject(String restaurantId) { suspend(restaurantId); }
    public void reinstate(String restaurantId) { perform("phục hồi nhà hàng", callback -> repository.setStatus(restaurantId, "active", callback)); }

    private interface RepositoryAction { void run(AdminRestaurantRepository.Completion completion); }
    private void perform(String action, RepositoryAction work) {
        loading.setValue(true);
        work.run(failure -> {
            if (failure == null) actionSuccess.postValue(true); else postError(action, failure);
            loading.postValue(false);
        });
    }

    public void loadDetail(String restaurantId) {
        loading.setValue(true);
        selected.setValue(null);
        repository.get(restaurantId, (restaurant, failure) -> {
            if (restaurant != null) selected.postValue(restaurant);
            if (failure != null) postError("tải chi tiết nhà hàng", failure);
            loading.postValue(false);
        });
    }

    private long createdAtMillis(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Date) return ((Date) value).getTime();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
    private void postError(String action, Exception failure) { error.postValue("Lỗi " + action + ": " + failure.getMessage()); }

    public LiveData<List<Restaurant>> getPending() { return pending; }
    public LiveData<List<Restaurant>> getActive() { return active; }
    public LiveData<List<Restaurant>> getSuspended() { return suspended; }
    public LiveData<Integer> getPendingCount() { return pendingCount; }
    public LiveData<Restaurant> getSelected() { return selected; }
    public LiveData<Boolean> getLoaded() { return loaded; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getError() { return error; }

    @Override protected void onCleared() {
        if (registration != null) registration.remove();
    }
}
