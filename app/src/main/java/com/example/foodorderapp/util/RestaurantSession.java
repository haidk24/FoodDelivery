package com.example.foodorderapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class RestaurantSession {

    private static final String PREF_NAME = "restaurant_session_pref";
    private static final String KEY_UID = "session_uid";
    private static final String KEY_RESTAURANT_ID = "session_restaurant_id";
    private static final String KEY_RESTAURANT_NAME = "session_restaurant_name";
    private static final String KEY_RESTAURANT_STATUS = "session_restaurant_status";

    public static void saveRestaurant(Context context, String uid, String restaurantId, String restaurantName, String status) {
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_RESTAURANT_ID, restaurantId)
                .putString(KEY_RESTAURANT_NAME, restaurantName)
                .putString(KEY_RESTAURANT_STATUS, status)
                .apply();
    }

    public static String getRestaurantId(Context context, String currentUid) {
        if (context == null) return "";
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedUid = pref.getString(KEY_UID, "");
        if (currentUid != null && currentUid.equals(savedUid)) {
            return pref.getString(KEY_RESTAURANT_ID, "");
        }
        return "";
    }

    public static String getRestaurantName(Context context, String currentUid) {
        if (context == null) return "";
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedUid = pref.getString(KEY_UID, "");
        if (currentUid != null && currentUid.equals(savedUid)) {
            return pref.getString(KEY_RESTAURANT_NAME, "");
        }
        return "";
    }

    public static String getRestaurantStatus(Context context, String currentUid) {
        if (context == null) return "";
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedUid = pref.getString(KEY_UID, "");
        if (currentUid != null && currentUid.equals(savedUid)) {
            return pref.getString(KEY_RESTAURANT_STATUS, "active");
        }
        return "";
    }

    public static void clear(Context context) {
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }
}
