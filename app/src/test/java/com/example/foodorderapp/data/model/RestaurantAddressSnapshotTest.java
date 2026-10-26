package com.example.foodorderapp.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RestaurantAddressSnapshotTest {
    @Test public void snapshotAddressNeverUsesPlaceholder() {
        Restaurant restaurant = new Restaurant();
        assertEquals("", restaurant.getAddressSnapshotText());
        assertEquals("Chua cap nhat", restaurant.getAddressText());
    }

    @Test public void buildsSnapshotFromFirestoreAddressMap() {
        Restaurant restaurant = new Restaurant();
        Map<String, Object> address = new HashMap<>();
        address.put("street", "1 Nguyễn Huệ");
        address.put("ward", "Bến Nghé");
        address.put("district", "Quận 1");
        address.put("city", "Hồ Chí Minh");
        restaurant.address = address;
        assertEquals("1 Nguyễn Huệ, Bến Nghé, Quận 1, Hồ Chí Minh", restaurant.getAddressSnapshotText());
    }
}
