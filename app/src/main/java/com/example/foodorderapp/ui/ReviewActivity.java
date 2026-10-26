package com.example.foodorderapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.repository.ReviewRepository;
import com.example.foodorderapp.viewmodel.ReviewViewModel;
import com.example.foodorderapp.viewmodel.ReviewViewModelFactory;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    private ReviewViewModel viewModel;

    private TextView tvFoodName;
    private ImageView[] stars = new ImageView[5];
    private EditText etComment;
    private ChipGroup chipGroup;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private boolean hasValidOrderInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        initViewModel();
        initViews();
        bindOrderInfo();
        observeViewModel();
        setupStarListeners();
        setupChipListener();

        btnSubmit.setOnClickListener(v -> {
            viewModel.setComment(etComment.getText().toString().trim());
            viewModel.submitReview();
        });
    }

    private void initViewModel() {
        ReviewViewModelFactory factory = new ReviewViewModelFactory(
            ReviewRepository.getInstance()
        );
        viewModel = new ViewModelProvider(this, factory).get(ReviewViewModel.class);
    }

    private void initViews() {
        tvFoodName  = findViewById(R.id.tvFoodName);
        etComment   = findViewById(R.id.etComment);
        chipGroup   = findViewById(R.id.chipGroup);
        btnSubmit   = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);
    }

    private void bindOrderInfo() {
        String orderId      = getIntent().getStringExtra("orderId");
        String customerId   = getIntent().getStringExtra("customerId");
        String restaurantId = getIntent().getStringExtra("restaurantId");
        String foodName     = getIntent().getStringExtra("foodName");

        if (customerId == null || customerId.trim().isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }

        hasValidOrderInfo = orderId != null && !orderId.trim().isEmpty()
                && customerId != null && !customerId.trim().isEmpty()
                && restaurantId != null && !restaurantId.trim().isEmpty();

        viewModel.setOrderInfo(orderId, customerId, restaurantId);
        if (foodName != null) tvFoodName.setText(foodName);

        if (!hasValidOrderInfo) {
            btnSubmit.setEnabled(false);
            Toast.makeText(this, "Thong tin don hang khong hop le", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        // Loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!isLoading);
            btnSubmit.setText(isLoading ? "Đang gửi..." : "Gửi đánh giá");
        });

        // Success
        viewModel.getIsSuccess().observe(this, isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                Toast.makeText(this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Error từ Firestore
        viewModel.getErrorMsg().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Validation error
        viewModel.getValidationError().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Rating thay đổi → cập nhật UI sao
        viewModel.getRating().observe(this, this::updateStarUI);
    }

    private void setupStarListeners() {
        for (int i = 0; i < stars.length; i++) {
            final int ratingValue = i + 1;
            stars[i].setOnClickListener(v -> viewModel.setRating(ratingValue));
        }
    }

    private void setupChipListener() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<String> selectedTags = new ArrayList<>();
            for (int id : checkedIds) {
                Chip chip = group.findViewById(id);
                if (chip != null) selectedTags.add(chip.getText().toString());
            }
            viewModel.setTags(selectedTags);
        });
    }

    private void updateStarUI(int rating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setImageResource(
                i < rating ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
            );
        }
    }
}
