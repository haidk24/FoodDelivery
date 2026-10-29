package com.example.foodorderapp.feature.review;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.data.repository.ReviewRepository;

public class ReviewViewModelFactory implements ViewModelProvider.Factory {

    private final ReviewRepository repository;

    public ReviewViewModelFactory(ReviewRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ReviewViewModel.class)) {
            return (T) new ReviewViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
