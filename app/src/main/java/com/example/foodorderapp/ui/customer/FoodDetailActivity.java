package com.example.foodorderapp.ui.customer;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.feature.customer.fooddetail.FoodDetailViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FoodDetailActivity — Food Detail screen refactored to match Stitch Warm Culinary Modern.
 * Features:
 * - Clean fixed Top Bar with back arrow, title, user avatar
 * - Full-width hero image with Favorite, Share, and Bestseller pill badges
 * - Discount badge, prep time, large typography for dish name and price
 * - Restaurant info tile with verified badge, rating and status
 * - Detailed narrative description
 * - Interactive Flavor Preference chips (toggles)
 * - Kitchen hygiene/commitment guarantee card
 * - Fixed bottom action bar with quantity stepper (- / +) and dynamic price calculation
 */
public class FoodDetailActivity extends AppCompatActivity {

    // Views — Top Bar
    private ImageButton btnBack;
    private MaterialCardView cardUserAvatar;
    private ImageView imgUserAvatar;

    // Views — Hero Visual
    private ImageView imgFood;
    private MaterialCardView btnFavorite;
    private ImageView imgFavoriteIcon;
    private MaterialCardView btnShare;
    private MaterialCardView cardBestsellerBadge;

    // Views — Main Content
    private LinearLayout layoutDiscountTag;
    private TextView tvDiscountTag;
    private TextView tvPrepTime;
    private TextView tvFoodName;
    private TextView tvNewPrice;
    private TextView tvOldPrice;

    // Views — Restaurant Tile
    private LinearLayout cardRestaurantInfo;
    private ImageView imgRestaurantLogo;
    private TextView tvRestaurantName;
    private TextView tvRestaurantRating;
    private TextView tvRestaurantReviews;

    // Views — Description
    private TextView tvDescription;

    // Views — Flavor Preference Chips
    private LinearLayout chipOptMildSpicy;
    private ImageView iconOptMildSpicy;
    private TextView textOptMildSpicy;
    private boolean isOptMildSpicySelected = false;

    private LinearLayout chipOptNoHerbs;
    private ImageView iconOptNoHerbs;
    private TextView textOptNoHerbs;
    private boolean isOptNoHerbsSelected = false;

    private LinearLayout chipOptExtraSauce;
    private ImageView iconOptExtraSauce;
    private TextView textOptExtraSauce;
    private boolean isOptExtraSauceSelected = false;

    // Views — Bottom Action Dock
    private MaterialCardView btnDecrement;
    private TextView tvQuantity;
    private MaterialCardView btnIncrement;
    private MaterialButton btnAddToCart;

    // State
    private Food currentFood;
    private int currentQuantity = 1;
    private boolean isFavorite = false;
    private final NumberFormat priceFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private FoodDetailViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        viewModel = new ViewModelProvider(this).get(FoodDetailViewModel.class);
        initViews();
        observeViewModel();
        setupListeners();
        loadUserInfo();
        getIntentData();
    }

    private void observeViewModel() {
        viewModel.getRestaurant().observe(this, restaurant -> {
            if (restaurant == null) return;
            String name = restaurant.getName();
            tvRestaurantName.setText(TextUtils.isEmpty(name) ? "Chưa cập nhật tên quán" : name);
            if (!TextUtils.isEmpty(restaurant.getImageUrl())) {
                Glide.with(this).load(restaurant.getImageUrl())
                        .placeholder(R.drawable.banghoa).error(R.drawable.banghoa)
                        .centerCrop().into(imgRestaurantLogo);
            }
            tvRestaurantRating.setText(restaurant.getRating() > 0
                    ? String.format(Locale.US, "%.1f", restaurant.getRating()) : "--");
        });
        viewModel.getRestaurantError().observe(this, error -> {
            if (error == null || error.trim().isEmpty()) return;
            tvRestaurantName.setText("Không tải được thông tin quán");
            tvRestaurantRating.setText("--");
            tvRestaurantReviews.setText("");
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        cardUserAvatar = findViewById(R.id.card_user_avatar);
        imgUserAvatar = findViewById(R.id.img_user_avatar);

        imgFood = findViewById(R.id.img_food);
        btnFavorite = findViewById(R.id.btn_favorite);
        imgFavoriteIcon = findViewById(R.id.img_favorite_icon);
        btnShare = findViewById(R.id.btn_share);
        cardBestsellerBadge = findViewById(R.id.card_bestseller_badge);

        layoutDiscountTag = findViewById(R.id.layout_discount_tag);
        tvDiscountTag = findViewById(R.id.tv_discount_tag);
        tvPrepTime = findViewById(R.id.tv_prep_time);
        tvFoodName = findViewById(R.id.tv_food_name);
        tvNewPrice = findViewById(R.id.tv_new_price);
        tvOldPrice = findViewById(R.id.tv_old_price);

        cardRestaurantInfo = findViewById(R.id.card_restaurant_info);
        imgRestaurantLogo = findViewById(R.id.img_restaurant_logo);
        tvRestaurantName = findViewById(R.id.tv_restaurant_name);
        tvRestaurantRating = findViewById(R.id.tv_restaurant_rating);
        tvRestaurantReviews = findViewById(R.id.tv_restaurant_reviews);

        tvDescription = findViewById(R.id.tv_description);

        chipOptMildSpicy = findViewById(R.id.chip_opt_mild_spicy);
        iconOptMildSpicy = findViewById(R.id.icon_opt_mild_spicy);
        textOptMildSpicy = findViewById(R.id.text_opt_mild_spicy);

        chipOptNoHerbs = findViewById(R.id.chip_opt_no_herbs);
        iconOptNoHerbs = findViewById(R.id.icon_opt_no_herbs);
        textOptNoHerbs = findViewById(R.id.text_opt_no_herbs);

        chipOptExtraSauce = findViewById(R.id.chip_opt_extra_sauce);
        iconOptExtraSauce = findViewById(R.id.icon_opt_extra_sauce);
        textOptExtraSauce = findViewById(R.id.text_opt_extra_sauce);

        btnDecrement = findViewById(R.id.btn_decrement);
        tvQuantity = findViewById(R.id.tv_quantity);
        btnIncrement = findViewById(R.id.btn_increment);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnShare.setOnClickListener(v -> shareDish());

        // Flavor chips toggle
        chipOptMildSpicy.setOnClickListener(v -> {
            isOptMildSpicySelected = !isOptMildSpicySelected;
            updateOptionChipUI(chipOptMildSpicy, iconOptMildSpicy, textOptMildSpicy, isOptMildSpicySelected);
        });

        chipOptNoHerbs.setOnClickListener(v -> {
            isOptNoHerbsSelected = !isOptNoHerbsSelected;
            updateOptionChipUI(chipOptNoHerbs, iconOptNoHerbs, textOptNoHerbs, isOptNoHerbsSelected);
        });

        chipOptExtraSauce.setOnClickListener(v -> {
            isOptExtraSauceSelected = !isOptExtraSauceSelected;
            updateOptionChipUI(chipOptExtraSauce, iconOptExtraSauce, textOptExtraSauce, isOptExtraSauceSelected);
        });

        // Quantity Stepper
        btnDecrement.setOnClickListener(v -> updateQuantity(-1));
        btnIncrement.setOnClickListener(v -> updateQuantity(1));

        btnAddToCart.setOnClickListener(v -> {
            if (currentFood != null) {
                // Micro-interaction bounce
                ScaleAnimation anim = new ScaleAnimation(1f, 0.95f, 1f, 0.95f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(120);
                btnAddToCart.startAnimation(anim);

                addFoodToCart(currentFood, currentQuantity);
            }
        });
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null && imgUserAvatar != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgUserAvatar);
        }
    }

    private void getIntentData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) return;

        currentFood = (Food) bundle.get("object_food");
        if (currentFood == null) return;

        // Load image
        Glide.with(this)
                .load(currentFood.getImageUrl())
                .placeholder(R.drawable.banh)
                .error(R.drawable.banh)
                .centerCrop()
                .into(imgFood);

        // Name
        tvFoodName.setText(currentFood.getName());

        // Discount Tag
        int discount = currentFood.getDiscountPercent();
        if (discount > 0) {
            tvDiscountTag.setText("-" + discount + "% ƯU ĐÃI HÔM NAY");
            layoutDiscountTag.setVisibility(View.VISIBLE);
        } else {
            layoutDiscountTag.setVisibility(View.GONE);
        }

        // Bestseller badge
        boolean isBestseller = currentFood.getOrderCount() >= 10 || discount > 0;
        cardBestsellerBadge.setVisibility(isBestseller ? View.VISIBLE : View.GONE);

        // Prices
        double newPrice = currentFood.getNewPrice();
        double oldPrice = currentFood.getOldPrice();
        tvNewPrice.setText(priceFormatter.format(newPrice) + " đ");

        if (oldPrice > newPrice && oldPrice > 0) {
            tvOldPrice.setText(priceFormatter.format(oldPrice) + " đ");
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvOldPrice.setVisibility(View.VISIBLE);
        } else {
            tvOldPrice.setVisibility(View.GONE);
        }

        // Description
        if (!TextUtils.isEmpty(currentFood.getDescription())) {
            tvDescription.setText(currentFood.getDescription());
        }

        // Restaurant details
        loadRestaurantDetails(currentFood.getRestaurantId());

        // Update stepper & button label
        updateQuantity(0);
    }

    private void loadRestaurantDetails(String restaurantId) {
        if (TextUtils.isEmpty(restaurantId)) {
            tvRestaurantName.setText("Chưa xác định quán");
            tvRestaurantRating.setText("--");
            tvRestaurantReviews.setText("");
            return;
        }

        viewModel.loadRestaurant(restaurantId.trim());
    }

    private void updateOptionChipUI(LinearLayout chipLayout, ImageView icon, TextView text, boolean isSelected) {
        if (isSelected) {
            chipLayout.setBackgroundResource(R.drawable.bg_chip_option_active);
            int primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);
            icon.setColorFilter(primaryColor);
            text.setTextColor(primaryColor);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chipLayout.setBackgroundResource(R.drawable.bg_chip_option_inactive);
            int normalColor = ContextCompat.getColor(this, R.color.colorTextPrimary);
            int mutedColor = ContextCompat.getColor(this, R.color.colorTextSecondary);
            icon.setColorFilter(mutedColor);
            text.setTextColor(normalColor);
            text.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        if (isFavorite) {
            imgFavoriteIcon.setImageResource(R.drawable.ic_favorite);
            imgFavoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
            Toast.makeText(this, "Đã lưu món vào danh sách yêu thích!", Toast.LENGTH_SHORT).show();
        } else {
            imgFavoriteIcon.setImageResource(R.drawable.ic_favorite_border);
            imgFavoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorTextPrimary));
            Toast.makeText(this, "Đã bỏ lưu món ăn", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareDish() {
        if (currentFood == null) return;
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Món ngon " + currentFood.getName() + " - " + priceFormatter.format(currentFood.getNewPrice()) + " đ trên Food Order Pro!");
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, "Chia sẻ món ăn");
            startActivity(shareIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Đã sao chép thông tin món ăn!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateQuantity(int delta) {
        int next = currentQuantity + delta;
        if (next < 1) next = 1;
        if (next > 99) next = 99;
        currentQuantity = next;

        tvQuantity.setText(String.valueOf(currentQuantity));

        // Disable or fade decrement button when quantity == 1
        btnDecrement.setAlpha(currentQuantity == 1 ? 0.4f : 1.0f);
        btnDecrement.setEnabled(currentQuantity > 1);

        // Update button text with total price
        if (currentFood != null) {
            double total = currentFood.getNewPrice() * currentQuantity;
            btnAddToCart.setText("Thêm vào giỏ hàng - " + priceFormatter.format(total) + " đ");
        }
    }

    private void addFoodToCart(Food food, int quantity) {
        List<String> selectedNotes = new ArrayList<>();
        if (isOptMildSpicySelected) selectedNotes.add("Ít cay");
        if (isOptNoHerbsSelected) selectedNotes.add("Không rau thơm");
        if (isOptExtraSauceSelected) selectedNotes.add("Thêm nước chấm");

        viewModel.addToCart(food, quantity);

        String noteMsg = selectedNotes.isEmpty() ? "" : " (" + TextUtils.join(", ", selectedNotes) + ")";
        Toast.makeText(this, "Đã thêm " + quantity + " phần " + food.getName() + noteMsg + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();
    }
}
