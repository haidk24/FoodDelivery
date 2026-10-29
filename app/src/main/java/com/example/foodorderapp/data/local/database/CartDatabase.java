package com.example.foodorderapp.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.foodorderapp.data.local.entity.CartFoodEntity;
import com.example.foodorderapp.data.local.dao.CartDao;

@Database(entities = {CartFoodEntity.class}, version = 4, exportSchema = false)
public abstract class CartDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "food.db";
    private static CartDatabase instance;

    public static synchronized CartDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), CartDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract CartDao cartDao();
}
