package com.example.foodorderapp.data.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Review {
    private String reviewId;
    private String orderId;
    private String customerId;
    private String restaurantId;
    private int rating;
    private String comment;
    private List<String> tags;
    private Timestamp createdAt;

    public Review() {}

    public Review(String orderId, String customerId, String restaurantId,
                  int rating, String comment, List<String> tags) {
        this.orderId      = orderId;
        this.customerId   = customerId;
        this.restaurantId = restaurantId;
        this.rating       = rating;
        this.comment      = comment;
        this.tags         = tags;
        this.createdAt    = Timestamp.now();
    }

    // Getters & Setters
    public String getReviewId()      { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getOrderId()       { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId()    { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getRestaurantId()  { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public int getRating()           { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment()       { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<String> getTags()    { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Timestamp getCreatedAt()  { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}