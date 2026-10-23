package com.example.foodorderapp.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.ui.admin.adapter.RestaurantAdapter;
import com.example.foodorderapp.viewModel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RestaurantApprovalFragment extends Fragment implements RestaurantAdapter.Listener {

    private static final int TAB_PENDING   = 0;
    private static final int TAB_ACTIVE    = 1;
    private static final int TAB_SUSPENDED = 2;

    private AdminViewModel viewModel;
    private RestaurantAdapter adapter;

    // Views
    private EditText edtSearch;
    private View btnFilter;

    // Tabs
    private View tabPending, tabActive, tabSuspended;
    private TextView tvTabPendingTitle, tvBadgePending;
    private TextView tvTabActiveTitle, tvBadgeActive;
    private TextView tvTabSuspendedTitle, tvBadgeSuspended;

    // Recycler & State
    private RecyclerView rvRestaurants;
    private View layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptyDesc;
    private ProgressBar progressBar;

    private int currentTab = TAB_PENDING;
    private String currentSearchQuery = "";

    private List<Restaurant> cachedPendingList = new ArrayList<>();
    private List<Restaurant> cachedActiveList = new ArrayList<>();
    private List<Restaurant> cachedSuspendedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restaurant_approval, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        bindViews(view);
        setupRecyclerView();
        setupTabs();
        setupSearch();
        observeViewModel();

        viewModel.observeRestaurants();
    }

    private void bindViews(View v) {
        edtSearch = v.findViewById(R.id.edt_search);
        btnFilter = v.findViewById(R.id.btn_filter);

        tabPending = v.findViewById(R.id.tab_pending);
        tvTabPendingTitle = v.findViewById(R.id.tv_tab_pending_title);
        tvBadgePending = v.findViewById(R.id.tv_badge_pending);

        tabActive = v.findViewById(R.id.tab_active);
        tvTabActiveTitle = v.findViewById(R.id.tv_tab_active_title);
        tvBadgeActive = v.findViewById(R.id.tv_badge_active);

        tabSuspended = v.findViewById(R.id.tab_suspended);
        tvTabSuspendedTitle = v.findViewById(R.id.tv_tab_suspended_title);
        tvBadgeSuspended = v.findViewById(R.id.tv_badge_suspended);

        rvRestaurants = v.findViewById(R.id.rv_restaurants);
        layoutEmptyState = v.findViewById(R.id.layout_empty_state);
        tvEmptyTitle = v.findViewById(R.id.tv_empty_title);
        tvEmptyDesc = v.findViewById(R.id.tv_empty_desc);
        progressBar = v.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new RestaurantAdapter(this, RestaurantAdapter.TYPE_PENDING);
        rvRestaurants.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRestaurants.setAdapter(adapter);
    }

    private void setupTabs() {
        tabPending.setOnClickListener(v -> selectTab(TAB_PENDING));
        tabActive.setOnClickListener(v -> selectTab(TAB_ACTIVE));
        tabSuspended.setOnClickListener(v -> selectTab(TAB_SUSPENDED));

        btnFilter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Bộ lọc nâng cao", Toast.LENGTH_SHORT).show();
        });

        // Khởi tạo tab ban đầu
        selectTab(TAB_PENDING);
    }

    private void selectTab(int tabIndex) {
        currentTab = tabIndex;

        // Reset backgrounds
        tabPending.setBackground(null);
        tabActive.setBackground(null);
        tabSuspended.setBackground(null);

        tvTabPendingTitle.setTextColor(0xFF334155);
        tvTabPendingTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvTabActiveTitle.setTextColor(0xFF334155);
        tvTabActiveTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvTabSuspendedTitle.setTextColor(0xFF334155);
        tvTabSuspendedTitle.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Highlight tab được chọn
        if (tabIndex == TAB_PENDING) {
            tabPending.setBackgroundResource(R.drawable.bg_tab_active_pill);
            tvTabPendingTitle.setTextColor(0xFF962808);
            tvTabPendingTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (tabIndex == TAB_ACTIVE) {
            tabActive.setBackgroundResource(R.drawable.bg_tab_active_pill);
            tvTabActiveTitle.setTextColor(0xFF962808);
            tvTabActiveTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tabSuspended.setBackgroundResource(R.drawable.bg_tab_active_pill);
            tvTabSuspendedTitle.setTextColor(0xFF962808);
            tvTabSuspendedTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        applyFilterAndDisplay();
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilterAndDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getPendingRestaurantList().observe(getViewLifecycleOwner(), list -> {
            cachedPendingList = (list != null) ? new ArrayList<>(list) : new ArrayList<>();
            ensureSamplePendingIfEmpty();
            tvBadgePending.setText(String.valueOf(cachedPendingList.size()));
            if (currentTab == TAB_PENDING) applyFilterAndDisplay();
        });

        viewModel.getActiveRestaurantList().observe(getViewLifecycleOwner(), list -> {
            cachedActiveList = (list != null) ? new ArrayList<>(list) : new ArrayList<>();
            ensureSampleActiveIfEmpty();
            tvBadgeActive.setText(String.valueOf(cachedActiveList.size()));
            if (currentTab == TAB_ACTIVE) applyFilterAndDisplay();
        });

        viewModel.getSuspendedRestaurantList().observe(getViewLifecycleOwner(), list -> {
            cachedSuspendedList = (list != null) ? new ArrayList<>(list) : new ArrayList<>();
            ensureSampleSuspendedIfEmpty();
            tvBadgeSuspended.setText(String.valueOf(cachedSuspendedList.size()));
            if (currentTab == TAB_SUSPENDED) applyFilterAndDisplay();
        });

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(requireContext(), "Cập nhật trạng thái nhà hàng thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ensureSamplePendingIfEmpty() {
        if (cachedPendingList.isEmpty()) {
            Restaurant sample = new Restaurant();
            sample.restaurantId = "sample_pending_1";
            sample.name = "Tiến Mạnh Quán 2";
            sample.phone = "0987 654 321";
            sample.status = "pending";
            sample.rating = 0;
            sample.hasLicence = true;
            sample.ownerId = "tienmanh";
            sample.address = "Số 88 Phố Huế, Q. Hai Bà Trưng, Hà Nội";
            cachedPendingList.add(sample);
        }
    }

    private void ensureSampleActiveIfEmpty() {
        if (cachedActiveList.isEmpty()) {
            Restaurant sample = new Restaurant();
            sample.restaurantId = "sample_active_1";
            sample.name = "Mạnh Tiến Quán – Cơm & Phở";
            sample.phone = "0912 345 678";
            sample.status = "active";
            sample.rating = 4.8;
            sample.address = "Hoàn Kiếm, Hà Nội";
            cachedActiveList.add(sample);
        }
    }

    private void ensureSampleSuspendedIfEmpty() {
        if (cachedSuspendedList.isEmpty()) {
            Restaurant sample = new Restaurant();
            sample.restaurantId = "sample_suspended_1";
            sample.name = "Phở Bát Đàn Cổ Truyền";
            sample.phone = "0904 112 233";
            sample.status = "suspended";
            sample.rating = 4.5;
            sample.address = "Cầu Giấy, Hà Nội";
            cachedSuspendedList.add(sample);
        }
    }

    private void applyFilterAndDisplay() {
        List<Restaurant> sourceList;
        if (currentTab == TAB_PENDING) {
            sourceList = cachedPendingList;
        } else if (currentTab == TAB_ACTIVE) {
            sourceList = cachedActiveList;
        } else {
            sourceList = cachedSuspendedList;
        }

        List<Restaurant> filtered = new ArrayList<>();
        for (Restaurant r : sourceList) {
            if (currentSearchQuery.isEmpty()) {
                filtered.add(r);
            } else {
                String name = r.name != null ? r.name.toLowerCase(Locale.getDefault()) : "";
                String addr = r.getAddressText() != null ? r.getAddressText().toLowerCase(Locale.getDefault()) : "";
                String phone = r.phone != null ? r.phone.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(currentSearchQuery) || addr.contains(currentSearchQuery) || phone.contains(currentSearchQuery)) {
                    filtered.add(r);
                }
            }
        }

        adapter.submitList(filtered);

        boolean isEmpty = filtered.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvRestaurants.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ── RestaurantAdapter.Listener callbacks ─────────────────────────

    @Override
    public void onApprove(Restaurant r) {
        if (r.restaurantId != null && !r.restaurantId.startsWith("sample_")) {
            viewModel.approveRestaurant(r.restaurantId);
        } else {
            Toast.makeText(requireContext(), "Đã duyệt thành công cho " + r.name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSuspend(Restaurant r) {
        if (r.restaurantId != null && !r.restaurantId.startsWith("sample_")) {
            viewModel.suspendRestaurant(r.restaurantId);
        } else {
            Toast.makeText(requireContext(), "Đã tạm ngưng cho " + r.name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReinstate(Restaurant r) {
        if (r.restaurantId != null && !r.restaurantId.startsWith("sample_")) {
            viewModel.reinstateRestaurant(r.restaurantId);
        } else {
            Toast.makeText(requireContext(), "Đã mở hoạt động lại cho " + r.name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewDetail(Restaurant r) {
        if (r.restaurantId != null && !r.restaurantId.startsWith("sample_")) {
            Bundle args = new Bundle();
            args.putString("restaurantId", r.restaurantId);
            try {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_restaurants_to_detail, args);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Xem chi tiết: " + r.name, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Chi tiết nhà hàng: " + r.name, Toast.LENGTH_SHORT).show();
        }
    }
}
