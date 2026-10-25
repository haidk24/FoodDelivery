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
                if (tvCategory != null) tvCategory.setText("Nhà hàng");
                if (tvRegisteredTime != null) {
                    tvRegisteredTime.setText("Đăng ký: " + formatCreatedAt(r.createdAt));
                }
                if (tvOwnerInfo != null) {
                    String phone = r.getPhone().isEmpty() ? "Chưa cập nhật" : r.getPhone();
                    tvOwnerInfo.setText("Mã chủ quán: " + (r.ownerId == null || r.ownerId.isEmpty() ? "Chưa cập nhật" : r.ownerId) + " • " + phone);
                }
                if (tvEmailInfo != null) {
                    tvEmailInfo.setText("Email chủ quán chưa có trong hồ sơ nhà hàng");
                }
                if (tvTagLicense != null) {
                    tvTagLicense.setText(r.hasLicence ? "Đã cập nhật giấy phép" : "Chưa xác nhận giấy phép");
                }
                if (tvTagDishes != null) {
                    tvTagDishes.setText("Số món xem trong chi tiết");
                }

                if (btnApprove != null) btnApprove.setOnClickListener(v -> listener.onApprove(r));
                if (btnReject != null) btnReject.setOnClickListener(v -> listener.onSuspend(r));

            } else if (type == TYPE_ACTIVE) {
                if (tvRating != null) {
                    tvRating.setText(r.rating > 0 ? String.format(Locale.getDefault(), "★ %.1f", r.rating) : "Chưa có đánh giá");
                }
                if (tvReviewsAndDishes != null) {
                    tvReviewsAndDishes.setText("Chưa có số liệu món trong hồ sơ");
                }
                if (tvOrdersToday != null) tvOrdersToday.setText("—");
                if (tvRevenueToday != null) tvRevenueToday.setText("—");
                if (tvCancelRate != null) tvCancelRate.setText("—");

                if (btnSuspend != null) btnSuspend.setOnClickListener(v -> listener.onSuspend(r));

            } else {
                if (tvPauseReason != null) {
                    tvPauseReason.setText("Nhà hàng đang tạm ngưng");
                }
                if (btnReinstate != null) btnReinstate.setOnClickListener(v -> listener.onReinstate(r));
            }

            if (btnDetail != null) {
                btnDetail.setOnClickListener(v -> listener.onViewDetail(r));
            }

            itemView.setOnClickListener(v -> listener.onViewDetail(r));
        }

        private String formatCreatedAt(Object value) {
            java.util.Date date = value instanceof com.google.firebase.Timestamp
                    ? ((com.google.firebase.Timestamp) value).toDate()
                    : value instanceof java.util.Date ? (java.util.Date) value : null;
            return date == null ? "Chưa cập nhật" : new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        }
    }
}
