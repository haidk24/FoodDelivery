package com.example.foodorderapp.data.model;


import java.util.List;
import java.util.Map;

public class ReportStats {

    // Tổng quan
    public long totalOrders;
    public long deliveredOrders;
    public long cancelledOrders;
    public long totalRevenue;

    // Tỉ lệ thanh toán (key = "COD"|"MoMo"|"VNPay", value = count)
    public Map<String, Long> paymentMethodCount;

    // Doanh thu theo ngày (key = "dd/MM", value = revenue)
    public Map<String, Long> revenueByDay;

    // Top nhà hàng (theo tổng đơn)
    public List<RestaurantStat> topRestaurants;

    public ReportStats() {}

    // ── Inner class ───────────────────────────────────────────────
    public static class RestaurantStat {
        public String restaurantId;
        public String name;
        public long   totalOrders;
        public long   totalRevenue;
        public double rating;

        public RestaurantStat() {}

        public RestaurantStat(String restaurantId, String name,
                              long totalOrders, long totalRevenue,
                              double rating) {
            this.restaurantId = restaurantId;
            this.name         = name;
            this.totalOrders  = totalOrders;
            this.totalRevenue = totalRevenue;
            this.rating       = rating;
        }
    }
}