package com.example.foodorderapp.ui.customer.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<Food> cartItems;
    private final OnCartItemClickListener listener;

    public interface OnCartItemClickListener {
        void onClickMinusFood(Food food, int position);
        void onClickPlusFood(Food food, int position);
        void onClickDeleteFood(Food food, int position);
    }

    public CartAdapter(List<Food> cartItems, OnCartItemClickListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    public void updateList(List<Food> newList) {
        this.cartItems = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Food food = cartItems.get(position);
        if (food == null) {
            return;
        }

        Glide.with(holder.imgFood.getContext())
                .load(food.getImageUrl())
                .placeholder(R.drawable.banh)
                .error(R.drawable.banh)
                .into(holder.imgFood);

        holder.tvFoodName.setText(food.getName());

        if (!TextUtils.isEmpty(food.getDescription())) {
            holder.tvFoodDesc.setVisibility(View.VISIBLE);
            holder.tvFoodDesc.setText(food.getDescription());
        } else {
            holder.tvFoodDesc.setVisibility(View.GONE);
        }

        holder.tvPrice.setText(String.format(Locale.getDefault(), "%,.0f đ", food.getNewPrice()));
        holder.tvCount.setText(String.valueOf(food.getCount()));

        holder.layoutMinus.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onClickMinusFood(food, currentPos);
            }
        });

        holder.layoutPlus.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onClickPlusFood(food, currentPos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onClickDeleteFood(food, currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgFood;
        private final TextView tvFoodName;
        private final TextView tvFoodDesc;
        private final TextView tvPrice;
        private final TextView tvCount;
        private final View layoutMinus;
        private final View layoutPlus;
        private final View btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.img_food);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDesc = itemView.findViewById(R.id.tv_food_desc);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvCount = itemView.findViewById(R.id.tv_count);
            layoutMinus = itemView.findViewById(R.id.layout_minus);
            layoutPlus = itemView.findViewById(R.id.layout_plus);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
