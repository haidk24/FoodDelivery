package com.example.foodorderapp.feature.customer.cart;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.repository.CartRepository;
import java.util.List;

public class CartViewModel extends AndroidViewModel {
    private final CartRepository repository;
    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository(application);
    }
    public List<Food> getItems() { return repository.getItems(); }
    public int getItemCount() {
        int count = 0;
        for (Food food : repository.getItems()) count += food.getCount();
        return count;
    }
    public void update(Food food) { repository.update(food); }
    public void addOrIncrement(Food food, int quantity) { repository.addOrIncrement(food, quantity); }
    public void delete(Food food) { repository.delete(food); }
    public void clear() { repository.clear(); }
}
