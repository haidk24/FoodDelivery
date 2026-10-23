package com.example.foodorderapp.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Restaurant;

import java.util.Locale;

public class RestaurantAdapter
        extends ListAdapter<Restaurant, RestaurantAdapter.VH> {

    public interface Listener {
        void onApprove(Restaurant r);    // pending → active
        void onSuspend(Restaurant r);    // active → suspended
        void onReinstate(Restaurant r);  // suspended → active
        void onViewDetail(Restaurant r);
    }

    // 3 loại view đúng với 3 status trong schema
    public static final int TYPE_PENDING   = 0;
    public static final int TYPE_ACTIVE    = 1;
    public static final int TYPE_SUSPENDED = 2;

    private final Listener listener;
    private final int      fallbackViewType;

    public RestaurantAdapter(Listener listener, int fallbackViewType) {
        super(new DiffUtil.ItemCallback<Restaurant>() {
            @Override public boolean areItemsTheSame(
                    @NonNull Restaurant a, @NonNull Restaurant b) {
                return a.restaurantId != null && a.restaurantId.equals(b.restaurantId);
            }
            @Override public boolean areContentsTheSame(
                    @NonNull Restaurant a, @NonNull Restaurant b) {
                return a.restaurantId != null && a.restaurantId.equals(b.restaurantId)
                        && safeEq(a.status, b.status)
                        && safeEq(a.name, b.name)
                        && safeEq(a.imageUrl, b.imageUrl);
            }
            private boolean safeEq(String a, String b) {
                return a == null ? b == null : a.equals(b);
            }
        });
        this.listener = listener;
        this.fallbackViewType = fallbackViewType;
    }

    @Override
    public int getItemViewType(int pos) {
        Restaurant r = getItem(pos);
        if (r != null && r.status != null) {
            if ("pending".equalsIgnoreCase(r.status)) return TYPE_PENDING;
            if ("suspended".equalsIgnoreCase(r.status)) return TYPE_SUSPENDED;
            if ("active".equalsIgnoreCase(r.status)) return TYPE_ACTIVE;
        }
        return fallbackViewType;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        int layout;
        switch (type) {
            case TYPE_PENDING:   layout = R.layout.item_restaurant_pending;   break;
            case TYPE_SUSPENDED: layout = R.layout.item_restaurant_suspended; break;
            default:             layout = R.layout.item_restaurant_active;    break;
        }
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false), type);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(getItem(pos), listener);
    }

    // ── ViewHolder ────────────────────────────────────────────────
    public static class VH extends RecyclerView.ViewHolder {

        final int type;

        // Views dùng chung
        TextView tvName, tvAddress, tvRating;
        ImageView imgRestaurant;
        View btnDetail;

        // Pending
        TextView tvCategory, tvRegisteredTime, tvOwnerInfo, tvEmailInfo, tvTagLicense, tvTagDishes;
        View btnApprove, btnReject;

        // Active
        TextView tvReviewsAndDishes, tvOrdersToday, tvRevenueToday, tvCancelRate;
        View btnSuspend;

        // Suspended
        TextView tvPauseReason;
        View btnReinstate;

        VH(View v, int type) {
            super(v);
            this.type = type;

            tvName        = v.findViewById(R.id.tv_rest_name);
            tvAddress     = v.findViewById(R.id.tv_rest_address);
            tvRating      = v.findViewById(R.id.tv_rating);
            imgRestaurant = v.findViewById(R.id.img_restaurant);
            btnDetail     = v.findViewById(R.id.btn_detail);

            if (type == TYPE_PENDING) {
                tvCategory       = v.findViewById(R.id.tv_rest_category);
                tvRegisteredTime = v.findViewById(R.id.tv_registered_time);
                tvOwnerInfo      = v.findViewById(R.id.tv_owner_info);
                tvEmailInfo      = v.findViewById(R.id.tv_email_info);
                tvTagLicense     = v.findViewById(R.id.tv_tag_license);
                tvTagDishes      = v.findViewById(R.id.tv_tag_dishes);
                btnApprove       = v.findViewById(R.id.btn_approve);
                btnReject        = v.findViewById(R.id.btn_reject);
            } else if (type == TYPE_ACTIVE) {
                tvReviewsAndDishes = v.findViewById(R.id.tv_reviews_and_dishes);
                tvOrdersToday      = v.findViewById(R.id.tv_orders_today);
                tvRevenueToday     = v.findViewById(R.id.tv_revenue_today);
                tvCancelRate       = v.findViewById(R.id.tv_cancel_rate);
                btnSuspend         = v.findViewById(R.id.btn_suspend);
            } else {
                tvPauseReason = v.findViewById(R.id.tv_pause_reason);
                btnReinstate  = v.findViewById(R.id.btn_reinstate);
            }
        }

        void bind(Restaurant r, Listener listener) {
            if (tvName != null) {
                tvName.setText(r.name != null ? r.name : "—");
            }

            if (tvAddress != null) {
                String addr = r.getAddressText();
                if (type == TYPE_ACTIVE || type == TYPE_SUSPENDED) {
                    if (r.phone != null && !r.phone.isEmpty()) {
                        addr = addr + " • " + r.phone;
                    }
                }
                tvAddress.setText(addr);
            }

            // Load ảnh nhà hàng
            if (imgRestaurant != null) {
                Glide.with(itemView.getContext())
                        .load(r.imageUrl)
                        .placeholder(R.drawable.banghoa)
                        .error(R.drawable.banghoa)
                        .centerCrop()
                        .into(imgRestaurant);
            }

            if (type == TYPE_PENDING) {
                if (tvCategory != null) {
                    tvCategory.setText("Món Việt & Cơm trưa văn phòng");
                }
                if (tvRegisteredTime != null) {
                    tvRegisteredTime.setText("Đăng ký: 25 phút trước");
                }
                if (tvOwnerInfo != null) {
                    String phone = (r.phone != null && !r.phone.isEmpty()) ? r.phone : "0987 654 321";
                    tvOwnerInfo.setText("Chủ quán: " + (r.name != null ? r.name : "Vũ Tiến Mạnh") + " • " + phone);
                }
                if (tvEmailInfo != null) {
                    String emailPrefix = (r.name != null) ? r.name.toLowerCase().replaceAll("[^a-z0-9]", "") : "tienmanh";
                    if (emailPrefix.isEmpty()) emailPrefix = "doitac";
                    tvEmailInfo.setText(emailPrefix + "@foodmail.vn");
                }
                if (tvTagLicense != null) {
                    tvTagLicense.setText("Đủ GPKD & VSATTP");
                }
                if (tvTagDishes != null) {
                    int dishes = (r.foods != null && !r.foods.isEmpty()) ? r.foods.size() : 32;
                    tvTagDishes.setText(dishes + " món đăng ký");
                }

                if (btnApprove != null) btnApprove.setOnClickListener(v -> listener.onApprove(r));
                if (btnReject != null) btnReject.setOnClickListener(v -> listener.onSuspend(r));

            } else if (type == TYPE_ACTIVE) {
                if (tvRating != null) {
                    tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", r.rating > 0 ? r.rating : 4.8));
                }
                if (tvReviewsAndDishes != null) {
                    int dishes = (r.foods != null && !r.foods.isEmpty()) ? r.foods.size() : 45;
                    tvReviewsAndDishes.setText("• 128 đánh giá • " + dishes + " món");
                }
                if (tvOrdersToday != null) tvOrdersToday.setText("64");
                if (tvRevenueToday != null) tvRevenueToday.setText("8.4M");
                if (tvCancelRate != null) tvCancelRate.setText("0.8%");

                if (btnSuspend != null) btnSuspend.setOnClickListener(v -> listener.onSuspend(r));

            } else {
                if (tvPauseReason != null) {
                    tvPauseReason.setText("Tự đóng cửa: Sửa chữa mặt bằng (đến 15/11)");
                }
                if (btnReinstate != null) btnReinstate.setOnClickListener(v -> listener.onReinstate(r));
            }

            if (btnDetail != null) {
                btnDetail.setOnClickListener(v -> listener.onViewDetail(r));
            }

            itemView.setOnClickListener(v -> listener.onViewDetail(r));
        }
    }
}