package com.example.foodorderapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.viewModel.AdminViewModel;

public class RestaurantDetailFragment extends Fragment {

    private AdminViewModel viewModel;
    private String restaurantId;

    private ImageView imgRestaurant;
    private TextView tvName, tvRating, tvAddress, tvStatus, tvOwnerId;
    private RatingBar ratingBar;
    private Button btnApprove, btnSuspend, btnReinstate;
    private ProgressBar progressBar;
    private View detailContent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restaurant_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        initViews(view);
        setupToolbar(view);
        observeData();

        if (restaurantId != null) {
            viewModel.loadRestaurantDetail(restaurantId);
        }
    }

    private void initViews(View v) {
        imgRestaurant = v.findViewById(R.id.img_restaurant);
        tvName = v.findViewById(R.id.tv_detail_name);
        tvRating = v.findViewById(R.id.tv_detail_rating);
        tvAddress = v.findViewById(R.id.tv_detail_address);
        tvStatus = v.findViewById(R.id.tv_detail_status);
        tvOwnerId = v.findViewById(R.id.tv_owner_id);
        ratingBar = v.findViewById(R.id.rating_bar);
        btnApprove = v.findViewById(R.id.btn_approve_detail);
        btnSuspend = v.findViewById(R.id.btn_suspend_detail);
        btnReinstate = v.findViewById(R.id.btn_reinstate_detail);
        progressBar = v.findViewById(R.id.progress_detail);
        detailContent = v.findViewById(R.id.detail_content);

        btnApprove.setOnClickListener(view -> viewModel.approveRestaurant(restaurantId));
        btnSuspend.setOnClickListener(view -> viewModel.suspendRestaurant(restaurantId));
        btnReinstate.setOnClickListener(view -> viewModel.reinstateRestaurant(restaurantId));
    }

    private void setupToolbar(View v) {
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> Navigation.findNavController(view).navigateUp());
    }

    private void observeData() {
        viewModel.getSelectedRestaurant().observe(getViewLifecycleOwner(), r -> {
            if (r != null) {
                displayRestaurant(r);
                detailContent.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (detailContent.getVisibility() != View.VISIBLE) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Thao tác thành công", Toast.LENGTH_SHORT).show();
                viewModel.loadRestaurantDetail(restaurantId); // Refresh dữ liệu
            }
        });
    }

    private void displayRestaurant(Restaurant r) {
        tvName.setText(r.name == null || r.name.trim().isEmpty() ? "Chưa cập nhật tên" : r.name);
        tvRating.setText(r.rating > 0 ? String.valueOf(r.rating) : "Chưa có đánh giá");
        ratingBar.setRating((float) r.rating);
        tvAddress.setText(r.getAddressText());
        tvOwnerId.setText("Owner ID: " + (r.ownerId == null || r.ownerId.trim().isEmpty() ? "Chưa cập nhật" : r.ownerId));

        // Hiển thị trạng thái
        tvStatus.setText(r.getStatusText());
        updateStatusStyle(r.status);

        // Hiển thị nút bấm tương ứng với trạng thái
        btnApprove.setVisibility("pending".equals(r.status) ? View.VISIBLE : View.GONE);
        btnSuspend.setVisibility("active".equals(r.status) ? View.VISIBLE : View.GONE);
        btnReinstate.setVisibility("suspended".equals(r.status) ? View.VISIBLE : View.GONE);

        if (r.imageUrl != null && !r.imageUrl.isEmpty()) {
            Glide.with(this).load(r.imageUrl).into(imgRestaurant);
        }
    }

    private void updateStatusStyle(String status) {
        if (status == null) return;
        int colorRes = R.color.white;
        int bgRes = R.drawable.bg_card_border_amber;

        switch (status) {
            case "active":
                bgRes = android.R.color.holo_green_dark;
                break;
            case "pending":
                bgRes = android.R.color.holo_orange_dark;
                break;
            case "suspended":
                bgRes = android.R.color.holo_red_dark;
                break;
        }
        tvStatus.setBackgroundResource(bgRes);
    }
}
