package com.example.foodorderapp.feature.admin.report;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.ReportStats;
import com.example.foodorderapp.data.repository.ReportRepository;

public class AdminReportViewModel extends ViewModel {

    private final ReportRepository repo = new ReportRepository();

    private final MutableLiveData<ReportStats> reportStats
            = new MutableLiveData<>();
    private final MutableLiveData<Boolean>     isLoading
            = new MutableLiveData<>(false);
    private final MutableLiveData<String>      errorMessage
            = new MutableLiveData<>();
    private final MutableLiveData<ReportRepository.SystemHealthStats> systemHealth = new MutableLiveData<>();

    // Kỳ hiện tại đang chọn
    private ReportRepository.Period currentPeriod
            = ReportRepository.Period.TODAY;
    private String restaurantScopeId;
    private java.util.Date customStartDate;
    private java.util.Date customEndDate;

    public void setRestaurantScope(String restaurantId) {
        this.restaurantScopeId = restaurantId;
    }

    public void loadSystemHealth() {
        repo.loadSystemHealth((stats, error) -> {
            if (error != null) errorMessage.postValue("Không tải được dữ liệu tổng quan: " + error.getMessage());
            else systemHealth.postValue(stats);
        });
    }

    public void loadReport(ReportRepository.Period period) {
        currentPeriod = period;
        customStartDate = null;
        customEndDate = null;
        repo.loadStats(period, restaurantScopeId, reportStats, isLoading, errorMessage);
    }

    public void refresh() {
        if (customStartDate != null && customEndDate != null) {
            repo.loadStats(customStartDate, customEndDate, restaurantScopeId, reportStats, isLoading, errorMessage);
            return;
        }
        loadReport(currentPeriod);
    }

    public void loadCustomReport(java.util.Date startDate, java.util.Date endDate) {
        customStartDate = startDate;
        customEndDate = endDate;
        repo.loadStats(startDate, endDate, restaurantScopeId, reportStats, isLoading, errorMessage);
    }

    public LiveData<ReportStats> getReportStats() { return reportStats; }
    public LiveData<Boolean>     getIsLoading()   { return isLoading; }
    public LiveData<String>      getErrorMessage(){ return errorMessage; }
    public LiveData<ReportRepository.SystemHealthStats> getSystemHealth() { return systemHealth; }
}
