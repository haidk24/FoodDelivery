package com.example.foodorderapp.ui.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.feature.restaurant.registration.RestaurantRegistrationViewModel;
import com.example.foodorderapp.feature.auth.AuthViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class RegisterRestaurantFragment extends Fragment {

    private TextInputEditText edtName, edtPhone, edtStreet, edtDistrict, edtCity;
    private CheckBox cbLicence;
    private Button btnRegister;
    private ProgressBar progressBar;
    private LinearLayout layoutForm, layoutPending;
    private TextView tvTitle;

    private FirebaseAuth auth;
    private RestaurantRegistrationViewModel registrationViewModel;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_restaurant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        registrationViewModel = new ViewModelProvider(this).get(RestaurantRegistrationViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews(view);
        observeViewModel();
        checkRegistrationStatus();
        setupListeners();
    }

    private void initViews(View v) {
        tvTitle = v.findViewById(R.id.tv_title);
        edtName = v.findViewById(R.id.edt_restaurant_name);
        edtPhone = v.findViewById(R.id.edt_restaurant_phone);
        edtStreet = v.findViewById(R.id.edt_street);
        edtDistrict = v.findViewById(R.id.edt_district);
        edtCity = v.findViewById(R.id.edt_city);
        cbLicence = v.findViewById(R.id.cb_licence);
        btnRegister = v.findViewById(R.id.btn_register);
        progressBar = v.findViewById(R.id.progress_bar);
        layoutForm = v.findViewById(R.id.layout_form);
        layoutPending = v.findViewById(R.id.layout_pending);
    }

    private void checkRegistrationStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        registrationViewModel.loadExisting(user.getUid());
    }

    private void observeViewModel() {
        registrationViewModel.getLoading().observe(getViewLifecycleOwner(), this::setLoading);
        registrationViewModel.getExistingRestaurant().observe(getViewLifecycleOwner(), restaurant -> {
            if (restaurant != null && "active".equals(restaurant.status)) {
                Toast.makeText(getContext(), "Bạn đã có nhà hàng đang hoạt động!", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) getActivity().onBackPressed();
            } else if (restaurant != null && "pending".equals(restaurant.status)) {
                showPendingUI();
            } else {
                showFormUI();
                loadUserPhone();
            }
        });
        registrationViewModel.getSubmitted().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                showPendingUI();
                Toast.makeText(getContext(), "Gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show();
            }
        });
        registrationViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserPhone() {
        authViewModel.getCurrentUserProfile().addOnSuccessListener(profile -> {
            if (profile != null && !profile.getPhone().isEmpty()) edtPhone.setText(profile.getPhone());
        });
    }

    private void showPendingUI() {
        layoutForm.setVisibility(View.GONE);
        layoutPending.setVisibility(View.VISIBLE);
        tvTitle.setText("Trạng thái đăng ký");
    }

    private void showFormUI() {
        layoutForm.setVisibility(View.VISIBLE);
        layoutPending.setVisibility(View.GONE);
        tvTitle.setText("Đăng ký mở nhà hàng");
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                submitRegistration();
            }
        });
    }

    private boolean validateForm() {
        if (edtName.getText().toString().trim().isEmpty()) {
            edtName.setError("Vui lòng nhập tên nhà hàng");
            return false;
        }
        if (edtPhone.getText().toString().trim().isEmpty()) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            return false;
        }
        if (edtStreet.getText().toString().trim().isEmpty() ||
            edtDistrict.getText().toString().trim().isEmpty() ||
            edtCity.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ địa chỉ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void submitRegistration() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> address = new HashMap<>();
        address.put("street", edtStreet.getText().toString().trim());
        address.put("district", edtDistrict.getText().toString().trim());
        address.put("city", edtCity.getText().toString().trim());

        registrationViewModel.submit(user.getUid(), edtName.getText().toString().trim(),
                edtPhone.getText().toString().trim(), address, cbLicence.isChecked());
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            layoutForm.setVisibility(View.GONE);
            layoutPending.setVisibility(View.GONE);
        }
    }
}
