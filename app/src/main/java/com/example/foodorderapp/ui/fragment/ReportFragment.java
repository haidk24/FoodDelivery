package com.example.foodorderapp.ui.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.ReportStats;
import com.example.foodorderapp.data.repository.ReportRepository;
import com.example.foodorderapp.ui.admin.ReportViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class ReportFragment extends Fragment {

    public static final String ARG_RESTAURANT_ID = "ARG_RESTAURANT_ID";

    private ReportViewModel viewModel;

    // Loading
    private ProgressBar progressBar;
    private View reportContent;
    private boolean hasReportData;

    // Tabs
    private TextView tabToday, tabWeek, tabMonth, tabCustom;

    // Hero Card
    private TextView tvHeroTitle, tvHeroRevenue, tvHeroOrderCount;

    // 2x2 Metric Cards
    private TextView tvStatTotalOrders, tvStatRevenue;
    private TextView tvStatDelivered, tvStatDeliveredPct;
    private TextView tvStatCancelled, tvStatCancelledPct;

    // Revenue Chart Card
    private TextView tvChartPeakSubtitle, pillPeakT7;
    private View barColT2, barColT3, barColT4, barColT5, barColT6, barColT7, barColCn;
    private TextView labelColT2, labelColT3, labelColT4, labelColT5, labelColT6, labelColT7, labelColCn;

    // Payment Breakdown Card
    private TextView tvPaymentTotalPill, tvPaymentCodDesc;
    private TextView tvPaymentCodPercent;
    private View paymentDonutFrame;

    // System Health
    private TextView tvHealthRestaurants, tvHealthPending, tvHealthUsers;

    private int selectedTabIndex = 1; // Default: 7 ngày (index 1)
    private final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            String restaurantId = args.getString(ARG_RESTAURANT_ID, "");
            if (restaurantId != null && !restaurantId.trim().isEmpty()) {
                viewModel.setRestaurantScope(restaurantId.trim());
            }
        }

        bindViews(view);
        setupTabs();
        observeViewModel();
        viewModel.getSystemHealth().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;
            if (tvHealthRestaurants != null) tvHealthRestaurants.setText(String.valueOf(stats.activeRestaurants));
            if (tvHealthPending != null) tvHealthPending.setText(String.valueOf(stats.pendingRestaurants));
            if (tvHealthUsers != null) tvHealthUsers.setText(String.valueOf(stats.users));
        });
        viewModel.loadSystemHealth();

        // Trigger default 7 days tab
        selectTab(1);
    }

    private void bindViews(@NonNull View v) {
        progressBar           = v.findViewById(R.id.progress_bar);
        reportContent         = v.findViewById(R.id.report_content);

        tabToday              = v.findViewById(R.id.tab_today);
        tabWeek               = v.findViewById(R.id.tab_week);
        tabMonth              = v.findViewById(R.id.tab_month);
        tabCustom             = v.findViewById(R.id.tab_custom);

        tvHeroTitle           = v.findViewById(R.id.tv_hero_title);
        tvHeroRevenue         = v.findViewById(R.id.tv_hero_revenue);
        tvHeroOrderCount       = v.findViewById(R.id.tv_hero_order_count);

        tvStatTotalOrders     = v.findViewById(R.id.tv_stat_total_orders);
        tvStatRevenue         = v.findViewById(R.id.tv_stat_revenue);
        tvStatDelivered       = v.findViewById(R.id.tv_stat_delivered);
        tvStatDeliveredPct    = v.findViewById(R.id.tv_stat_delivered_pct);
        tvStatCancelled       = v.findViewById(R.id.tv_stat_cancelled);
        tvStatCancelledPct    = v.findViewById(R.id.tv_stat_cancelled_pct);

        tvChartPeakSubtitle   = v.findViewById(R.id.tv_chart_peak_subtitle);
        pillPeakT7            = v.findViewById(R.id.pill_peak_t7);

        barColT2              = v.findViewById(R.id.bar_col_t2);
        barColT3              = v.findViewById(R.id.bar_col_t3);
        barColT4              = v.findViewById(R.id.bar_col_t4);
        barColT5              = v.findViewById(R.id.bar_col_t5);
        barColT6              = v.findViewById(R.id.bar_col_t6);
        barColT7              = v.findViewById(R.id.bar_col_t7);
        barColCn              = v.findViewById(R.id.bar_col_cn);

        labelColT2            = v.findViewById(R.id.label_col_t2);
        labelColT3            = v.findViewById(R.id.label_col_t3);
        labelColT4            = v.findViewById(R.id.label_col_t4);
        labelColT5            = v.findViewById(R.id.label_col_t5);
        labelColT6            = v.findViewById(R.id.label_col_t6);
        labelColT7            = v.findViewById(R.id.label_col_t7);
        labelColCn            = v.findViewById(R.id.label_col_cn);

        tvPaymentTotalPill    = v.findViewById(R.id.tv_payment_total_pill);
        tvPaymentCodDesc      = v.findViewById(R.id.tv_payment_cod_desc);
        tvPaymentCodPercent   = v.findViewById(R.id.tv_payment_cod_percent);
        paymentDonutFrame     = v.findViewById(R.id.payment_donut_frame);

        tvHealthRestaurants   = v.findViewById(R.id.tv_health_restaurants);
        tvHealthPending       = v.findViewById(R.id.tv_health_pending);
        tvHealthUsers         = v.findViewById(R.id.tv_health_users);
    }

    private void setupTabs() {
        tabToday.setOnClickListener(v -> selectTab(0));
        tabWeek.setOnClickListener(v -> selectTab(1));
        tabMonth.setOnClickListener(v -> selectTab(2));
        tabCustom.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int index) {
        selectedTabIndex = index;

        // Reset all tabs
        resetTabStyle(tabToday);
        resetTabStyle(tabWeek);
        resetTabStyle(tabMonth);
        resetTabStyle(tabCustom);

        switch (index) {
            case 0:
                setActiveTabStyle(tabToday);
                tvHeroTitle.setText("TỔNG GIÁ TRỊ ĐƠN HÀNG (HÔM NAY)");
                viewModel.loadReport(ReportRepository.Period.TODAY);
                break;
            case 1:
                setActiveTabStyle(tabWeek);
                tvHeroTitle.setText("TỔNG GIÁ TRỊ ĐƠN HÀNG (7 NGÀY)");
                viewModel.loadReport(ReportRepository.Period.WEEK);
                break;
            case 2:
                setActiveTabStyle(tabMonth);
                tvHeroTitle.setText("TỔNG GIÁ TRỊ ĐƠN HÀNG (30 NGÀY)");
                viewModel.loadReport(ReportRepository.Period.MONTH);
                break;
            case 3:
                setActiveTabStyle(tabCustom);
                showCustomDateRangePicker();
                break;
        }
    }

    private void showCustomDateRangePicker() {
        java.util.Calendar initial = java.util.Calendar.getInstance();
        initial.add(java.util.Calendar.DAY_OF_YEAR, -6);
        android.app.DatePickerDialog startDialog = new android.app.DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            java.util.Calendar start = java.util.Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Calendar endInitial = java.util.Calendar.getInstance();
            android.app.DatePickerDialog endDialog = new android.app.DatePickerDialog(requireContext(), (endPicker, endYear, endMonth, endDay) -> {
                java.util.Calendar end = java.util.Calendar.getInstance();
                end.set(endYear, endMonth, endDay, 23, 59, 59);
                end.set(java.util.Calendar.MILLISECOND, 999);
                if (start.after(end)) {
                    Toast.makeText(requireContext(), "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedTabIndex = 3;
                tvHeroTitle.setText("TỔNG GIÁ TRỊ ĐƠN HÀNG (TÙY CHỌN)");
                viewModel.loadCustomReport(start.getTime(), end.getTime());
            }, endInitial.get(java.util.Calendar.YEAR), endInitial.get(java.util.Calendar.MONTH), endInitial.get(java.util.Calendar.DAY_OF_MONTH));
            endDialog.setTitle("Chọn ngày kết thúc");
            endDialog.show();
        }, initial.get(java.util.Calendar.YEAR), initial.get(java.util.Calendar.MONTH), initial.get(java.util.Calendar.DAY_OF_MONTH));
        startDialog.setTitle("Chọn ngày bắt đầu");
        startDialog.show();
    }

    private void setActiveTabStyle(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_report_tab_active);
        tab.setTextColor(0xFF962808);
        tab.setTypeface(null, Typeface.BOLD);
    }

    private void resetTabStyle(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(0xFF64748B);
        tab.setTypeface(null, Typeface.NORMAL);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) && !hasReportData ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getReportStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                populateReportData(stats);
                hasReportData = true;
                reportContent.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && isAdded()) {
                if (!hasReportData) {
                    reportContent.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateReportData(@NonNull ReportStats stats) {
        long total = stats.totalOrders;
        long delivered = stats.deliveredOrders;
        long cancelled = stats.cancelledOrders;
        long revenue = stats.totalRevenue;
        String revStr = currencyFormatter.format(revenue) + " đ";
        tvHeroRevenue.setText(revStr);
        tvHeroOrderCount.setText(currencyFormatter.format(delivered) + " đơn hoàn tất");
        tvStatTotalOrders.setText(currencyFormatter.format(total) + " đơn");
        tvStatRevenue.setText(revStr);
        tvStatDelivered.setText(currencyFormatter.format(delivered) + " đơn");
        tvStatCancelled.setText(currencyFormatter.format(cancelled) + " đơn");
        double delPct = total > 0 ? delivered * 100.0 / total : 0;
        double canPct = total > 0 ? cancelled * 100.0 / total : 0;
        tvStatDeliveredPct.setText(String.format(Locale.US, "%.1f%%", delPct));
        tvStatCancelledPct.setText(String.format(Locale.US, "%.1f%%", canPct));
        tvPaymentTotalPill.setText("Tổng " + currencyFormatter.format(total) + " đơn");
        long codCount = stats.paymentMethodCount == null ? 0 : stats.paymentMethodCount.getOrDefault("COD", 0L);
        double codPercent = total > 0 ? codCount * 100.0 / total : 0;
        tvPaymentCodDesc.setText(currencyFormatter.format(codCount) + " đơn COD");
        if (tvPaymentCodPercent != null) tvPaymentCodPercent.setText(String.format(Locale.US, "%.1f%%", codPercent));
        if (paymentDonutFrame != null) paymentDonutFrame.setVisibility(total > 0 && codCount == total ? View.VISIBLE : View.GONE);
        renderRevenueChart(stats);
    }

    private void renderRevenueChart(@NonNull ReportStats stats) {
        java.util.List<Long> values = new java.util.ArrayList<>(stats.revenueByDay.values());
        java.util.List<String> dates = new java.util.ArrayList<>(stats.revenueByDay.keySet());
        View[] bars = {barColT2, barColT3, barColT4, barColT5, barColT6, barColT7, barColCn};
        TextView[] labels = {labelColT2, labelColT3, labelColT4, labelColT5, labelColT6, labelColT7, labelColCn};
        long max = 0;
        for (Long value : values) if (value != null) max = Math.max(max, value);
        int first = Math.max(0, values.size() - bars.length);
        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEE", Locale.getDefault());
        java.text.SimpleDateFormat keyFormat = new java.text.SimpleDateFormat("dd/MM", Locale.getDefault());
        for (int i = 0; i < bars.length; i++) {
            int index = first + i;
            boolean visible = index < values.size();
            if (bars[i] != null) {
                bars[i].setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                if (visible) {
                    int heightDp = max == 0 ? 4 : Math.max(4, Math.round(100f * values.get(index) / max));
                    ViewGroup.LayoutParams params = bars[i].getLayoutParams();
                    params.height = (int) (heightDp * getResources().getDisplayMetrics().density);
                    bars[i].setLayoutParams(params);
                }
            }
            if (labels[i] != null) {
                if (!visible) labels[i].setVisibility(View.INVISIBLE);
                else {
                    labels[i].setVisibility(View.VISIBLE);
                    try {
                        java.util.Date date = keyFormat.parse(dates.get(index));
                        labels[i].setText(date == null ? dates.get(index) : dayFormat.format(date));
                    } catch (java.text.ParseException e) { labels[i].setText(dates.get(index)); }
                }
            }
        }
        if (tvChartPeakSubtitle != null) tvChartPeakSubtitle.setText("Doanh thu đơn hoàn tất theo ngày");
        if (pillPeakT7 != null) pillPeakT7.setVisibility(View.GONE);
    }

}
