package com.example.foodorderapp.ui.admin;

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
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.ui.admin.adapter.UserAdapter;
import com.example.foodorderapp.viewModel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserListFragment extends Fragment implements UserAdapter.Listener {

    private static final String FILTER_ALL        = "all";
    private static final String FILTER_CUSTOMER   = "customer";
    private static final String FILTER_RESTAURANT = "restaurant";
    private static final String FILTER_BANNED     = "banned";

    private AdminViewModel viewModel;
    private UserAdapter adapter;

    // Metrics
    private TextView tvTotalUsers, tvActiveUsers, tvBannedUsers;
    private View btnAddUser;

    // Search & Filter
    private EditText edtSearchUser;
    private View btnFilterDialog;

    // Chips
    private LinearLayout chipFilterAll, chipFilterCustomer, chipFilterRestaurant, chipFilterBanned;
    private TextView tvFilterAllTitle, tvBadgeAll;
    private TextView tvFilterCustomerTitle, tvBadgeCustomer;
    private ImageView imgCheckCustomer;
    private TextView tvFilterRestaurantTitle, tvBadgeRestaurant;
    private TextView tvFilterBannedTitle, tvBadgeBanned;

    // List & Empty state
    private RecyclerView rvUsers;
    private View layoutEmptyUsers;
    private ProgressBar progressUserList;

    // Footer
    private View btnLoadMoreUsers;
    private TextView tvLoadMoreLabel, tvRecordsSummary;
    private ProgressBar progressLoadMore;

    private String currentFilter = FILTER_CUSTOMER;
    private String currentSearchQuery = "";
    private boolean isExpanded = false;
    private final List<User> allLoadedUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        bindViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        setupActions();
        observeViewModel();

        // Load data
        viewModel.loadFirstPageUsers("all");
    }

    private void bindViews(View v) {
        tvTotalUsers = v.findViewById(R.id.tv_total_users);
        tvActiveUsers = v.findViewById(R.id.tv_active_users);
        tvBannedUsers = v.findViewById(R.id.tv_banned_users);
        btnAddUser = v.findViewById(R.id.btn_add_user);

        edtSearchUser = v.findViewById(R.id.edt_search_user);
        btnFilterDialog = v.findViewById(R.id.btn_filter_dialog);

        chipFilterAll = v.findViewById(R.id.chip_filter_all);
        tvFilterAllTitle = v.findViewById(R.id.tv_filter_all_title);
        tvBadgeAll = v.findViewById(R.id.tv_badge_all);

        chipFilterCustomer = v.findViewById(R.id.chip_filter_customer);
        tvFilterCustomerTitle = v.findViewById(R.id.tv_filter_customer_title);
        tvBadgeCustomer = v.findViewById(R.id.tv_badge_customer);
        imgCheckCustomer = v.findViewById(R.id.img_check_customer);

        chipFilterRestaurant = v.findViewById(R.id.chip_filter_restaurant);
        tvFilterRestaurantTitle = v.findViewById(R.id.tv_filter_restaurant_title);
        tvBadgeRestaurant = v.findViewById(R.id.tv_badge_restaurant);

        chipFilterBanned = v.findViewById(R.id.chip_filter_banned);
        tvFilterBannedTitle = v.findViewById(R.id.tv_filter_banned_title);
        tvBadgeBanned = v.findViewById(R.id.tv_badge_banned);

        rvUsers = v.findViewById(R.id.rv_users);
        layoutEmptyUsers = v.findViewById(R.id.layout_empty_users);
        progressUserList = v.findViewById(R.id.progress_user_list);

        btnLoadMoreUsers = v.findViewById(R.id.btn_load_more_users);
        tvLoadMoreLabel = v.findViewById(R.id.tv_load_more_label);
        tvRecordsSummary = v.findViewById(R.id.tv_records_summary);
        progressLoadMore = v.findViewById(R.id.progress_load_more);
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(this);
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipFilterAll.setOnClickListener(v -> selectFilter(FILTER_ALL));
        chipFilterCustomer.setOnClickListener(v -> selectFilter(FILTER_CUSTOMER));
        chipFilterRestaurant.setOnClickListener(v -> selectFilter(FILTER_RESTAURANT));
        chipFilterBanned.setOnClickListener(v -> selectFilter(FILTER_BANNED));

        updateChipVisuals();
    }

    private void selectFilter(String filter) {
        currentFilter = filter;
        updateChipVisuals();
        applyFilterAndDisplay();
    }

    private void updateChipVisuals() {
        // Reset all to inactive
        setChipInactive(chipFilterAll, tvFilterAllTitle, tvBadgeAll);
        setChipInactive(chipFilterCustomer, tvFilterCustomerTitle, tvBadgeCustomer);
        setChipInactive(chipFilterRestaurant, tvFilterRestaurantTitle, tvBadgeRestaurant);
        setChipInactive(chipFilterBanned, tvFilterBannedTitle, tvBadgeBanned);
        imgCheckCustomer.setVisibility(View.GONE);

        // Highlight selected
        switch (currentFilter) {
            case FILTER_ALL:
                setChipActive(chipFilterAll, tvFilterAllTitle, tvBadgeAll);
                break;
            case FILTER_CUSTOMER:
                setChipActive(chipFilterCustomer, tvFilterCustomerTitle, tvBadgeCustomer);
                imgCheckCustomer.setVisibility(View.VISIBLE);
                break;
            case FILTER_RESTAURANT:
                setChipActive(chipFilterRestaurant, tvFilterRestaurantTitle, tvBadgeRestaurant);
                break;
            case FILTER_BANNED:
                setChipActive(chipFilterBanned, tvFilterBannedTitle, tvBadgeBanned);
                break;
        }
    }

    private void setChipActive(LinearLayout chip, TextView title, TextView badge) {
        chip.setBackgroundResource(R.drawable.bg_btn_filter_user_active);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setTextColor(0xFFFFFFFF);
        badge.setBackgroundResource(R.drawable.bg_badge_pill_gray);
        badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7F1D1D));
    }

    private void setChipInactive(LinearLayout chip, TextView title, TextView badge) {
        chip.setBackgroundResource(R.drawable.bg_btn_filter_user_inactive);
        title.setTextColor(0xFF475569);
        title.setTypeface(null, android.graphics.Typeface.NORMAL);
        badge.setTextColor(0xFF475569);
        badge.setBackgroundResource(R.drawable.bg_badge_pill_gray);
        badge.setBackgroundTintList(null);
    }

    private void setupSearch() {
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilterAndDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupActions() {
        btnAddUser.setOnClickListener(v -> showAddUserDialog());

        if (btnFilterDialog != null) btnFilterDialog.setVisibility(View.GONE);

        btnLoadMoreUsers.setOnClickListener(v -> {
            isExpanded = true;
            applyFilterAndDisplay();
            Toast.makeText(requireContext(), "Đã hiển thị toàn bộ người dùng", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressUserList != null) {
                progressUserList.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getUserList().observe(getViewLifecycleOwner(), users -> {
            allLoadedUsers.clear();
            if (users != null) {
                allLoadedUsers.addAll(users);
            }
            updateMetricsAndBadges();
            applyFilterAndDisplay();
        });

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(requireContext(), "Thao tác thành công!", Toast.LENGTH_SHORT).show();
                viewModel.loadFirstPageUsers("all");
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMetricsAndBadges() {
        int total = allLoadedUsers.size();
        int active = 0;
        int banned = 0;
        int customers = 0;
        int restaurants = 0;

        for (User u : allLoadedUsers) {
            if ("banned".equalsIgnoreCase(u.getStatus())) banned++;
            else if ("active".equalsIgnoreCase(u.getStatus())) active++;

            if ("restaurant".equalsIgnoreCase(u.getRole())) restaurants++;
            else if ("customer".equalsIgnoreCase(u.getRole())) customers++;
        }

        tvTotalUsers.setText(String.valueOf(total));
        tvActiveUsers.setText(String.valueOf(active));
        tvBannedUsers.setText(String.valueOf(banned));
        tvBadgeAll.setText(String.valueOf(total));
        tvBadgeCustomer.setText(String.valueOf(customers));
        tvBadgeRestaurant.setText(String.valueOf(restaurants));
        tvBadgeBanned.setText(String.valueOf(banned));
    }

    private void applyFilterAndDisplay() {
        List<User> filtered = new ArrayList<>();

        for (User u : allLoadedUsers) {
            boolean matchesFilter = true;
            if (FILTER_RESTAURANT.equals(currentFilter)) {
                matchesFilter = "restaurant".equalsIgnoreCase(u.getRole());
            } else if (FILTER_BANNED.equals(currentFilter)) {
                matchesFilter = "banned".equalsIgnoreCase(u.getStatus());
            }
            if (FILTER_CUSTOMER.equals(currentFilter)) {
                matchesFilter = "customer".equalsIgnoreCase(u.getRole());
            }

            if (!matchesFilter) continue;

            if (!currentSearchQuery.isEmpty()) {
                String name = u.getDisplayName() != null ? u.getDisplayName().toLowerCase(Locale.getDefault()) : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase(Locale.getDefault()) : "";
                String phone = u.getPhone() != null ? u.getPhone().toLowerCase(Locale.getDefault()) : "";
                String address = u.getAddress() != null ? u.getAddress().toLowerCase(Locale.getDefault()) : "";
                String restaurant = u.getRestaurantName() != null ? u.getRestaurantName().toLowerCase(Locale.getDefault()) : "";

                if (!name.contains(currentSearchQuery) && !email.contains(currentSearchQuery)
                        && !phone.contains(currentSearchQuery) && !address.contains(currentSearchQuery)
                        && !restaurant.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filtered.add(u);
        }

        List<User> toDisplay;
        if (!isExpanded && currentSearchQuery.isEmpty()) {
            int limit = Math.min(filtered.size(), 4);
            toDisplay = new ArrayList<>(filtered.subList(0, limit));
            btnLoadMoreUsers.setVisibility(filtered.size() > 4 ? View.VISIBLE : View.GONE);
            tvLoadMoreLabel.setText("Xem thêm " + Math.max(0, filtered.size() - limit) + " người dùng khác");
            tvRecordsSummary.setText("Hiển thị " + limit + " trên tổng số " + filtered.size() + " bản ghi");
        } else {
            toDisplay = filtered;
            btnLoadMoreUsers.setVisibility(View.GONE);
            tvRecordsSummary.setText("Hiển thị " + filtered.size() + " trên tổng số " + filtered.size() + " bản ghi");
        }

        adapter.submitList(toDisplay);

        boolean isEmpty = toDisplay.isEmpty();
        layoutEmptyUsers.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAddUserDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null);
        EditText edtName = dialogView.findViewById(R.id.edt_new_user_name);
        EditText edtEmail = dialogView.findViewById(R.id.edt_new_user_email);
        EditText edtPhone = dialogView.findViewById(R.id.edt_new_user_phone);
        EditText edtPassword = dialogView.findViewById(R.id.edt_new_user_password);

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm tài khoản người dùng")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String email = edtEmail.getText().toString().trim();
                    String phone = edtPhone.getText().toString().trim();
                    String password = edtPassword.getText().toString();
                    if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
                        Toast.makeText(requireContext(), "Nhập tên, email hợp lệ và mật khẩu ít nhất 6 ký tự.", Toast.LENGTH_LONG).show();
                    } else {
                        createUserAccount(name, email, phone, password);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createUserAccount(String name, String email, String phone, String password) {
        com.google.firebase.FirebaseApp secondaryApp = null;
        try {
            String appName = "admin-user-creation-" + System.currentTimeMillis();
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(
                    requireContext(), com.google.firebase.FirebaseApp.getInstance().getOptions(), appName);
            com.google.firebase.FirebaseApp finalSecondaryApp = secondaryApp;
            com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp)
                    .createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        if (result.getUser() == null) {
                            finalSecondaryApp.delete();
                            Toast.makeText(requireContext(), "Không tạo được tài khoản xác thực.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String uid = result.getUser().getUid();
                        User newUser = new User();
                        newUser.setUid(uid);
                        newUser.setDisplayName(name);
                        newUser.setEmail(email);
                        newUser.setPhone(phone);
                        newUser.setRole("customer");
                        newUser.setStatus("active");
                        newUser.setCreatedAt(com.google.firebase.Timestamp.now());
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(unused -> {
                                    finalSecondaryApp.delete();
                                    if (isAdded()) Toast.makeText(requireContext(), "Đã tạo tài khoản người dùng.", Toast.LENGTH_SHORT).show();
                                    viewModel.loadFirstPageUsers("all");
                                })
                                .addOnFailureListener(error -> {
                                    result.getUser().delete().addOnCompleteListener(deleteTask -> {
                                        finalSecondaryApp.delete();
                                        if (isAdded()) Toast.makeText(requireContext(), "Không lưu được hồ sơ Firestore nên đã hủy tài khoản vừa tạo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                });
                    })
                    .addOnFailureListener(error -> {
                        finalSecondaryApp.delete();
                        if (isAdded()) Toast.makeText(requireContext(), "Không tạo được tài khoản: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception error) {
            if (secondaryApp != null) secondaryApp.delete();
            if (isAdded()) Toast.makeText(requireContext(), "Không khởi tạo được luồng tạo tài khoản: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── UserAdapter.Listener callbacks ───────────────────────────────

    @Override
    public void onUserClick(User user) {
        onViewDetail(user);
    }

    @Override
    public void onViewDetail(User user) {
        if (user.getUid() != null) {
            Bundle args = new Bundle();
            args.putString("uid", user.getUid());
            try {
                Navigation.findNavController(requireView()).navigate(R.id.action_users_to_detail, args);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Chi tiết người dùng: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onViewOrders(User user) {
        if (user.getUid() == null) return;
        Toast.makeText(requireContext(), "Đang tải đơn hàng...", Toast.LENGTH_SHORT).show();
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("customerId", user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    java.util.List<String> rows = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getString("orderId");
                        if (id == null) id = doc.getId();
                        String status = doc.getString("status");
                        Object rawAmount = doc.get("totalAmount");
                        String amountText = rawAmount instanceof Number ? ((Number) rawAmount).longValue() + " đ" : "—";
                        rows.add("#" + (id.length() > 8 ? id.substring(0, 8) : id)
                                + " • " + (status == null ? "Chưa cập nhật trạng thái" : status)
                                + " • " + amountText);
                    }
                    if (rows.isEmpty()) rows.add("Không tìm thấy đơn hàng của tài khoản này.");
                    new AlertDialog.Builder(requireContext()).setTitle("Đơn hàng của " + user.getDisplayName())
                            .setItems(rows.toArray(new String[0]), null).setPositiveButton("Đóng", null).show();
                })
                .addOnFailureListener(error -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Không tải được đơn hàng: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onRolePermission(User user) {
        final String[] roles = {"Khách hàng (customer)", "Chủ quán (restaurant)", "Tài xế giao hàng (shipper)", "Quản trị viên (admin)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Phân quyền cho " + user.getDisplayName())
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = "customer";
                    if (which == 1) selectedRole = "restaurant";
                    else if (which == 2) selectedRole = "shipper";
                    else if (which == 3) selectedRole = "admin";

                    user.setRole(selectedRole);
                    if (user.getUid() != null) {
                        viewModel.updateUser(user);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onUnlockUser(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Mở khóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn mở khóa tài khoản cho " + user.getDisplayName() + "?")
                .setPositiveButton("Mở khóa", (dialog, which) -> {
                    if (user.getUid() != null) {
                        viewModel.unbanUser(user.getUid());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onMoreMenu(User user, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Xem chi tiết");
        popup.getMenu().add(0, 2, 1, "Phân quyền tài khoản");

        if ("banned".equalsIgnoreCase(user.getStatus())) {
            popup.getMenu().add(0, 3, 2, "Mở khóa tài khoản");
        } else {
            popup.getMenu().add(0, 4, 2, "Khóa tài khoản");
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    onViewDetail(user);
                    return true;
                case 2:
                    onRolePermission(user);
                    return true;
                case 3:
                    onUnlockUser(user);
                    return true;
                case 4:
                    confirmBanUser(user);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmBanUser(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Khóa tài khoản")
                .setMessage("Khóa tài khoản của " + user.getDisplayName() + " do vi phạm?")
                .setPositiveButton("Khóa", (dialog, which) -> {
                    if (user.getUid() != null) {
                        viewModel.banUser(user.getUid());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
