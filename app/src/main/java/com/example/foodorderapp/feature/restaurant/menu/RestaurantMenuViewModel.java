package com.example.foodorderapp.feature.restaurant.menu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.foodorderapp.data.remote.firebase.FoodFirebaseDataSource;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.repository.FoodRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestaurantMenuViewModel extends ViewModel {
    private final FoodRepository repository = new FoodRepository();
    private final MutableLiveData<List<Food>> foods = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private ListenerRegistration registration;
    private String restaurantId = "";
    public LiveData<List<Food>> getFoods() { return foods; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getLoading() { return loading; }
    public void observeRestaurant(String id) {
        if (id == null || id.trim().isEmpty() || id.equals(restaurantId)) return;
        clear(); restaurantId = id;
        loading.setValue(true);
        error.setValue(null);
        registration = repository.observeRestaurantFoods(id, new FoodFirebaseDataSource.FoodsListener() {
            @Override public void onFoods(List<Food> value) { foods.postValue(value); error.postValue(null); loading.postValue(false); }
            @Override public void onError(Exception cause) { foods.postValue(new ArrayList<>()); error.postValue(cause.getMessage()); loading.postValue(false); }
        });
    }
    public void setAvailable(String foodId, boolean value, FoodFirebaseDataSource.Completion completion) {
        repository.setAvailable(restaurantId, foodId, value, completion);
    }
    public void delete(String foodId, FoodFirebaseDataSource.Completion completion) {
        repository.delete(restaurantId, foodId, completion);
    }
    public void delete(String restaurantId, String foodId, FoodFirebaseDataSource.Completion completion) {
        repository.delete(restaurantId, foodId, completion);
    }
    public void resolveRestaurantId(String userId, FoodFirebaseDataSource.RestaurantIdCallback callback) { repository.resolveRestaurantId(userId, callback); }
    public void create(String restaurantId, Food food, FoodFirebaseDataSource.Completion completion) { repository.create(restaurantId, food, completion); }
    public void update(String restaurantId, String foodId, Map<String, Object> updates, FoodFirebaseDataSource.Completion completion) { repository.update(restaurantId, foodId, updates, completion); }
    public void getRestaurantName(String restaurantId, FoodFirebaseDataSource.RestaurantNameCallback callback) { repository.getRestaurantName(restaurantId, callback); }
    public void getRestaurant(String restaurantId, FoodFirebaseDataSource.RestaurantCallback callback) { repository.getRestaurant(restaurantId, callback); }
    private void clear() { if (registration != null) { registration.remove(); registration = null; } }
    @Override protected void onCleared() { clear(); }
}
