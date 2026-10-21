package com.example.foodorderapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.foodorderapp.ui.MainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.adapter.FoodHomeAdapter;
import com.example.foodorderapp.data.database.FoodDatabase;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.ui.activity.FoodDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HomeFragment — Refactored to match Warm Culinary Modern (Stitch) design:
 * - Sticky AppBarLayout with search & filter controls
 * - Delivery address, user avatar, and notification badge
 * - Promotional hero banner with dynamic deal highlighting and 1-tap order
 * - Category filter chip group with smooth horizontal scrolling
 * - Featured restaurant showcase card
 * - Row-style food item list with discount badges, descriptions, and quick add-to-cart FAB
 * - Full search, filter, and sorting integration
 */
public class HomeFragment extends Fragment {

    // Views — Header
    private MaterialCardView cardAppLogo;
    private TextView tvDeliveryAddress;
    private ImageButton btnNotification;
    private View badgeNotification;
    private MaterialCardView cardUserAvatar;
    private ImageView imgUserAvatar;

    // Views — Search & Filter
    private EditText etSearchFood;
    private MaterialCardView btnFilter;
    private View badgeFilter;

    // Views — Promo Banner
    private TextView tvBannerTitle;
    private TextView tvBannerSubtitle;
    private TextView tvPromoCode;
    private ImageView imgBannerFood;
    private MaterialButton btnBannerOrder;
    private Food featuredBannerFood;

    // Views — Categories
    private ChipGroup chipGroupCategory;

    // Views — Restaurant Showcase
    private TextView tvRestaurantSectionTitle;
    private MaterialCardView cardRestaurantShowcase;
    private ImageView imgRestaurantLogo;
    private TextView tvRestaurantName;
    private TextView tvRestaurantAddress;
    private TextView tvRestaurantRating;
    private TextView tvRestaurantReviews;
    private Restaurant featuredRestaurant;
    private String selectedRestaurantId = null;

    // Views — Food List
    private RecyclerView rvPopular;
    private FoodHomeAdapter foodHomeAdapter;
    private LinearLayout layoutEmpty;
    private TextView tvSeeAll;

    // Data
    private FirebaseFirestore mFirestore;
    private static final List<Restaurant> sCachedRestaurants = new ArrayList<>();
    private static final List<Food> sCachedFoods = new ArrayList<>();
    private final List<Restaurant> allRestaurants = new ArrayList<>();
    private final List<Food> allFoods = new ArrayList<>();
    private final List<Food> displayedFoods = new ArrayList<>();

    // Filter states
    private String currentQuery = "";
    private String selectedCategory = "Tất cả";
    private int currentSortMode = 0; // 0: Default, 1: Discount %, 2: Bestseller, 3: Price low->high, 4: Price high->low

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFirestore = FirebaseFirestore.getInstance();

        initViews(view);
        setupFoodRecyclerView();
        setupSearchAndFilter();
        setupCategoryChips();
        setupBannerAndShowcaseClicks();
        loadUserInfo();

        // Immediately use memory cache if available to prevent any flashing or swapping
        if (!sCachedRestaurants.isEmpty() && !sCachedFoods.isEmpty()) {
            allRestaurants.clear();
            allRestaurants.addAll(sCachedRestaurants);
            allFoods.clear();
            allFoods.addAll(sCachedFoods);
            updateShowcaseRestaurant();
            updatePromoBanner();
            updateDynamicCategoryChips();
            applyFilters();
        }

        loadDataFromFirestore();
    }

    private void initViews(View view) {
        // Header
        cardAppLogo = view.findViewById(R.id.card_app_logo);
        tvDeliveryAddress = view.findViewById(R.id.tv_delivery_address);
        btnNotification = view.findViewById(R.id.btn_notification);
        badgeNotification = view.findViewById(R.id.badge_notification);
        cardUserAvatar = view.findViewById(R.id.card_user_avatar);
        imgUserAvatar = view.findViewById(R.id.img_user_avatar);

        // Search & Filter
        etSearchFood = view.findViewById(R.id.et_search_food);
        btnFilter = view.findViewById(R.id.btn_filter);
        badgeFilter = view.findViewById(R.id.badge_filter);

        // Banner
        tvBannerTitle = view.findViewById(R.id.tv_banner_title);
        tvBannerSubtitle = view.findViewById(R.id.tv_banner_subtitle);
        tvPromoCode = view.findViewById(R.id.tv_promo_code);
        imgBannerFood = view.findViewById(R.id.img_banner_food);
        btnBannerOrder = view.findViewById(R.id.btn_banner_order);

        // Category Chips
        chipGroupCategory = view.findViewById(R.id.chip_group_category);

        // Restaurant Showcase
        tvRestaurantSectionTitle = view.findViewById(R.id.tv_restaurant_section_title);
        cardRestaurantShowcase = view.findViewById(R.id.card_restaurant_showcase);
        imgRestaurantLogo = view.findViewById(R.id.img_restaurant_logo);
        tvRestaurantName = view.findViewById(R.id.tv_restaurant_name);
        tvRestaurantAddress = view.findViewById(R.id.tv_restaurant_address);
        tvRestaurantRating = view.findViewById(R.id.tv_restaurant_rating);
        tvRestaurantReviews = view.findViewById(R.id.tv_restaurant_reviews);

        // Food List
        rvPopular = view.findViewById(R.id.rv_popular);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvSeeAll = view.findViewById(R.id.tv_see_all);
    }

    private void setupFoodRecyclerView() {
        foodHomeAdapter = new FoodHomeAdapter(displayedFoods, new FoodHomeAdapter.OnFoodActionListener() {
            @Override
            public void onFoodClick(Food food) {
                openFoodDetail(food);
            }

            @Override
            public void onAddToCart(Food food) {
                addFoodToCart(food);
            }
        });

        rvPopular.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        rvPopular.setAdapter(foodHomeAdapter);
    }

    private void setupSearchAndFilter() {
        etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (cardAppLogo != null) {
            cardAppLogo.setOnClickListener(v -> openNavigationDrawer());
        }

        btnFilter.setOnClickListener(v -> showSortFilterDialog());

        btnNotification.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Bạn không có thông báo mới", Toast.LENGTH_SHORT).show();
            if (badgeNotification != null) {
                badgeNotification.setVisibility(View.GONE);
            }
        });

        View.OnClickListener openProfileListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfile();
            }
        };
        if (cardUserAvatar != null) {
            cardUserAvatar.setOnClickListener(openProfileListener);
        }
        if (imgUserAvatar != null) {
            imgUserAvatar.setOnClickListener(openProfileListener);
        }

        tvSeeAll.setOnClickListener(v -> resetAllFilters());
    }

    private void openNavigationDrawer() {
        if (getActivity() != null) {
            androidx.drawerlayout.widget.DrawerLayout drawer = getActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        }
    }

    private void setupCategoryChips() {
        if (chipGroupCategory == null) return;

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = getString(R.string.home_category_all);
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    selectedCategory = chip.getText().toString().trim();
                } else {
                    selectedCategory = getString(R.string.home_category_all);
                }
            }
            applyFilters();
        });
    }

    private void setupBannerAndShowcaseClicks() {
        btnBannerOrder.setOnClickListener(v -> {
            if (featuredBannerFood != null) {
                openFoodDetail(featuredBannerFood);
            } else if (!allFoods.isEmpty()) {
                openFoodDetail(allFoods.get(0));
            }
        });

        cardRestaurantShowcase.setOnClickListener(v -> {
            if (featuredRestaurant == null) return;

            if (selectedRestaurantId != null && selectedRestaurantId.equals(featuredRestaurant.getId())) {
                selectedRestaurantId = null;
                Toast.makeText(getContext(), "Đang hiển thị tất cả nhà hàng", Toast.LENGTH_SHORT).show();
            } else {
                selectedRestaurantId = featuredRestaurant.getId();
                Toast.makeText(getContext(), "Lọc món của: " + featuredRestaurant.getName(), Toast.LENGTH_SHORT).show();
            }
            applyFilters();
        });
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tvDeliveryAddress != null) {
            tvDeliveryAddress.setText("Giao tới: Cầu Giấy, Hà Nội");
            if (user != null) {
                mFirestore.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (!isAdded()) return;
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                String address = documentSnapshot.getString("address");
                                if (!TextUtils.isEmpty(address)) {
                                    tvDeliveryAddress.setText("Giao tới: " + address);
                                }
                            }
                        });

                if (user.getPhotoUrl() != null && getContext() != null && imgUserAvatar != null) {
                    Glide.with(this)
                            .load(user.getPhotoUrl())
                            .circleCrop()
                            .into(imgUserAvatar);
                }
            }
        }
    }

    private void loadDataFromFirestore() {
        mFirestore.collection("restaurants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    allRestaurants.clear();
                    allFoods.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        applyFilters();
                        return;
                    }

                    AtomicInteger pendingRequests = new AtomicInteger(queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Restaurant restaurant = new Restaurant();
                        restaurant.setId(document.getId());
                        restaurant.setName(document.getString("name"));
                        restaurant.setImageUrl(document.getString("imageUrl"));
                        restaurant.setAddress(document.get("address") instanceof com.example.foodorderapp.data.model.Address ?
                                (com.example.foodorderapp.data.model.Address) document.get("address") : null);

                        Double ratingVal = document.getDouble("rating");
                        if (ratingVal != null) {
                            restaurant.setRating(ratingVal);
                        }

                        document.getReference().collection("foods").get()
                                .addOnSuccessListener(foodSnapshots -> {
                                    List<Food> foodList = new ArrayList<>();
                                    for (QueryDocumentSnapshot foodDoc : foodSnapshots) {
                                        Food food = foodDoc.toObject(Food.class);
                                        if (food == null) continue;

                                        if (TextUtils.isEmpty(food.getFoodId())) {
                                            food.setFoodId(foodDoc.getId());
                                        }
                                        food.setRestaurantId(document.getId());
                                        foodList.add(food);
                                        allFoods.add(food);
                                    }

                                    if (!foodList.isEmpty()) {
                                        restaurant.setFoods(foodList);
                                        allRestaurants.add(restaurant);
                                    }
                                    onDataLoaded(pendingRequests);
                                })
                                .addOnFailureListener(e -> onDataLoaded(pendingRequests));
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onDataLoaded(AtomicInteger pendingRequests) {
        if (pendingRequests.decrementAndGet() == 0) {
            // Sort restaurants deterministically (by rating DESC, then name ASC)
            Collections.sort(allRestaurants, (a, b) -> {
                int ratingCmp = Double.compare(b.getRating(), a.getRating());
                if (ratingCmp != 0) return ratingCmp;
                String n1 = a.getName() != null ? a.getName() : "";
                String n2 = b.getName() != null ? b.getName() : "";
                return n1.compareTo(n2);
            });

            // Sort foods deterministically (by discount % DESC, then name ASC)
            Collections.sort(allFoods, (a, b) -> {
                int discCmp = Integer.compare(b.getDiscountPercent(), a.getDiscountPercent());
                if (discCmp != 0) return discCmp;
                String n1 = a.getName() != null ? a.getName() : "";
                String n2 = b.getName() != null ? b.getName() : "";
                return n1.compareTo(n2);
            });

            // Update in-memory cache
            sCachedRestaurants.clear();
            sCachedRestaurants.addAll(allRestaurants);
            sCachedFoods.clear();
            sCachedFoods.addAll(allFoods);

            updateShowcaseRestaurant();
            updatePromoBanner();
            updateDynamicCategoryChips();
            applyFilters();
        }
    }

    private void updateShowcaseRestaurant() {
        if (!isAdded() || allRestaurants.isEmpty()) return;

        // Pick highest rated restaurant, or first one
        Restaurant bestRestaurant = allRestaurants.get(0);
        for (Restaurant r : allRestaurants) {
            if (r.getRating() > bestRestaurant.getRating()) {
                bestRestaurant = r;
            }
        }
        featuredRestaurant = bestRestaurant;

        if (tvRestaurantSectionTitle != null) {
            tvRestaurantSectionTitle.setText("Món ngon từ " + featuredRestaurant.getName());
        }
        tvRestaurantName.setText(featuredRestaurant.getName());
        if (!TextUtils.isEmpty(featuredRestaurant.getAddressText())) {
            tvRestaurantAddress.setText(featuredRestaurant.getAddressText());
        } else {
            tvRestaurantAddress.setText("Chi nhánh Hoàn Kiếm, Hà Nội");
        }
        double rating = featuredRestaurant.getRating() > 0 ? featuredRestaurant.getRating() : 4.8;
        tvRestaurantRating.setText(String.format(Locale.US, "%.1f", rating));

        if (!TextUtils.isEmpty(featuredRestaurant.getImageUrl()) && getContext() != null) {
            Glide.with(this)
                    .load(featuredRestaurant.getImageUrl())
                    .placeholder(R.drawable.banghoa)
                    .error(R.drawable.banghoa)
                    .centerCrop()
                    .into(imgRestaurantLogo);
        }
    }

    private void updatePromoBanner() {
        if (!isAdded()) return;

        // Find food with highest discount
        Food bestDeal = null;
        for (Food f : allFoods) {
            if (f.getDiscountPercent() > 0) {
                if (bestDeal == null || f.getDiscountPercent() > bestDeal.getDiscountPercent()) {
                    bestDeal = f;
                }
            }
        }

        tvBannerTitle.setText("Giảm 50%");
        tvBannerSubtitle.setText("Cho đơn hàng đầu tiên hôm nay");
        if (tvPromoCode != null) {
            tvPromoCode.setText("MÃ: NEW50");
        }

        if (bestDeal != null) {
            featuredBannerFood = bestDeal;
            if (!TextUtils.isEmpty(bestDeal.getImageUrl()) && getContext() != null) {
                Glide.with(this)
                        .load(bestDeal.getImageUrl())
                        .placeholder(R.drawable.banghoa)
                        .error(R.drawable.banghoa)
                        .centerCrop()
                        .into(imgBannerFood);
            }
        }
    }

    private void updateDynamicCategoryChips() {
        if (!isAdded() || getContext() == null || chipGroupCategory == null) return;

        Set<String> categories = new HashSet<>();
        for (Food food : allFoods) {
            if (!TextUtils.isEmpty(food.getCategory())) {
                categories.add(food.getCategory().trim());
            }
        }

        for (String cat : categories) {
            if (TextUtils.isEmpty(cat)) continue;

            boolean alreadyExists = false;
            for (int i = 0; i < chipGroupCategory.getChildCount(); i++) {
                View child = chipGroupCategory.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (isCategoryMatch(cat, chip.getText().toString().trim())) {
                        alreadyExists = true;
                        break;
                    }
                }
            }

            if (!alreadyExists) {
                Chip newChip = new Chip(requireContext(), null, R.style.Widget_FoodOrder_CategoryChip);
                newChip.setText(cat);
                newChip.setCheckable(true);
                newChip.setClickable(true);
                chipGroupCategory.addView(newChip);
            }
        }
    }

    private void applyFilters() {
        if (!isAdded()) return;

        String allLabel = getString(R.string.home_category_all);
        String query = currentQuery.toLowerCase(Locale.ROOT);
        boolean isAllCat = TextUtils.isEmpty(selectedCategory) || selectedCategory.equalsIgnoreCase(allLabel);

        List<Food> filtered = new ArrayList<>();
        for (Food food : allFoods) {
            if (food == null) continue;

            // Restaurant filter
            if (selectedRestaurantId != null && !selectedRestaurantId.equals(food.getRestaurantId())) {
                continue;
            }

            // Category filter
            if (!isAllCat && !isCategoryMatch(food.getCategory(), selectedCategory)) {
                continue;
            }

            // Text search (name, description, restaurant name)
            if (!TextUtils.isEmpty(query)) {
                String foodName = food.getName() == null ? "" : food.getName().toLowerCase(Locale.ROOT);
                String desc = food.getDescription() == null ? "" : food.getDescription().toLowerCase(Locale.ROOT);
                String restName = getRestaurantName(food.getRestaurantId()).toLowerCase(Locale.ROOT);

                if (!foodName.contains(query) && !desc.contains(query) && !restName.contains(query)) {
                    continue;
                }
            }

            filtered.add(food);
        }

        // Sorting
        switch (currentSortMode) {
            case 1: // Discount high to low
                Collections.sort(filtered, (f1, f2) -> Integer.compare(f2.getDiscountPercent(), f1.getDiscountPercent()));
                break;
            case 2: // Bestseller (orderCount)
                Collections.sort(filtered, (f1, f2) -> Integer.compare(f2.getOrderCount(), f1.getOrderCount()));
                break;
            case 3: // Price low to high
                Collections.sort(filtered, (f1, f2) -> Double.compare(f1.getNewPrice(), f2.getNewPrice()));
                break;
            case 4: // Price high to low
                Collections.sort(filtered, (f1, f2) -> Double.compare(f2.getNewPrice(), f1.getNewPrice()));
                break;
            default:
                break;
        }

        displayedFoods.clear();
        displayedFoods.addAll(filtered);
        foodHomeAdapter.setData(displayedFoods);

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(displayedFoods.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rvPopular != null) {
            rvPopular.setVisibility(displayedFoods.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (tvSeeAll != null) {
            int total = allFoods.isEmpty() ? displayedFoods.size() : allFoods.size();
            tvSeeAll.setText("Xem tất cả (" + total + ")");
        }
    }

    private boolean isCategoryMatch(String foodCategory, String selectedCategory) {
        if (TextUtils.isEmpty(selectedCategory) || "Tất cả".equalsIgnoreCase(selectedCategory)) {
            return true;
        }
        if (TextUtils.isEmpty(foodCategory)) {
            return false;
        }

        String fCat = foodCategory.trim().toLowerCase(Locale.ROOT);
        String sCat = selectedCategory.trim().toLowerCase(Locale.ROOT);

        if (fCat.equals(sCat) || fCat.contains(sCat) || sCat.contains(fCat)) {
            return true;
        }

        if (sCat.contains("&")) {
            String[] parts = sCat.split("&");
            for (String part : parts) {
                if (fCat.contains(part.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getRestaurantName(String restaurantId) {
        if (restaurantId == null) return "";
        for (Restaurant r : allRestaurants) {
            if (restaurantId.equals(r.getId())) {
                return r.getName() == null ? "" : r.getName();
            }
        }
        return "";
    }

    private void showSortFilterDialog() {
        String[] options = {
                "Mặc định (Tất cả)",
                "🔥 Khuyến mãi nhiều nhất",
                "⭐ Bán chạy nhất",
                "💰 Giá: Thấp đến cao",
                "💎 Giá: Cao đến thấp"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Sắp xếp & Lọc món")
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    if (badgeFilter != null) {
                        badgeFilter.setVisibility(currentSortMode != 0 ? View.VISIBLE : View.GONE);
                    }
                    applyFilters();
                    dialog.dismiss();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void resetAllFilters() {
        currentQuery = "";
        if (etSearchFood != null) {
            etSearchFood.setText("");
        }

        selectedCategory = getString(R.string.home_category_all);
        if (chipGroupCategory != null) {
            chipGroupCategory.check(R.id.chip_all);
        }

        selectedRestaurantId = null;
        currentSortMode = 0;
        if (badgeFilter != null) {
            badgeFilter.setVisibility(View.GONE);
        }

        applyFilters();
        Toast.makeText(getContext(), "Đã hiển thị lại tất cả món", Toast.LENGTH_SHORT).show();
    }

    private void openFoodDetail(Food food) {
        if (food == null || getContext() == null) return;
        Intent intent = new Intent(getContext(), FoodDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_food", food);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void addFoodToCart(Food food) {
        if (food == null || getContext() == null) return;
        try {
            List<Food> list = FoodDatabase.getInstance(requireContext())
                    .foodDao()
                    .checkFoodInCart(food.getFoodId());

            if (list != null && !list.isEmpty()) {
                Food foodInCart = list.get(0);
                foodInCart.setCount(foodInCart.getCount() + 1);
                FoodDatabase.getInstance(requireContext()).foodDao().updateFood(foodInCart);
            } else {
                food.setCount(1);
                FoodDatabase.getInstance(requireContext()).foodDao().insertFood(food);
            }
            Toast.makeText(requireContext(), "Đã thêm " + food.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi thêm giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
