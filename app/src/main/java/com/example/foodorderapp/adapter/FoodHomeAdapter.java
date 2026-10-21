package com.example.foodorderapp.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * FoodHomeAdapter — Row-style food item adapter for Home screen.
 * Matches the Stitch "Khám phá món ngon" design:
 * - Food image (88dp) on the right
 * - Name, description, price on the left
 * - Discount / Bestseller badge
 * - FAB add-to-cart button overlaid on image (bottom-right)
 */
public class FoodHomeAdapter extends RecyclerView.Adapter<FoodHomeAdapter.FoodHomeViewHolder> {

    private List<Food> mListFood;
    private final OnFoodActionListener listener;

    /** Combined listener for item click and add-to-cart */
    public interface OnFoodActionListener {
        void onFoodClick(Food food);
        void onAddToCart(Food food);
    }

    public FoodHomeAdapter(List<Food> foods, OnFoodActionListener listener) {
        this.mListFood = foods;
        this.listener = listener;
    }

    /** Update list and refresh */
    public void setData(List<Food> foods) {
        this.mListFood = foods;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodHomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_home, parent, false);
        return new FoodHomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodHomeViewHolder holder, int position) {
        Food food = mListFood.get(position);
        if (food == null) return;

        // ── Food name ──────────────────────────────────────────────
        holder.tvFoodName.setText(food.getName());

        // ── Description ────────────────────────────────────────────
        String desc = food.getDescription();
        if (desc != null && !desc.isEmpty()) {
            holder.tvFoodDescription.setText(desc);
            holder.tvFoodDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvFoodDescription.setVisibility(View.GONE);
        }

        // ── Price ──────────────────────────────────────────────────
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        double newPrice = food.getNewPrice();
        double oldPrice = food.getOldPrice();

        holder.tvNewPrice.setText(fmt.format(newPrice) + " đ");

        if (oldPrice > newPrice && oldPrice > 0) {
            holder.tvOldPrice.setText(fmt.format(oldPrice) + " đ");
            holder.tvOldPrice.setPaintFlags(
                    holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvOldPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvOldPrice.setVisibility(View.GONE);
        }

        // ── Discount / Bestseller badge ────────────────────────────
        int discount = food.getDiscountPercent();
        boolean isBestseller = food.getOrderCount() >= 10; // threshold for bestseller badge

        if (discount > 0) {
            holder.tvDiscountBadge.setText("-" + discount + "%");
            holder.tvDiscountBadge.setVisibility(View.VISIBLE);
            holder.tvBestsellerBadge.setVisibility(View.GONE);
        } else if (isBestseller) {
            holder.tvBestsellerBadge.setVisibility(View.VISIBLE);
            holder.tvDiscountBadge.setVisibility(View.GONE);
        } else {
            holder.tvDiscountBadge.setVisibility(View.GONE);
            holder.tvBestsellerBadge.setVisibility(View.GONE);
        }

        // ── Food image ─────────────────────────────────────────────
        Glide.with(holder.imgFood.getContext())
                .load(food.getImageUrl())
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .placeholder(R.drawable.banh)
                .error(R.drawable.banh)
                .centerCrop()
                .into(holder.imgFood);

        // ── Click listeners ────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFoodClick(food);
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            // Spring micro-animation matching Stitch JS behavior
            ScaleAnimation scale = new ScaleAnimation(
                    1f, 0.8f, 1f, 0.8f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            scale.setDuration(100);
            scale.setRepeatCount(1);
            scale.setRepeatMode(ScaleAnimation.REVERSE);
            v.startAnimation(scale);

            if (listener != null) listener.onAddToCart(food);
        });
    }

    @Override
    public int getItemCount() {
        return mListFood == null ? 0 : mListFood.size();
    }

    public static class FoodHomeViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgFood;
        final TextView tvFoodName;
        final TextView tvFoodDescription;
        final TextView tvNewPrice;
        final TextView tvOldPrice;
        final TextView tvDiscountBadge;
        final TextView tvBestsellerBadge;
        final View btnAddToCart;

        public FoodHomeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.img_food);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDescription = itemView.findViewById(R.id.tv_food_description);
            tvNewPrice = itemView.findViewById(R.id.tv_new_price);
            tvOldPrice = itemView.findViewById(R.id.tv_old_price);
            tvDiscountBadge = itemView.findViewById(R.id.tv_discount_badge);
            tvBestsellerBadge = itemView.findViewById(R.id.tv_bestseller_badge);
            btnAddToCart = itemView.findViewById(R.id.btn_add_to_cart);
        }
    }
}
