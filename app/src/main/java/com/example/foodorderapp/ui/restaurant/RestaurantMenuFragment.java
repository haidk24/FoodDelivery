package com.example.foodorderapp.ui.restaurant;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.feature.restaurant.menu.RestaurantMenuViewModel;
import com.example.foodorderapp.ui.restaurant.AddFoodActivity;
import com.example.foodorderapp.ui.restaurant.EditFoodActivity;
import com.example.foodorderapp.ui.restaurant.adapter.FoodAdapter;

import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuFragment extends Fragment {
    private static final String ARG_RESTAURANT_ID = "restaurant_id";
    private final List<Food> foods = new ArrayList<>();
    private RestaurantMenuViewModel menuViewModel;
    private FoodAdapter adapter;
    private String restaurantId = "";
    private String selectedCategory = "Tất cả";
    private String availabilityFilter = "Tất cả";
    private String searchQuery = "";
    private RecyclerView recycler;
    private ProgressBar progress;
    private LinearLayout empty;
    private EditText search;
    private ImageView clearSearch;
    private TextView activeCount, hiddenCount, chipAll, chipMain, chipAppetizer, chipDrink, chipDessert;

    public static RestaurantMenuFragment newInstance(String restaurantId) {
        RestaurantMenuFragment fragment = new RestaurantMenuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESTAURANT_ID, restaurantId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.fragment_restaurant_menu, container, false);
        menuViewModel = new ViewModelProvider(this).get(RestaurantMenuViewModel.class);
        if (getArguments() != null) restaurantId = value(getArguments().getString(ARG_RESTAURANT_ID));
        prepareEmbeddedLayout(root);
        bind(root);
        setupRecycler();
        setupFilters(root);
        root.findViewById(R.id.fabAddFood).setOnClickListener(v -> addFood());
        if (!restaurantId.isEmpty()) observeFoods();
        return root;
    }

    private void prepareEmbeddedLayout(View root) {
        root.setFitsSystemWindows(false);
        root.findViewById(R.id.restaurant_top_bar).setVisibility(View.GONE);
        root.findViewById(R.id.restaurant_top_divider).setVisibility(View.GONE);
        root.findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
        View content = root.findViewById(R.id.restaurant_screen_content);
        ViewGroup.MarginLayoutParams contentParams = (ViewGroup.MarginLayoutParams) content.getLayoutParams();
        contentParams.bottomMargin = 0;
        content.setLayoutParams(contentParams);
        View fab = root.findViewById(R.id.fabAddFood);
        ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        fabParams.bottomMargin = dp(16);
        fab.setLayoutParams(fabParams);
    }

    private void bind(View root) {
        recycler = root.findViewById(R.id.rvMenu);
        progress = root.findViewById(R.id.progress_bar);
        empty = root.findViewById(R.id.layout_empty_state);
        search = root.findViewById(R.id.et_search_food);
        clearSearch = root.findViewById(R.id.btn_clear_search);
        activeCount = root.findViewById(R.id.tv_count_active);
        hiddenCount = root.findViewById(R.id.tv_count_hidden);
        chipAll = root.findViewById(R.id.chip_all);
        chipMain = root.findViewById(R.id.chip_main);
        chipAppetizer = root.findViewById(R.id.chip_appetizer);
        chipDrink = root.findViewById(R.id.chip_drink);
        chipDessert = root.findViewById(R.id.chip_dessert);
    }

    private void setupRecycler() {
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FoodAdapter(requireContext(), foods, restaurantId);
        adapter.setActionListener(new FoodAdapter.OnFoodActionListener() {
            @Override public void onEdit(Food food) { editFood(food); }
            @Override public void onSetAvailability(Food food, boolean available) {
                menuViewModel.setAvailable(food.getFoodId(), available, error -> {
                    if (!isAdded()) return;
                    if (error != null) Toast.makeText(requireContext(), "Cập nhật trạng thái thất bại: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    else Toast.makeText(requireContext(), available ? "Đã mở bán lại món" : "Đã tạm ẩn món", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onDelete(Food food) {
                menuViewModel.delete(food.getFoodId(), error -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), error == null ? "Đã xóa món thành công" : "Lỗi xóa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        adapter.setCountListener((active, hidden, total, main, appetizer, drink, dessert) -> {
            activeCount.setText(active + " món đang bán");
            hiddenCount.setText(hidden + " món tạm ẩn");
            chipAll.setText("Tất cả (" + total + ")");
            chipMain.setText("Món chính (" + main + ")");
            chipAppetizer.setText("Khai vị (" + appetizer + ")");
            chipDrink.setText("Đồ uống (" + drink + ")");
            chipDessert.setText("Tráng miệng (" + dessert + ")");
            empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });
        recycler.setAdapter(adapter);
    }

    private void setupFilters(View root) {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                clearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.filter(searchQuery, selectedCategory);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        clearSearch.setOnClickListener(v -> search.setText(""));
        root.findViewById(R.id.btn_filter_tune).setOnClickListener(v -> {
            String[] choices = {"Tất cả", "Đang bán", "Tạm ẩn"};
            int checked = "Đang bán".equals(availabilityFilter) ? 1 : "Tạm ẩn".equals(availabilityFilter) ? 2 : 0;
            new AlertDialog.Builder(requireContext()).setTitle("Lọc theo trạng thái món")
                    .setSingleChoiceItems(choices, checked, (dialog, which) -> {
                        availabilityFilter = choices[which];
                        adapter.setAvailabilityFilter(availabilityFilter);
                        adapter.filter(searchQuery, selectedCategory);
                        dialog.dismiss();
                    }).setNegativeButton("Đóng", null).show();
        });
        setupChip(chipAll, "Tất cả");
        setupChip(chipMain, "Món chính");
        setupChip(chipAppetizer, "Khai vị");
        setupChip(chipDrink, "Đồ uống");
        setupChip(chipDessert, "Tráng miệng");
    }

    private void setupChip(TextView chip, String category) {
        chip.setOnClickListener(v -> {
            selectedCategory = category;
            for (TextView item : new TextView[]{chipAll, chipMain, chipAppetizer, chipDrink, chipDessert}) {
                item.setBackgroundResource(R.drawable.bg_chip_category_unselected);
                item.setTextColor(0xFF334155);
                item.setTypeface(null, Typeface.NORMAL);
            }
            chip.setBackgroundResource(R.drawable.bg_chip_category_selected);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(null, Typeface.BOLD);
            adapter.filter(searchQuery, selectedCategory);
        });
    }

    private void addFood() {
        if (restaurantId.isEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy nhà hàng hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), AddFoodActivity.class);
        intent.putExtra("EXTRA_RESTAURANT_ID", restaurantId);
        startActivity(intent);
    }

    private void editFood(Food food) {
        Intent intent = new Intent(requireContext(), EditFoodActivity.class);
        intent.putExtra("EXTRA_RESTAURANT_ID", restaurantId);
        intent.putExtra("EXTRA_FOOD_ID", food.getFoodId());
        intent.putExtra("EXTRA_NAME", food.getName());
        intent.putExtra("EXTRA_PRICE", food.getPrice());
        intent.putExtra("EXTRA_OLD_PRICE", food.getOldPrice());
        intent.putExtra("EXTRA_NEW_PRICE", food.getNewPrice());
        intent.putExtra("EXTRA_DISCOUNT", food.getDiscountPercent());
        intent.putExtra("EXTRA_DESC", food.getDescription());
        intent.putExtra("EXTRA_IMAGE_URL", food.getImageUrl());
        intent.putExtra("EXTRA_CATEGORY", food.getCategory());
        intent.putExtra("EXTRA_IS_AVAILABLE", food.isAvailable());
        startActivity(intent);
    }

    public void setRestaurantId(String id) {
        String next = value(id);
        if (next.equals(restaurantId)) return;
        restaurantId = next;
        if (adapter != null) adapter.setRestaurantId(next);
        if (isAdded() && !next.isEmpty()) observeFoods();
    }

    private void observeFoods() {
        progress.setVisibility(View.VISIBLE);
        menuViewModel.getFoods().observe(getViewLifecycleOwner(), values -> {
            if (Boolean.TRUE.equals(menuViewModel.getLoading().getValue())) return;
            renderFoods(values);
        });
        menuViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            progress.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Không tải được thực đơn: " + message, Toast.LENGTH_LONG).show();
        });
        menuViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (!loading && menuViewModel.getError().getValue() == null) renderFoods(menuViewModel.getFoods().getValue());
        });
        menuViewModel.observeRestaurant(restaurantId);
    }

    private void renderFoods(List<Food> values) {
        if (adapter == null) return;
        progress.setVisibility(View.GONE);
        foods.clear();
        if (values != null) foods.addAll(values);
        adapter.updateData(foods);
        adapter.setAvailabilityFilter(availabilityFilter);
        adapter.filter(searchQuery, selectedCategory);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String value(String text) { return text == null ? "" : text.trim(); }

    @Override public void onDestroyView() {
        super.onDestroyView();
    }
}
