package com.example.foodorderapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.foodorderapp.data.local.entity.CartFoodEntity;

@Database(entities = {CartFoodEntity.class}, version = 4, exportSchema = false)
public abstract class FoodDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "food.db";
    private static FoodDatabase instance;

    public static synchronized FoodDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), FoodDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract FoodDao foodDao();
}
