package com.example.foodorderapp.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FoodCategoryTest {
    @Test public void normalizesLegacyCategoriesToCanonicalValues() {
        assertEquals(FoodCategory.MAIN, FoodCategory.normalize("Cơm"));
        assertEquals(FoodCategory.APPETIZER, FoodCategory.normalize("Khai vi"));
        assertEquals(FoodCategory.DRINK, FoodCategory.normalize("Nước ép"));
        assertEquals(FoodCategory.DESSERT, FoodCategory.normalize("Dessert"));
    }

    @Test public void matchesAllAndCanonicalCategories() {
        assertTrue(FoodCategory.matches("Đồ uống", "Tất cả"));
        assertTrue(FoodCategory.matches("nuoc", FoodCategory.DRINK));
        assertFalse(FoodCategory.matches("Tráng miệng", FoodCategory.MAIN));
    }
}
