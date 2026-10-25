package com.example.foodorderapp.data.model;

import java.text.Normalizer;
import java.util.Locale;

/** Canonical categories stored in Firestore and shown in both customer and restaurant apps. */
public final class FoodCategory {
    public static final String MAIN = "Món chính";
    public static final String APPETIZER = "Khai vị";
    public static final String DRINK = "Đồ uống";
    public static final String DESSERT = "Tráng miệng";
    public static final String[] OPTIONS = {MAIN, APPETIZER, DRINK, DESSERT};

    private FoodCategory() {
    }

    public static String normalize(String category) {
        String key = key(category);
        if (key.isEmpty() || key.contains("chua phan loai")) return MAIN;
        if (key.startsWith("khai vi")) return APPETIZER;
        if (key.startsWith("mon nuoc")) return MAIN;
        if (key.contains("do uong") || key.contains("nuoc") || key.contains("drink")) return DRINK;
        if (key.startsWith("trang mieng") || key.contains("dessert")) return DESSERT;
        if (key.startsWith("mon chinh") || key.contains("dac san")
                || key.contains("com") || key.contains("bun") || key.contains("pho")
                || key.contains("mi")) return MAIN;
        return MAIN;
    }

    public static boolean matches(String foodCategory, String selectedCategory) {
        return selectedCategory == null || selectedCategory.trim().isEmpty()
                || "Tất cả".equalsIgnoreCase(selectedCategory)
                || normalize(foodCategory).equals(normalize(selectedCategory));
    }

    private static String key(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd');
        return normalized.trim().toLowerCase(Locale.ROOT);
    }
}
