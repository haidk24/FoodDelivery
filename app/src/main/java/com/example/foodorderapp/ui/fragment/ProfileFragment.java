package com.example.foodorderapp.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.example.foodorderapp.ui.MainActivity;
import com.example.foodorderapp.ui.SignInActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileFragment extends Fragment {

    private ImageButton btnBack;
    private ImageView imgProfileAvatar;
    private MaterialCardView btnChangeAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfilePhone;
    private MaterialCardView btnEditProfile;
    private TextView tvTotalOrdersCount;
    private MaterialCardView cardRegisterRestaurant;
    private LinearLayout layoutMyOrders;
    private LinearLayout badgeShippingContainer;
    private TextView tvBadgeShippingText;
    private LinearLayout layoutDeliveryAddress;
    private TextView tvProfileAddress;
    private LinearLayout layoutChangePassword;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private SharedPreferences sharedPreferences;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        if (getContext() != null) {
            sharedPreferences = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        }

        initViews(view);
        setupListeners();
        loadUserProfile();
        loadOrdersStats();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        imgProfileAvatar = view.findViewById(R.id.img_profile_avatar);
        btnChangeAvatar = view.findViewById(R.id.btn_change_avatar);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfilePhone = view.findViewById(R.id.tv_profile_phone);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        tvTotalOrdersCount = view.findViewById(R.id.tv_total_orders_count);
        cardRegisterRestaurant = view.findViewById(R.id.card_register_restaurant);
        layoutMyOrders = view.findViewById(R.id.layout_my_orders);
        badgeShippingContainer = view.findViewById(R.id.badge_shipping_container);
        tvBadgeShippingText = view.findViewById(R.id.tv_badge_shipping_text);
        layoutDeliveryAddress = view.findViewById(R.id.layout_delivery_address);
        tvProfileAddress = view.findViewById(R.id.tv_profile_address);
        layoutChangePassword = view.findViewById(R.id.layout_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng đổi ảnh đại diện đang được hoàn thiện", Toast.LENGTH_SHORT).show();
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        cardRegisterRestaurant.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new RegisterRestaurantFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        layoutMyOrders.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                com.google.android.material.bottomnavigation.BottomNavigationView bnv =
                        mainActivity.findViewById(R.id.bottom_navigation);
                if (bnv != null) {
                    bnv.setSelectedItemId(R.id.nav_myorder);
                } else {
                    mainActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new MyOrderFragment())
                            .commit();
                }
            } else if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new MyOrderFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        layoutDeliveryAddress.setOnClickListener(v -> showEditAddressDialog());

        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Default from Firebase Auth
        String name = user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = "Người dùng";
        }
        tvProfileName.setText(name);

        String email = user.getEmail();
        tvProfileEmail.setText(email != null ? email : "Chưa cập nhật email");

        if (user.getPhotoUrl() != null && getContext() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgProfileAvatar);
        }

        // Load saved address
        if (sharedPreferences != null) {
            String savedAddress = sharedPreferences.getString("delivery_address", "Cầu Giấy, Hà Nội (Mặc định)");
            tvProfileAddress.setText(savedAddress);
            String savedPhone = sharedPreferences.getString("user_phone", "");
            if (!savedPhone.isEmpty()) {
                tvProfilePhone.setText(savedPhone);
            }
        }

        // Fetch user doc from Firestore
        mFirestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firestoreName = doc.getString("displayName");
                        if (firestoreName != null && !firestoreName.isEmpty()) {
                            tvProfileName.setText(firestoreName);
                        }
                        String phone = doc.getString("phone");
                        if (phone != null && !phone.isEmpty()) {
                            tvProfilePhone.setText(phone);
                        }
                        String address = doc.getString("address");
                        if (address != null && !address.isEmpty()) {
                            tvProfileAddress.setText(address);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Ignore errors, keep auth fallback
                });
    }

    private void loadOrdersStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        mFirestore.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int completed = 0;
                    int shipping = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if (status != null) {
                            String s = status.toLowerCase();
                            if (s.contains("hoàn tất") || s.contains("completed") || s.contains("thành công") || s.contains("giao thành công")) {
                                completed++;
                            } else if (s.contains("đang giao") || s.contains("shipping") || s.contains("dang_giao")) {
                                shipping++;
                            }
                        }
                    }

                    // Fallback to total documents if completed == 0 for presentation
                    if (completed == 0 && !queryDocumentSnapshots.isEmpty()) {
                        completed = queryDocumentSnapshots.size();
                    }

                    tvTotalOrdersCount.setText(String.valueOf(completed));

                    if (shipping > 0) {
                        badgeShippingContainer.setVisibility(View.VISIBLE);
                        tvBadgeShippingText.setText(shipping + " đang giao");
                    } else {
                        badgeShippingContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvTotalOrdersCount.setText("0");
                    badgeShippingContainer.setVisibility(View.GONE);
                });
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chỉnh sửa thông tin");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText etName = new EditText(requireContext());
        etName.setHint("Họ và tên");
        etName.setText(tvProfileName.getText());
        layout.addView(etName);

        final EditText etPhone = new EditText(requireContext());
        etPhone.setHint("Số điện thoại");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setText(tvProfilePhone.getText().toString().equals("0987 654 321") ? "" : tvProfilePhone.getText());
        layout.addView(etPhone);

        builder.setView(layout);

        builder.setPositiveButton("Lưu thay đổi", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build());

                mFirestore.collection("users").document(user.getUid())
                        .update("displayName", newName, "phone", newPhone);
            }

            tvProfileName.setText(newName);
            if (!newPhone.isEmpty()) {
                tvProfilePhone.setText(newPhone);
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putString("user_phone", newPhone).apply();
                }
            }

            Toast.makeText(getContext(), "Đã cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showEditAddressDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Địa chỉ giao hàng");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText etAddress = new EditText(requireContext());
        etAddress.setHint("Nhập địa chỉ giao hàng mới");
        etAddress.setText(tvProfileAddress.getText());
        layout.addView(etAddress);

        builder.setView(layout);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String newAddress = etAddress.getText().toString().trim();
            if (!newAddress.isEmpty()) {
                tvProfileAddress.setText(newAddress);
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putString("delivery_address", newAddress).apply();
                }

                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    mFirestore.collection("users").document(user.getUid())
                            .update("address", newAddress);
                }

                Toast.makeText(getContext(), "Đã cập nhật địa chỉ giao hàng!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Đổi mật khẩu");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText etNewPassword = new EditText(requireContext());
        etNewPassword.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        final EditText etConfirmPassword = new EditText(requireContext());
        etConfirmPassword.setHint("Xác nhận mật khẩu mới");
        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.length() < 6) {
                Toast.makeText(getContext(), "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(newPass)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Lỗi đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showLogoutConfirmDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất tài khoản")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng Food Order Pro?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    mAuth.signOut();
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), SignInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
