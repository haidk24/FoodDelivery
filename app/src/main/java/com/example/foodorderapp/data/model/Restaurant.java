package com.example.foodorderapp.data.model;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Restaurant {
    // Public fields kept for compatibility with merged admin code.
    public String restaurantId;
    public String ownerId;
    public String name;
    public String phone;
    public String imageUrl;
    public Object address;
    public GeoPoint location;
    public String status;
    public double rating;
    public boolean hasLicence;
    public Object createdAt;
    public List<Food> foods = new ArrayList<>();

    public Restaurant() {
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return cleanText(name);
    }

    public void setName(String name) {
        this.name = cleanText(name);
    }

    public String getPhone() {
        return cleanText(phone);
    }

    public void setPhone(String phone) {
        this.phone = cleanText(phone);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Address getAddress() {
        return address instanceof Address ? (Address) address : null;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getId() {
        return restaurantId;
    }

    public void setId(String id) {
        this.restaurantId = id;
    }

    public List<Food> getFoods() {
        return foods == null ? new ArrayList<>() : foods;
    }

    public void setFoods(List<Food> foods) {
        this.foods = foods == null ? new ArrayList<>() : foods;
    }

    @Exclude
    public String getAddressText() {
        if (address == null) {
            return "Chua cap nhat";
        }

        if (address instanceof Address) {
            Address a = (Address) address;
            return joinAddressParts(a.getStreet(), a.getWard(), a.getDistrict(), a.getCity());
        }

        if (address instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) address;
            return joinAddressParts(
                    toText(map.get("street")),
                    toText(map.get("ward")),
                    toText(map.get("district")),
                    toText(map.get("city"))
            );
        }

        return cleanText(String.valueOf(address));
    }

    @Exclude
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase();
        }
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
    }

    @Exclude
    public String getStatusText() {
        if (status == null) {
            return "Khong xac dinh";
        }
        switch (status) {
            case "pending":
                return "Cho duyet";
            case "active":
                return "Dang hoat dong";
            case "suspended":
                return "Tam ngung";
            default:
                return status;
        }
    }

    private String joinAddressParts(String street, String ward, String district, String city) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, street);
        appendPart(builder, ward);
        appendPart(builder, district);
        appendPart(builder, city);
        return builder.length() == 0 ? "Chua cap nhat" : builder.toString();
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }
}
