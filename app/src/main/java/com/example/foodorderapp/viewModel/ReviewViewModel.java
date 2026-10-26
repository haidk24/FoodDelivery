package com.example.foodorderapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.Review;
import com.example.foodorderapp.data.repository.ReviewRepository;

import java.util.List;

public class ReviewViewModel extends ViewModel {

    private final ReviewRepository repository;

    // Input state
    private final MutableLiveData<Integer> rating     = new MutableLiveData<>(0);
    private final MutableLiveData<String>  comment    = new MutableLiveData<>("");
    private final MutableLiveData<List<String>> tags  = new MutableLiveData<>();

    // Output state
    private final MutableLiveData<Boolean> isLoading  = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSuccess  = new MutableLiveData<>();
    private final MutableLiveData<String>  errorMsg   = new MutableLiveData<>();
    private final MutableLiveData<String>  validationError = new MutableLiveData<>();

    // Order info
    private String orderId;
    private String customerId;
    private String restaurantId;

    public ReviewViewModel(ReviewRepository repository) {
        this.repository = repository;
    }

    // --- Setters từ View ---

    public void setOrderInfo(String orderId, String customerId, String restaurantId) {
        this.orderId      = orderId;
        this.customerId   = customerId;
        this.restaurantId = restaurantId;
    }

    public void setRating(int value) {
        rating.setValue(value);
    }

    public void setComment(String value) {
        comment.setValue(value);
    }

    public void setTags(List<String> selectedTags) {
        tags.setValue(selectedTags);
    }

    // --- Getters LiveData cho View observe ---

    public LiveData<Integer> getRating()       { return rating; }
    public LiveData<Boolean> getIsLoading()    { return isLoading; }
    public LiveData<Boolean> getIsSuccess()    { return isSuccess; }
    public LiveData<String>  getErrorMsg()     { return errorMsg; }
    public LiveData<String>  getValidationError() { return validationError; }

    // --- Logic Submit ---

    public void submitReview() {
        Integer ratingValue = rating.getValue();

        if (ratingValue == null || ratingValue == 0) {
            validationError.setValue("Vui lòng chọn số sao");
            return;
        }

        if (orderId == null || orderId.trim().isEmpty()
                || customerId == null || customerId.trim().isEmpty()
                || restaurantId == null || restaurantId.trim().isEmpty()) {
            validationError.setValue("Thong tin don hang khong hop le");
            return;
        }

        isLoading.setValue(true);

        Review review = new Review(
            orderId,
            customerId,
            restaurantId,
            ratingValue,
            comment.getValue(),
            tags.getValue()
        );

        repository.submitReview(review, orderId, restaurantId, isSuccess, errorMsg, isLoading);
    }
}
