package com.example.foodorderapp.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.FoodCategory;
import com.example.foodorderapp.ui.EditFoodActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private final Context context;
    private final List<Food> originalList;
    private final List<Food> displayedList;
    private String restaurantId;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private OnFoodCountChangedListener countListener;
    private String availabilityFilter = "Tất cả";
    private String currentQuery = "";
    private String currentCategory = "Tất cả";

    public interface OnFoodCountChangedListener {
        void onCountChanged(int activeCount, int hiddenCount, int totalCount, int mainCount, int appCount, int drinkCount, int dessertCount);
    }

    public FoodAdapter(Context context, List<Food> foodList, String restaurantId) {
        this.context = context;
        this.originalList = new ArrayList<>();
        if (foodList != null) {
            this.originalList.addAll(foodList);
        }
        this.displayedList = new ArrayList<>(this.originalList);
        this.restaurantId = restaurantId == null ? "" : restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId == null ? "" : restaurantId;
    }

    public void setAvailabilityFilter(String filter) {
        availabilityFilter = filter;
    }

    public void setCountListener(OnFoodCountChangedListener listener) {
        this.countListener = listener;
        notifyCountChanged();
    }

    public void updateData(List<Food> newFoods) {
        if (newFoods != originalList) {
            originalList.clear();
            if (newFoods != null) {
                originalList.addAll(newFoods);
            }
        }
        filter(currentQuery, currentCategory);
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.replace('đ', 'd').replace('Đ', 'd').toLowerCase();
    }

    public void filter(String query, String category) {
        currentQuery = query == null ? "" : query;
        currentCategory = category == null ? "Tất cả" : category;
        displayedList.clear();

        for (Food f : originalList) {
            if (matchesQuery(f) && matchesAvailability(f) && matchesCategory(f, currentCategory)) {
                displayedList.add(f);
            }
        }
        notifyDataSetChanged();
        notifyCountChanged();
    }

    private boolean matchesQuery(Food food) {
        String rawQuery = currentQuery.trim().toLowerCase();
        if (rawQuery.isEmpty()) return true;

        String normalizedQuery = stripAccents(rawQuery);
        String name = food.getName() == null ? "" : food.getName().toLowerCase();
        String description = food.getDescription() == null ? "" : food.getDescription().toLowerCase();
        return name.contains(rawQuery)
                || stripAccents(name).contains(normalizedQuery)
                || description.contains(rawQuery)
                || stripAccents(description).contains(normalizedQuery);
    }

    private boolean matchesAvailability(Food food) {
        return "Tất cả".equals(availabilityFilter)
                || ("Đang bán".equals(availabilityFilter) == food.isAvailable());
    }

    private boolean matchesCategory(Food food, String requestedCategory) {
        return FoodCategory.matches(food.getCategory(), requestedCategory);
    }

    private String categoryKey(String category) {
        String normalized = FoodCategory.normalize(category);
        if (FoodCategory.MAIN.equals(normalized)) return "main";
        if (FoodCategory.APPETIZER.equals(normalized)) return "appetizer";
        if (FoodCategory.DRINK.equals(normalized)) return "drink";
        if (FoodCategory.DESSERT.equals(normalized)) return "dessert";
        return "main";
    }

    private void notifyCountChanged() {
        if (countListener != null) {
            int active = 0;
            int hidden = 0;
            int main = 0;
            int app = 0;
            int drink = 0;
            int dessert = 0;
            int totalForChips = 0;
            for (Food f : originalList) {
                if (f.isAvailable()) active++;
                else hidden++;

                // Chip danh mục phản ánh đúng các món còn lại sau bộ lọc trạng thái và tìm kiếm.
                if (!matchesAvailability(f) || !matchesQuery(f)) continue;
                totalForChips++;

                String category = categoryKey(f.getCategory());
                if ("main".equals(category)) main++;
                else if ("appetizer".equals(category)) app++;
                else if ("drink".equals(category)) drink++;
                else if ("dessert".equals(category)) dessert++;
            }
            countListener.onCountChanged(active, hidden, totalForChips, main, app, drink, dessert);
        }
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_manage, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = displayedList.get(position);
        boolean isAvailable = food.isAvailable();

        // 1. Tên món & tag (Ẩn)
        holder.tvFoodName.setText(food.getName() != null ? food.getName() : "");
        holder.tvHiddenTag.setVisibility(isAvailable ? View.GONE : View.VISIBLE);

        // 2. Mô tả & Banner cảnh báo khi ẩn
        if (isAvailable) {
            holder.layoutHiddenAlert.setVisibility(View.GONE);
            if (food.getDescription() != null && !food.getDescription().trim().isEmpty()) {
                holder.tvFoodDesc.setText(food.getDescription());
                holder.tvFoodDesc.setVisibility(View.VISIBLE);
            } else {
                holder.tvFoodDesc.setVisibility(View.GONE);
            }
        } else {
            // Khi món bị ẩn, hiển thị banner cảnh báo
            holder.layoutHiddenAlert.setVisibility(View.VISIBLE);
            holder.tvFoodDesc.setVisibility(View.GONE);
        }

        // 3. Huy hiệu trạng thái trên ảnh
        if (isAvailable) {
            holder.tvBadgeStatus.setText("MỞ BÁN");
            holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_food_active);
        } else {
            holder.tvBadgeStatus.setText("Tạm ẩn");
            holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_food_hidden);
        }

        // 4. Giá & Khuyến mãi
        double newPrice = food.getNewPrice();
        double oldPrice = food.getOldPrice();
        int discount = food.getDiscountPercent();

        holder.tvFoodNewPrice.setText(currencyFormat.format(newPrice) + " đ");

        if (oldPrice > newPrice && oldPrice > 0) {
            holder.tvFoodOldPrice.setText(currencyFormat.format(oldPrice) + " đ");
            holder.tvFoodOldPrice.setPaintFlags(holder.tvFoodOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvFoodOldPrice.setVisibility(View.VISIBLE);

            holder.tvFoodDiscount.setText("-" + Math.max(discount, 1) + "%");
            holder.tvFoodDiscount.setVisibility(View.VISIBLE);
        } else {
            holder.tvFoodOldPrice.setVisibility(View.GONE);
            holder.tvFoodDiscount.setVisibility(View.GONE);
        }

        // 5. Load ảnh món
        loadFoodImage(food.getImageUrl(), holder.imgFood);

        // 6. Switch & Trạng thái phục vụ
        holder.switchAvailability.setOnCheckedChangeListener(null);
        holder.switchAvailability.setChecked(isAvailable);
        holder.tvAvailabilityLabel.setText(isAvailable ? "Đang phục vụ" : "Đang tạm ẩn");

        holder.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateFoodAvailability(food, isChecked);
        });

        // 7. Nút hành động phía dưới: Sửa / Bán lại / Xóa
        if (isAvailable) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnReactivate.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnReactivate.setVisibility(View.VISIBLE);
        }

        holder.btnEdit.setOnClickListener(v -> openEditFood(food));
        holder.btnReactivate.setOnClickListener(v -> updateFoodAvailability(food, true));
        holder.btnDelete.setOnClickListener(v -> confirmDeleteFood(food));

        // 8. Menu 3 chấm
        holder.btnOptions.setOnClickListener(v -> showPopupMenu(holder.btnOptions, food));
    }

    private void loadFoodImage(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (imageUrl.startsWith("data:image")) {
                int commaIndex = imageUrl.indexOf(',');
                if (commaIndex > 0 && commaIndex < imageUrl.length() - 1) {
                    try {
                        String base64Part = imageUrl.substring(commaIndex + 1);
                        byte[] decoded = Base64.decode(base64Part, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_search_input_gray)
                        .centerCrop()
                        .into(imageView);
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_restaurant);
        imageView.setBackgroundColor(0xFFF1F5F9);
    }

    private void updateFoodAvailability(Food food, boolean available) {
        if (restaurantId == null || restaurantId.isEmpty() || food.getFoodId() == null) {
            Toast.makeText(context, "Không xác định được nhà hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("restaurants")
                .document(restaurantId)
                .collection("foods")
                .document(food.getFoodId())
                .update("isAvailable", available)
                .addOnSuccessListener(aVoid -> {
                    food.setAvailable(available);
                    filter(currentQuery, currentCategory);
                    String msg = available ? "Đã mở bán lại món: " + food.getName() : "Đã tạm ẩn món: " + food.getName();
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Cập nhật trạng thái thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                });
    }

    private void openEditFood(Food food) {
        Intent intent = new Intent(context, EditFoodActivity.class);
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
        context.startActivity(intent);
    }

    private void confirmDeleteFood(Food food) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc chắn muốn xóa món \"" + food.getName() + "\" khỏi thực đơn?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (restaurantId.isEmpty() || food.getFoodId() == null) return;
                    FirebaseFirestore.getInstance().collection("restaurants")
                            .document(restaurantId).collection("foods").document(food.getFoodId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                originalList.remove(food);
                                filter(currentQuery, currentCategory);
                                Toast.makeText(context, "Đã xóa món thành công!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(context, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showPopupMenu(View anchor, Food food) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "Sửa món");
        popup.getMenu().add(0, 2, 1, food.isAvailable() ? "Tạm ẩn món" : "Mở bán lại");
        popup.getMenu().add(0, 3, 2, "Xóa món");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                openEditFood(food);
            } else if (item.getItemId() == 2) {
                updateFoodAvailability(food, !food.isAvailable());
            } else if (item.getItemId() == 3) {
                confirmDeleteFood(food);
            }
            return true;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return displayedList != null ? displayedList.size() : 0;
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood;
        TextView tvBadgeStatus;
        TextView tvFoodName, tvHiddenTag;
        FrameLayout btnOptions;
        TextView tvFoodDesc;
        LinearLayout layoutHiddenAlert;
        TextView tvFoodNewPrice, tvFoodOldPrice, tvFoodDiscount;
        SwitchCompat switchAvailability;
        TextView tvAvailabilityLabel;
        LinearLayout btnEdit, btnReactivate;
        FrameLayout btnDelete;

        public FoodViewHolder(@NonNull View v) {
            super(v);
            imgFood               = v.findViewById(R.id.imgFood);
            tvBadgeStatus         = v.findViewById(R.id.tvBadgeStatus);
            tvFoodName            = v.findViewById(R.id.tvFoodName);
            tvHiddenTag           = v.findViewById(R.id.tvHiddenTag);
            btnOptions            = v.findViewById(R.id.btnOptions);
            tvFoodDesc            = v.findViewById(R.id.tvFoodDesc);
            layoutHiddenAlert     = v.findViewById(R.id.layoutHiddenAlert);
            tvFoodNewPrice        = v.findViewById(R.id.tvFoodNewPrice);
            tvFoodOldPrice        = v.findViewById(R.id.tvFoodOldPrice);
            tvFoodDiscount        = v.findViewById(R.id.tvFoodDiscount);
            switchAvailability    = v.findViewById(R.id.switchAvailability);
            tvAvailabilityLabel   = v.findViewById(R.id.tvAvailabilityLabel);
            btnEdit               = v.findViewById(R.id.btnEdit);
            btnReactivate         = v.findViewById(R.id.btnReactivate);
            btnDelete             = v.findViewById(R.id.btnDelete);
        }
    }
}
