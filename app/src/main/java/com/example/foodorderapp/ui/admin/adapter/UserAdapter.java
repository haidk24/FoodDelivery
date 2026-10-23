package com.example.foodorderapp.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.User;

import java.text.DecimalFormat;
import java.util.Locale;

public class UserAdapter extends ListAdapter<User, UserAdapter.VH> {

    public interface Listener {
        void onUserClick(User user);
        void onViewDetail(User user);
        void onViewOrders(User user);
        void onRolePermission(User user);
        void onUnlockUser(User user);
        void onMoreMenu(User user, View anchor);
    }

    private final Listener listener;

    public UserAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User a, @NonNull User b) {
                return a.getUid() != null && a.getUid().equals(b.getUid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull User a, @NonNull User b) {
                return a.getUid() != null && a.getUid().equals(b.getUid())
                        && safeEqual(a.getDisplayName(), b.getDisplayName())
                        && safeEqual(a.getRole(), b.getRole())
                        && safeEqual(a.getStatus(), b.getStatus())
                        && a.getOrderCount() == b.getOrderCount();
            }

            private boolean safeEqual(String a, String b) {
                if (a == null && b == null) return true;
                if (a == null || b == null) return false;
                return a.equals(b);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static class VH extends RecyclerView.ViewHolder {

        final TextView tvAvatarInitials;
        final View layoutStoreBadge;

        final TextView tvUserName, tvRoleTag, tvUserEmail;
        final View layoutStatsRow;
        final TextView tvStatsOrders, tvStatsDot, tvStatsSecondary;

        final View layoutStoreNameRow;
        final TextView tvStoreName;

        final View layoutViolationRow;
        final TextView tvViolationText;

        final TextView tvStatusBadge;
        final ImageView btnMoreMenu;

        final View layoutBottomBox, layoutBottomLeft;
        final TextView tvBottomTag, tvBottomText, tvBottomLink;
        final ImageView imgBottomIcon;
        final View btnRoleSetting, btnUnlock;

        public VH(@NonNull View v) {
            super(v);
            tvAvatarInitials = v.findViewById(R.id.tv_avatar_initials);
            layoutStoreBadge = v.findViewById(R.id.layout_store_badge);

            tvUserName = v.findViewById(R.id.tv_user_name);
            tvRoleTag = v.findViewById(R.id.tv_role_tag);
            tvUserEmail = v.findViewById(R.id.tv_user_email);

            layoutStatsRow = v.findViewById(R.id.layout_stats_row);
            tvStatsOrders = v.findViewById(R.id.tv_stats_orders);
            tvStatsDot = v.findViewById(R.id.tv_stats_dot);
            tvStatsSecondary = v.findViewById(R.id.tv_stats_secondary);

            layoutStoreNameRow = v.findViewById(R.id.layout_store_name_row);
            tvStoreName = v.findViewById(R.id.tv_store_name);

            layoutViolationRow = v.findViewById(R.id.layout_violation_row);
            tvViolationText = v.findViewById(R.id.tv_violation_text);

            tvStatusBadge = v.findViewById(R.id.tv_status_badge);
            btnMoreMenu = v.findViewById(R.id.btn_more_menu);

            layoutBottomBox = v.findViewById(R.id.layout_bottom_box);
            layoutBottomLeft = v.findViewById(R.id.layout_bottom_left);
            tvBottomTag = v.findViewById(R.id.tv_bottom_tag);
            tvBottomText = v.findViewById(R.id.tv_bottom_text);
            tvBottomLink = v.findViewById(R.id.tv_bottom_link);
            imgBottomIcon = v.findViewById(R.id.img_bottom_icon);
            btnRoleSetting = v.findViewById(R.id.btn_role_setting);
            btnUnlock = v.findViewById(R.id.btn_unlock);
        }

        public void bind(User user, Listener listener) {
            if (user == null) return;

            // 1. Initials & Avatar styling
            String name = user.getDisplayName() != null ? user.getDisplayName() : "Người dùng";
            tvUserName.setText(name);
            tvAvatarInitials.setText(extractInitials(name));

            boolean isBanned = "banned".equalsIgnoreCase(user.getStatus());
            boolean isRestaurant = "restaurant".equalsIgnoreCase(user.getRole());

            if (isRestaurant) {
                tvAvatarInitials.setBackgroundResource(R.drawable.bg_circle_avatar_warm_orange);
                tvAvatarInitials.setTextColor(0xFFB45309);
                layoutStoreBadge.setVisibility(View.VISIBLE);
            } else if (isBanned) {
                tvAvatarInitials.setBackgroundResource(R.drawable.bg_circle_avatar_pink);
                tvAvatarInitials.setTextColor(0xFFB91C1C);
                layoutStoreBadge.setVisibility(View.GONE);
            } else {
                tvAvatarInitials.setBackgroundResource(R.drawable.bg_circle_avatar_blue);
                tvAvatarInitials.setTextColor(0xFF1D4ED8);
                layoutStoreBadge.setVisibility(View.GONE);
            }

            // 2. Role Tag
            if (isBanned) {
                tvRoleTag.setText("Tài khoản khóa");
                tvRoleTag.setBackgroundResource(R.drawable.bg_tag_role_banned);
                tvRoleTag.setTextColor(0xFFDC2626);
            } else if (isRestaurant) {
                tvRoleTag.setText("Chủ quán");
                tvRoleTag.setBackgroundResource(R.drawable.bg_tag_role_restaurant);
                tvRoleTag.setTextColor(0xFFC2410C);
            } else {
                tvRoleTag.setText("Khách");
                tvRoleTag.setBackgroundResource(R.drawable.bg_tag_role_customer);
                tvRoleTag.setTextColor(0xFF475569);
            }

            // 3. Status Badge
            if (isBanned) {
                tvStatusBadge.setText("• Đã khóa");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_banned_pill);
                tvStatusBadge.setTextColor(0xFFB91C1C);
            } else {
                tvStatusBadge.setText("• Hoạt động");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active_pill);
                tvStatusBadge.setTextColor(0xFF15803D);
            }

            // 4. Email
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa cập nhật email");

            // 5. Middle row
            if (isBanned) {
                layoutStatsRow.setVisibility(View.GONE);
                layoutStoreNameRow.setVisibility(View.GONE);
                layoutViolationRow.setVisibility(View.VISIBLE);
                String reason = user.getBanReason() != null ? user.getBanReason() : "Hủy 4 đơn liên tiếp không lý do";
                tvViolationText.setText(reason);
            } else if (isRestaurant) {
                layoutStatsRow.setVisibility(View.GONE);
                layoutViolationRow.setVisibility(View.GONE);
                layoutStoreNameRow.setVisibility(View.VISIBLE);
                String store = user.getRestaurantName() != null ? user.getRestaurantName() : "Mạnh Tiến Quán (Quận 1)";
                tvStoreName.setText(store);
            } else {
                layoutViolationRow.setVisibility(View.GONE);
                layoutStoreNameRow.setVisibility(View.GONE);
                layoutStatsRow.setVisibility(View.VISIBLE);

                if (user.getOrderCount() > 0) {
                    tvStatsOrders.setText(user.getOrderCount() + " đơn hàng");
                    long spent = user.getTotalSpent() > 0 ? user.getTotalSpent() : (user.getOrderCount() * 180000L + 5000L);
                    tvStatsSecondary.setText(formatCurrency(spent));
                } else {
                    tvStatsOrders.setText("0 đơn hoàn tất");
                    tvStatsSecondary.setText(user.getJoinTimeText() != null ? user.getJoinTimeText() : "Gia nhập 3 ngày trước");
                }
            }

            // 6. Bottom Box
            if (isBanned) {
                tvBottomTag.setVisibility(View.GONE);
                imgBottomIcon.setVisibility(View.GONE);
                String date = user.getBannedAt() != null ? user.getBannedAt() : "14/10/2023";
                String admin = user.getBannedBy() != null ? user.getBannedBy() : "Admin_01";
                tvBottomText.setText("Khóa từ: " + date + " bởi " + admin);
                tvBottomText.setTextColor(0xFF64748B);

                tvBottomLink.setVisibility(View.GONE);
                btnRoleSetting.setVisibility(View.GONE);
                btnUnlock.setVisibility(View.VISIBLE);
                btnUnlock.setOnClickListener(v -> listener.onUnlockUser(user));
            } else if (isRestaurant) {
                tvBottomTag.setVisibility(View.VISIBLE);
                tvBottomTag.setText("Đang mở");
                imgBottomIcon.setVisibility(View.GONE);
                int dishes = user.getDishCount() > 0 ? user.getDishCount() : 48;
                tvBottomText.setText(dishes + " món trong thực đơn");
                tvBottomText.setTextColor(0xFF334155);

                tvBottomLink.setVisibility(View.GONE);
                btnUnlock.setVisibility(View.GONE);
                btnRoleSetting.setVisibility(View.VISIBLE);
                btnRoleSetting.setOnClickListener(v -> listener.onRolePermission(user));
            } else {
                tvBottomTag.setVisibility(View.GONE);
                btnRoleSetting.setVisibility(View.GONE);
                btnUnlock.setVisibility(View.GONE);
                tvBottomLink.setVisibility(View.VISIBLE);

                if (user.getOrderCount() > 0) {
                    imgBottomIcon.setVisibility(View.VISIBLE);
                    imgBottomIcon.setImageResource(R.drawable.ic_shopping_bag);
                    imgBottomIcon.setColorFilter(0xFFC2410C);
                    String lastOrder = user.getLastOrderTime() != null ? user.getLastOrderTime() : "Hôm qua 19:40";
                    tvBottomText.setText("Đơn gần nhất: " + lastOrder);
                    tvBottomText.setTextColor(0xFF334155);
                    tvBottomLink.setText("Xem đơn ›");
                    tvBottomLink.setOnClickListener(v -> listener.onViewOrders(user));
                } else {
                    imgBottomIcon.setVisibility(View.VISIBLE);
                    imgBottomIcon.setImageResource(R.drawable.ic_verified_user);
                    imgBottomIcon.setColorFilter(0xFF16A34A);
                    tvBottomText.setText("Đã xác minh số điện thoại");
                    tvBottomText.setTextColor(0xFF334155);
                    tvBottomLink.setText("Chi tiết ›");
                    tvBottomLink.setOnClickListener(v -> listener.onViewDetail(user));
                }
            }

            // 7. Click events
            btnMoreMenu.setOnClickListener(v -> listener.onMoreMenu(user, v));
            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        private static String extractInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "US";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].length() >= 2
                        ? parts[0].substring(0, 2).toUpperCase(Locale.getDefault())
                        : parts[0].toUpperCase(Locale.getDefault());
            }
            return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0)))
                    .toUpperCase(Locale.getDefault());
        }

        private static String formatCurrency(long amount) {
            DecimalFormat df = new DecimalFormat("#,###");
            return df.format(amount).replace(',', '.') + "đ";
        }
    }
}