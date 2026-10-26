package com.example.foodorderapp.feature.customer.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.repository.HomeRepository;

public class HomeViewModel extends ViewModel {
    private final HomeRepository repository = new HomeRepository();
    private final MutableLiveData<HomeRepository.HomeData> data = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private boolean loaded;

    public void load() {
        if (loaded || Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        repository.load((value, failure) -> {
            if (value != null) {
                data.postValue(value);
                loaded = true;
            }
            if (failure != null) error.postValue(failure.getMessage());
            loading.postValue(false);
        });
    }

    public void refresh() {
        loaded = false;
        load();
    }

    public LiveData<HomeRepository.HomeData> getData() { return data; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
