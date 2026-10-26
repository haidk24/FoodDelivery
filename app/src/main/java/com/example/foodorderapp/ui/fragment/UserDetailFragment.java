package com.example.foodorderapp.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.feature.admin.users.AdminUsersViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDetailFragment extends Fragment {

    private AdminUsersViewModel viewModel;
    private User currentUser;

    // Views
    private TextView tvInitials, tvName, tvEmail, tvStatusBadge;
    private TextView tvPhone, tvRole, tvCreatedAt, tvOrderCount;
    private Button   btnBanUnban;
    private ImageButton btnBack, btnEditUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fragment_user_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(AdminUsersViewModel.class);

        bindViews(view);
        setupListeners(view);
        observeData();

        // Lấy uid từ arguments (Navigation Safe Args)
        if (getArguments() != null) {
            String uid = getArguments().getString("uid", "");
            if (!uid.isEmpty()) {
                viewModel.loadDetail(uid);
            }
        }
    }

    private void bindViews(View v) {
        btnBack      = v.findViewById(R.id.btn_back);
        btnEditUser  = v.findViewById(R.id.btn_edit_user);
        tvInitials   = v.findViewById(R.id.tv_initials);
        tvName       = v.findViewById(R.id.tv_name);
        tvEmail      = v.findViewById(R.id.tv_email);
        tvStatusBadge = v.findViewById(R.id.tv_status_badge);
        tvPhone      = v.findViewById(R.id.tv_phone);
        tvRole       = v.findViewById(R.id.tv_role);
        tvCreatedAt  = v.findViewById(R.id.tv_created_at);
        tvOrderCount = v.findViewById(R.id.tv_order_count);
        btnBanUnban  = v.findViewById(R.id.btn_ban_unban);
    }

    private void setupListeners(View view) {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        if (btnEditUser != null) {
            btnEditUser.setOnClickListener(v -> {
                if (currentUser != null) {
                    showEditUserDialog(currentUser);
                }
            });
        }
    }

    private void observeData() {
        viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                renderUser(user);
            }
        });

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(),
                        "Thao tác thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderUser(User user) {
        tvInitials.setText(getInitials(user.getDisplayName()));
        tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "—");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        tvPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa cập nhật");
        tvRole.setText(mapRole(user.getRole()));
        tvOrderCount.setText(user.getOrderCount() + " đơn");
        tvCreatedAt.setText(formatDate(user.getCreatedAt()));

        boolean isBanned = "banned".equals(user.getStatus());
        if (isBanned) {
            tvStatusBadge.setText("Bị khóa");
            tvStatusBadge.setTextColor(0xFFFF3B30);
            btnBanUnban.setText("Mở khóa tài khoản");
            btnBanUnban.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFE8F5EC));
            btnBanUnban.setTextColor(0xFF1E6B3A);
        } else if ("active".equalsIgnoreCase(user.getStatus())) {
            tvStatusBadge.setText("Hoạt động");
            tvStatusBadge.setTextColor(0xFF34C759);
            btnBanUnban.setText("Khóa tài khoản");
            btnBanUnban.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFDECEA));
            btnBanUnban.setTextColor(0xFF8B1A1A);
        } else {
            tvStatusBadge.setText("Chưa xác định");
            btnBanUnban.setText("Không thể khóa: thiếu trạng thái");
            btnBanUnban.setEnabled(false);
        }

        String currentUid = "";
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        } catch (Exception ignored) {}
        
        if ("active".equalsIgnoreCase(user.getStatus()) || isBanned) {
            btnBanUnban.setEnabled(user.getUid() != null && !user.getUid().equals(currentUid));
        }

        btnBanUnban.setOnClickListener(v -> {
            if (isBanned) {
                showConfirmDialog(
                        "Mở khóa tài khoản",
                        "Mở khóa \"" + user.getDisplayName() + "\"?",
                        () -> viewModel.unban(user.getUid()));
            } else {
                showConfirmDialog(
                        "Khóa tài khoản",
                        "Khóa \"" + user.getDisplayName() + "\"?\nHọ sẽ không thể đăng nhập.",
                        () -> viewModel.ban(user.getUid()));
            }
        });

    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chỉnh sửa thông tin");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Name
        final EditText etName = new EditText(requireContext());
        etName.setHint("Họ tên");
        etName.setText(user.getDisplayName());
        layout.addView(etName);

        // Phone
        final EditText etPhone = new EditText(requireContext());
        etPhone.setHint("Số điện thoại");
        etPhone.setText(user.getPhone());
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        EditText etVehicle = new EditText(requireContext());
        etVehicle.setHint("Phương tiện giao hàng (nếu là tài xế)");
        etVehicle.setText(user.getVehicleDescription());
        layout.addView(etVehicle);

        // Role Spinner
        TextView tvRoleLabel = new TextView(requireContext());
        tvRoleLabel.setText("Vai trò:");
        tvRoleLabel.setPadding(0, 20, 0, 10);
        layout.addView(tvRoleLabel);

        final Spinner spinnerRole = new Spinner(requireContext());
        String[] roles = {"customer", "restaurant", "shipper", "admin"};
        String[] roleLabels = {"Khách hàng", "Nhà hàng", "Tài xế giao hàng", "Admin"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, roleLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Set current role
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(user.getRole())) {
                spinnerRole.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerRole);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newVehicle = etVehicle.getText().toString().trim();
            String newRole = roles[spinnerRole.getSelectedItemPosition()];

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            user.setDisplayName(newName);
            user.setPhone(newPhone);
            user.setVehicleDescription(newVehicle);
            user.setRole(newRole);
            viewModel.update(user);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (d, w) -> onConfirm.run())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0))
                + String.valueOf(parts[parts.length - 1].charAt(0)))
                .toUpperCase();
    }

    private String mapRole(String role) {
        if (role == null) return "—";
        switch (role) {
            case "customer":   return "Khách hàng";
            case "restaurant": return "Nhà hàng";
            case "shipper":    return "Shipper";
            case "admin":      return "Admin";
            default:           return role;
        }
    }

    private String formatDate(Object createdAt) {
        if (createdAt == null) return "—";
        try {
            if (createdAt instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) createdAt).toDate();
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(date);
            }
        } catch (Exception e) { /* ignore */ }
        return "—";
    }
}
