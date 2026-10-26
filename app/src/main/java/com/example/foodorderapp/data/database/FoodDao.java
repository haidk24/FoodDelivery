package com.example.foodorderapp.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.foodorderapp.data.local.entity.CartFoodEntity;

import java.util.List;

@Dao
public interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CartFoodEntity food);

    @Query("SELECT * FROM cart_items")
    List<CartFoodEntity> getAll();

    @Query("SELECT * FROM cart_items WHERE cartKey=:cartKey LIMIT 1")
    CartFoodEntity findByKey(String cartKey);

    @Update
    void update(CartFoodEntity food);

    @Query("DELETE FROM cart_items WHERE cartKey=:cartKey")
    void deleteByKey(String cartKey);

    @Query("DELETE FROM cart_items")
    void clear();
}
